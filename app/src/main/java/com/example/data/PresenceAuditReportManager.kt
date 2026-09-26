package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@JsonClass(generateAdapter = true)
data class ProdutoAusenteResumo(
    val id: String,
    val nome: String,
    val codigoBarras: String,
    val setor: String,
    val quantidadeEstoque: Int = 0,
    val quantidadeEsperada: Int = quantidadeEstoque
)

@JsonClass(generateAdapter = true)
data class RelatorioAuditoriaPresenca(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val lojaId: String,
    val setor: String,
    val iniciadaPorNome: String,
    val iniciadaPorMatricula: String,
    val finalizadaPorNome: String,
    val finalizadaPorMatricula: String,
    val dataInicioMs: Long,
    val dataFimMs: Long = System.currentTimeMillis(),
    val totalEsperado: Int,
    val totalBipados: Int,
    val totalNaoLocalizados: Int,
    val taxaPresenca: Int, // 0 - 100 %
    val totalQtdEstoqueBipado: Int = 0,
    val totalQtdEstoqueFaltante: Int = 0,
    val itensConfirmados: List<AuditItemScanned> = emptyList(),
    val produtosNaoBipados: List<ProdutoAusenteResumo> = emptyList() // Prioridade: exibidos primeiro
) {
    fun getDataFormatada(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        return sdf.format(Date(dataFimMs))
    }

    fun getDuracaoFormatada(): String {
        val diffSec = ((dataFimMs - dataInicioMs) / 1000).coerceAtLeast(0)
        val min = diffSec / 60
        val sec = diffSec % 60
        return "${min}m ${sec}s"
    }
}

@JsonClass(generateAdapter = true)
data class ItemInventarioResumo(
    val produtoId: String,
    val produtoNome: String,
    val codigoBarras: String,
    val setor: String,
    val quantidadeEstoque: Int,
    val quantidadeContada: Int,
    val divergencia: Int // quantidadeContada - quantidadeEstoque
)

@JsonClass(generateAdapter = true)
data class RelatorioAuditoriaInventario(
    val id: String = UUID.randomUUID().toString(),
    val lojaId: String,
    val setor: String,
    val responsavelNome: String,
    val responsavelMatricula: String,
    val dataHoraMs: Long = System.currentTimeMillis(),
    val totalItensAuditados: Int,
    val totalDivergencias: Int,
    val totalEstoqueSistema: Int,
    val totalContagemFisica: Int,
    val itens: List<ItemInventarioResumo> = emptyList()
) {
    fun getDataFormatada(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        return sdf.format(Date(dataHoraMs))
    }
}

