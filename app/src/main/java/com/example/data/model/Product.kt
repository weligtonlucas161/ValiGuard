package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExpiryStatus(val label: String, val badgeColorHex: Long) {
    EXPIRED("Vencido", 0xFFEF4444),
    MUST_REMOVE_1_DAY("Retirar da Área de Venda (≤1 dia)", 0xFFDC2626),
    MARKDOWN_REBAIXA("Solicitar Rebaixa (≤15 dias)", 0xFFF97316),
    WARNING("Atenção (16-30 dias)", 0xFFEAB308),
    SAFE("No Prazo (>30 dias)", 0xFF3B82F6)
}

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // Setor (Laticínios, Carnes, etc.)
    val section: String = "", // Seção da gôndola/área de venda (ex: Queijos, Iogurtes, Frios)
    val barcode: String = "", // Código de barras lido
    val internalCode: String = "", // Código interno / SKU / PLU do supermercado
    val quantity: Int,
    val unit: String = "un", // un, kg, cx, pct, g
    val expiryDate: Long, // Epoch timestamp em ms
    val batchCode: String = "", // Lote
    val location: String = "", // Gôndola / Geladeira
    val regularPrice: Double = 0.0,
    val markdownPrice: Double = 0.0,
    val markdownRequested: Boolean = false,
    val markdownDiscountPercent: Int = 30, // Padrão 30%
    val markdownRequestedAt: Long? = null, // Data/hora em que a rebaixa foi solicitada
    val markdownStatus: String = "NOT_REQUESTED", // NOT_REQUESTED, PENDING_APPROVAL, ACCEPTED, REJECTED
    val markdownCheckedAt: Long? = null, // Data/hora da verificação se foi aceita ou recusada
    val markdownRequestCount: Int = 0, // Contador de vezes que a rebaixa foi solicitada
    val isRemovedFromSales: Boolean = false, // Retirado da gôndola 1 dia antes
    val removalConfirmedAt: Long? = null, // Data/hora da retirada da área de venda
    val supabaseId: Long? = null, // ID gerado pela tabela 'produtos' do Supabase
    val notes: String = "",
    val lojaId: String = "", // Vínculo multi-tenant obrigatório por Loja
    val lastAuditedDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class CardAlertState {
        REMOVED,                  // Já recolhido da área de venda (cinza)
        EXPIRED,                  // Vencido (vermelho)
        BLINKING_PURPLE_1_DAY,    // Faltando 1 dia para vencer (piscando em roxo)
        BLINKING_YELLOW_MARKDOWN, // Rebaixa solicitada ou <= 15 dias até aprovação (piscando em amarelo)
        SOLID_GREEN_ACCEPTED,     // Rebaixa aprovada/aceita pela gerência (verde sólido, não pisca)
        WARNING_ATTENTION,        // 16-30 dias (atenção âmbar)
        NORMAL_SAFE               // > 30 dias (seguro)
    }

    fun getCardAlertState(referenceTime: Long = System.currentTimeMillis()): CardAlertState {
        if (isRemovedFromSales) return CardAlertState.REMOVED
        val days = getDaysRemaining(referenceTime)
        if (days < 0) return CardAlertState.EXPIRED
        // Faltando 1 dia ou menos -> pisca em roxo
        if (days <= 1) return CardAlertState.BLINKING_PURPLE_1_DAY
        // Rebaixa aceita -> verde sólido (não pisca pois já entrou em rebaixa)
        if (markdownStatus == "ACCEPTED") return CardAlertState.SOLID_GREEN_ACCEPTED
        // Faltando 15 dias para vencer OU rebaixa solicitada aguardando aceite -> pisca em amarelo
        if (days <= 15 || (markdownRequested && markdownStatus == "PENDING_APPROVAL")) {
            return CardAlertState.BLINKING_YELLOW_MARKDOWN
        }
        if (days <= 30) return CardAlertState.WARNING_ATTENTION
        return CardAlertState.NORMAL_SAFE
    }

    fun getFormattedCreatedAt(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }
    fun getDaysRemaining(referenceTime: Long = System.currentTimeMillis()): Long {
        val calendarRef = Calendar.getInstance().apply {
            timeInMillis = referenceTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calendarExp = Calendar.getInstance().apply {
            timeInMillis = expiryDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = calendarExp.timeInMillis - calendarRef.timeInMillis
        return diffMillis / (24 * 60 * 60 * 1000)
    }

    /**
     * Dias decorridos desde a última solicitação de rebaixa
     */
    fun getDaysSinceMarkdownRequest(referenceTime: Long = System.currentTimeMillis()): Long {
        val reqTime = markdownRequestedAt ?: return 0L
        val diff = referenceTime - reqTime
        return (diff / (24 * 60 * 60 * 1000)).coerceAtLeast(0L)
    }

    /**
     * Lembrete Diário: Todo dia fica lembrando para verificar se a rebaixa foi aceita ou não.
     * Ativo para produtos com rebaixa solicitada e que estão pendentes de resposta da gerência.
     */
    fun isDueForDailyAcceptanceCheck(): Boolean {
        if (isRemovedFromSales) return false
        val days = getDaysRemaining()
        if (days <= 1) return false
        return markdownRequested && markdownStatus == "PENDING_APPROVAL"
    }

    /**
     * Lembrete a cada 3 dias para solicitar uma nova rebaixa:
     * - Se a rebaixa foi recusada/não aceita e já se passaram 3 dias, OU
     * - Se a rebaixa anterior foi solicitada há 3 ou mais dias sem conclusão/aceite, OU
     * - Se o produto está na faixa de rebaixa (<=15 dias) e nunca foi solicitada
     */
    fun isDueForNewMarkdownRequest(referenceTime: Long = System.currentTimeMillis()): Boolean {
        if (isRemovedFromSales) return false
        val days = getDaysRemaining(referenceTime)
        if (days !in 2..15) return false

        // Nunca solicitada: elegível para primeira solicitação
        if (!markdownRequested || markdownStatus == "NOT_REQUESTED") {
            return true
        }

        val daysSinceLast = getDaysSinceMarkdownRequest(referenceTime)
        // Se foi recusada ou pendente há 3 dias ou mais
        return (markdownStatus == "REJECTED" || markdownStatus == "PENDING_APPROVAL") && daysSinceLast >= 3
    }

    /**
     * Faltando 15 dias ou menos -> Alerta de Rebaixa ativo
     */
    fun isMarkdownEligible(referenceTime: Long = System.currentTimeMillis()): Boolean {
        val days = getDaysRemaining(referenceTime)
        return days in 0..15
    }

    /**
     * Faltando 1 dia ou menos -> Deve ser retirado da área de venda
     */
    fun isMustRemoveFromSales(referenceTime: Long = System.currentTimeMillis()): Boolean {
        val days = getDaysRemaining(referenceTime)
        return days in 0..1
    }

    /**
     * Produto em fase de monitoramento (≤ 15 dias ou vencido)
     */
    fun isInMonitoring(referenceTime: Long = System.currentTimeMillis()): Boolean {
        val days = getDaysRemaining(referenceTime)
        return days <= 15
    }

    fun getExpiryStatus(referenceTime: Long = System.currentTimeMillis()): ExpiryStatus {
        val days = getDaysRemaining(referenceTime)
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= 1 -> ExpiryStatus.MUST_REMOVE_1_DAY
            days <= 15 -> ExpiryStatus.MARKDOWN_REBAIXA
            days <= 30 -> ExpiryStatus.WARNING
            else -> ExpiryStatus.SAFE
        }
    }

    fun getFormattedExpiryDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(expiryDate))
    }

    fun getFormattedLastAudit(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(lastAuditedDate))
    }

    fun getFormattedRemovalTime(): String? {
        return removalConfirmedAt?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        }
    }

    fun getFormattedMarkdownRequestedDate(): String? {
        return markdownRequestedAt?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        }
    }

    fun getFormattedMarkdownRequestTime(): String? = getFormattedMarkdownRequestedDate()

    fun getFormattedMarkdownCheckedDate(): String? {
        return markdownCheckedAt?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        }
    }

    fun calculateSuggestedMarkdownPrice(): Double {
        if (regularPrice <= 0.0) return 0.0
        val discountMultiplier = 1.0 - (markdownDiscountPercent.toDouble() / 100.0)
        return regularPrice * discountMultiplier
    }
}
