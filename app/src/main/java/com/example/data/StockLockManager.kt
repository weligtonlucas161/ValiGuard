package com.example.data

import android.util.Log
import com.example.data.supabase.AppLog
import com.example.data.supabase.RemoteProductLock
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import com.example.util.CryptoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProductLockInfo(
    val produtoId: String,
    val usuarioNome: String,
    val usuarioMatricula: String,
    val timestamp: Long
)

/**
 * Gerenciador em Tempo Real de Bloqueio e Monitoramento de Produtos no Estoque.
 *
 * - Quando o usuário X abre um produto para edição (EditarProdutoDialog),
 *   o produto fica imediatamente bloqueado para edição simultânea.
 * - Para todos os outros usuários, o card exibe com destaque:
 *   "🔒 Em edição por [Nome] ([Matrícula])" e as ações de edição/exclusão ficam indisponíveis.
 * - Heartbeat a cada 8 segundos mantém o bloqueio ativo.
 * - Timeout automático de 30 segundos libera o produto caso o app feche inesperadamente.
 * - Suporta sincronização primária via tabela dedicada 'produto_bloqueios' no Supabase,
 *   com fallback transparente para 'app_logs'.
 */
object StockLockManager {
    private const val TAG = "StockLockManager"
    private const val LOCK_EXPIRATION_MS = 30_000L
    private const val HEARTBEAT_INTERVAL_MS = 8_000L

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _lockedProducts = MutableStateFlow<Map<String, ProductLockInfo>>(emptyMap())
    val lockedProducts: StateFlow<Map<String, ProductLockInfo>> = _lockedProducts.asStateFlow()

    private var pollingJob: Job? = null
    private val heartbeatJobs = mutableMapOf<String, Job>() // key = produtoId

    /**
     * Inicia a escuta em tempo real (polling ágil de 1.5s) dos status de bloqueio na loja.
     */
    fun startLockMonitoring(lojaId: String, scope: CoroutineScope? = null) {
        if (lojaId.isBlank()) return
        pollingJob?.cancel()
        val runningScope = scope ?: managerScope
        pollingJob = runningScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    var loadedFromTable = false
                    val map = mutableMapOf<String, ProductLockInfo>()

                    // 1. Tentar tabela dedicada 'produto_bloqueios' (máxima velocidade e confiabilidade)
                    try {
                        val remoteLocks = SupabaseClient.instance.from("produto_bloqueios")
                            .select {
                                eq("loja_id", lojaId)
                            }
                            .decodeList<RemoteProductLock>()

                        for (lock in remoteLocks) {
                            if (lock.produto_id.isNotBlank() && (now - lock.timestamp) < LOCK_EXPIRATION_MS) {
                                map[lock.produto_id] = ProductLockInfo(
                                    produtoId = lock.produto_id,
                                    usuarioNome = lock.usuario_nome,
                                    usuarioMatricula = lock.usuario_matricula,
                                    timestamp = lock.timestamp
                                )
                            }
                        }
                        loadedFromTable = true
                    } catch (e: Exception) {
                        // Tabela ainda não criada ou erro de conexão, faz fallback para app_logs
                        loadedFromTable = false
                    }

                    // 2. Fallback para 'app_logs' caso a tabela dedicada ainda não exista
                    if (!loadedFromTable) {
                        try {
                            val recentLogs = SupabaseClient.instance.from("app_logs")
                                .select {
                                    eq("loja_id", lojaId)
                                }
                                .decodeList<AppLog>()

                            val lockLogs = recentLogs
                                .filter { it.acao == "PRODUTO_LOCK" || it.acao == "PRODUTO_UNLOCK" }
                                .sortedBy { it.timestamp }

                            for (log in lockLogs) {
                                val detalhesDecrypted = CryptoUtils.decrypt(log.detalhes)
                                if (log.acao == "PRODUTO_LOCK" && detalhesDecrypted.startsWith("LOCK|")) {
                                    val parts = detalhesDecrypted.split("|").associate { part ->
                                        val idx = part.indexOf(":")
                                        if (idx > 0) part.substring(0, idx) to part.substring(idx + 1) else "" to ""
                                    }
                                    val pid = parts["PID"] ?: ""
                                    val user = parts["USER"] ?: log.usuario_nome
                                    val mat = parts["MAT"] ?: log.usuario_matricula
                                    val ts = parts["TS"]?.toLongOrNull() ?: log.timestamp

                                    if (pid.isNotBlank() && (now - ts) < LOCK_EXPIRATION_MS) {
                                        map[pid] = ProductLockInfo(
                                            produtoId = pid,
                                            usuarioNome = CryptoUtils.decrypt(user),
                                            usuarioMatricula = mat,
                                            timestamp = ts
                                        )
                                    }
                                } else if (log.acao == "PRODUTO_UNLOCK" && detalhesDecrypted.startsWith("UNLOCK|")) {
                                    val parts = detalhesDecrypted.split("|").associate { part ->
                                        val idx = part.indexOf(":")
                                        if (idx > 0) part.substring(0, idx) to part.substring(idx + 1) else "" to ""
                                    }
                                    val pid = parts["PID"] ?: ""
                                    if (pid.isNotBlank()) {
                                        map.remove(pid)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Fallback de logs falhou: ${e.message}")
                        }
                    }

                    // Preserva locks que foram iniciados localmente por este dispositivo se ainda não expiraram
                    val localCurrent = _lockedProducts.value
                    localCurrent.forEach { (pid, info) ->
                        if ((now - info.timestamp) < LOCK_EXPIRATION_MS && !map.containsKey(pid)) {
                            map[pid] = info
                        }
                    }

                    _lockedProducts.value = map
                } catch (e: Exception) {
                    Log.w(TAG, "Erro no monitoramento de locks: ${e.message}")
                }

                delay(1500) // Polling rápido para resposta ágil entre dispositivos
            }
        }
    }