@JsonClass(generateAdapter = true)
data class ItemValidadeAreaResumo(
    val id: String,
    val produtoNome: String,
    val codigoBarras: String,
    val plu: String?,
    val setor: String,
    val secao: String,
    val validadeAnterior: String,
    val validadeNova: String,
    val quantidadeAreaVendas: Int,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class RelatorioAuditoriaValidade(
    val id: String = UUID.randomUUID().toString(),
    val lojaId: String,
    val setor: String,
    val secao: String,
    val responsavelNome: String,
    val responsavelMatricula: String,
    val dataInicioMs: Long,
    val dataFimMs: Long = System.currentTimeMillis(),
    val totalItensAuditados: Int,
    val totalPecasAreaVendas: Int,
    val itens: List<ItemValidadeAreaResumo> = emptyList()
) {
    fun getDataFormatada(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        return sdf.format(Date(dataFimMs))
    }

    fun getDuracaoFormatada(): String {
        val diffSec = ((dataFimMs - dataInicioMs) / 1000).coerceAtLeast(0)
        val min = diffSec / 60
        val sec = diffSec % 60
        return "${min}m ${sec}s"
    }
}

/**
 * Gerenciador de armazenamento e consulta dos relatórios oficiais de auditoria.
 * Suporta Auditoria de Presença, Inventário e Validade com acesso restrito a usuários Master.
 */
object PresenceAuditReportManager {
    private const val TAG = "PresenceReportManager"
    private const val PREFS_NAME = "presence_audit_reports_prefs"
    private const val KEY_REPORTS_LIST = "key_presence_reports_json"
    private const val KEY_INVENTORY_REPORTS = "key_inventory_reports_json"
    private const val KEY_VALIDITY_REPORTS = "key_validity_reports_json"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val listTypePresence = Types.newParameterizedType(List::class.java, RelatorioAuditoriaPresenca::class.java)
    private val adapterPresence by lazy { moshi.adapter<List<RelatorioAuditoriaPresenca>>(listTypePresence) }

    private val listTypeInventory = Types.newParameterizedType(List::class.java, RelatorioAuditoriaInventario::class.java)
    private val adapterInventory by lazy { moshi.adapter<List<RelatorioAuditoriaInventario>>(listTypeInventory) }

    private val listTypeValidity = Types.newParameterizedType(List::class.java, RelatorioAuditoriaValidade::class.java)
    private val adapterValidity by lazy { moshi.adapter<List<RelatorioAuditoriaValidade>>(listTypeValidity) }

    private val _relatorios = MutableStateFlow<List<RelatorioAuditoriaPresenca>>(emptyList())
    val relatorios: StateFlow<List<RelatorioAuditoriaPresenca>> = _relatorios.asStateFlow()

    private val _relatoriosInventario = MutableStateFlow<List<RelatorioAuditoriaInventario>>(emptyList())
    val relatoriosInventario: StateFlow<List<RelatorioAuditoriaInventario>> = _relatoriosInventario.asStateFlow()

    private val _relatoriosValidade = MutableStateFlow<List<RelatorioAuditoriaValidade>>(emptyList())
    val relatoriosValidade: StateFlow<List<RelatorioAuditoriaValidade>> = _relatoriosValidade.asStateFlow()

    fun init(context: Context, lojaId: String? = null) {
        carregarRelatorios(context, lojaId)
        carregarRelatoriosInventario(context, lojaId)
        carregarRelatoriosValidade(context, lojaId)
    }

    // --- PRESENÇA ---
    fun carregarRelatorios(context: Context, lojaId: String? = null): List<RelatorioAuditoriaPresenca> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_REPORTS_LIST, null)
        val list = if (!json.isNullOrBlank()) {
            try {
                adapterPresence.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao decodificar relatórios salvos: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        val filtered = if (!lojaId.isNullOrBlank() && lojaId != "default") {
            list.filter { it.lojaId == lojaId || it.lojaId == "default" || it.lojaId.isBlank() }
        } else {
            list
        }.sortedByDescending { it.dataFimMs }

        _relatorios.value = filtered
        return filtered
    }

    fun salvarRelatorio(context: Context, relatorio: RelatorioAuditoriaPresenca) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val atuais = carregarRelatorios(context, null).toMutableList()
        
        atuais.removeAll { it.id == relatorio.id || it.sessionId == relatorio.sessionId }
        atuais.add(0, relatorio)

        try {
            val json = adapterPresence.toJson(atuais)
            prefs.edit().putString(KEY_REPORTS_LIST, json).apply()
            _relatorios.value = atuais.sortedByDescending { it.dataFimMs }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao serializar relatório de presença: ${e.message}")
        }
    }

    // --- INVENTÁRIO ---
    fun carregarRelatoriosInventario(context: Context, lojaId: String? = null): List<RelatorioAuditoriaInventario> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_INVENTORY_REPORTS, null)
        val list = if (!json.isNullOrBlank()) {
            try {
                adapterInventory.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao decodificar relatórios de inventário: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        val filtered = if (!lojaId.isNullOrBlank() && lojaId != "default") {
            list.filter { it.lojaId == lojaId || it.lojaId == "default" || it.lojaId.isBlank() }
        } else {
            list
        }.sortedByDescending { it.dataHoraMs }

        _relatoriosInventario.value = filtered
        return filtered
    }

    fun salvarRelatorioInventario(context: Context, relatorio: RelatorioAuditoriaInventario) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val atuais = carregarRelatoriosInventario(context, null).toMutableList()
        
        atuais.removeAll { it.id == relatorio.id }
        atuais.add(0, relatorio)

        try {
            val json = adapterInventory.toJson(atuais)
            prefs.edit().putString(KEY_INVENTORY_REPORTS, json).apply()
            _relatoriosInventario.value = atuais.sortedByDescending { it.dataHoraMs }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao serializar relatório de inventário: ${e.message}")
        }
    }

    // --- VALIDADE ---
    fun carregarRelatoriosValidade(context: Context, lojaId: String? = null): List<RelatorioAuditoriaValidade> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_VALIDITY_REPORTS, null)
        val list = if (!json.isNullOrBlank()) {
            try {
                adapterValidity.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao decodificar relatórios de validade: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        val filtered = if (!lojaId.isNullOrBlank() && lojaId != "default") {
            list.filter { it.lojaId == lojaId || it.lojaId == "default" || it.lojaId.isBlank() }
        } else {
            list
        }.sortedByDescending { it.dataFimMs }

        _relatoriosValidade.value = filtered
        return filtered
    }

    fun salvarRelatorioValidade(context: Context, relatorio: RelatorioAuditoriaValidade) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val atuais = carregarRelatoriosValidade(context, null).toMutableList()
        
        atuais.removeAll { it.id == relatorio.id }
        atuais.add(0, relatorio)

        try {
            val json = adapterValidity.toJson(atuais)
            prefs.edit().putString(KEY_VALIDITY_REPORTS, json).apply()
            _relatoriosValidade.value = atuais.sortedByDescending { it.dataFimMs }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao serializar relatório de validade: ${e.message}")
        }
    }
}
