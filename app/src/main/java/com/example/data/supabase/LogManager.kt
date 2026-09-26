package com.example.data.supabase

import android.util.Log
import com.example.util.CryptoUtils
import com.example.util.decrypted
import com.example.util.encrypted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gerenciador centralizado de logs e trilha de auditoria dos usuários do app.
 * Mantém persistência local em memória e sincronização assíncrona com a tabela 'app_logs' do Supabase.
 */
object LogManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _logs = MutableStateFlow<List<AppLog>>(emptyList())
    val logs = _logs.asStateFlow()

    init {
        // Inicializar alguns logs padrão de demonstração para os usuários matriz
        val now = System.currentTimeMillis()
        val initialLogs = listOf(
            AppLog(
                usuario_matricula = "123456",
                usuario_nome = "Gerente de Validade",
                loja_id = "default",
                acao = "LOGIN",
                detalhes = "Acesso ao painel administrativo Master",
                timestamp = now - (1000 * 60 * 45) // 45 min atrás
            ),
            AppLog(
                usuario_matricula = "123456",
                usuario_nome = "Gerente de Validade",
                loja_id = "default",
                acao = "AUDITORIA_INVENTARIO",
                detalhes = "Conferência geral de estoque e ajuste de divergências",
                timestamp = now - (1000 * 60 * 20)
            ),
            AppLog(
                usuario_matricula = "654321",
                usuario_nome = "Operador de Perecíveis",
                loja_id = "default",
                acao = "LOGIN",
                detalhes = "Início de turno e acesso ao controle de perecíveis",
                timestamp = now - (1000 * 60 * 60 * 2) // 2h atrás
            ),
            AppLog(
                usuario_matricula = "654321",
                usuario_nome = "Operador de Perecíveis",
                loja_id = "default",
                acao = "AUDITORIA_PRESENCA",
                detalhes = "Conferiu e confirmou presença de lote de Iogurte em área de vendas",
                timestamp = now - (1000 * 60 * 35)
            )
        )
        _logs.value = initialLogs
    }

    /**
     * Registra uma ação realizada por qualquer usuário.
     */
    fun recordLog(
        usuarioMatricula: String,
        usuarioNome: String,
        lojaId: String,
        acao: String,
        detalhes: String
    ) {
        val newLog = AppLog(
            usuario_matricula = usuarioMatricula,
            usuario_nome = usuarioNome,
            loja_id = lojaId,
            acao = acao,
            detalhes = detalhes,
            timestamp = System.currentTimeMillis()
        )

        // 1. Atualizar memória instantaneamente para UI fluida
        _logs.value = listOf(newLog) + _logs.value

        // 2. Sincronizar assincronamente com Supabase (com criptografia ponta-a-ponta)
        scope.launch {
            try {
                val remoteLog = newLog.encrypted()
                SupabaseClient.instance.from("app_logs").insert(remoteLog)
            } catch (e: Exception) {
                Log.d("LogManager", "Tabela 'app_logs' do Supabase não configurada ou erro: ${e.message}")
            }
        }
    }

    /**
     * Retorna os logs de um usuário específico ordenados pelo mais recente.
     */
    suspend fun getLogsForUser(matricula: String): List<AppLog> {
        val memoryList = _logs.value.filter { it.usuario_matricula == matricula }
        return try {
            val remote = SupabaseClient.instance.from("app_logs")
                .select { eq("usuario_matricula", matricula) }
                .decodeList<AppLog>()
                .map { it.decrypted() }
            // Combinar e desduplicar por ID
            val combined = (memoryList + remote).distinctBy { it.id }
            combined.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            memoryList.sortedByDescending { it.timestamp }
        }
    }
}