    /**
     * Adquire o bloqueio de edição exclusiva de um produto e inicia o heartbeat periódico.
     */
    suspend fun lockProduct(produtoId: String, usuario: Usuario, lojaId: String, scope: CoroutineScope? = null) = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val lockInfo = ProductLockInfo(
                produtoId = produtoId,
                usuarioNome = usuario.nome,
                usuarioMatricula = usuario.matricula,
                timestamp = now
            )

            // Atualização otimista local imediata
            val map = _lockedProducts.value.toMutableMap()
            map[produtoId] = lockInfo
            _lockedProducts.value = map

            // 1. Tentar gravar na tabela dedicada 'produto_bloqueios'
            val remoteObj = RemoteProductLock(
                produto_id = produtoId,
                loja_id = lojaId,
                usuario_matricula = usuario.matricula,
                usuario_nome = usuario.nome,
                timestamp = now
            )
            try {
                SupabaseClient.instance.from("produto_bloqueios").upsert(remoteObj)
            } catch (e: Exception) {
                try {
                    SupabaseClient.instance.from("produto_bloqueios").delete { eq("produto_id", produtoId) }
                    SupabaseClient.instance.from("produto_bloqueios").insert(remoteObj)
                } catch (_: Exception) {}
            }

            // 2. Gravar no log de auditoria / fallback
            val lockPayload = "LOCK|PID:$produtoId|USER:${usuario.nome}|MAT:${usuario.matricula}|TS:$now"
            val log = AppLog(
                usuario_matricula = usuario.matricula,
                usuario_nome = CryptoUtils.encrypt(usuario.nome),
                loja_id = lojaId,
                acao = "PRODUTO_LOCK",
                detalhes = CryptoUtils.encrypt(lockPayload),
                timestamp = now
            )
            try {
                SupabaseClient.instance.from("app_logs").insert(log)
            } catch (_: Exception) {}

            // 3. Inicia heartbeat contínuo gerenciado no managerScope
            heartbeatJobs[produtoId]?.cancel()
            heartbeatJobs[produtoId] = managerScope.launch {
                while (isActive) {
                    delay(HEARTBEAT_INTERVAL_MS)
                    try {
                        val hbNow = System.currentTimeMillis()
                        // Atualiza timestamp local
                        val currentMap = _lockedProducts.value.toMutableMap()
                        if (currentMap.containsKey(produtoId)) {
                            currentMap[produtoId] = currentMap[produtoId]!!.copy(timestamp = hbNow)
                            _lockedProducts.value = currentMap
                        }

                        // Atualiza tabela dedicada
                        try {
                            SupabaseClient.instance.from("produto_bloqueios")
                                .update(mapOf("timestamp" to hbNow)) {
                                    eq("produto_id", produtoId)
                                }
                        } catch (_: Exception) {}

                        // Atualiza log
                        val hbPayload = "LOCK|PID:$produtoId|USER:${usuario.nome}|MAT:${usuario.matricula}|TS:$hbNow"
                        val hbLog = AppLog(
                            usuario_matricula = usuario.matricula,
                            usuario_nome = CryptoUtils.encrypt(usuario.nome),
                            loja_id = lojaId,
                            acao = "PRODUTO_LOCK",
                            detalhes = CryptoUtils.encrypt(hbPayload),
                            timestamp = hbNow
                        )
                        SupabaseClient.instance.from("app_logs").insert(hbLog)
                    } catch (e: Exception) {
                        Log.w(TAG, "Heartbeat lock falhou: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao bloquear produto: ${e.message}")
        }
    }

    /**
     * Libera o bloqueio exclusivo do produto.
     */
    suspend fun unlockProduct(produtoId: String, usuario: Usuario, lojaId: String) = withContext(Dispatchers.IO) {
        try {
            heartbeatJobs[produtoId]?.cancel()
            heartbeatJobs.remove(produtoId)

            val now = System.currentTimeMillis()

            // Remoção local imediata
            val map = _lockedProducts.value.toMutableMap()
            map.remove(produtoId)
            _lockedProducts.value = map

            // 1. Remover da tabela dedicada
            try {
                SupabaseClient.instance.from("produto_bloqueios").delete {
                    eq("produto_id", produtoId)
                }
            } catch (_: Exception) {}

            // 2. Gravar no log de auditoria
            val unlockPayload = "UNLOCK|PID:$produtoId|USER:${usuario.nome}|MAT:${usuario.matricula}|TS:$now"
            val log = AppLog(
                usuario_matricula = usuario.matricula,
                usuario_nome = CryptoUtils.encrypt(usuario.nome),
                loja_id = lojaId,
                acao = "PRODUTO_UNLOCK",
                detalhes = CryptoUtils.encrypt(unlockPayload),
                timestamp = now
            )
            try {
                SupabaseClient.instance.from("app_logs").insert(log)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desbloquear produto: ${e.message}")
        }
    }

    /**
     * Retorna a informação de bloqueio caso o produto esteja em edição por OUTRO usuário.
     * Se estiver livre ou em edição pelo próprio usuário autenticado, retorna null.
     */
    fun getLockByOther(produtoId: String, currentMatricula: String): ProductLockInfo? {
        val lock = _lockedProducts.value[produtoId] ?: return null
        val now = System.currentTimeMillis()
        if (now - lock.timestamp > LOCK_EXPIRATION_MS) return null
        return if (lock.usuarioMatricula != currentMatricula) lock else null
    }
}
