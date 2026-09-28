package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.model.AuditItem
import com.example.data.model.AuditSession
import com.example.data.model.AuditType
import com.example.data.model.Product
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AuditPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    /**
     * Gera o relatório PDF da auditoria e salva no diretório de documentos do app.
     * Retorna o File gerado.
     */
    fun generateAuditPdf(
        context: Context,
        session: AuditSession,
        items: List<AuditItem>
    ): File {
        val document = PdfDocument()

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(30, 41, 59) // Slate 800
        }

        val primaryPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(37, 99, 235) // Blue 600
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249) // Slate 100
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(30, 58, 138) // Deep Blue
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 1f
        }

        val altRowPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // Slate 50
        }

        // Layout pagination
        val itemsPerPage = 22
        val totalPages = if (items.isEmpty()) 1 else ((items.size - 1) / itemsPerPage) + 1

        val auditTitle = when (session.auditType) {
            AuditType.VALIDITY.code -> "AUDITORIA DE VALIDADE E LOTES"
            AuditType.PRESENCE.code -> "AUDITORIA DE PRESENÇA DE PRODUTOS"
            AuditType.STOCK.code -> "AUDITORIA DE ESTOQUE FÍSICO"
            else -> "RELATÓRIO DE AUDITORIA"
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var y = MARGIN

            // 1. Top Header Banner
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 68f, headerBgPaint)

            // Accent bar
            canvas.drawRect(MARGIN, y, MARGIN + 6f, y + 68f, primaryPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 15f
            textPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText(auditTitle, MARGIN + 16f, y + 26f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 9.5f
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText("SUPERMERCADO VAREJO • SISTEMA DE GESTÃO DE VALIDADE E AUDITORIA", MARGIN + 16f, y + 42f, textPaint)
            canvas.drawText("Data/Hora: $generatedDate | Operador: ${session.operatorName}", MARGIN + 16f, y + 56f, textPaint)

            y += 78f

            // 2. Summary KPI Box (on first page)
            if (pageIndex == 0) {
                drawSummaryMetrics(canvas, session, items, y, CONTENT_WIDTH, textPaint, linePaint)
                y += 56f
            }

            // 3. Table Column Headers
            drawTableHeaders(canvas, session.auditType, y, tableHeaderPaint, textPaint)
            y += 24f

            // 4. Table Rows for this page
            val startItemIndex = pageIndex * itemsPerPage
            val endItemIndex = (startItemIndex + itemsPerPage).coerceAtMost(items.size)

            for (i in startItemIndex until endItemIndex) {
                val item = items[i]
                val isAlt = (i % 2 == 1)
                if (isAlt) {
                    canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 20f, altRowPaint)
                }

                drawTableRow(canvas, session.auditType, item, y + 14f, textPaint)
                canvas.drawLine(MARGIN, y + 20f, MARGIN + CONTENT_WIDTH, y + 20f, linePaint)
                y += 20f
            }

            // 5. Page Footer
            val footerY = PAGE_HEIGHT - MARGIN + 14f
            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(148, 163, 184)
            canvas.drawLine(MARGIN, footerY - 14f, MARGIN + CONTENT_WIDTH, footerY - 14f, linePaint)
            canvas.drawText("Supermercado Gestão de Validade • Relatório Oficial", MARGIN, footerY, textPaint)
            val pageStr = "Página ${pageIndex + 1} de $totalPages"
            val pageStrWidth = textPaint.measureText(pageStr)
            canvas.drawText(pageStr, MARGIN + CONTENT_WIDTH - pageStrWidth, footerY, textPaint)

            document.finishPage(page)
        }

        // Save file
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
        val safeDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Auditoria_${session.auditType}_$safeDate.pdf"
        val file = File(outputDir, fileName)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    private fun drawSummaryMetrics(
        canvas: Canvas,
        session: AuditSession,
        items: List<AuditItem>,
        startY: Float,
        width: Float,
        textPaint: Paint,
        linePaint: Paint
    ) {
        val bgPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        canvas.drawRoundRect(MARGIN, startY, MARGIN + width, startY + 44f, 6f, 6f, bgPaint)
        canvas.drawRoundRect(MARGIN, startY, MARGIN + width, startY + 44f, 6f, 6f, linePaint.apply { style = Paint.Style.STROKE })

        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(51, 65, 85)

        when (session.auditType) {
            AuditType.VALIDITY.code -> {
                val totalSales = items.sumOf { it.quantitySalesArea }
                val totalStock = items.sumOf { it.quantityStockArea }
                val totalPieces = totalSales + totalStock
                canvas.drawText("Total de Itens Auditados: ${items.size}", MARGIN + 14f, startY + 18f, textPaint)
                canvas.drawText("Peças em Área de Vendas: $totalSales un", MARGIN + 190f, startY + 18f, textPaint)
                canvas.drawText("Peças em Depósito / Estoque: $totalStock un", MARGIN + 350f, startY + 18f, textPaint)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Volume Total Apurado: $totalPieces un", MARGIN + 14f, startY + 34f, textPaint)
            }
            AuditType.PRESENCE.code -> {
                val present = items.count { it.isPresent }
                val missing = items.size - present
                val percent = if (items.isNotEmpty()) (present * 100 / items.size) else 0
                canvas.drawText("Total na Lista: ${items.size}", MARGIN + 14f, startY + 18f, textPaint)
                textPaint.color = Color.rgb(22, 101, 52) // Green
                canvas.drawText("Bipados (Presentes): $present", MARGIN + 160f, startY + 18f, textPaint)
                textPaint.color = Color.rgb(185, 28, 28) // Red
                canvas.drawText("Faltantes: $missing", MARGIN + 310f, startY + 18f, textPaint)
                textPaint.color = Color.rgb(30, 58, 138)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Taxa de Presença: $percent%", MARGIN + 420f, startY + 18f, textPaint)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = Color.rgb(100, 116, 139)
                canvas.drawText("Status da auditoria: Concluída com reset da lista para próxima sessão.", MARGIN + 14f, startY + 34f, textPaint)
            }
            AuditType.STOCK.code -> {
                val totalPiecesSales = items.filter { it.location == "Área de Vendas" }.sumOf { it.quantitySalesArea }
                val totalPiecesDepot = items.filter { it.location != "Área de Vendas" }.sumOf { it.quantityStockArea }
                val totalCount = totalPiecesSales + totalPiecesDepot
                canvas.drawText("Registros de Contagem: ${items.size}", MARGIN + 14f, startY + 18f, textPaint)
                canvas.drawText("Área de Vendas: $totalPiecesSales un", MARGIN + 190f, startY + 18f, textPaint)
                canvas.drawText("Depósito / Estoque: $totalPiecesDepot un", MARGIN + 350f, startY + 18f, textPaint)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Total Físico Auditado: $totalCount un", MARGIN + 14f, startY + 34f, textPaint)
            }
        }
    }

    private fun drawTableHeaders(
        canvas: Canvas,
        auditType: String,
        y: Float,
        headerBgPaint: Paint,
        textPaint: Paint
    ) {
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 22f, headerBgPaint)

        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 9f

        val headerY = y + 15f
        when (auditType) {
            AuditType.VALIDITY.code -> {
                canvas.drawText("CÓD. BARRAS", MARGIN + 6f, headerY, textPaint)
                canvas.drawText("PRODUTO", MARGIN + 90f, headerY, textPaint)
                canvas.drawText("LOTE", MARGIN + 270f, headerY, textPaint)
                canvas.drawText("VALIDADE", MARGIN + 340f, headerY, textPaint)
                canvas.drawText("VENDAS", MARGIN + 410f, headerY, textPaint)
                canvas.drawText("DEPÓSITO", MARGIN + 465f, headerY, textPaint)
            }
            AuditType.PRESENCE.code -> {
                canvas.drawText("CÓD. BARRAS", MARGIN + 6f, headerY, textPaint)
                canvas.drawText("PRODUTO", MARGIN + 90f, headerY, textPaint)
                canvas.drawText("SETOR", MARGIN + 280f, headerY, textPaint)
                canvas.drawText("STATUS", MARGIN + 380f, headerY, textPaint)
                canvas.drawText("HORÁRIO BIPADO", MARGIN + 445f, headerY, textPaint)
            }
            AuditType.STOCK.code -> {
                canvas.drawText("CÓD. BARRAS", MARGIN + 6f, headerY, textPaint)
                canvas.drawText("PRODUTO", MARGIN + 90f, headerY, textPaint)
                canvas.drawText("SETOR", MARGIN + 280f, headerY, textPaint)
                canvas.drawText("LOCAL AUDITADO", MARGIN + 375f, headerY, textPaint)
                canvas.drawText("QUANTIDADE", MARGIN + 465f, headerY, textPaint)
            }
        }
    }

    private fun drawTableRow(
        canvas: Canvas,
        auditType: String,
        item: AuditItem,
        rowY: Float,
        textPaint: Paint
    ) {
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 8.5f
        textPaint.color = Color.rgb(30, 41, 59)

        val truncName = if (item.productName.length > 34) item.productName.take(32) + ".." else item.productName

        when (auditType) {
            AuditType.VALIDITY.code -> {
                canvas.drawText(item.barcode.take(13), MARGIN + 6f, rowY, textPaint)
                canvas.drawText(truncName, MARGIN + 90f, rowY, textPaint)
                canvas.drawText(item.batch ?: "-", MARGIN + 270f, rowY, textPaint)
                canvas.drawText(item.getFormattedExpiryDate(), MARGIN + 340f, rowY, textPaint)
                canvas.drawText("${item.quantitySalesArea} un", MARGIN + 410f, rowY, textPaint)
                canvas.drawText("${item.quantityStockArea} un", MARGIN + 465f, rowY, textPaint)
            }
            AuditType.PRESENCE.code -> {
                canvas.drawText(item.barcode.take(13), MARGIN + 6f, rowY, textPaint)
                canvas.drawText(truncName, MARGIN + 90f, rowY, textPaint)
                canvas.drawText(item.sector?.take(16) ?: "-", MARGIN + 280f, rowY, textPaint)

                if (item.isPresent) {
                    textPaint.color = Color.rgb(22, 101, 52)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("PRESENTE", MARGIN + 380f, rowY, textPaint)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textPaint.color = Color.rgb(71, 85, 105)
                    canvas.drawText(item.getFormattedScannedTime(), MARGIN + 445f, rowY, textPaint)
                } else {
                    textPaint.color = Color.rgb(185, 28, 28)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("FALTANTE", MARGIN + 380f, rowY, textPaint)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textPaint.color = Color.rgb(148, 163, 184)
                    canvas.drawText("Não bipado", MARGIN + 445f, rowY, textPaint)
                }
            }
            AuditType.STOCK.code -> {
                canvas.drawText(item.barcode.take(13), MARGIN + 6f, rowY, textPaint)
                canvas.drawText(truncName, MARGIN + 90f, rowY, textPaint)
                canvas.drawText(item.sector?.take(16) ?: "-", MARGIN + 280f, rowY, textPaint)
                canvas.drawText(item.location ?: "Área de Vendas", MARGIN + 375f, rowY, textPaint)
                val qty = if (item.location == "Área de Vendas") item.quantitySalesArea else item.quantityStockArea
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("$qty un", MARGIN + 465f, rowY, textPaint)
            }
        }
    }

    /**
     * Gera o Relatório Detalhado de Prioridade de Vencimento para Rebaixa Urgente (Gerência).
     * Contém: Código EAN, Código PLU, Descrição, Data de Entrada, Data de Validade,
     * Quantidade Contada e Preço Atual, com área para assinatura da gerência.
     */
     fun generateUrgentMarkdownReportPdf(
        context: Context,
        operatorName: String,
        operatorRegistration: String,
        products: List<Product>
    ): File {
        val document = PdfDocument()

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(30, 41, 59)
        }

        val primaryPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(234, 88, 12) // Orange 600
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(255, 247, 237) // Orange 50
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(194, 65, 12) // Dark Orange
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        val altRowPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
        }

        val urgentRowPaint = Paint().apply {
            color = Color.rgb(254, 242, 242) // Light red for <=1 day
        }

        val itemsPerPage = 18
        val totalPages = if (products.isEmpty()) 1 else ((products.size - 1) / itemsPerPage) + 1
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var y = MARGIN

            // 1. Top Header Banner
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 74f, headerBgPaint)
            canvas.drawRect(MARGIN, y, MARGIN + 6f, y + 74f, primaryPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 13f
            textPaint.color = Color.rgb(154, 52, 18)
            canvas.drawText("RELATÓRIO DE PRIORIDADE DE VENCIMENTO - REBAIXA URGENTE", MARGIN + 16f, y + 24f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 9f
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText("Solicitação Formal à Gerência de Loja para Autorização e Rebaixa de Preço", MARGIN + 16f, y + 40f, textPaint)

            textPaint.textSize = 8.5f
            canvas.drawText("Operador: $operatorName (Matrícula: $operatorRegistration) | Emitido em: $generatedDate", MARGIN + 16f, y + 56f, textPaint)
            canvas.drawText("Página ${pageIndex + 1} de $totalPages", MARGIN + CONTENT_WIDTH - 70f, y + 56f, textPaint)

            y += 84f

            // 2. Summary stats bar on first page
            if (pageIndex == 0) {
                val statsBg = Paint().apply { color = Color.rgb(241, 245, 249) }
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 28f, statsBg)

                textPaint.textSize = 8.5f
                textPaint.color = Color.rgb(15, 23, 42)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val totalQty = products.sumOf { it.quantity }
                val urgent1d = products.count { it.getDaysRemaining() <= 1 && !it.isRemovedFromSales }
                val markdown15d = products.count { it.getDaysRemaining() in 2..15 && !it.isRemovedFromSales }

                canvas.drawText(
                    "TOTAL PRODUTOS: ${products.size}  |  QUANTIDADE CONTADA: $totalQty un  |  REMOVER ≤1d: $urgent1d  |  REBAIXA ≤15d: $markdown15d",
                    MARGIN + 12f,
                    y + 18f,
                    textPaint
                )
                y += 34f
            }

            // 3. Table Header
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 22f, tableHeaderPaint)
            textPaint.color = Color.WHITE
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8f

            canvas.drawText("CÓD EAN", MARGIN + 4f, y + 15f, textPaint)
            canvas.drawText("PLU", MARGIN + 72f, y + 15f, textPaint)
            canvas.drawText("DESCRIÇÃO DO PRODUTO", MARGIN + 115f, y + 15f, textPaint)
            canvas.drawText("ENTRADA", MARGIN + 265f, y + 15f, textPaint)
            canvas.drawText("VALIDADE", MARGIN + 318f, y + 15f, textPaint)
            canvas.drawText("QTD", MARGIN + 382f, y + 15f, textPaint)
            canvas.drawText("PREÇO ATUAL", MARGIN + 418f, y + 15f, textPaint)
            canvas.drawText("REBAIXA (-30%)", MARGIN + 472f, y + 15f, textPaint)

            y += 22f

            // 4. Table Rows
            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, products.size)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 7.5f

            for (i in startIndex until endIndex) {
                val product = products[i]
                val daysRem = product.getDaysRemaining()
                val isUrgent1d = daysRem <= 1 && !product.isRemovedFromSales

                val rowBg = when {
                    isUrgent1d -> urgentRowPaint
                    i % 2 == 1 -> altRowPaint
                    else -> null
                }

                if (rowBg != null) {
                    canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 20f, rowBg)
                }

                canvas.drawLine(MARGIN, y + 20f, MARGIN + CONTENT_WIDTH, y + 20f, linePaint)

                textPaint.color = if (isUrgent1d) Color.rgb(185, 28, 28) else Color.rgb(30, 41, 59)
                val rowY = y + 14f

                // EAN (barcode)
                val eanText = product.barcode.ifBlank { "S/ CÓD" }.take(13)
                canvas.drawText(eanText, MARGIN + 4f, rowY, textPaint)

                // PLU (internal code)
                val pluText = product.internalCode.ifBlank { "-" }.take(8)
                canvas.drawText(pluText, MARGIN + 72f, rowY, textPaint)

                // Descrição
                val nameText = product.name.take(28)
                canvas.drawText(nameText, MARGIN + 115f, rowY, textPaint)

                // Data de entrada
                canvas.drawText(product.getFormattedCreatedAt(), MARGIN + 265f, rowY, textPaint)

                // Validade & dias
                val valText = "${product.getFormattedExpiryDate()} (${daysRem}d)"
                canvas.drawText(valText, MARGIN + 318f, rowY, textPaint)

                // Quantidade contada
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${product.quantity} ${product.unit}", MARGIN + 382f, rowY, textPaint)

                // Preço atual
                val priceText = if (product.regularPrice > 0) String.format(Locale.getDefault(), "R$ %.2f", product.regularPrice) else "R$ 0,00"
                canvas.drawText(priceText, MARGIN + 418f, rowY, textPaint)

                // Preço com rebaixa sugerida
                val markdownPrice = product.calculateSuggestedMarkdownPrice()
                val suggestedText = if (markdownPrice > 0) String.format(Locale.getDefault(), "R$ %.2f", markdownPrice) else "-"
                textPaint.color = Color.rgb(194, 65, 12)
                canvas.drawText(suggestedText, MARGIN + 472f, rowY, textPaint)

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                y += 20f
            }

            // 5. Manager Approval Box on Last Page
            if (pageIndex == totalPages - 1) {
                y += 18f
                val approvalBoxPaint = Paint().apply {
                    color = Color.rgb(241, 245, 249)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f
                }
                val approvalFillPaint = Paint().apply {
                    color = Color.rgb(248, 250, 252)
                }

                val boxHeight = 70f
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + boxHeight, approvalFillPaint)
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + boxHeight, approvalBoxPaint)

                textPaint.textSize = 9f
                textPaint.color = Color.rgb(15, 23, 42)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("PARECER E AUTORIZAÇÃO DA GERÊNCIA DE LOJA:", MARGIN + 12f, y + 18f, textPaint)

                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("[   ] REBAIXA APROVADA CONFORME SOLICITADO       [   ] REBAIXA NÃO AUTORIZADA", MARGIN + 12f, y + 36f, textPaint)
                canvas.drawText("Assinatura do Gerente: _________________________________________   Data: _____/_____/________", MARGIN + 12f, y + 54f, textPaint)
            }

            document.finishPage(page)
        }

        val fileName = "relatorio_rebaixa_urgente_${System.currentTimeMillis()}.pdf"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(storageDir, fileName)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    /**
     * Retorna Uri segura usando FileProvider para abrir ou compartilhar o arquivo PDF.
     */
    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Abre o visualizador padrão de PDF do sistema.
     */
    fun openPdf(context: Context, file: File) {
        val uri = getFileUri(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdf(context, file)
        }
    }

    /**
     * Dispara o menu nativo de compartilhamento do Android.
     */
    fun sharePdf(context: Context, file: File) {
        val uri = getFileUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório de Auditoria - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Segue em anexo o relatório oficial de auditoria.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Compartilhar Relatório de Auditoria (PDF)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
