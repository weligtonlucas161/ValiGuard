package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.supabase.AppLog
import com.example.data.supabase.LogManager
import com.example.data.supabase.Produto
import com.example.data.supabase.RemotePresenceBip
import com.example.data.supabase.RemotePresenceSession
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import com.example.util.CryptoUtils
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class AuditItemScanned(
    val produtoId: String,
    val codigoBarras: String,
    val nome: String,
    val setor: String,
    val operadorMatricula: String,
    val operadorNome: String,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}

@JsonClass(generateAdapter = true)
data class PresenceAuditSession(
    val sessionId: String,
    val lojaId: String,
    val setor: String, // Setor da auditoria ("Frios", "Mercearia", etc. ou "Todos")
    val iniciadaPorNome: String,
    val iniciadaPorMatricula: String,
    val iniciadaEmMs: Long = System.currentTimeMillis(),
    val status: String = "ATIVA", // "ATIVA", "PAUSADA", "CONCLUIDA"
    val itensBipados: Map<String, AuditItemScanned> = emptyMap(), // key = produtoId
    val participantes: List<String> = emptyList(), // lista de nomes dos operadores
    val ultimoBip: AuditItemScanned? = null
)

sealed class BipResult {
    data class Success(val item: AuditItemScanned) : BipResult()
    data class Duplicate(val existingItem: AuditItemScanned) : BipResult()
    object NotFound : BipResult()
}

/**
 * Gerenciador em Tempo Real da Auditoria de Presença Compartilhada.
 * - Sincronização automática para TODOS os operadores e Masters da loja.
 * - Conexão automática à auditoria ativa assim que a tela é aberta.
 * - Prevenção rígida de bip duplo: se qualquer operador bipar um item,
 *   todos os outros aparelhos são atualizados instantaneamente (1.5s) e qualquer tentativa
 *   de bipar o mesmo item é bloqueada emitindo alerta sonoro e visual.
 * - Suporta tabelas dedicadas 'auditoria_presenca_sessoes' e 'auditoria_presenca_bips' no Supabase,
 *   com fallback transparente para 'app_logs'.
 */
object PresenceAuditSessionManager {

