package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.supabase.LogManager
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.UserFeedback
import com.example.util.CryptoUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gerenciador de Feedbacks enviados pelos usuários (Operadores e Masters)
 * para a interface de Administração (ADM).
 */
object FeedbackManager {
    private const val PREFS_NAME = "super_app_feedbacks_prefs"
    private const val KEY_FEEDBACKS_JSON = "feedbacks_list_json"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val listType = Types.newParameterizedType(List::class.java, UserFeedback::class.java)
    private val adapter by lazy { moshi.adapter<List<UserFeedback>>(listType) }

    /**
     * Salva ou envia um feedback.
     */
    fun enviarFeedback(
        context: Context,
        feedback: UserFeedback,
        onComplete: (Boolean) -> Unit
    ) {
        // 1. Salva localmente com garantia imediata
        val atuais = getFeedbacks(context).toMutableList()
        atuais.removeAll { it.id == feedback.id }
        atuais.add(0, feedback) // mais recente primeiro
        salvarLocal(context, atuais)

        // 2. Registra em LogManager para auditoria centralizada
        LogManager.recordLog(
            usuarioMatricula = feedback.usuario_matricula,
            usuarioNome = feedback.usuario_nome,
            lojaId = feedback.loja_id,
            acao = "USER_FEEDBACK",
            detalhes = "[Para ${feedback.destino} - ${feedback.categoria} - ${feedback.estrelas}★] ${feedback.mensagem.take(60)}"
        )

        // 3. Tenta salvar na tabela 'feedbacks' do Supabase assincronamente (100% alinhada com o SQL da tabela)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val remote = com.example.data.supabase.RemoteFeedback(
                    id = feedback.id,
                    matricula = feedback.usuario_matricula,
                    loja_id = if (feedback.loja_id.isNotBlank()) feedback.loja_id else null,
                    mensagem = CryptoUtils.encrypt("[DESTINO:${feedback.destino}][TIPO:${feedback.tipo_sugestao}][${feedback.categoria}] ${feedback.mensagem}"),
                    avaliacao = feedback.estrelas
                )
                SupabaseClient.instance.from("feedbacks").insert(remote)
            } catch (e: Exception) {
                Log.w("FeedbackManager", "Erro ao inserir na tabela 'feedbacks' do Supabase: ${e.message}")
            }
            withContext(Dispatchers.Main) {
                onComplete(true)
            }
        }
    }

    /**
     * Retorna feedbacks destinados ao Gestor Master da loja (melhorias na empresa).
     */
    fun getFeedbacksParaMaster(context: Context, lojaId: String?): List<UserFeedback> {
        return getFeedbacks(context).filter {
            it.destino.equals("MASTER", ignoreCase = true) &&
                    (lojaId.isNullOrBlank() || it.loja_id.isBlank() || it.loja_id == lojaId)
        }
    }

    /**
     * Retorna feedbacks destinados ao Administrador Geral (melhorias no app).
     */
    fun getFeedbacksParaAdm(context: Context): List<UserFeedback> {
        return getFeedbacks(context).filter {
            it.destino.equals("ADM", ignoreCase = true)
        }
    }

    /**
     * Recupera todos os feedbacks salvos localmente.
     */
    fun getFeedbacks(context: Context): List<UserFeedback> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FEEDBACKS_JSON, null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Erro ao carregar feedbacks locais", e)
            emptyList()
        }
    }

    /**
     * Sincroniza feedbacks remotos do Supabase com os locais (para o ADM ver tudo atualizado).
     */
    suspend fun carregarFeedbacksComSupabase(context: Context): List<UserFeedback> = withContext(Dispatchers.IO) {
        val locais = getFeedbacks(context).toMutableList()
        try {
            val remotos = SupabaseClient.instance.from("feedbacks")
                .select()
                .decodeList<com.example.data.supabase.RemoteFeedback>()

            val mapa = LinkedHashMap<String, UserFeedback>()
            // Converte os feedbacks do banco para UserFeedback enriquecendo com os dados locais
            remotos.forEach { rem ->
                val localMatch = locais.firstOrNull { it.id == rem.id }
                var rawMsg = CryptoUtils.decrypt(rem.mensagem)
                var parsedDestino = localMatch?.destino ?: "ADM"
                var parsedTipo = localMatch?.tipo_sugestao ?: "APP"

                if (rawMsg.contains("[DESTINO:")) {
                    parsedDestino = rawMsg.substringAfter("[DESTINO:").substringBefore("]")
                    rawMsg = rawMsg.replace("[DESTINO:$parsedDestino]", "")
                }
                if (rawMsg.contains("[TIPO:")) {
                    parsedTipo = rawMsg.substringAfter("[TIPO:").substringBefore("]")
                    rawMsg = rawMsg.replace("[TIPO:$parsedTipo]", "")
                }

                val parsedCategoria = if (rawMsg.startsWith("[")) {
                    val cat = rawMsg.substringAfter("[").substringBefore("]")
                    rawMsg = rawMsg.substringAfter("] ").trim()
                    cat
                } else localMatch?.categoria ?: "Sugestão"

                mapa[rem.id] = UserFeedback(
                    id = rem.id,
                    usuario_matricula = rem.matricula,
                    usuario_nome = localMatch?.usuario_nome ?: "Matrícula ${rem.matricula}",
                    usuario_cargo = localMatch?.usuario_cargo ?: "Colaborador",
                    loja_id = rem.loja_id ?: localMatch?.loja_id ?: "",
                    loja_nome = localMatch?.loja_nome ?: "",
                    estrelas = rem.avaliacao,
                    categoria = parsedCategoria,
                    mensagem = rawMsg.trim(),
                    status = localMatch?.status ?: "PENDENTE",
                    resposta_adm = localMatch?.resposta_adm,
                    timestamp = localMatch?.timestamp ?: System.currentTimeMillis(),
                    destino = parsedDestino,
                    tipo_sugestao = parsedTipo
                )
            }

            // depois locais para não perder os ainda pendentes
            locais.forEach {
                if (!mapa.containsKey(it.id)) {
                    mapa[it.id] = it
                }
            }
            val mesclados = mapa.values.sortedByDescending { it.timestamp }
            salvarLocal(context, mesclados)
            mesclados
        } catch (e: Exception) {
            Log.w("FeedbackManager", "Usando cache local de feedbacks: ${e.message}")
            locais.sortedByDescending { it.timestamp }
        }
    }

    /**
     * Atualiza o status do feedback (ex: LIDO, RESOLVIDO) e resposta opcional do ADM.
     */
    fun atualizarStatusFeedback(
        context: Context,
        feedbackId: String,
        novoStatus: String,
        respostaAdm: String? = null
    ) {
        val atuais = getFeedbacks(context).map {
            if (it.id == feedbackId) {
                it.copy(
                    status = novoStatus,
                    resposta_adm = respostaAdm ?: it.resposta_adm
                )
            } else {
                it
            }
        }
        salvarLocal(context, atuais)

        // Atualiza no Supabase se possível
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fb = atuais.find { it.id == feedbackId }
                if (fb != null) {
                    SupabaseClient.instance.from("feedbacks")
                        .update(fb) {
                            eq("id", feedbackId)
                        }
                }
            } catch (e: Exception) {
                Log.w("FeedbackManager", "Erro ao atualizar feedback remoto: ${e.message}")
            }
        }
    }

    /**
     * Exclui um feedback.
     */
    fun excluirFeedback(context: Context, feedbackId: String) {
        val atuais = getFeedbacks(context).filterNot { it.id == feedbackId }
        salvarLocal(context, atuais)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseClient.instance.from("feedbacks")
                    .delete {
                        eq("id", feedbackId)
                    }
            } catch (e: Exception) {
                Log.w("FeedbackManager", "Erro ao excluir feedback remoto: ${e.message}")
            }
        }
    }

    private fun salvarLocal(context: Context, lista: List<UserFeedback>) {
        try {
            val json = adapter.toJson(lista)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FEEDBACKS_JSON, json).apply()
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Erro ao salvar feedbacks localmente", e)
        }
    }
}
