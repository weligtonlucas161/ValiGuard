package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "product_history",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"])]
)
data class ProductHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String = "",
    val actionType: String, // e.g. "CADASTRO", "CONFERENCIA_SEGUNDA", "REBAIXA_SOLICITADA", "AJUSTE_ESTOQUE", "ALERTA_VENCIMENTO", "RETIRADA_AREA_VENDA"
    val title: String,
    val details: String,
    val quantityAtTime: Int = 0,
    val expiryDateAtTime: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getFormattedExpiryDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(expiryDateAtTime))
    }
}
