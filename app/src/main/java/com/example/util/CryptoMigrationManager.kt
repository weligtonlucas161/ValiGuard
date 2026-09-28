package com.example.util

import android.util.Log
import com.example.data.supabase.Loja
import com.example.data.supabase.Produto
import com.example.data.supabase.RemoteFeedback
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MigrationReport(
    val produtosMigrados: Int = 0,
    val usuariosMigrados: Int = 0,
    val lojasMigradas: Int = 0,
    val feedbacksMigrados: Int = 0,
    val sucesso: Boolean = true,
    val mensagem: String = ""
)

/**
 * Utilitário de Migração Criptográfica:
 * Varre o banco de dados Supabase em busca de registros legados (inseridos antes da criptografia)
 * e os regrava com a camada de criptografia AES-256 (prefixo "ENC:"),
 * garantindo que o usuário não precise reinserir informações manualmente.
 */
object CryptoMigrationManager {
    private const val TAG = "CryptoMigrationManager"

    suspend fun migrarTodosDadosNaoCriptografados(lojaId: String? = null): MigrationReport = withContext(Dispatchers.IO) {
        var countProd = 0
        var countUser = 0
        var countLojas = 0
        var countFeedbacks = 0

        val supabase = SupabaseClient.instance

        try {
            // 1. Migração de Produtos
            val listaProdutos = if (lojaId.isNullOrBlank()) {
                supabase.from("produtos").select().decodeList<Produto>()
            } else {
                supabase.from("produtos").select { eq("loja_id", lojaId) }.decodeList<Produto>()
            }

            for (p in listaProdutos) {
                val precisaCriptografar = !p.nome.startsWith(CryptoUtils.PREFIX) ||
                        !p.codigo_barras.startsWith(CryptoUtils.PREFIX) ||
                        !p.setor.startsWith(CryptoUtils.PREFIX) ||
                        (p.plu != null && p.plu.isNotBlank() && !p.plu.startsWith(CryptoUtils.PREFIX))

                if (precisaCriptografar) {
                    val pEncrypted = p.encrypted()
                    try {
                        supabase.from("produtos").update(pEncrypted) {
                            eq("id", p.id)
                        }
                        countProd++
                    } catch (e: Exception) {
                        Log.w(TAG, "Falha ao migrar produto ${p.id}: ${e.message}")
                    }
                }
            }

            // 2. Migração de Usuários
            try {
                val usuarios = supabase.from("usuarios").select().decodeList<Usuario>()
                for (u in usuarios) {
                    val precisaCriptografar = !u.nome.startsWith(CryptoUtils.PREFIX) ||
                            (u.setor != null && u.setor.isNotBlank() && !u.setor.startsWith(CryptoUtils.PREFIX))

                    if (precisaCriptografar) {
                        val encNome = CryptoUtils.encrypt(u.nome)
                        val encSetor = u.setor?.let { CryptoUtils.encrypt(it) }
                        try {
                            supabase.from("usuarios").update(
                                mapOf(
                                    "nome" to encNome,
                                    "setor" to encSetor
                                )
                            ) {
                                eq("matricula", u.matricula)
                            }
                            countUser++
                        } catch (e: Exception) {
                            Log.w(TAG, "Falha ao migrar usuário ${u.matricula}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Aviso na migração de usuários: ${e.message}")
            }

            // 3. Migração de Lojas
            try {
                val lojas = supabase.from("lojas").select().decodeList<Loja>()
                for (l in lojas) {
                    if (!l.nome_loja.startsWith(CryptoUtils.PREFIX)) {
                        val encNome = CryptoUtils.encrypt(l.nome_loja)
                        try {
                            supabase.from("lojas").update(mapOf("nome_loja" to encNome)) {
                                eq("id", l.id)
                            }
                            countLojas++
                        } catch (e: Exception) {
                            Log.w(TAG, "Falha ao migrar loja ${l.id}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Aviso na migração de lojas: ${e.message}")
            }

            // 4. Migração de Feedbacks
            try {
                val feedbacks = supabase.from("feedbacks").select().decodeList<RemoteFeedback>()
                for (f in feedbacks) {
                    if (!f.mensagem.startsWith(CryptoUtils.PREFIX)) {
                        val encMsg = CryptoUtils.encrypt(f.mensagem)
                        try {
                            supabase.from("feedbacks").update(mapOf("mensagem" to encMsg)) {
                                eq("id", f.id)
                            }
                            countFeedbacks++
                        } catch (e: Exception) {
                            Log.w(TAG, "Falha ao migrar feedback ${f.id}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Aviso na migração de feedbacks: ${e.message}")
            }

            Log.i(TAG, "Migração concluída com sucesso: $countProd produtos, $countUser usuários, $countLojas lojas, $countFeedbacks feedbacks.")
            MigrationReport(
                produtosMigrados = countProd,
                usuariosMigrados = countUser,
                lojasMigradas = countLojas,
                feedbacksMigrados = countFeedbacks,
                sucesso = true,
                mensagem = "Camada criptográfica aplicada com sucesso!"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro crítico na migração: ${e.message}", e)
            MigrationReport(
                sucesso = false,
                mensagem = e.message ?: "Erro durante migração"
            )
        }
    }
}