    private const val TAG = "PresenceAuditManager"
    private const val PREFS_NAME = "presence_audit_sessions_prefs"
    private const val KEY_ACTIVE_SESSION = "key_active_presence_session"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }
    private val sessionAdapter by lazy { moshi.adapter(PresenceAuditSession::class.java) }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentSession = MutableStateFlow<PresenceAuditSession?>(null)
    val currentSession: StateFlow<PresenceAuditSession?> = _currentSession.asStateFlow()

    private var pollingJob: Job? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedJson = prefs.getString(KEY_ACTIVE_SESSION, null)
        if (!savedJson.isNullOrBlank()) {
            try {
                val restored = sessionAdapter.fromJson(savedJson)
                if (restored != null && restored.status == "ATIVA") {
                    _currentSession.value = restored
                }
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao restaurar sessão salva: ${e.message}")
            }
        }
    }

    private fun persistSession(context: Context, session: PresenceAuditSession?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (session == null || session.status != "ATIVA") {
            prefs.edit().remove(KEY_ACTIVE_SESSION).apply()
        } else {
            try {
                val json = sessionAdapter.toJson(session)
                prefs.edit().putString(KEY_ACTIVE_SESSION, json).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar sessão no cache: ${e.message}")
            }
        }
    }

    /**
     * Inicia a sincronização contínua (polling a cada 1.5s) com o Supabase.
     * Deve ser chamada assim que qualquer tela de auditoria for aberta para que todos vejam
     * as sessões ativas e bips em tempo real automaticamente.
     */
    fun startRealtimeSync(
        context: Context,
        lojaId: String,
        scope: CoroutineScope? = null
    ) {
        if (lojaId.isBlank()) return
        pollingJob?.cancel()
        val runningScope = scope ?: managerScope
        pollingJob = runningScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    var syncedFromTables = false

                    // 1. Tentar tabelas dedicadas de alta performance no Supabase
                    try {
                        val remoteSessions = SupabaseClient.instance.from("auditoria_presenca_sessoes")
                            .select {
                                eq("loja_id", lojaId)
                                eq("status", "ATIVA")
                                order("timestamp", ascending = false)
                                limit(1)
                            }
                            .decodeList<RemotePresenceSession>()

                        if (remoteSessions.isNotEmpty()) {
                            val activeRemote = remoteSessions.first()

                            // Buscar todos os bips registrados nesta sessão
                            val remoteBips = SupabaseClient.instance.from("auditoria_presenca_bips")
                                .select {
                                    eq("sessao_id", activeRemote.id)
                                }
                                .decodeList<RemotePresenceBip>()

                            val bipsMap = remoteBips.associate { bip ->
                                bip.produto_id to AuditItemScanned(
                                    produtoId = bip.produto_id,
                                    codigoBarras = bip.codigo_barras,
                                    nome = bip.produto_nome,
                                    setor = bip.setor,
                                    operadorMatricula = bip.operador_matricula,
                                    operadorNome = bip.operador_nome,
                                    timestampMs = bip.timestamp
                                )
                            }.toMutableMap()

                            // Preserva bips locais que possam ainda estar sendo enviados
                            val currentLocal = _currentSession.value
                            if (currentLocal != null && currentLocal.sessionId == activeRemote.id) {
                                currentLocal.itensBipados.forEach { (pid, item) ->
                                    if (!bipsMap.containsKey(pid)) {
                                        bipsMap[pid] = item
                                    }
                                }
                            }

                            val allParticipants = (listOf(activeRemote.iniciada_por_nome) + remoteBips.map { it.operador_nome }).distinct()

                            val updatedSession = PresenceAuditSession(
                                sessionId = activeRemote.id,
                                lojaId = activeRemote.loja_id,
                                setor = activeRemote.setor,
                                iniciadaPorNome = activeRemote.iniciada_por_nome,
                                iniciadaPorMatricula = activeRemote.iniciada_por_matricula,
                                iniciadaEmMs = activeRemote.timestamp,
                                status = "ATIVA",
                                itensBipados = bipsMap,
                                participantes = allParticipants,
                                ultimoBip = bipsMap.values.maxByOrNull { it.timestampMs }
                            )

                            withContext(Dispatchers.Main) {
                                _currentSession.value = updatedSession
                                persistSession(context, updatedSession)
                            }
                            syncedFromTables = true
                        } else {
                            // Nenhuma sessão ativa no servidor
                            if (_currentSession.value != null && _currentSession.value?.lojaId == lojaId) {
                                withContext(Dispatchers.Main) {
                                    _currentSession.value = null
                                    persistSession(context, null)
                                }
                            }
                            syncedFromTables = true
                        }
                    } catch (e: Exception) {
                        // Tabelas dedicadas ainda não existem no Supabase do usuário, fazer fallback
                        syncedFromTables = false
                    }

                    // 2. Fallback para 'app_logs'
                    if (!syncedFromTables) {
                        try {
                            val logsRemotos = SupabaseClient.instance.from("app_logs")
                                .select {
                                    eq("loja_id", lojaId)
                                }
                                .decodeList<AppLog>()
                                .map { it.copy(detalhes = CryptoUtils.decrypt(it.detalhes)) }

                            val auditLogs = logsRemotos.filter { it.acao.startsWith("AUDITORIA_PRESENCA") }
                            val startLogs = auditLogs.filter { it.acao == "AUDITORIA_PRESENCA_START" }.sortedByDescending { it.timestamp }
                            val fimLogs = auditLogs.filter { it.acao == "AUDITORIA_PRESENCA_FIM" }.sortedByDescending { it.timestamp }

                            val latestStart = startLogs.firstOrNull()
                            val latestFim = fimLogs.firstOrNull()

                            val isSessionActive = latestStart != null && (latestFim == null || latestStart.timestamp > latestFim.timestamp)

                            if (isSessionActive && latestStart != null) {
                                val parts = latestStart.detalhes.split("|").associate { part ->
                                    val idx = part.indexOf(":")
                                    if (idx > 0) part.substring(0, idx) to part.substring(idx + 1) else "" to ""
                                }
                                val sid = parts["ID"] ?: "presenca_${lojaId}_${latestStart.timestamp}"
                                val setor = parts["SETOR"] ?: "Todos"
                                val iniciadaPor = parts["OP"] ?: latestStart.usuario_nome

                                val sessionBips = auditLogs
                                    .filter { it.acao == "AUDITORIA_PRESENCA_BIP" && it.timestamp >= latestStart.timestamp }
                                    .mapNotNull { log ->
                                        if (!log.detalhes.startsWith("BIP|")) return@mapNotNull null
                                        val bipParts = log.detalhes.split("|").associate { p ->
                                            val idx = p.indexOf(":")
                                            if (idx > 0) p.substring(0, idx) to p.substring(idx + 1) else "" to ""
                                        }
                                        val pid = bipParts["PID"] ?: ""
                                        if (pid.isBlank()) return@mapNotNull null
                                        val ean = bipParts["EAN"] ?: ""
                                        val nome = bipParts["NOME"] ?: "Produto"
                                        val bSetor = bipParts["SETOR"] ?: setor
                                        val opNome = bipParts["OP"] ?: log.usuario_nome
                                        val opMat = bipParts["MAT"] ?: log.usuario_matricula
                                        val timeMs = bipParts["TIME"]?.toLongOrNull() ?: log.timestamp
                                        AuditItemScanned(pid, ean, nome, bSetor, opMat, opNome, timeMs)
                                    }

                                val bipsMap = sessionBips.associateBy { it.produtoId }.toMutableMap()
                                val participants = (listOf(iniciadaPor) + sessionBips.map { it.operadorNome }).distinct()

                                val reconstructed = PresenceAuditSession(
                                    sessionId = sid,
                                    lojaId = lojaId,
                                    setor = setor,
                                    iniciadaPorNome = iniciadaPor,
                                    iniciadaPorMatricula = latestStart.usuario_matricula,
                                    iniciadaEmMs = latestStart.timestamp,
                                    status = "ATIVA",
                                    itensBipados = bipsMap,
                                    participantes = participants,
                                    ultimoBip = bipsMap.values.maxByOrNull { it.timestampMs }
                                )

                                withContext(Dispatchers.Main) {
                                    _currentSession.value = reconstructed
                                    persistSession(context, reconstructed)
                                }
                            } else {
                                if (_currentSession.value != null && _currentSession.value?.lojaId == lojaId) {
                                    withContext(Dispatchers.Main) {
                                        _currentSession.value = null
                                        persistSession(context, null)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Fallback via logs falhou: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Erro no ciclo de sincronização de auditoria: ${e.message}")
                }

                delay(1500L) // Polling rápido para sincronização contínua
            }
        }
    }

    /**
     * Inicia uma nova auditoria ou conecta à sessão já existente do mesmo setor/loja.
     */
    fun startOrJoinSession(
        context: Context,
        lojaId: String,
        setor: String,
        currentUser: Usuario,
        scope: CoroutineScope
    ): PresenceAuditSession {
        val existing = _currentSession.value

        val sessionToUse = if (existing != null && existing.status == "ATIVA" && existing.lojaId == lojaId) {
            val updatedParticipants = if (!existing.participantes.contains(currentUser.nome)) {
                existing.participantes + currentUser.nome
            } else {
                existing.participantes
            }
            existing.copy(participantes = updatedParticipants)
        } else {
            val dateTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanSetor = setor.trim().replace(" ", "_").lowercase(Locale.getDefault())
            val newSessionId = "presenca_${lojaId}_${cleanSetor}_$dateTag"
            PresenceAuditSession(
                sessionId = newSessionId,
                lojaId = lojaId,
                setor = setor,
                iniciadaPorNome = currentUser.nome,
                iniciadaPorMatricula = currentUser.matricula,
                iniciadaEmMs = System.currentTimeMillis(),
                status = "ATIVA",
                participantes = listOf(currentUser.nome)
            )
        }

        _currentSession.value = sessionToUse
        persistSession(context, sessionToUse)

        // Publicar na tabela dedicada do Supabase
        managerScope.launch {
            try {
                val remoteSession = RemotePresenceSession(
                    id = sessionToUse.sessionId,
                    loja_id = sessionToUse.lojaId,
                    setor = sessionToUse.setor,
                    iniciada_por_nome = sessionToUse.iniciadaPorNome,
                    iniciada_por_matricula = sessionToUse.iniciadaPorMatricula,
                    status = "ATIVA",
                    timestamp = sessionToUse.iniciadaEmMs
                )
                SupabaseClient.instance.from("auditoria_presenca_sessoes").upsert(remoteSession)
            } catch (_: Exception) {}

            // Registrar no log
            LogManager.recordLog(
                usuarioMatricula = currentUser.matricula,
                usuarioNome = currentUser.nome,
                lojaId = lojaId,
                acao = "AUDITORIA_PRESENCA_START",
                detalhes = "START|ID:${sessionToUse.sessionId}|SETOR:${sessionToUse.setor}|OP:${currentUser.nome}|HORA:${sessionToUse.iniciadaEmMs}"
            )
        }

        // Iniciar polling em tempo real
        startRealtimeSync(context, lojaId, scope)

        return sessionToUse
    }

    /**
     * Processa a bipagem de um produto com prevenção rigorosa de bip duplo.
     * Retorna Duplicate se alguém já bipou o produto nesta sessão (local ou remotamente),
     * ou Success adicionando à lista e sincronizando com todos os aparelhos.
     */
    suspend fun processBip(
        context: Context,
        produto: Produto,
        currentUser: Usuario
    ): BipResult = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: return@withContext BipResult.NotFound

        // 1. Verificação local imediata na memória
        val jaBipadoPorId = session.itensBipados[produto.id]
        val jaBipadoPorEan = session.itensBipados.values.find {
            it.codigoBarras.equals(produto.codigo_barras, ignoreCase = true) ||
            (!produto.plu.isNullOrBlank() && it.codigoBarras.equals(produto.plu, ignoreCase = true))
        }

        val duplicadoLocal = jaBipadoPorId ?: jaBipadoPorEan
        if (duplicadoLocal != null) {
            return@withContext BipResult.Duplicate(duplicadoLocal)
        }

        // 2. Verificação remota rápida no Supabase para evitar colisão entre dois operadores bipando ao mesmo tempo
        try {
            val remoteBipMatch = SupabaseClient.instance.from("auditoria_presenca_bips")
                .select {
                    eq("sessao_id", session.sessionId)
                    eq("produto_id", produto.id)
                }
                .decodeSingleOrNull<RemotePresenceBip>()

            if (remoteBipMatch != null) {
                val existing = AuditItemScanned(
                    produtoId = remoteBipMatch.produto_id,
                    codigoBarras = remoteBipMatch.codigo_barras,
                    nome = remoteBipMatch.produto_nome,
                    setor = remoteBipMatch.setor,
                    operadorMatricula = remoteBipMatch.operador_matricula,
                    operadorNome = remoteBipMatch.operador_nome,
                    timestampMs = remoteBipMatch.timestamp
                )
                // Atualiza localmente
                val updatedMap = session.itensBipados.toMutableMap().apply { put(produto.id, existing) }
                _currentSession.value = session.copy(itensBipados = updatedMap)
                persistSession(context, _currentSession.value)
                return@withContext BipResult.Duplicate(existing)
            }
        } catch (_: Exception) {}

        // 3. Criar registro do item bipado
        val item = AuditItemScanned(
            produtoId = produto.id,
            codigoBarras = produto.codigo_barras,
            nome = produto.nome,
            setor = produto.setor,
            operadorMatricula = currentUser.matricula,
            operadorNome = currentUser.nome,
            timestampMs = System.currentTimeMillis()
        )

        val updatedMap = session.itensBipados.toMutableMap().apply {
            put(produto.id, item)
        }

        val updatedParticipants = if (!session.participantes.contains(currentUser.nome)) {
            session.participantes + currentUser.nome
        } else {
            session.participantes
        }

        val updatedSession = session.copy(
            itensBipados = updatedMap,
            participantes = updatedParticipants,
            ultimoBip = item
        )

        withContext(Dispatchers.Main) {
            _currentSession.value = updatedSession
            persistSession(context, updatedSession)
        }

        // 4. Gravar imediatamente no Supabase (tabela dedicada e log)
        try {
            val remoteBip = RemotePresenceBip(
                id = "${session.sessionId}_${produto.id}",
                sessao_id = session.sessionId,
                loja_id = session.lojaId,
                produto_id = produto.id,
                codigo_barras = produto.codigo_barras,
                produto_nome = produto.nome,
                setor = produto.setor,
                operador_matricula = currentUser.matricula,
                operador_nome = currentUser.nome,
                timestamp = item.timestampMs
            )
            SupabaseClient.instance.from("auditoria_presenca_bips").upsert(remoteBip)
        } catch (_: Exception) {}

        LogManager.recordLog(
            usuarioMatricula = currentUser.matricula,
            usuarioNome = currentUser.nome,
            lojaId = session.lojaId,
            acao = "AUDITORIA_PRESENCA_BIP",
            detalhes = "BIP|ID:${session.sessionId}|PID:${produto.id}|EAN:${produto.codigo_barras}|NOME:${produto.nome}|SETOR:${produto.setor}|OP:${currentUser.nome}|MAT:${currentUser.matricula}|TIME:${item.timestampMs}"
        )

        return@withContext BipResult.Success(item)
    }

    /**
     * Remove um produto da lista de confirmados (desfaz a bipagem).
     */
    suspend fun undoBip(context: Context, produtoId: String) = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: return@withContext
        val updatedMap = session.itensBipados.toMutableMap().apply {
            remove(produtoId)
        }
        val updatedSession = session.copy(itensBipados = updatedMap)
        withContext(Dispatchers.Main) {
            _currentSession.value = updatedSession
            persistSession(context, updatedSession)
        }

        try {
            SupabaseClient.instance.from("auditoria_presenca_bips").delete {
                eq("sessao_id", session.sessionId)
                eq("produto_id", produtoId)
            }
        } catch (_: Exception) {}
    }

    /**
     * Finaliza a sessão ativa da auditoria para todos os usuários.
     */
    suspend fun completeSession(context: Context, currentUser: Usuario) = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: return@withContext

        // Atualiza status remoto
        try {
            SupabaseClient.instance.from("auditoria_presenca_sessoes").update(mapOf("status" to "CONCLUIDA")) {
                eq("id", session.sessionId)
            }
        } catch (_: Exception) {}

        LogManager.recordLog(
            usuarioMatricula = currentUser.matricula,
            usuarioNome = currentUser.nome,
            lojaId = session.lojaId,
            acao = "AUDITORIA_PRESENCA_FIM",
            detalhes = "CONCLUIDA|ID:${session.sessionId}|SETOR:${session.setor}|TOTAL:${session.itensBipados.size}|OP:${currentUser.nome}"
        )

        withContext(Dispatchers.Main) {
            _currentSession.value = null
            persistSession(context, null)
        }
        stopPolling()
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
