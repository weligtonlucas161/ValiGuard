package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Product
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitário para exportação do Relatório de Prioridade de Vencimento e Rebaixa
 * no formato CSV (Comma-Separated Values / Ponto e Vírgula), compatível com Excel,
 * Google Planilhas e LibreOffice Calc.
 */
object ExpiryCsvExporter {

    fun generateUrgentMarkdownReportCsv(
        context: Context,
        operatorName: String,
        operatorRegistration: String,
        products: List<Product>
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val csvFile = File(exportDir, "relatorio_prioridade_vencimento_$timeStamp.csv")

        val sortedProducts = products.sortedWith(
            compareBy<Product> {
                val days = it.getDaysRemaining()
                when {
                    days in 0..1 -> 0 // Vence hoje ou amanhã (Roxo)
                    days in 2..15 -> 1 // Rebaixa urgente (Amarelo/Laranja)
                    days < 0 -> 2 // Já vencido
                    else -> 3
                }
            }.thenBy { it.expiryDate }
        )

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val generatedAt = fullDateFormat.format(Date())

        FileWriter(csvFile).use { writer ->
            // Cabeçalho institucional do supermercado com BOM UTF-8 para Excel
            writer.write("\uFEFF") // UTF-8 BOM
            writer.write("# RELATÓRIO DE PRIORIDADE DE VENCIMENTO E REBAIXA\n")
            writer.write("# Gerado em:;$generatedAt\n")
            writer.write("# Operador:;$operatorName;Matrícula:;$operatorRegistration\n")
            writer.write("# Total de Itens:;${sortedProducts.size}\n")
            writer.write("\n")

            // Colunas da planilha CSV
            val headers = listOf(
                "Prioridade",
                "Status Validade",
                "Status Rebaixa",
                "Dias Restantes",
                "Código de Barras (EAN)",
                "Código Interno / PLU",
                "Descrição do Produto",
                "Setor / Categoria",
                "Seção / Gôndola",
                "Lote",
                "Qtd em Gôndola",
                "Qtd em Estoque",
                "Unidade",
                "Data de Entrada",
                "Data de Validade",
                "Preço Regular (R$)",
                "Preço com Rebaixa (R$)",
                "Retirado da Área de Vendas",
                "Observações"
            )
            writer.write(headers.joinToString(";") + "\n")

            val now = System.currentTimeMillis()
            for (p in sortedProducts) {
                val days = p.getDaysRemaining(now)
                val priorityLabel = when {
                    days in 0..1 -> "1 - CRÍTICO (≤1d)"
                    days in 2..15 -> "2 - REBAIXA URGENTE (≤15d)"
                    days < 0 -> "4 - VENCIDO"
                    else -> "3 - MONITORAMENTO"
                }

                val statusValidade = when {
                    days < 0L -> "VENCIDO"
                    days == 0L -> "VENCE HOJE"
                    days == 1L -> "VENCE AMANHÃ (1d)"
                    days <= 15L -> "ALERTA (≤15d)"
                    else -> "NO PRAZO"
                }

                val statusRebaixa = when {
                    p.markdownStatus == "ACCEPTED" -> "REBAIXA ACEITA"
                    p.markdownRequested -> "REBAIXA SOLICITADA"
                    else -> "NÃO SOLICITADO"
                }

                val markdownPrice = if (p.markdownDiscountPercent > 0 && p.regularPrice > 0) {
                    val disc = p.regularPrice * (1.0 - (p.markdownDiscountPercent / 100.0))
                    String.format(Locale.GERMAN, "%.2f", disc)
                } else {
                    String.format(Locale.GERMAN, "%.2f", p.regularPrice)
                }

                val row = listOf(
                    escapeCsv(priorityLabel),
                    escapeCsv(statusValidade),
                    escapeCsv(statusRebaixa),
                    days.toString(),
                    escapeCsv(p.barcode),
                    escapeCsv(p.internalCode),
                    escapeCsv(p.name),
                    escapeCsv(p.category),
                    escapeCsv(p.section),
                    escapeCsv(p.batchCode),
                    p.quantity.toString(),
                    p.quantity.toString(),
                    escapeCsv(p.unit),
                    escapeCsv(dateFormat.format(Date(p.createdAt))),
                    escapeCsv(dateFormat.format(Date(p.expiryDate))),
                    String.format(Locale.GERMAN, "%.2f", p.regularPrice),
                    markdownPrice,
                    if (p.isRemovedFromSales) "SIM" else "NÃO",
                    escapeCsv(p.notes)
                )

                writer.write(row.joinToString(";") + "\n")
            }
        }

        return csvFile
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return if (clean.contains(";") || clean.contains("\n") || clean.contains("\"")) {
            "\"$clean\""
        } else {
            clean
        }
    }

    fun shareCsv(context: Context, csvFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório de Prioridade de Vencimento - Supermercado (CSV)")
            putExtra(Intent.EXTRA_TEXT, "Planilha CSV contendo produtos em prioridade de vencimento e rebaixa.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Planilha CSV"))
    }
}
