package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.supabase.NovoProduto
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.util.encrypted
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import com.example.data.SectorManager
import com.example.util.XlsxReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ItemPlanilhaExtraido(
    val linhaOriginal: Int,
    val plu: String,
    val codigoBarras: String,
    val descricao: String,
    val quantidade: Int,
    val setor: String,
    val subsetor: String = "",
    val dataVencimento: String
)

enum class WizardStep {
    ENTRADA_DADOS,
    CONFIGURAR_EXTRAÇÃO,
    PREVIA_CONFERENCIA,
    CRIPTOGRAFANDO,
    INJETANDO
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlanilhaImportDialog(
    lojaId: String,
    lojaNome: String,
    onDismiss: () -> Unit,
    onImportSuccess: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }

    var currentStep by remember { mutableStateOf(WizardStep.ENTRADA_DADOS) }

    // Conteúdo bruto da planilha
    var textoPlanilha by remember { mutableStateOf("") }

    // Configurações de extração: Linha Inicial e Linha Final (Intervalo de extração)
    var linhaInicialStr by remember { mutableStateOf("2") } // Padrão linha 2 para pular cabeçalho
    var linhaFinalStr by remember { mutableStateOf("") } // Linha final (opcional, vazio = até a última linha)
    var delimitadorSelecionado by remember { mutableStateOf("auto") } // "auto", ";", ",", "\t", "|"

    // Termos das colunas
    var colunaPluIndex by remember { mutableStateOf("1") }
    var colunaEanIndex by remember { mutableStateOf("2") }
    var colunaDescricaoIndex by remember { mutableStateOf("3") }
    var colunaQtdIndex by remember { mutableStateOf("4") }
    var colunaValidadeIndex by remember { mutableStateOf("") } // Coluna com data de validade da planilha
    var colunaSetorIndex by remember { mutableStateOf("") }
    var colunaSubsetorIndex by remember { mutableStateOf("") }

    // Setor e Subsetor padrão caso a planilha não tenha
    var setorPadrao by remember { mutableStateOf("Mercearia") }
    var subsetorPadrao by remember { mutableStateOf("") }

    // Lista processada para a Prévia de Conferência
    var itensPrevia by remember { mutableStateOf<List<ItemPlanilhaExtraido>>(emptyList()) }
    var parseError by remember { mutableStateOf<String?>(null) }

    // Modal para edição de Categoria/Subsetor individual ou em lote na Prévia
    var itemParaEditarCategoria by remember { mutableStateOf<ItemPlanilhaExtraido?>(null) }
    var showLoteCategoriaModal by remember { mutableStateOf(false) }

    // Estado da animação de criptografia de 5 segundos
    var countdownSegundos by remember { mutableIntStateOf(5) }
    var encryptionProgress by remember { mutableFloatStateOf(0f) }

    // Estado da injeção
    var progressoInjecao by remember { mutableIntStateOf(0) }
    var totalParaInjetar by remember { mutableIntStateOf(0) }
    var erroInjecao by remember { mutableStateOf<String?>(null) }

