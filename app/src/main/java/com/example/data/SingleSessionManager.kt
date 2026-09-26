package com.example.data

import android.util.Log
import com.example.data.supabase.AppLog
import com.example.data.supabase.RemoteActiveSession
import com.example.data.supabase.SupabaseClient
import com.example.util.CryptoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface SessionValidationResult {
    object Allowed : SessionValidationResult
    data class Blocked(val usuarioNome: String, val lastSeenMs: Long) : SessionValidationResult
}

/**
 * Gerenciador de Login Único (Single Active Session).
 *
 * Impede que o mesmo usuário mantenha dois logins simultaneamente em aparelhos distintos.
 * Se o usuário já possui sessão ativa, o novo login é bloqueado.
 * Quando o aplicativo é fechado ou o logout é realizado, a sessão é encerrada automaticamente.
 * Possui tolerância de heartbeat (40s): se o processo for finalizado de forma abrupta,
 * a sessão expira automaticamente por inatividade.
 */
object SingleSessionManager {
    private const val TAG = "SingleSessionManager"
    const val SESSION_TIMEOUT_MS = 40_000L
    private const val HEARTBEAT_INTERVAL_MS = 12_000L

    val currentSessionToken: String = UUID.randomUUID().toString()

    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    private var activeMatricula: String? = null
    private var activeNome: String? = null
    private var activeLojaId: String? = null

    /**
     * Valida se o usuário informado pode realizar login.
     * Retorna [SessionValidationResult.Allowed] se livre ou [SessionValidationResult.Blocked] se já ativo em outro local.
     */
    suspend fun checkCanLogin(matricula: String): SessionValidationResult = withContext(Dispatchers.IO) {
        val cleanMatricula = matricula.trim()
        val now = System.currentTimeMillis()

        // 1. Tentar verificar na tabela primária 'sessoes_ativas'
        try {
            val sessions = SupabaseClient.instance.from("sessoes_ativas")
                .select {
                    eq("matricula", cleanMatricula)
                }
                .decodeList<RemoteActiveSession>()

            val existing = sessions.firstOrNull()
            if (existing != null) {
                val age = now - existing.timestamp
                val isAnotherDevice = existing.token != currentSessionToken
                if (age < SESSION_TIMEOUT_MS && isAnotherDevice) {
                    Log.d(TAG, "Login bloqueado: Sessão ativa na tabela 'sessoes_ativas' para $cleanMatricula")
                    return@withContext SessionValidationResult.Blocked(
                        usuarioNome = existing.usuario_nome,
                        lastSeenMs = existing.timestamp
                    )
                }
            }
            return@withContext SessionValidationResult.Allowed
        } catch (e: Exception) {
            Log.d(TAG, "Tabela sessoes_ativas indisponível (${e.message}), executando fallback para app_logs")
        }

        // 2. Fallback resiliente via 'app_logs'
        try {
            val logs = SupabaseClient.instance.from("app_logs")
                .select {
                    eq("usuario_matricula", cleanMatricula)
                    order("timestamp", ascending = false)
                    limit(10)
                }
                .decodeList<AppLog>()

            for (log in logs) {
                if (log.acao == "SESSION_TERMINATED" || log.acao == "LOGOUT") {
                    // Sessão foi encerrada formalmente
                    break
                }
                if (log.acao == "ACTIVE_SESSION") {
                    val rawDetails = CryptoUtils.decrypt(log.detalhes)
                    val parts = rawDetails.split("|")
                    val remoteToken = parts.getOrNull(0) ?: ""
                    val remoteTs = parts.getOrNull(1)?.toLongOrNull() ?: log.timestamp
                    val remoteNome = parts.getOrNull(2) ?: log.usuario_nome

                    val age = now - remoteTs
                    val isAnotherDevice = remoteToken != currentSessionToken
                    if (age < SESSION_TIMEOUT_MS && isAnotherDevice) {
                        Log.d(TAG, "Login bloqueado via fallback app_logs para $cleanMatricula")
                        return@withContext SessionValidationResult.Blocked(
                            usuarioNome = remoteNome,
                            lastSeenMs = remoteTs
                        )
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro no fallback de validação de sessão: ${e.message}")
        }

        SessionValidationResult.Allowed
    }

    /**
     * Registra a sessão ativa do usuário e inicia o heartbeat periódico em segundo plano.
     */
    fun startSession(matricula: String, nome: String, lojaId: String) {
        activeMatricula = matricula.trim()
        activeNome = nome.trim()
        activeLojaId = lojaId.trim()

        heartbeatJob?.cancel()
        heartbeatJob = sessionScope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    /**
     * Envia o pulso de vida da sessão para o Supabase.
     */
    private suspend fun sendHeartbeat() {
        val mat = activeMatricula ?: return
        val nome = activeNome ?: "Usuário"
        val loja = activeLojaId ?: ""
        val now = System.currentTimeMillis()

        val sessionPayload = RemoteActiveSession(
            matricula = mat,
            token = currentSessionToken,
            usuario_nome = nome,
            loja_id = loja,
            timestamp = now
        )

        var savedInTable = false
        try {
            SupabaseClient.instance.from("sessoes_ativas")
                .upsert(sessionPayload)
            savedInTable = true
        } catch (e: Exception) {
            // Ignora e faz fallback
        }

        if (!savedInTable) {
            try {
                val detailsEncrypted = CryptoUtils.encrypt("$currentSessionToken|$now|$nome")
                val logEntry = AppLog(
                    id = UUID.randomUUID().toString(),
                    usuario_matricula = mat,
                    usuario_nome = nome,
                    loja_id = loja,
                    acao = "ACTIVE_SESSION",
                    detalhes = detailsEncrypted,
                    timestamp = now
                )
                SupabaseClient.instance.from("app_logs").insert(logEntry)
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao gravar heartbeat em app_logs: ${e.message}")
            }
        }
    }

    /**
     * Encerra a sessão ativa imediatamente, liberando o login para outros dispositivos.
     */
    fun terminateSession() {
        val mat = activeMatricula
        val nome = activeNome
        val loja = activeLojaId

        heartbeatJob?.cancel()
        heartbeatJob = null
        activeMatricula = null
        activeNome = null
        activeLojaId = null

        if (mat.isNullOrBlank()) return

        sessionScope.launch {
            try {
                SupabaseClient.instance.from("sessoes_ativas")
                    .delete {
                        eq("matricula", mat)
                        eq("token", currentSessionToken)
                    }
            } catch (e: Exception) {
                Log.d(TAG, "Erro ao remover de sessoes_ativas: ${e.message}")
            }

            try {
                val logEntry = AppLog(
                    id = UUID.randomUUID().toString(),
                    usuario_matricula = mat,
                    usuario_nome = nome ?: "Usuário",
                    loja_id = loja ?: "",
                    acao = "SESSION_TERMINATED",
                    detalhes = CryptoUtils.encrypt("$currentSessionToken|${System.currentTimeMillis()}|TERMINATED"),
                    timestamp = System.currentTimeMillis()
                )
                SupabaseClient.instance.from("app_logs").insert(logEntry)
            } catch (e: Exception) {
                Log.d(TAG, "Erro ao gravar SESSION_TERMINATED: ${e.message}")
            }
        }
    }
}
