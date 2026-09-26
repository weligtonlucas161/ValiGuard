package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.supabase.SessionHolder
import org.json.JSONObject

enum class StatusRebaixa {
    NENHUMA,
    PENDENTE,
    CONFIRMADA,
    NAO_APLICADA
}

data class RebaixaInfo(
    val status: StatusRebaixa,
    val dataSolicitada: Long = 0L,
    val dataConfirmada: Long = 0L,
    val retiradoDeArea: Boolean = false,
    val dataRetirada: Long = 0L
)

object RebaixaManager {

    private const val PREFS_NAME = "rebaixa_prefs"
    private const val DOIS_DIAS_MILLIS = 2 * 24 * 60 * 60 * 1000L // 48 horas

    private fun getPrefs(context: Context): SharedPreferences {
        val lojaId = SessionHolder.currentUser?.loja_id ?: "default"
        return context.getSharedPreferences("${PREFS_NAME}_$lojaId", Context.MODE_PRIVATE)
    }

    fun getInfo(context: Context, produtoId: String): RebaixaInfo {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(produtoId, null) ?: return RebaixaInfo(StatusRebaixa.NENHUMA)
        return try {
            val obj = JSONObject(jsonStr)
            val statusStr = obj.optString("status", StatusRebaixa.NENHUMA.name)
            val status = try { StatusRebaixa.valueOf(statusStr) } catch (e: Exception) { StatusRebaixa.NENHUMA }
            RebaixaInfo(
                status = status,
                dataSolicitada = obj.optLong("dataSolicitada", 0L),
                dataConfirmada = obj.optLong("dataConfirmada", 0L),
                retiradoDeArea = obj.optBoolean("retiradoDeArea", false),
                dataRetirada = obj.optLong("dataRetirada", 0L)
            )
        } catch (e: Exception) {
            RebaixaInfo(StatusRebaixa.NENHUMA)
        }
    }

    private fun salvarInfo(context: Context, produtoId: String, info: RebaixaInfo) {
        val prefs = getPrefs(context)
        try {
            val obj = JSONObject().apply {
                put("status", info.status.name)
                put("dataSolicitada", info.dataSolicitada)
                put("dataConfirmada", info.dataConfirmada)
                put("retiradoDeArea", info.retiradoDeArea)
                put("dataRetirada", info.dataRetirada)
            }
            prefs.edit().putString(produtoId, obj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun solicitarRebaixa(context: Context, produtoId: String) {
        val current = getInfo(context, produtoId)
        val updated = current.copy(
            status = StatusRebaixa.PENDENTE,
            dataSolicitada = System.currentTimeMillis()
        )
        salvarInfo(context, produtoId, updated)
    }

    fun confirmarRebaixa(context: Context, produtoId: String, confirmou: Boolean) {
        val current = getInfo(context, produtoId)
        val updated = current.copy(
            status = if (confirmou) StatusRebaixa.CONFIRMADA else StatusRebaixa.NAO_APLICADA,
            dataConfirmada = System.currentTimeMillis()
        )
        salvarInfo(context, produtoId, updated)
    }

    fun setRetiradoDeArea(context: Context, produtoId: String, retirado: Boolean) {
        val current = getInfo(context, produtoId)
        val updated = current.copy(
            retiradoDeArea = retirado,
            dataRetirada = if (retirado) System.currentTimeMillis() else 0L
        )
        salvarInfo(context, produtoId, updated)
    }

    /**
     * "todo dia alerta para olhar se houve rebaixa":
     * Alerta ativo se a rebaixa foi solicitada e está pendente de confirmação.
     */
    fun deveAlertarRebaixaHoje(context: Context, produtoId: String): Boolean {
        val info = getInfo(context, produtoId)
        return info.status == StatusRebaixa.PENDENTE
    }

    /**
     * "e a cada 2 dias pede para fazer uma nova rebaixa":
     * Alerta ativo se já houve rebaixa confirmada e passaram-se 2 ou mais dias.
     */
    fun devePedirNovaRebaixa2Dias(context: Context, produtoId: String): Boolean {
        val info = getInfo(context, produtoId)
        if (info.status == StatusRebaixa.CONFIRMADA && info.dataConfirmada > 0L) {
            val diferenca = System.currentTimeMillis() - info.dataConfirmada
            return diferenca >= DOIS_DIAS_MILLIS
        }
        return false
    }

    /**
     * "quando falta 1 dia para vencer alerta para retirar de área"
     */
    fun precisaAlertaRetirarArea1Dia(daysRemaining: Long, retiradoDeArea: Boolean): Boolean {
        return daysRemaining in 0..1 && !retiradoDeArea
    }

    /**
     * "quando venceu e não foi retirado de área o produto exibe em prioridade e piscando em roxo pulsante."
     */
    fun isCriticoVencidoNaoRetirado(daysRemaining: Long, retiradoDeArea: Boolean): Boolean {
        return daysRemaining < 0 && !retiradoDeArea
    }
}