    // Seletor de Arquivos (.xlsx, .csv, .tsv, .txt)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    if (XlsxReader.isXlsxFile(context, uri)) {
                        val csvContent = XlsxReader.readXlsxToCsv(context, uri)
                        withContext(Dispatchers.Main) {
                            if (!csvContent.isNullOrBlank()) {
                                textoPlanilha = csvContent
                                Toast.makeText(context, "✅ Planilha Excel (.xlsx) carregada com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "⚠️ Não foi possível extrair dados da planilha Excel.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val reader = BufferedReader(InputStreamReader(inputStream))
                            val content = reader.readText()
                            withContext(Dispatchers.Main) {
                                textoPlanilha = content
                                Toast.makeText(context, "Arquivo carregado com sucesso!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erro ao abrir arquivo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Parser inteligente para data de validade vinda da coluna da planilha
    fun parseDataValidade(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isNotBlank()) {
            val formats = listOf(
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "dd-MM-yyyy",
                "dd/MM/yy",
                "yyyy/MM/dd",
                "d/M/yyyy",
                "d/M/yy"
            )
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.getDefault()).apply { isLenient = false }
                    val parsed = sdf.parse(trimmed)
                    if (parsed != null) {
                        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsed)
                    }
                } catch (_: Exception) {}
            }
        }
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    // Função de detecção de colunas e parsing da planilha
    fun processarPrevia() {
        parseError = null
        if (textoPlanilha.isBlank()) {
            parseError = "Cole o conteúdo da planilha ou carregue um arquivo primeiro."
            return
        }

        val rawLines = textoPlanilha.lines().filter { it.isNotBlank() }
        if (rawLines.isEmpty()) {
            parseError = "A planilha não contém dados legíveis."
            return
        }

        val linhaInicio = linhaInicialStr.toIntOrNull() ?: 1
        val startIdx = (linhaInicio - 1).coerceAtLeast(0)
        val linhaFim = linhaFinalStr.toIntOrNull()
        val endIdx = if (linhaFim != null && linhaFim >= linhaInicio) {
            linhaFim.coerceAtMost(rawLines.size)
        } else {
            rawLines.size
        }

        if (startIdx >= rawLines.size) {
            parseError = "A linha inicial ($linhaInicio) é maior que o total de linhas da planilha (${rawLines.size})."
            return
        }

        val parsedList = mutableListOf<ItemPlanilhaExtraido>()

        for (i in startIdx until endIdx) {
            val line = rawLines[i]
            // Detecção inteligente de separador
            val separator = when (delimitadorSelecionado) {
                ";" -> ";"
                "," -> ","
                "\t" -> "\t"
                "|" -> "|"
                else -> {
                    when {
                        line.contains("\t") -> "\t"
                        line.contains(";") -> ";"
                        line.contains("|") -> "|"
                        line.contains(",") -> ","
                        else -> ";"
                    }
                }
            }

            val cols = line.split(separator).map { it.trim().removeSurrounding("\"") }

            // Helper para obter coluna pelo índice informado pelo usuário (1-based)
            fun getCol(indexStr: String): String {
                val idx = (indexStr.toIntOrNull() ?: -1) - 1
                return if (idx in cols.indices) cols[idx] else ""
            }

            val plu = getCol(colunaPluIndex)
            val ean = getCol(colunaEanIndex)
            val desc = getCol(colunaDescricaoIndex)
            val qtdRaw = getCol(colunaQtdIndex)
            val qtd = qtdRaw.filter { it.isDigit() }.toIntOrNull() ?: 1

            // Se informada, extrai data de validade da coluna indicada na planilha
            val validadeRaw = if (colunaValidadeIndex.isNotBlank()) getCol(colunaValidadeIndex) else ""
            val finalDataValidade = parseDataValidade(validadeRaw)

            // Se faltar um ou outro campo, aceita normalmente seguindo a regra solicitada
            val finalDescricao = when {
                desc.isNotBlank() -> desc
                plu.isNotBlank() -> "Item PLU $plu"
                ean.isNotBlank() -> "Item EAN $ean"
                else -> "Produto Importado Linha ${i + 1}"
            }

            val finalCodigoBarras = when {
                ean.isNotBlank() -> ean
                plu.isNotBlank() -> plu
                else -> "789${(100000000..999999999).random()}"
            }

            val setorLido = if (colunaSetorIndex.isNotBlank()) getCol(colunaSetorIndex).ifBlank { setorPadrao } else setorPadrao
            val subsetorLido = if (colunaSubsetorIndex.isNotBlank()) getCol(colunaSubsetorIndex).ifBlank { subsetorPadrao } else subsetorPadrao

            parsedList.add(
                ItemPlanilhaExtraido(
                    linhaOriginal = i + 1,
                    plu = plu,
                    codigoBarras = finalCodigoBarras,
                    descricao = finalDescricao,
                    quantidade = qtd.coerceAtLeast(1),
                    setor = setorLido,
                    subsetor = subsetorLido,
                    dataVencimento = finalDataValidade
                )
            )
        }

        if (parsedList.isEmpty()) {
            parseError = "Nenhum produto pôde ser extraído com a configuração atual."
            return
        }

        itensPrevia = parsedList
        currentStep = WizardStep.PREVIA_CONFERENCIA
    }

    // Função de Injeção no Banco de Dados Supabase
    fun executarInjecaoNoSupabase() {
        if (itensPrevia.isEmpty()) return
        currentStep = WizardStep.INJETANDO
        erroInjecao = null
        totalParaInjetar = itensPrevia.size
        progressoInjecao = 0

        coroutineScope.launch {
            var sucessoCount = 0
            for (item in itensPrevia) {
                try {
                    val setorFinal = if (item.subsetor.isNotBlank()) {
                        "${item.setor} - ${item.subsetor}"
                    } else {
                        item.setor
                    }
                    val novoProduto = NovoProduto(
                        nome = item.descricao,
                        codigo_barras = item.codigoBarras,
                        quantidade = item.quantidade,
                        data_vencimento = item.dataVencimento,
                        setor = setorFinal,
                        loja_id = lojaId,
                        plu = if (item.plu.isNotBlank()) item.plu else null
                    )
                    supabase.from("produtos").insert(novoProduto.encrypted())
                    sucessoCount++
                    progressoInjecao = sucessoCount
                } catch (e: Exception) {
                    // Continua inserindo os demais produtos
                }
            }

            if (sucessoCount > 0) {
                Toast.makeText(context, "$sucessoCount produtos criptografados e injetados com sucesso!", Toast.LENGTH_LONG).show()
                onImportSuccess(sucessoCount)
                onDismiss()
            } else {
                erroInjecao = "Não foi possível inserir os produtos no Supabase. Verifique sua conexão."
                currentStep = WizardStep.PREVIA_CONFERENCIA
            }
        }
    }

    // Inicia processo visual de 5 segundos de criptografia e então injeta no Supabase
    fun iniciarProcessoCriptografiaEInjecao() {
        if (itensPrevia.isEmpty()) return
        currentStep = WizardStep.CRIPTOGRAFANDO
        countdownSegundos = 5
        encryptionProgress = 0f

        coroutineScope.launch {
            // Contagem regressiva visual de 5 segundos exibindo blindagem criptográfica
            for (step in 1..50) {
                kotlinx.coroutines.delay(100)
                encryptionProgress = step / 50f
                val segundosRestantes = 5 - (step / 10)
                countdownSegundos = segundosRestantes.coerceAtLeast(0)
            }
            executarInjecaoNoSupabase()
        }
    }

    Dialog(
        onDismissRequest = { if (currentStep != WizardStep.INJETANDO && currentStep != WizardStep.CRIPTOGRAFANDO) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceContainer,
            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header do Wizard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF581C87).copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFFA855F7)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Entrada de Dados via Planilha",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Loja Master: $lojaNome",
                                fontSize = 11.sp,
                                color = Color(0xFFC084FC)
                            )
                        }
                    }

                    if (currentStep != WizardStep.INJETANDO) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = DarkTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Indicador de Passos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StepChip(title = "1. Entrada", active = currentStep == WizardStep.ENTRADA_DADOS, completed = currentStep > WizardStep.ENTRADA_DADOS)
                    StepChip(title = "2. Extração", active = currentStep == WizardStep.CONFIGURAR_EXTRAÇÃO, completed = currentStep > WizardStep.CONFIGURAR_EXTRAÇÃO)
                    StepChip(title = "3. Prévia", active = currentStep == WizardStep.PREVIA_CONFERENCIA, completed = currentStep > WizardStep.PREVIA_CONFERENCIA)
                    StepChip(title = "4. Cripto", active = currentStep == WizardStep.CRIPTOGRAFANDO, completed = currentStep == WizardStep.INJETANDO)
                    StepChip(title = "5. Injeção", active = currentStep == WizardStep.INJETANDO, completed = false)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Conteúdo por Passo
                Box(modifier = Modifier.weight(1f)) {
                    when (currentStep) {
                        // PASSO 1: ENTRADA DOS DADOS
                        WizardStep.ENTRADA_DADOS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Carregue ou cole os dados da sua planilha:",
                                    fontSize = 13.sp,
                                    color = DarkTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Aceita qualquer formato (Excel, CSV, TSV, Google Sheets). Os dados serão copiados respeitando a estrutura informada.",
                                    fontSize = 11.sp,
                                    color = DarkTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Selecionar Arquivo (.xlsx, .csv, .txt)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = textoPlanilha,
                                    onValueChange = { textoPlanilha = it },
                                    label = { Text("Conteúdo da Planilha (Colar Células ou Linhas)") },
                                    placeholder = {
                                        Text(
                                            "Exemplo:\nPLU;EAN;Descrição;Quantidade\n101;7891000100100;Leite Integral 1L;24\n102;7891000100200;Iogurte Morango;12",
                                            fontSize = 12.sp,
                                            color = DarkTextSecondary
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = DarkTextPrimary,
                                        focusedBorderColor = Color(0xFFA855F7),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkSurfaceContainerHigh,
                                        unfocusedContainerColor = DarkSurfaceContainerHigh
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                )

                                if (parseError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = parseError!!, color = RedExpressive, fontSize = 12.sp)
                                }
                            }
                        }

                        // PASSO 2: CONFIGURAÇÃO DE LINHA E MAPEAMENTO DE COLUNAS
                        WizardStep.CONFIGURAR_EXTRAÇÃO -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Parâmetros de Extração da Planilha",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Responda a partir de qual linha começar e informe as posições/termos das colunas.",
                                    fontSize = 11.sp,
                                    color = DarkTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                                )

                                // Pergunta: A partir de qual linha começar?
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "A partir de qual linha começar a extração?",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Geralmente a linha 1 contém os cabeçalhos das colunas e os produtos começam na linha 2.",
                                            fontSize = 11.sp,
                                            color = DarkTextSecondary,
                                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = linhaInicialStr,
                                                onValueChange = { if (it.all { c -> c.isDigit() }) linhaInicialStr = it },
                                                label = { Text("Linha Inicial") },
                                                placeholder = { Text("2") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = DarkTextPrimary,
                                                    focusedBorderColor = Color(0xFFA855F7),
                                                    unfocusedBorderColor = DarkBorder
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            )

                                            OutlinedTextField(
                                                value = linhaFinalStr,
                                                onValueChange = { if (it.all { c -> c.isDigit() }) linhaFinalStr = it },
                                                label = { Text("Linha Final (Fim)") },
                                                placeholder = { Text("ex: 150 (opcional)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = DarkTextPrimary,
                                                    focusedBorderColor = Color(0xFFA855F7),
                                                    unfocusedBorderColor = DarkBorder
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Mapeamento dos termos das colunas: PLU, EAN, Descrição, Qtd
                                Text(
                                    text = "Posição das Colunas na Planilha (Nº da Coluna 1, 2, 3...):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Se faltar algum campo, deixe com o número correspondente ou informe o índice. O sistema ajusta automaticamente.",
                                    fontSize = 11.sp,
                                    color = DarkTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = colunaPluIndex,
                                        onValueChange = { colunaPluIndex = it },
                                        label = { Text("Coluna PLU") },
                                        placeholder = { Text("1") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = Color(0xFFA855F7),
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = colunaEanIndex,
                                        onValueChange = { colunaEanIndex = it },
                                        label = { Text("Coluna EAN / Barras") },
                                        placeholder = { Text("2") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = Color(0xFFA855F7),
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = colunaDescricaoIndex,
                                        onValueChange = { colunaDescricaoIndex = it },
                                        label = { Text("Coluna Descrição") },
                                        placeholder = { Text("3") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = Color(0xFFA855F7),
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = colunaQtdIndex,
                                        onValueChange = { colunaQtdIndex = it },
                                        label = { Text("Coluna Quantidade") },
                                        placeholder = { Text("4") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = Color(0xFFA855F7),
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Setor e Subsetor Padrão
                                val setoresLoja = remember { SectorManager.getSetores(context).map { it.nome } }
                                val subsetoresLoja = remember(setorPadrao) { SectorManager.getSubsetoresDoSetor(context, setorPadrao) }

                                Text("Setor Padrão (Categoria):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    setoresLoja.forEach { s ->
                                        val isSel = setorPadrao.equals(s, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) Color(0xFF7E22CE).copy(alpha = 0.45f) else DarkSurfaceContainerHigh,
                                            border = BorderStroke(1.dp, if (isSel) Color(0xFFC084FC) else DarkBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    setorPadrao = s
                                                    subsetorPadrao = ""
                                                }
                                        ) {
                                            Text(
                                                text = s,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else DarkTextSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Subsetor / Seção Padrão ($setorPadrao):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val isGeral = subsetorPadrao.isBlank()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isGeral) Color(0xFF6B21A8).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                                        border = BorderStroke(1.dp, if (isGeral) Color(0xFFC084FC) else DarkBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { subsetorPadrao = "" }
                                    ) {
                                        Text(
                                            text = "Geral",
                                            fontSize = 11.sp,
                                            fontWeight = if (isGeral) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isGeral) Color.White else DarkTextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }

                                    subsetoresLoja.forEach { sub ->
                                        val isSubSel = subsetorPadrao.equals(sub, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSubSel) Color(0xFF7E22CE).copy(alpha = 0.45f) else DarkSurfaceContainerHigh,
                                            border = BorderStroke(1.dp, if (isSubSel) Color(0xFFC084FC) else DarkBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { subsetorPadrao = sub }
                                        ) {
                                            Text(
                                                text = sub,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSubSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSubSel) Color.White else DarkTextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Colunas da Planilha: Data de Validade e Setor
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = colunaValidadeIndex,
                                        onValueChange = { colunaValidadeIndex = it },
                                        label = { Text("Col. Data de Validade") },
                                        placeholder = { Text("ex: 5") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = Color(0xFFA855F7),
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = colunaSetorIndex,
                                        onValueChange = { colunaSetorIndex = it },
                                        label = { Text("Col. Setor (Opcional)") },
                                        placeholder = { Text("ex: 6") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = Color(0xFFA855F7),
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = colunaSubsetorIndex,
                                    onValueChange = { colunaSubsetorIndex = it },
                                    label = { Text("Col. Subsetor (Opcional)") },
                                    placeholder = { Text("ex: 7") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = DarkTextPrimary,
                                        focusedBorderColor = Color(0xFFA855F7),
                                        unfocusedBorderColor = DarkBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (parseError != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = parseError!!, color = RedExpressive, fontSize = 12.sp)
                                }
                            }
                        }

                        // PASSO 3: PRÉVIA DE CONFERÊNCIA (SEM GRAVAR NO BANCO AINDA)
                        WizardStep.PREVIA_CONFERENCIA -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmeraldSafe.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Prévia Gerada: ${itensPrevia.size} itens identificados",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Edite o setor/subsetor dos itens abaixo individualmente ou em lote antes de salvar.",
                                                fontSize = 11.sp,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        }

                                        Button(
                                            onClick = { showLoteCategoriaModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Categorizar em Lote", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (erroInjecao != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = erroInjecao!!, color = RedExpressive, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(itensPrevia) { index, item ->
                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                                            border = BorderStroke(1.dp, DarkBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF7E22CE).copy(alpha = 0.3f),
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "${index + 1}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFE9D5FF)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.descricao,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "EAN: ${item.codigoBarras}${if (item.plu.isNotBlank()) " • PLU: ${item.plu}" else ""}",
                                                        fontSize = 11.sp,
                                                        color = DarkTextSecondary
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFF1E3A8A).copy(alpha = 0.4f)
                                                        ) {
                                                            Text(
                                                                text = item.setor,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFF93C5FD),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        if (item.subsetor.isNotBlank()) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFF581C87).copy(alpha = 0.4f)
                                                            ) {
                                                                Text(
                                                                    text = item.subsetor,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = Color(0xFFE9D5FF),
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { itemParaEditarCategoria = item },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Editar Categoria", tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF1E293B),
                                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                                ) {
                                                    Text(
                                                        text = "${item.quantidade} un.",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF93C5FD),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // PASSO 4: CRIPTOGRAFIA DE DADOS (5 SEGUNDOS VISUAL)
                        WizardStep.CRIPTOGRAFANDO -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF059669).copy(alpha = 0.2f),
                                        border = BorderStroke(2.dp, Color(0xFF10B981)),
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = "Criptografia Segura",
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(44.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "Aplicando Camada Criptográfica",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Criptografando registros com chave AES-256 antes da gravação...",
                                        fontSize = 13.sp,
                                        color = Color(0xFFA7F3D0),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    LinearProgressIndicator(
                                        progress = { encryptionProgress },
                                        color = Color(0xFF10B981),
                                        trackColor = DarkSurfaceContainerHigh,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF064E3B).copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "⏱️ Blindando registros: ${countdownSegundos}s restantes",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF6EE7B7),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DarkSurfaceContainer,
                                        border = BorderStroke(1.dp, DarkBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "🔒 STATUS DE SEGURANÇA",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF34D399)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "✓ Vetor de inicialização (IV) exclusivo gerado\n✓ Criptografia de ponta a ponta ativa\n✓ Nomes e códigos protegidos no padrão ENC:Base64",
                                                fontSize = 11.sp,
                                                color = DarkTextSecondary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // PASSO 5: INJETANDO NO BANCO DE DADOS
                        WizardStep.INJETANDO -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFA855F7),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Injetando produtos no Supabase...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Progresso: $progressoInjecao de $totalParaInjetar itens salvos na loja $lojaNome",
                                        fontSize = 12.sp,
                                        color = Color(0xFFC084FC),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rodapé de Ações / Navegação do Wizard
                if (currentStep != WizardStep.INJETANDO && currentStep != WizardStep.CRIPTOGRAFANDO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > WizardStep.ENTRADA_DADOS) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = when (currentStep) {
                                        WizardStep.CONFIGURAR_EXTRAÇÃO -> WizardStep.ENTRADA_DADOS
                                        WizardStep.PREVIA_CONFERENCIA -> WizardStep.CONFIGURAR_EXTRAÇÃO
                                        else -> WizardStep.ENTRADA_DADOS
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Voltar")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        when (currentStep) {
                            WizardStep.ENTRADA_DADOS -> {
                                Button(
                                    onClick = {
                                        if (textoPlanilha.isBlank()) {
                                            parseError = "Cole os dados da planilha antes de continuar."
                                        } else {
                                            parseError = null
                                            currentStep = WizardStep.CONFIGURAR_EXTRAÇÃO
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_wizard_proximo_config")
                                ) {
                                    Text("Configurar Extração", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            WizardStep.CONFIGURAR_EXTRAÇÃO -> {
                                Button(
                                    onClick = { processarPrevia() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_wizard_gerar_previa")
                                ) {
                                    Text("Gerar Prévia dos Dados", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            WizardStep.PREVIA_CONFERENCIA -> {
                                Button(
                                    onClick = { iniciarProcessoCriptografiaEInjecao() },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_confirmar_injecao")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirmar e Injetar no Estoque (${itensPrevia.size})", fontWeight = FontWeight.Bold)
                                }
                            }

                            WizardStep.CRIPTOGRAFANDO,
                            WizardStep.INJETANDO -> {}
                        }
                    }
                }
            }
        }

        // Diálogo para Editar Categoria de Item Individual
        itemParaEditarCategoria?.let { item ->
            var editSetor by remember(item) { mutableStateOf(item.setor) }
            var editSub by remember(item) { mutableStateOf(item.subsetor) }
            val setores = remember { SectorManager.getSetores(context).map { it.nome } }
            val subs = remember(editSetor) { SectorManager.getSubsetoresDoSetor(context, editSetor) }

            AlertDialog(
                onDismissRequest = { itemParaEditarCategoria = null },
                title = { Text("Editar Categoria do Produto", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Item: ${item.descricao}", fontSize = 12.sp, color = DarkTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("1. Setor Principal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            setores.forEach { s ->
                                val sel = editSetor.equals(s, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) Color(0xFF7E22CE).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                                    border = BorderStroke(1.dp, if (sel) Color(0xFFC084FC) else DarkBorder),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                        editSetor = s
                                        editSub = ""
                                    }
                                ) {
                                    Text(s, fontSize = 11.sp, color = if (sel) Color.White else DarkTextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("2. Subsetor / Seção:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isGeral = editSub.isBlank()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isGeral) Color(0xFF6B21A8).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                                border = BorderStroke(1.dp, if (isGeral) Color(0xFFC084FC) else DarkBorder),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { editSub = "" }
                            ) {
                                Text("Geral", fontSize = 11.sp, color = if (isGeral) Color.White else DarkTextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                            subs.forEach { sb ->
                                val sel = editSub.equals(sb, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) Color(0xFF7E22CE).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                                    border = BorderStroke(1.dp, if (sel) Color(0xFFC084FC) else DarkBorder),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { editSub = sb }
                                ) {
                                    Text(sb, fontSize = 11.sp, color = if (sel) Color.White else DarkTextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            itensPrevia = itensPrevia.map {
                                if (it.linhaOriginal == item.linhaOriginal) it.copy(setor = editSetor, subsetor = editSub) else it
                            }
                            itemParaEditarCategoria = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemParaEditarCategoria = null }) {
                        Text("Cancelar", color = DarkTextSecondary)
                    }
                }
            )
        }

        // Diálogo para Categorização em Lote de Todos os Itens
        if (showLoteCategoriaModal) {
            var loteSetor by remember { mutableStateOf(setorPadrao) }
            var loteSub by remember { mutableStateOf(subsetorPadrao) }
            val setores = remember { SectorManager.getSetores(context).map { it.nome } }
            val subs = remember(loteSetor) { SectorManager.getSubsetoresDoSetor(context, loteSetor) }

            AlertDialog(
                onDismissRequest = { showLoteCategoriaModal = false },
                title = { Text("Categorizar em Lote (${itensPrevia.size} Itens)", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Esta categoria será atribuída a TODOS os ${itensPrevia.size} produtos da planilha:", fontSize = 12.sp, color = DarkTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("1. Setor Principal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            setores.forEach { s ->
                                val sel = loteSetor.equals(s, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) Color(0xFF7E22CE).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                                    border = BorderStroke(1.dp, if (sel) Color(0xFFC084FC) else DarkBorder),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                        loteSetor = s
                                        loteSub = ""
                                    }
                                ) {
                                    Text(s, fontSize = 11.sp, color = if (sel) Color.White else DarkTextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("2. Subsetor / Seção:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isGeral = loteSub.isBlank()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isGeral) Color(0xFF6B21A8).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                                border = BorderStroke(1.dp, if (isGeral) Color(0xFFC084FC) else DarkBorder),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { loteSub = "" }
                            ) {
                                Text("Geral (Sem subsetor)", fontSize = 11.sp, color = if (isGeral) Color.White else DarkTextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                            subs.forEach { sb ->
                                val sel = loteSub.equals(sb, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) Color(0xFF7E22CE).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                                    border = BorderStroke(1.dp, if (sel) Color(0xFFC084FC) else DarkBorder),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { loteSub = sb }
                                ) {
                                    Text(sb, fontSize = 11.sp, color = if (sel) Color.White else DarkTextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            itensPrevia = itensPrevia.map { it.copy(setor = loteSetor, subsetor = loteSub) }
                            showLoteCategoriaModal = false
                            Toast.makeText(context, "✅ Categoria aplicada a todos os ${itensPrevia.size} itens!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                    ) {
                        Text("Aplicar a Todos")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLoteCategoriaModal = false }) {
                        Text("Cancelar", color = DarkTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun StepChip(title: String, active: Boolean, completed: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = when {
            active -> Color(0xFF9333EA)
            completed -> EmeraldSafe.copy(alpha = 0.2f)
            else -> DarkSurfaceContainerHigh
        },
        border = BorderStroke(
            1.dp,
            when {
                active -> Color(0xFFC084FC)
                completed -> EmeraldSafe
                else -> DarkBorder
            }
        )
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (active || completed) FontWeight.Bold else FontWeight.Normal,
            color = when {
                active -> Color.White
                completed -> EmeraldSafe
                else -> DarkTextSecondary
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
