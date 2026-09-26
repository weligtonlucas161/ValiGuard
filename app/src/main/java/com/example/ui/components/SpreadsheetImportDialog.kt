package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.util.SpreadsheetImporter
import com.example.util.XlsxReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Diálogo para importação flexível de planilhas (XLSX, CSV, Excel tabulado ou texto colado).
 * Permite selecionar arquivo (.xlsx / .csv / .txt) ou colar o conteúdo de uma planilha.
 * Mapeia dinamicamente as colunas PLU, Descrição e Quantidade vinculando à loja do usuário.
 */
@Composable
fun SpreadsheetImportDialog(
    lojaId: String,
    nomeLoja: String,
    onDismiss: () -> Unit,
    onImportSuccess: (rawContent: String, count: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var rawInputText by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf<String?>(null) }
    var isReadingFile by remember { mutableStateOf(false) }
    var parsePreview by remember { mutableStateOf<SpreadsheetImporter.ImportResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Launcher do seletor de arquivos CSV / Texto
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isReadingFile = true
                errorMessage = null
                try {
                    val content = withContext(Dispatchers.IO) {
                        if (XlsxReader.isXlsxFile(context, uri)) {
                            XlsxReader.readXlsxToCsv(context, uri) ?: ""
                        } else {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                stream.bufferedReader().readText()
                            } ?: ""
                        }
                    }
                    fileName = uri.lastPathSegment ?: "planilha.xlsx"
                    rawInputText = content
                    val preview = SpreadsheetImporter.parseSpreadsheet(content, lojaId)
                    parsePreview = preview
                } catch (e: Exception) {
                    errorMessage = "Erro ao ler arquivo: ${e.message}"
                } finally {
                    isReadingFile = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp)
            .testTag("spreadsheet_import_dialog"),
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BlueExpressive.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = BlueExpressive,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Importar Planilha / CSV",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "Vínculo automático: $nomeLoja",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlueExpressive
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = DarkTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "O leitor detecta automaticamente as colunas de PLU, Descrição e Quantidade (separadas por vírgula, ponto-e-vírgula ou tabulação). Todos os itens serão cadastrados no estoque desta loja.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Opção 1: Selecionar Arquivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueExpressive),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (fileName != null) "Arquivo Selecionado" else "Escolher Arquivo (.xlsx/.csv/.txt)", fontSize = 12.sp)
                    }

                    if (isReadingFile) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BlueExpressive)
                    }
                }

                if (fileName != null) {
                    Text(
                        text = "📄 $fileName",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSafe,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Opção 2: Ou colar dados manualmente
                Text(
                    text = "Ou cole as linhas da planilha abaixo:",
                    style = MaterialTheme.typography.labelMedium,
                    color = DarkTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = {
                        rawInputText = it
                        if (it.isNotBlank()) {
                            parsePreview = SpreadsheetImporter.parseSpreadsheet(it, lojaId)
                        } else {
                            parsePreview = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 180.dp),
                    placeholder = {
                        Text(
                            "PLU;Descricao;Quantidade\n78912345;Arroz Branco 5kg;50\n78954321;Feijão Carioca 1kg;30",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = DarkTextSecondary.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = DarkTextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceContainerHigh,
                        unfocusedContainerColor = DarkSurfaceContainer,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    )
                )

                // Prévia da Importação
                parsePreview?.let { preview ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSafe,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Prévia: ${preview.successProducts.size} produto(s) identificados",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = EmeraldSafe,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preview.detectedColumns,
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                            if (preview.ignoredLines > 0) {
                                Text(
                                    text = "Linhas em branco/ignoradas: ${preview.ignoredLines}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF59E0B),
                                    fontSize = 10.sp
                                )
                            }

                            // Mostra até 3 itens de exemplo
                            Spacer(modifier = Modifier.height(8.dp))
                            preview.successProducts.take(3).forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${p.name.take(24)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkTextPrimary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${p.quantity} un",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BlueExpressive,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                errorMessage?.let { err ->
                    Text(
                        text = "⚠️ $err",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val preview = parsePreview ?: SpreadsheetImporter.parseSpreadsheet(rawInputText, lojaId)
                    if (preview.successProducts.isNotEmpty()) {
                        onImportSuccess(rawInputText, preview.successProducts.size)
                        onDismiss()
                    } else {
                        errorMessage = "Nenhum produto válido detectado para importar."
                    }
                },
                enabled = (parsePreview?.successProducts?.isNotEmpty() == true) || rawInputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
            ) {
                Text("Confirmar Importação (${parsePreview?.successProducts?.size ?: 0})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = DarkTextSecondary)
            }
        }
    )
}
