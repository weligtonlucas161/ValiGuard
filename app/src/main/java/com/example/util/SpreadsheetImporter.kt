package com.example.util

import com.example.data.model.Product
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Importador dinâmico de planilhas e dados tabulares (CSV, TSV, texto colado ou planilhas exportadas).
 * Mapeia dinamicamente colunas flexíveis identificando PLU/Código, Descrição do Produto, Quantidade e Validade.
 */
object SpreadsheetImporter {

    data class ImportResult(
        val successProducts: List<Product>,
        val ignoredLines: Int,
        val detectedColumns: String
    )

    private fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    /**
     * Processa conteúdo de texto linha por linha, mapeia colunas dinamicamente
     * e vincula todos os registros à [lojaId] do usuário conectado.
     */
    fun parseSpreadsheet(
        rawContent: String,
        lojaId: String,
        defaultSector: String = "Mercearia"
    ): ImportResult {
        val lines = rawContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ImportResult(emptyList(), 0, "Planilha vazia")
        }

        // Detecta o delimitador mais provável: vírgula, ponto-e-vírgula, tabulação ou pipe
        val firstFew = lines.take(5).joinToString("\n")
        val delimiter = when {
            firstFew.count { it == ';' } >= firstFew.count { it == ',' } && firstFew.count { it == ';' } > 0 -> ";"
            firstFew.count { it == '\t' } > 0 -> "\t"
            firstFew.count { it == '|' } > 0 -> "|"
            else -> ","
        }

        var colPlu = -1
        var colDesc = -1
        var colQtd = -1
        var colValidade = -1
        var colSetor = -1
        var colSecao = -1

        var startIndex = 0
        val firstTokens = splitLine(lines[0], delimiter)

        // Verifica se a primeira linha é um cabeçalho
        var hasHeader = false
        firstTokens.forEachIndexed { index, token ->
            val norm = normalize(token)
            if (norm.contains("plu") || norm.contains("cod") || norm.contains("sku") || norm.contains("ean") || norm.contains("barras") || norm.contains("ref")) {
                colPlu = index
                hasHeader = true
            } else if (norm.contains("desc") || norm.contains("prod") || norm.contains("item") || norm.contains("nome")) {
                colDesc = index
                hasHeader = true
            } else if (norm.contains("qtd") || norm.contains("quant") || norm.contains("estoque") || norm.contains("saldo")) {
                colQtd = index
                hasHeader = true
            } else if (norm.contains("val") || norm.contains("venc") || norm.contains("data")) {
                colValidade = index
                hasHeader = true
            } else if (norm.contains("setor") || norm.contains("categoria") || norm.contains("depto")) {
                colSetor = index
                hasHeader = true
            } else if (norm.contains("secao") || norm.contains("secao") || norm.contains("sub")) {
                colSecao = index
                hasHeader = true
            }
        }

        if (hasHeader) {
            startIndex = 1
        } else {
            // Se não houver cabeçalho explícito, tenta deduzir pela ordem convencional:
            // Col 0: PLU/Código, Col 1: Descrição, Col 2: Quantidade
            colPlu = 0
            colDesc = if (firstTokens.size > 1) 1 else -1
            colQtd = if (firstTokens.size > 2) 2 else -1
        }

        // Se ainda não achou colunas essenciais, faz fallback seguro
        if (colDesc == -1 && firstTokens.size >= 2) colDesc = 1
        if (colPlu == -1) colPlu = 0
        if (colQtd == -1 && firstTokens.size >= 3) colQtd = 2

        val resultProducts = mutableListOf<Product>()
        var ignored = 0
        val now = System.currentTimeMillis()

        for (i in startIndex until lines.size) {
            val line = lines[i]
            val tokens = splitLine(line, delimiter)
            if (tokens.isEmpty()) {
                ignored++
                continue
            }

            val rawPlu = if (colPlu in tokens.indices) tokens[colPlu].trim() else ""
            val rawDesc = if (colDesc in tokens.indices) tokens[colDesc].trim() else ""
            val rawQtd = if (colQtd in tokens.indices) tokens[colQtd].trim() else "0"
            val rawVal = if (colValidade in tokens.indices) tokens[colValidade].trim() else ""
            val rawSetor = if (colSetor in tokens.indices) tokens[colSetor].trim() else defaultSector
            val rawSecao = if (colSecao in tokens.indices) tokens[colSecao].trim() else ""

            if (rawDesc.isBlank() && rawPlu.isBlank()) {
                ignored++
                continue
            }

            // Normaliza quantidade
            val cleanQtdStr = rawQtd.replace(",", ".").replace("[^0-9.]".toRegex(), "")
            val parsedQtd = cleanQtdStr.toDoubleOrNull()?.toInt() ?: 0

            // Normaliza data de validade
            val expiryMillis = parseExpiryDate(rawVal) ?: (now + 30L * 86400000L)

            val finalName = if (rawDesc.isNotBlank()) rawDesc else "Produto PLU $rawPlu"
            val isBarcode = rawPlu.length in 8..14 && rawPlu.all { it.isDigit() }

            val product = Product(
                name = finalName,
                category = if (rawSetor.isNotBlank()) rawSetor else defaultSector,
                section = rawSecao,
                barcode = if (isBarcode) rawPlu else "",
                internalCode = rawPlu,
                quantity = parsedQtd,
                unit = "un",
                expiryDate = expiryMillis,
                lojaId = lojaId.trim(),
                createdAt = now
            )
            resultProducts.add(product)
        }

        val detectedInfo = "Delimitador: '$delimiter' | Colunas: PLU=$colPlu, Descrição=$colDesc, Qtd=$colQtd"
        return ImportResult(resultProducts, ignored, detectedInfo)
    }

    private fun splitLine(line: String, delimiter: String): List<String> {
        // Suporta aspas em CSV
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var insideQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> insideQuotes = !insideQuotes
                ch.toString() == delimiter && !insideQuotes -> {
                    tokens.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString())
        return tokens.map { it.replace("\"", "").trim() }
    }

    private fun parseExpiryDate(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd",
            "dd/MM/yy", "dd.MM.yyyy", "yyyy/MM/dd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault()).apply { isLenient = false }
                val date = sdf.parse(dateStr)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }
}
