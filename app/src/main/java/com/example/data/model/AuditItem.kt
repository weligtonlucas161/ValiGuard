package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "audit_items",
    foreignKeys = [
        ForeignKey(
            entity = AuditSession::class,
            parentColumns = ["id"],
            childColumns = ["auditSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["auditSessionId"]),
        Index(value = ["barcode"])
    ]
)
data class AuditItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val auditSessionId: Long,
    val productId: Long? = null,
    val barcode: String,
    val productName: String,
    val internalCode: String? = null,
    val sector: String? = null,
    val batch: String? = null, // para validade
    val expiryDate: Long? = null, // para validade
    val quantitySalesArea: Int = 0, // para validade e estoque
    val quantityStockArea: Int = 0, // para validade e estoque
    val location: String? = null, // "Área de Vendas", "Depósito" para estoque
    val isPresent: Boolean = false, // para presença: true = bipado, false = pendente
    val scannedAt: Long? = null,
    val notes: String? = null
) {
    fun getFormattedExpiryDate(): String {
        return expiryDate?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(it))
        } ?: "-"
    }

    fun getFormattedScannedTime(): String {
        return scannedAt?.let {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(it))
        } ?: "-"
    }
}
