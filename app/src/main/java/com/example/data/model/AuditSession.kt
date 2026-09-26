package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AuditType(val label: String, val code: String) {
    VALIDITY("Auditoria de Validade", "VALIDITY"),
    PRESENCE("Auditoria de Presença", "PRESENCE"),
    STOCK("Auditoria de Estoque", "STOCK"),
    MARKDOWN_URGENCY("Rebaixa Urgente (Gerência)", "MARKDOWN_URGENCY")
}

enum class AuditStatus {
    IN_PROGRESS,
    COMPLETED
}

@Entity(tableName = "audit_sessions")
data class AuditSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val auditType: String, // VALIDITY, PRESENCE, STOCK
    val status: String = AuditStatus.IN_PROGRESS.name,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val operatorName: String = "Operador",
    val totalItems: Int = 0,
    val presentCount: Int = 0,
    val missingCount: Int = 0,
    val pdfFilePath: String? = null,
    val notes: String? = null
) {
    fun getFormattedStartTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(startedAt))
    }

    fun getFormattedCompletedTime(): String {
        return completedAt?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        } ?: "-"
    }
}
