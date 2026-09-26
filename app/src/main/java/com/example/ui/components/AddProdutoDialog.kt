package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.NovoProduto
import com.example.data.supabase.Produto
import com.example.data.supabase.SessionHolder
import com.example.util.decrypted
import com.example.util.encrypted
import com.example.data.supabase.SupabaseClient
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.util.BarcodeScannerHelper
import com.example.util.SoundFeedbackHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProdutoDialog(
    onDismiss: () -> Unit,
    onProductAdded: (Produto) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }
    val currentUser = SessionHolder.currentUser ?: return
    val currentLoja = SessionHolder.currentLoja

    var nome by remember { mutableStateOf("") }
    var codigoBarras by remember { mutableStateOf("") }
    var plu by remember { mutableStateOf("") }
    var quantidade by remember { mutableIntStateOf(10) }

    // Data de vencimento padrão (30 dias a partir de hoje: YYYY-MM-DD)
    val defaultDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    var dataVencimento by remember { mutableStateOf(defaultDate) }

    val setoresDisponiveis = listOf("Laticínios", "Mercearia", "Hortifrúti", "Padaria", "Açougue", "Bebidas", "Fiambreria", "Congelados", "Higiene & Limpeza")
    val userSector = remember(currentUser) { com.example.data.CargoManager.resolveUserSector(currentUser) }
    val isSectorRestricted = userSector != null
    var setorSelecionado by remember { mutableStateOf(userSector ?: setoresDisponiveis.first()) }
    var setorExpanded by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun scanBarcode() {
        BarcodeScannerHelper.startScan(
            context = context,
            onSuccess = { barcode ->
                SoundFeedbackHelper.playSuccessBeep(context)
                codigoBarras = barcode.trim()
            },
            onError = {
                Toast.makeText(context, "Scanner: ${it.localizedMessage ?: "Tente digitar o código"}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun salvarProduto() {
        val cleanNome = nome.trim()
        val cleanCodigo = codigoBarras.trim()
        val cleanData = dataVencimento.trim()

        if (cleanNome.isEmpty()) {
            errorMessage = "Informe o nome do produto"
            return
        }
        if (cleanCodigo.isEmpty()) {
            errorMessage = "Informe ou escaneie o código de barras"
            return
        }
        if (cleanData.isEmpty()) {
            errorMessage = "Informe a data de vencimento (AAAA-MM-DD)"
            return
        }

        if (!com.example.data.CargoManager.podeRealizarAcaoNoSetor(currentUser, setorSelecionado)) {
            errorMessage = "Acesso Negado: Seu usuário pode cadastrar produtos apenas no setor '$userSector'."
            return
        }

        val cleanPlu = plu.trim()

        errorMessage = null
        isSubmitting = true

        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    // Item 4: O Master/Operador cadastra produtos vinculando obrigatoriamente a 'loja_id' do usuário atualmente conectado.
                    // O banco gera o UUID do produto automaticamente.
                    val novoProduto = NovoProduto(
                        nome = cleanNome,
                        codigo_barras = cleanCodigo,
                        quantidade = quantidade,
                        data_vencimento = cleanData,
                        setor = setorSelecionado,
                        loja_id = currentUser.loja_id, // Vínculo estrito com a loja do operador
                        plu = cleanPlu.ifBlank { null }
                    )

                    val produtoCriado = supabase.from("produtos")
                        .insert(novoProduto.encrypted()) { select() }
                        .decodeSingle<Produto>()
                        .decrypted()

                    // Trilha de auditoria / Logs do usuário
                    com.example.data.supabase.LogManager.recordLog(
                        usuarioMatricula = currentUser.matricula,
                        usuarioNome = currentUser.nome,
                        lojaId = currentUser.loja_id,
                        acao = "CADASTRO_PRODUTO",
                        detalhes = "Cadastrou '$cleanNome' (EAN: $cleanCodigo${if (cleanPlu.isNotBlank()) ", PLU: $cleanPlu" else ""}) com $quantidade un."
                    )

                    onProductAdded(produtoCriado)
                }
            } catch (e: Exception) {
                errorMessage = "Falha ao cadastrar: ${e.localizedMessage ?: "Erro desconhecido"}"
            } finally {
                isSubmitting = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    tint = BlueExpressive,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cadastrar Produto (Item 4)", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Info do vínculo obrigatório com a loja do usuário
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Vinculado à Loja do Usuário:",
                                fontSize = 10.sp,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = currentLoja?.nome_loja ?: "Loja do Operador",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Nome
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Produto") },
                    placeholder = { Text("Ex: Queijo Mussarela Fatiado 200g") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_prod_nome")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Código de Barras com atalho do scanner
                OutlinedTextField(
                    value = codigoBarras,
                    onValueChange = { codigoBarras = it },
                    label = { Text("Código de Barras") },
                    placeholder = { Text("Ex: 7891000100101") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { scanBarcode() }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear Código de Barras",
                                tint = BlueExpressive
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_prod_codigo_barras")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Código PLU (Opcional para hortifrúti / balança)
                OutlinedTextField(
                    value = plu,
                    onValueChange = { plu = it },
                    label = { Text("Código PLU (Opcional)") },
                    placeholder = { Text("Ex: 1024 / Balança / FLV") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Numbers,
                            contentDescription = null,
                            tint = DarkTextSecondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_prod_plu")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quantidade com controles + e -
                Text("Quantidade em Estoque", fontSize = 12.sp, color = DarkTextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (quantidade > 1) quantidade-- },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Diminuir", tint = DarkTextPrimary)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$quantidade unidades",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = { quantidade++ },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = BlueExpressive)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Data de Vencimento
                OutlinedTextField(
                    value = dataVencimento,
                    onValueChange = { dataVencimento = it },
                    label = { Text("Data de Vencimento (AAAA-MM-DD)") },
                    placeholder = { Text("2026-10-15") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_prod_vencimento")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Setor Dropdown ou Bloqueio por Setor
                if (isSectorRestricted) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFA855F7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_prod_setor_locked")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Setor Vinculado ao seu Cargo:", fontSize = 10.sp, color = DarkTextSecondary)
                                Text(
                                    text = setorSelecionado,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Seu usuário atua exclusivamente neste setor.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE9D5FF)
                                )
                            }
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = setorExpanded,
                        onExpandedChange = { setorExpanded = !setorExpanded }
                    ) {
                        OutlinedTextField(
                            value = setorSelecionado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Setor do Supermercado") },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null, tint = BlueExpressive)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = setorExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = BlueExpressive,
                                unfocusedBorderColor = DarkBorder
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dialog_prod_setor")
                        )

                        ExposedDropdownMenu(
                            expanded = setorExpanded,
                            onDismissRequest = { setorExpanded = false },
                            modifier = Modifier.background(DarkSurfaceContainerHigh)
                        ) {
                            setoresDisponiveis.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, color = Color.White) },
                                    onClick = {
                                        setorSelecionado = s
                                        setorExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Mensagem de Erro
                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = err, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { salvarProduto() },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_confirm_add_prod")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Salvar no Supabase", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancelar", color = DarkTextSecondary)
            }
        },
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(18.dp)
    )
}
