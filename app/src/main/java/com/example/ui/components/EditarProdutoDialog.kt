package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SectorManager
import com.example.data.StockLockManager
import com.example.data.supabase.LogManager
import com.example.data.supabase.Produto
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.util.encrypted
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.util.BarcodeScannerHelper
import com.example.util.SoundFeedbackHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditarProdutoDialog(
    produto: Produto,
    onDismiss: () -> Unit,
    onProdutoUpdated: (Produto) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = SessionHolder.currentUser

    var nome by remember { mutableStateOf(produto.nome) }
    var codigoBarras by remember { mutableStateOf(produto.codigo_barras) }
    var plu by remember { mutableStateOf(produto.plu ?: "") }
    var quantidadeStr by remember { mutableStateOf(produto.quantidade.toString()) }
    var dataVencimento by remember { mutableStateOf(produto.data_vencimento) }

    // Divide setor atual em Setor Principal e Subsetor
    val partesSetor = remember(produto.setor) {
        when {
            produto.setor.contains(" - ") -> produto.setor.split(" - ", limit = 2)
            produto.setor.contains(" / ") -> produto.setor.split(" / ", limit = 2)
            produto.setor.contains(" > ") -> produto.setor.split(" > ", limit = 2)
            else -> listOf(produto.setor)
        }
    }
    var setorSelecionado by remember { mutableStateOf(partesSetor.firstOrNull()?.trim() ?: "Geral") }
    var subsetorSelecionado by remember { mutableStateOf(if (partesSetor.size > 1) partesSetor[1].trim() else "") }
    var novoSubsetorTexto by remember { mutableStateOf("") }
    var showNovoSubsetorInput by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var erroValidacao by remember { mutableStateOf<String?>(null) }

    val lockedMap by StockLockManager.lockedProducts.collectAsState()
    val lockByOther = remember(lockedMap, produto.id) {
        StockLockManager.getLockByOther(produto.id, currentUser?.matricula ?: "")
    }

    DisposableEffect(produto.id) {
        if (currentUser != null && lockByOther == null) {
            coroutineScope.launch {
                StockLockManager.lockProduct(produto.id, currentUser, currentUser.loja_id)
            }
        }
        onDispose {
            if (currentUser != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    StockLockManager.unlockProduct(produto.id, currentUser, currentUser.loja_id)
                }
            }
        }
    }

    // Setores disponíveis cadastrados na loja
    val setoresDisponiveis = remember {
        val daLoja = SectorManager.getSetores(context).map { it.nome }
        if (daLoja.isEmpty()) {
            listOf("Fiambreria", "Açougue", "Padaria", "Hortifruti", "Laticínios", "Mercearia", "Congelados", "Geral")
        } else {
            daLoja
        }
    }

    // Seletor de Data Android Nativo
    val calendar = remember {
        Calendar.getInstance().apply {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(produto.data_vencimento)
                if (parsed != null) time = parsed
            } catch (_: Exception) {}
        }
    }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dataVencimento = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("dialog_editar_produto")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Topo com Título e Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Editar Produto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Atualize os dados no estoque",
                                fontSize = 11.sp,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }
                    IconButton(
                        onClick = { if (!isSaving) onDismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Aviso de bloqueio por outro usuário
                if (lockByOther != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔒 Aberto por ${lockByOther.usuarioNome}. Edição bloqueada para evitar sobrescrita.",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Mensagem de erro de validação
                if (erroValidacao != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = erroValidacao!!,
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Nome do Produto
                OutlinedTextField(
                    value = nome,
                    onValueChange = {
                        nome = it
                        erroValidacao = null
                    },
                    label = { Text("Nome da Mercadoria") },
                    leadingIcon = {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF60A5FA))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_nome_produto")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Código de Barras (EAN) com botão de Scanner
                OutlinedTextField(
                    value = codigoBarras,
                    onValueChange = {
                        codigoBarras = it
                        erroValidacao = null
                    },
                    label = { Text("Código de Barras (EAN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = DarkTextSecondary)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            BarcodeScannerHelper.startScan(
                                context = context,
                                onSuccess = { scanned -> codigoBarras = scanned }
                            )
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear", tint = Color(0xFF60A5FA))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_ean_produto")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Linha com PLU e Quantidade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = plu,
                        onValueChange = { plu = it },
                        label = { Text("PLU (Balança)") },
                        placeholder = { Text("Opcional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = DarkBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_edit_plu_produto")
                    )

                    OutlinedTextField(
                        value = quantidadeStr,
                        onValueChange = {
                            quantidadeStr = it
                            erroValidacao = null
                        },
                        label = { Text("Quantidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = DarkBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_edit_quantidade_produto")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Data de Vencimento
                OutlinedTextField(
                    value = dataVencimento,
                    onValueChange = {
                        dataVencimento = it
                        erroValidacao = null
                    },
                    label = { Text("Data de Vencimento (AAAA-MM-DD)") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFF59E0B))
                    },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar Data", tint = Color(0xFFF59E0B))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_vencimento_produto")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Seletor de Setor Principal (Categoria)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1. Setor Principal (Categoria):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    setoresDisponiveis.forEach { s ->
                        val selecionado = setorSelecionado.equals(s, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selecionado) Color(0xFF2563EB).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                if (selecionado) Color(0xFF3B82F6) else DarkBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    setorSelecionado = s
                                    subsetorSelecionado = "" // reseta subsetor ao trocar setor principal
                                }
                        ) {
                            Text(
                                text = s,
                                fontSize = 11.sp,
                                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                                color = if (selecionado) Color.White else DarkTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Seletor de Subsetor (Seção Específica)
                val subsetoresDoSetor = remember(setorSelecionado) {
                    SectorManager.getSubsetoresDoSetor(context, setorSelecionado)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2. Subsetor / Seção:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = { showNovoSubsetorInput = !showNovoSubsetorInput },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo Subsetor", fontSize = 11.sp, color = Color(0xFFC084FC))
                    }
                }

                if (showNovoSubsetorInput) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = novoSubsetorTexto,
                            onValueChange = { novoSubsetorTexto = it },
                            placeholder = { Text("Nome do novo subsetor...", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = Color(0xFFC084FC),
                                unfocusedBorderColor = DarkBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clean = novoSubsetorTexto.trim()
                                if (clean.isNotBlank()) {
                                    SectorManager.adicionarSubsetor(context, setorSelecionado, clean)
                                    subsetorSelecionado = clean
                                    novoSubsetorTexto = ""
                                    showNovoSubsetorInput = false
                                    Toast.makeText(context, "Subsetor '$clean' adicionado a $setorSelecionado!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Adicionar", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Opção "Nenhum / Geral"
                    val isNenhum = subsetorSelecionado.isBlank()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isNenhum) Color(0xFF6B21A8).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                        border = BorderStroke(1.dp, if (isNenhum) Color(0xFFC084FC) else DarkBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { subsetorSelecionado = "" }
                    ) {
                        Text(
                            text = "Geral (Sem subsetor)",
                            fontSize = 11.sp,
                            fontWeight = if (isNenhum) FontWeight.Bold else FontWeight.Normal,
                            color = if (isNenhum) Color.White else DarkTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    subsetoresDoSetor.forEach { sub ->
                        val subSelecionado = subsetorSelecionado.equals(sub, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (subSelecionado) Color(0xFF7E22CE).copy(alpha = 0.45f) else DarkSurfaceContainerHigh,
                            border = BorderStroke(1.dp, if (subSelecionado) Color(0xFFC084FC) else DarkBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { subsetorSelecionado = sub }
                        ) {
                            Text(
                                text = sub,
                                fontSize = 11.sp,
                                fontWeight = if (subSelecionado) FontWeight.Bold else FontWeight.Normal,
                                color = if (subSelecionado) Color.White else DarkTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botões de Ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (!isSaving) onDismiss() },
                        enabled = !isSaving
                    ) {
                        Text("Cancelar", color = DarkTextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val nomeTrim = nome.trim()
                            val eanTrim = codigoBarras.trim()
                            val qtd = quantidadeStr.trim().toIntOrNull()
                            val dataTrim = dataVencimento.trim()

                            if (nomeTrim.isEmpty()) {
                                erroValidacao = "Informe o nome do produto."
                                return@Button
                            }
                            if (eanTrim.isEmpty()) {
                                erroValidacao = "Informe o código de barras."
                                return@Button
                            }
                            if (qtd == null || qtd < 0) {
                                erroValidacao = "Quantidade inválida."
                                return@Button
                            }
                            if (dataTrim.isEmpty() || !dataTrim.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                erroValidacao = "Data de vencimento deve estar no formato AAAA-MM-DD (ex: 2026-10-31)."
                                return@Button
                            }

                            // Validação de permissão de setor
                            if (currentUser != null && !com.example.data.CargoManager.podeRealizarAcaoNoSetor(currentUser, setorSelecionado)) {
                                val setorPermitido = com.example.data.CargoManager.resolveUserSector(currentUser)
                                erroValidacao = "Você só pode alocar produtos no seu setor atribuído ($setorPermitido)."
                                return@Button
                            }

                            val setorFinal = if (subsetorSelecionado.isNotBlank()) {
                                "$setorSelecionado - ${subsetorSelecionado.trim()}"
                            } else {
                                setorSelecionado
                            }

                            isSaving = true
                            coroutineScope.launch {
                                val produtoAtualizado = produto.copy(
                                    nome = nomeTrim,
                                    codigo_barras = eanTrim,
                                    plu = if (plu.isNotBlank()) plu.trim() else null,
                                    quantidade = qtd,
                                    data_vencimento = dataTrim,
                                    setor = setorFinal
                                )

                                try {
                                    SupabaseClient.instance
                                        .from("produtos")
                                        .update(produtoAtualizado.encrypted()) {
                                            eq("id", produto.id)
                                        }

                                    LogManager.recordLog(
                                        usuarioMatricula = currentUser?.matricula ?: "000000",
                                        usuarioNome = currentUser?.nome ?: "Usuário",
                                        lojaId = currentUser?.loja_id ?: "",
                                        acao = "EDICAO_PRODUTO",
                                        detalhes = "Editou produto '${produtoAtualizado.nome}' (EAN: ${produtoAtualizado.codigo_barras}, Qtd: ${produtoAtualizado.quantidade}, Venc: ${produtoAtualizado.data_vencimento}, Categoria: ${produtoAtualizado.setor})"
                                    )

                                    SoundFeedbackHelper.playSuccessBeep(context)
                                    Toast.makeText(context, "Produto \"$nomeTrim\" atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                                    onProdutoUpdated(produtoAtualizado)
                                    onDismiss()
                                } catch (e: Exception) {
                                    erroValidacao = "Erro ao salvar no banco: ${e.message}"
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving && lockByOther == null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_salvar_edicao_produto")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvando...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar Alterações", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
