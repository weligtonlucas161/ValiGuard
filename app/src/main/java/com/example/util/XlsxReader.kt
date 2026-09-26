package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Leitor nativo ultra-leve de arquivos Excel (.xlsx).
 * Não depende de bibliotecas externas pesadas como Apache POI.
 * Processa a estrutura OpenXML (ZIP + XML) nativamente utilizando XmlPullParser do Android SDK.
 */
object XlsxReader {

    data class XlsxSheetData(
        val sheetName: String,
        val rows: List<List<String>>
    ) {
        fun toCsvString(delimiter: String = ";"): String {
            val sb = StringBuilder()
            rows.forEach { row ->
                val line = row.joinToString(delimiter) { col ->
                    if (col.contains(delimiter) || col.contains("\"") || col.contains("\n")) {
                        "\"" + col.replace("\"", "\"\"") + "\""
                    } else {
                        col
                    }
                }
                sb.append(line).append("\n")
            }
            return sb.toString()
        }
    }

    /**
     * Verifica se o arquivo/URI corresponde a uma planilha Excel .xlsx.
     */
    fun isXlsxFile(context: Context, uri: Uri, fileName: String? = null): Boolean {
        if (fileName != null && fileName.endsWith(".xlsx", ignoreCase = true)) {
            return true
        }
        val path = uri.path?.lowercase() ?: ""
        if (path.endsWith(".xlsx")) return true

        // Verifica magic bytes (PK\x03\x04 de arquivo ZIP)
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Lê uma planilha Excel .xlsx a partir de um Uri e extrai as linhas como texto CSV.
     */
    fun readXlsxToCsv(context: Context, uri: Uri, delimiter: String = ";"): String? {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        return stream.use { readXlsxToCsv(it, delimiter) }
    }

    /**
     * Lê um arquivo .xlsx a partir de um InputStream e retorna como CSV delimitado.
     */
    fun readXlsxToCsv(inputStream: InputStream, delimiter: String = ";"): String? {
        val sheetData = readFirstSheet(inputStream) ?: return null
        return sheetData.toCsvString(delimiter)
    }

    /**
     * Lê a primeira aba de dados da planilha .xlsx.
     */
    fun readFirstSheet(inputStream: InputStream): XlsxSheetData? {
        return try {
            val allBytes = inputStream.readBytes()
            var sharedStringsBytes: ByteArray? = null
            var sheetBytes: ByteArray? = null
            var sheetName = "Planilha1"

            // Passo 1: Extrair sharedStrings.xml e a primeira planilha (sheet1.xml)
            ZipInputStream(ByteArrayInputStream(allBytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                        sharedStringsBytes = zip.readBytes()
                    } else if (sheetBytes == null && (
                            name.equals("xl/worksheets/sheet1.xml", ignoreCase = true) ||
                            name.matches(Regex("xl/worksheets/sheet[0-9]+\\.xml", RegexOption.IGNORE_CASE))
                        )) {
                        sheetBytes = zip.readBytes()
                        sheetName = name.substringAfterLast("/").substringBeforeLast(".")
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (sheetBytes == null) {
                return null
            }

            // Passo 2: Fazer parse dos Shared Strings
            val sharedStrings = parseSharedStrings(sharedStringsBytes)

            // Passo 3: Fazer parse dos dados da planilha (células e linhas)
            val rows = parseSheetRows(sheetBytes, sharedStrings)

            XlsxSheetData(sheetName, rows)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parseia o arquivo xl/sharedStrings.xml.
     * Excel armazena textos repetidos nesta tabela para economia de espaço.
     */
    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null || bytes.isEmpty()) return emptyList()

        val list = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var eventType = parser.eventType
        var insideSi = false
        var currentSiText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            insideSi = true
                            currentSiText = StringBuilder()
                        }
                        "t" -> {
                            if (insideSi) {
                                val text = parser.nextText()
                                currentSiText.append(text)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        insideSi = false
                        list.add(currentSiText.toString().trim())
                    }
                }
            }
            eventType = parser.next()
        }

        return list
    }

    /**
     * Parseia as linhas e células de xl/worksheets/sheet1.xml.
     */
    private fun parseSheetRows(sheetBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(sheetBytes), "UTF-8")

        var eventType = parser.eventType
        var currentRowCells = mutableMapOf<Int, String>()
        var currentCellRef = ""
        var currentCellType = ""
        var currentCellValue = ""
        var insideValueTag = false
        var insideInlineStr = false
        var inlineStrText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowCells = mutableMapOf()
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentCellValue = ""
                            insideInlineStr = false
                            inlineStrText = StringBuilder()
                        }
                        "v" -> {
                            insideValueTag = true
                        }
                        "is" -> {
                            insideInlineStr = true
                        }
                        "t" -> {
                            if (insideInlineStr) {
                                val text = parser.nextText()
                                inlineStrText.append(text)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideValueTag) {
                        currentCellValue += parser.text ?: ""
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            insideValueTag = false
                        }
                        "c" -> {
                            val colIndex = colRefToIndex(currentCellRef)
                            val finalVal = when (currentCellType) {
                                "s" -> {
                                    val strIdx = currentCellValue.trim().toIntOrNull()
                                    if (strIdx != null && strIdx in sharedStrings.indices) {
                                        sharedStrings[strIdx]
                                    } else {
                                        currentCellValue.trim()
                                    }
                                }
                                "inlineStr" -> inlineStrText.toString().trim()
                                "b" -> if (currentCellValue.trim() == "1") "VERDADEIRO" else "FALSO"
                                else -> {
                                    // Números ou strings diretas
                                    val raw = currentCellValue.trim()
                                    // Se for número com ponto zero (ex: 15.0 para quantidade inteira), limpa o .0
                                    if (raw.endsWith(".0") && raw.substringBeforeLast(".0").all { it.isDigit() }) {
                                        raw.substringBeforeLast(".0")
                                    } else {
                                        raw
                                    }
                                }
                            }
                            currentRowCells[colIndex] = finalVal
                        }
                        "row" -> {
                            if (currentRowCells.isNotEmpty()) {
                                val maxCol = (currentRowCells.keys.maxOrNull() ?: -1)
                                val rowList = ArrayList<String>(maxCol + 1)
                                for (c in 0..maxCol) {
                                    rowList.add(currentRowCells[c] ?: "")
                                }
                                // Apenas adiciona linhas que não são totalmente vazias
                                if (rowList.any { it.isNotBlank() }) {
                                    rows.add(rowList)
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return rows
    }

    /**
     * Converte uma referência de célula como "A1", "B5", "AA12" no índice da coluna (0-based).
     */
    private fun colRefToIndex(cellRef: String): Int {
        if (cellRef.isBlank()) return 0
        var colLetters = ""
        for (char in cellRef.uppercase()) {
            if (char in 'A'..'Z') {
                colLetters += char
            } else {
                break
            }
        }
        if (colLetters.isEmpty()) return 0

        var index = 0
        for (char in colLetters) {
            index = index * 26 + (char - 'A' + 1)
        }
        return (index - 1).coerceAtLeast(0)
    }
}
