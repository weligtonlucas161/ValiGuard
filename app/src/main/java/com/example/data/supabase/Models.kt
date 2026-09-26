package com.example.data.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class Loja(
    val id: String = UUID.randomUUID().toString(),
    val nome_loja: String,
    val cor_borda: String,
    val ativa: Boolean = true
)

@JsonClass(generateAdapter = true)
data class NovaLoja(
    val nome_loja: String,
    val cor_borda: String,
    val ativa: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Usuario(
    val matricula: String,
    val nome: String,
    val cargo: String, // "adm", "master", "operador" ou cargo customizado
    val loja_id: String,
    val ativo: Boolean = true,
    val ultimo_ping: String? = null,
    @Json(ignore = true) @Transient val setor: String? = null // Setor local resolvido ou null (não existe na tabela remota do Supabase)
)

@JsonClass(generateAdapter = true)
data class NovoUsuario(
    val matricula: String,
    val nome: String,
    val cargo: String,
    val loja_id: String,
    val ativo: Boolean = true,
    val ultimo_ping: String? = null,
    @Json(ignore = true) @Transient val setor: String? = null
)

@JsonClass(generateAdapter = true)
data class Produto(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val codigo_barras: String,
    val quantidade: Int,
    val data_vencimento: String,
    val setor: String,
    val loja_id: String,
    val plu: String? = null
)

@JsonClass(generateAdapter = true)
data class NovoProduto(
    val nome: String,
    val codigo_barras: String,
    val quantidade: Int,
    val data_vencimento: String,
    val setor: String,
    val loja_id: String,
    val plu: String? = null
)

@JsonClass(generateAdapter = true)
data class AppLog(
    val id: String = UUID.randomUUID().toString(),
    val usuario_matricula: String,
    val usuario_nome: String,
    val loja_id: String,
    val acao: String, // "LOGIN", "CADASTRO_PRODUTO", "EXCLUSAO_PRODUTO", "AUDITORIA_PRESENCA", "AUDITORIA_INVENTARIO", "AUDITORIA_VALIDADE", "REBAIXA_SOLICITADA", "STATUS_USUARIO"
    val detalhes: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AtualizarUsuario(
    val nome: String,
    val cargo: String,
    val ativo: Boolean,
    @Json(ignore = true) @Transient val setor: String? = null
)

@JsonClass(generateAdapter = true)
data class AtualizarPing(
    val ultimo_ping: String
)

@JsonClass(generateAdapter = true)
data class AtualizarProdutoQtd(
    val quantidade: Int
)

@JsonClass(generateAdapter = true)
data class AtualizarProdutoValidade(
    val data_vencimento: String
)

@JsonClass(generateAdapter = true)
data class AtualizarProdutoSetor(
    val setor: String
)

@JsonClass(generateAdapter = true)
data class AtualizarLojaStatus(
    val ativa: Boolean
)

@JsonClass(generateAdapter = true)
data class AtualizarLoja(
    val nome_loja: String,
    val cor_borda: String,
    val ativa: Boolean
)

@JsonClass(generateAdapter = true)
data class AtualizarUsuarioCompleto(
    val nome: String,
    val cargo: String,
    val loja_id: String,
    val ativo: Boolean,
    @Json(ignore = true) @Transient val setor: String? = null
)

@JsonClass(generateAdapter = true)
data class Cargo(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val setor: String? = null, // Setor ao qual o cargo é restrito (ou null/vazio se livre)
    val loja_id: String? = null,
    val descricao: String = ""
)

/**
 * Modelo de Feedback 100% alinhado com a tabela 'feedbacks' do banco Supabase:
 * CREATE TABLE public.feedbacks (
 *     id UUID PRIMARY KEY,
 *     matricula VARCHAR(8) NOT NULL,
 *     loja_id UUID,
 *     mensagem TEXT NOT NULL,
 *     avaliacao INTEGER CHECK (avaliacao BETWEEN 1 AND 5),
 *     created_at TIMESTAMPTZ DEFAULT NOW()
 * );
 */
@JsonClass(generateAdapter = true)
data class RemoteFeedback(
    val id: String = UUID.randomUUID().toString(),
    val matricula: String,
    val loja_id: String? = null,
    val mensagem: String,
    val avaliacao: Int = 5,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class UserFeedback(
    val id: String = UUID.randomUUID().toString(),
    val usuario_matricula: String,
    val usuario_nome: String,
    val usuario_cargo: String,
    val loja_id: String,
    val loja_nome: String = "",
    val estrelas: Int = 5,
    val categoria: String = "Sugestão", // "Elogio", "Sugestão", "Problema", "Dúvida"
    val mensagem: String,
    val status: String = "PENDENTE", // "PENDENTE", "LIDO", "RESOLVIDO"
    val resposta_adm: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val destino: String = "ADM", // "ADM" (melhorias no App) ou "MASTER" (melhorias na Empresa/Loja)
    val tipo_sugestao: String = "APP" // "APP" ou "EMPRESA"
)

@JsonClass(generateAdapter = true)
data class RemoteProductLock(
    val produto_id: String,
    val loja_id: String,
    val usuario_matricula: String,
    val usuario_nome: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class RemotePresenceSession(
    val id: String,
    val loja_id: String,
    val setor: String = "Todos",
    val iniciada_por_nome: String,
    val iniciada_por_matricula: String,
    val status: String = "ATIVA", // "ATIVA", "CONCLUIDA"
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class RemotePresenceBip(
    val id: String, // sessao_id || '_' || produto_id
    val sessao_id: String,
    val loja_id: String,
    val produto_id: String,
    val codigo_barras: String,
    val produto_nome: String,
    val setor: String,
    val operador_matricula: String,
    val operador_nome: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class RemoteActiveSession(
    val matricula: String,
    val token: String,
    val usuario_nome: String,
    val loja_id: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

