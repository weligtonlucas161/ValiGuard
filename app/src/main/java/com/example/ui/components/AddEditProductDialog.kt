package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.data.model.ProductCatalog
import com.example.data.model.Sector
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.ExpressiveButtonShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressiveDialogShape
import com.example.ui.theme.OrangeMarkdown
import com.example.ui.theme.OrangeMarkdownBorder
import com.example.ui.theme.OrangeMarkdownContainer
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer
import com.example.util.BarcodeScannerHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val UNIT_OPTIONS = listOf("un", "kg", "cx", "pct", "g")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditProductDialog(
    productToEdit: Product?,
    availableSectors: List<Sector>,
    onAddNewSectorClick: () -> Unit,
    lookupCatalogByBarcode: suspend (String) -> ProductCatalog?,
    lookupProductByBarcode: (suspend (String) -> Product?)? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        category: String,
        section: String,
        barcode: String,
        internalCode: String,
        quantity: Int,
        unit: String,
        expiryDate: Long,
        batchCode: String,
        location: String,
        regularPrice: Double,
        markdownDiscountPercent: Int,
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isEditing = productToEdit != null

    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(productToEdit?.category ?: availableSectors.firstOrNull()?.name ?: "Laticínios") }
    var section by remember { mutableStateOf(productToEdit?.section ?: "") }
    var barcode by remember { mutableStateOf(productToEdit?.barcode ?: "") }
    var internalCode by remember { mutableStateOf(productToEdit?.internalCode ?: "") }
    var quantityText by remember { mutableStateOf(productToEdit?.quantity?.toString() ?: "1") }
    var selectedUnit by remember { mutableStateOf(productToEdit?.unit ?: "un") }
    var batchCode by remember { mutableStateOf(productToEdit?.batchCode ?: "") }
    var location by remember { mutableStateOf(productToEdit?.location ?: "") }
    var regularPriceText by remember { mutableStateOf(if (productToEdit != null && productToEdit.regularPrice > 0) productToEdit.regularPrice.toString() else "") }
    var markdownDiscountPercent by remember { mutableIntStateOf(productToEdit?.markdownDiscountPercent ?: 30) }
    var notes by remember { mutableStateOf(productToEdit?.notes ?: "") }
    var catalogAutoFilled by remember { mutableStateOf(false) }

    // Expiry date state
    val defaultCalendar = Calendar.getInstance().apply {
        if (productToEdit != null) {
            timeInMillis = productToEdit.expiryDate
        } else {
            add(Calendar.DAY_OF_YEAR, 14) // Default 14 days (triggers 15-day markdown alert)
        }
    }
    var expiryDateMillis by remember { mutableLongStateOf(defaultCalendar.timeInMillis) }
    var nameError by remember { mutableStateOf(false) }

    // Auto-fill product from DB or catalog whenever a valid barcode is scanned or typed
    fun onBarcodeReceived(code: String) {
        val trimmed = code.trim()
        barcode = trimmed
        if (trimmed.isNotBlank()) {
            coroutineScope.launch {
                // 1. Busca primeiro nos produtos já cadastrados no banco de dados
                val existing = lookupProductByBarcode?.invoke(trimmed)
                if (existing != null) {
                    name = existing.name
                    internalCode = existing.internalCode
                    quantityText = existing.quantity.toString()
                    selectedUnit = existing.unit
                    if (existing.category.isNotBlank()) selectedCategory = existing.category
                    if (existing.section.isNotBlank()) section = existing.section
                    if (existing.location.isNotBlank()) location = existing.location
                    if (existing.regularPrice > 0) regularPriceText = existing.regularPrice.toString()
                    if (existing.batchCode.isNotBlank()) batchCode = existing.batchCode
                    catalogAutoFilled = true
                    Toast.makeText(
                        context,
                        "Dados carregados do banco: PLU ${existing.internalCode} • Qtd: ${existing.quantity}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // 2. Fallback para o catálogo padrão
                    val catalogItem = lookupCatalogByBarcode(trimmed)
                    if (catalogItem != null) {
                        name = catalogItem.name
                        internalCode = catalogItem.internalCode
                        if (catalogItem.category.isNotBlank()) {
                            selectedCategory = catalogItem.category
                        }
                        if (catalogItem.unit.isNotBlank()) {
                            selectedUnit = catalogItem.unit
                        }
                        if (catalogItem.regularPrice > 0) {
                            regularPriceText = catalogItem.regularPrice.toString()
                        }
                        if (catalogItem.defaultLocation.isNotBlank() && location.isBlank()) {
                            location = catalogItem.defaultLocation
                        }
                        catalogAutoFilled = true
                        Toast.makeText(context, "Produto encontrado no catálogo local!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Google Native Code Scanner launcher
    fun launchGoogleBarcodeScanner() {
        BarcodeScannerHelper.startScan(
            context = context,
            onSuccess = { scannedCode ->
                onBarcodeReceived(scannedCode)
            },
            onError = { error ->
                Toast.makeText(
                    context,
                    "Não foi possível abrir o leitor Google: ${error.localizedMessage ?: "Verifique o Google Play Services"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    fun showDatePicker() {
        val currentCal = Calendar.getInstance().apply { timeInMillis = expiryDateMillis }
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                expiryDateMillis = chosen.timeInMillis
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    fun setQuickExpiryDays(days: Int) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
        expiryDateMillis = cal.timeInMillis
    }

    val formattedExpiry = remember(expiryDateMillis) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(expiryDateMillis))
    }

    val daysRemaining = remember(expiryDateMillis) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val exp = Calendar.getInstance().apply {
            timeInMillis = expiryDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        (exp - today) / (24 * 60 * 60 * 1000)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("add_edit_product_dialog"),
            shape = ExpressiveDialogShape,
            color = DarkSurface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "Editar Produto" else "Novo Produto do Setor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "Controle de validade e reposição de estoque",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = DarkTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barcode Section with Google Native Scanner Button
                Surface(
                    color = DarkSurfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Código de Barras (EAN / UPC)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )

                            // Native Google Code Scanner trigger
                            Button(
                                onClick = { launchGoogleBarcodeScanner() },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                                shape = ExpressiveChipShape,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_google_barcode_scan")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Bipar com Google Scanner",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bipar (Google)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { onBarcodeReceived(it) },
                            placeholder = { Text("Ex: 7891000100123", color = DarkTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlueExpressive,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = DarkTextPrimary,
                                unfocusedTextColor = DarkTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_barcode")
                        )

                        // Auto-filled confirmation badge
                        AnimatedVisibility(visible = catalogAutoFilled) {
                            Row(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Nome, código e dados recuperados do catálogo!",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Nome do Produto *") },
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text("O nome do produto é obrigatório", color = RedExpressive)
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Internal Code (Código Interno / SKU / PLU do Supermercado)
                OutlinedTextField(
                    value = internalCode,
                    onValueChange = { internalCode = it },
                    label = { Text("Código Interno / SKU / PLU") },
                    placeholder = { Text("Ex: 1042 ou SKU-LAT-101", color = DarkTextSecondary) },
                    singleLine = true,
                    trailingIcon = {
                        if (internalCode.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val existing = lookupProductByBarcode?.invoke(internalCode.trim())
                                        if (existing != null) {
                                            name = existing.name
                                            if (existing.barcode.isNotBlank() && barcode.isBlank()) barcode = existing.barcode
                                            quantityText = existing.quantity.toString()
                                            selectedUnit = existing.unit
                                            if (existing.category.isNotBlank()) selectedCategory = existing.category
                                            catalogAutoFilled = true
                                            Toast.makeText(context, "Produto encontrado pelo PLU: ${existing.name}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "PLU não localizado no sistema/Supabase", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Buscar por PLU",
                                    tint = BlueExpressive
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_internal_code")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sector / Category Selection with "+ Novo Setor" Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Setor do Supermercado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )

                    TextButton(
                        onClick = onAddNewSectorClick,
                        modifier = Modifier.testTag("btn_dialog_add_sector")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = BlueExpressive,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Novo Setor",
                            color = BlueExpressive,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableSectors.forEach { sector ->
                        val isSelected = selectedCategory.equals(sector.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = sector.name },
                            label = { Text(sector.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueExpressiveContainer,
                                selectedLabelColor = Color(0xFF93C5FD),
                                containerColor = DarkSurfaceContainer,
                                labelColor = DarkTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) BlueExpressive else DarkBorder,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section / Sub-location identification (Seção do Setor)
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Seção / Identificação (ex: Gôndola 02, Prateleira B, Iogurtes)") },
                    placeholder = { Text("Ex: Iogurtes e Sobremesas", color = DarkTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_section")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Expiry Date Card
                Surface(
                    color = DarkSurfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            daysRemaining < 0 -> RedExpressive
                            daysRemaining <= 1 -> RedExpressive
                            daysRemaining <= 15 -> OrangeMarkdown
                            else -> BlueExpressive.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Data de Vencimento *",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary
                                )
                                Text(
                                    text = formattedExpiry,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        daysRemaining < 0 -> RedExpressive
                                        daysRemaining <= 1 -> RedExpressive
                                        daysRemaining <= 15 -> OrangeMarkdown
                                        else -> DarkTextPrimary
                                    }
                                )
                            }

                            Button(
                                onClick = { showDatePicker() },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                                shape = ExpressiveChipShape,
                                modifier = Modifier.testTag("btn_select_expiry_date")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Alterar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Days countdown indicator
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = when {
                                daysRemaining < 0 -> RedExpressiveContainer
                                daysRemaining <= 1 -> RedExpressiveContainer
                                daysRemaining <= 15 -> OrangeMarkdownContainer
                                else -> BlueExpressiveContainer.copy(alpha = 0.6f)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when {
                                    daysRemaining < 0 -> "⚠️ PRODUTO VENCIDO há ${-daysRemaining} dia(s)"
                                    daysRemaining == 0L -> "🚨 VENCE HOJE - RETIRAR DA ÁREA DE VENDA"
                                    daysRemaining == 1L -> "🚨 VENCE AMANHÃ - RETIRAR 1 DIA ANTES DA ÁREA DE VENDA"
                                    daysRemaining <= 15 -> "🏷️ Faltam $daysRemaining dias - ALERTA DE REBAIXA ATIVO (≤15d)"
                                    else -> "✅ Faltam $daysRemaining dias - Prazo regular"
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    daysRemaining < 0 -> RedExpressive
                                    daysRemaining <= 1 -> Color(0xFFFCA5A5)
                                    daysRemaining <= 15 -> Color(0xFFFDBA74)
                                    else -> Color(0xFF93C5FD)
                                }
                            )
                        }

                        // Quick buttons for expiry
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Atalhos de Vencimento:",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                1 to "1 dia",
                                4 to "4 dias",
                                7 to "7 dias",
                                14 to "14 dias",
                                30 to "30 dias"
                            ).forEach { (d, label) ->
                                OutlinedButton(
                                    onClick = { setQuickExpiryDays(d) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextPrimary),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quantity and Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Quantidade *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_quantity")
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unidade",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            UNIT_OPTIONS.forEach { unitOpt ->
                                val isSelected = selectedUnit == unitOpt
                                Surface(
                                    color = if (isSelected) BlueExpressive else DarkSurfaceContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { selectedUnit = unitOpt }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = unitOpt,
                                        color = if (isSelected) Color.White else DarkTextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Batch and Shelf Location Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = batchCode,
                        onValueChange = { batchCode = it },
                        label = { Text("Lote") },
                        placeholder = { Text("Ex: L-902", color = DarkTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_batch_code")
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Local / Gôndola") },
                        placeholder = { Text("Ex: Geladeira 01", color = DarkTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_location")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Regular Price and Markdown Discount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = regularPriceText,
                        onValueChange = { regularPriceText = it.replace(",", ".") },
                        label = { Text("Preço Regular (R$)") },
                        placeholder = { Text("0.00", color = DarkTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_regular_price")
                    )

                    OutlinedTextField(
                        value = markdownDiscountPercent.toString(),
                        onValueChange = {
                            val num = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                            markdownDiscountPercent = num.coerceIn(5, 90)
                        },
                        label = { Text("Rebaixa Sugerida (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeMarkdown,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_markdown_percent")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações do Setor") },
                    placeholder = { Text("Ex: Giro rápido, repor na ponta de gôndola...", color = DarkTextSecondary) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = ExpressiveButtonShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }
                            val qty = quantityText.toIntOrNull() ?: 1
                            val price = regularPriceText.toDoubleOrNull() ?: 0.0

                            onSave(
                                productToEdit?.id ?: 0L,
                                name.trim(),
                                selectedCategory.trim(),
                                section.trim(),
                                barcode.trim(),
                                internalCode.trim(),
                                qty,
                                selectedUnit,
                                expiryDateMillis,
                                batchCode.trim(),
                                location.trim(),
                                price,
                                markdownDiscountPercent,
                                notes.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                        shape = ExpressiveButtonShape,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_save_product")
                    ) {
                        Text(
                            text = if (isEditing) "Atualizar Produto" else "Salvar Produto",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
