package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditItem
import com.example.data.model.AuditSession
import com.example.data.model.AuditType
import com.example.data.model.Product
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import com.example.util.AuditPdfGenerator
import com.example.util.BarcodeScannerHelper
import com.example.util.CollectorBroadcastScannerEffect
import com.example.util.SoundFeedbackHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AuditScreenTab(val label: String, val icon: @Composable () -> Unit) {
    VALIDITY("Validade", { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }),
    PRESENCE("Presença", { Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp)) }),
    STOCK("Estoque", { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp)) }),
    HISTORY("Relatórios", { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) })
}

@Composable
fun AuditScreen(
    currentAuditType: AuditType,
    activeSession: AuditSession?,
    activeItems: List<AuditItem>,
    completedAudits: List<AuditSession>,
    operatorName: String,
    isGeneratingPdf: Boolean,
    lastGeneratedPdf: File?,
    allProducts: List<Product>,
    onSelectAuditType: (AuditType) -> Unit,
    onSetOperatorName: (String) -> Unit,
    onRecordValidityItem: (barcode: String, name: String, code: String, sector: String, batch: String, expiryDate: Long, qtySales: Int, qtyStock: Int) -> Unit,
    onScanPresenceBarcode: (barcode: String) -> Unit,
    onMarkPresenceItem: (itemId: Long) -> Unit,
    onUnmarkPresenceItem: (itemId: Long) -> Unit,
    onRecordStockItem: (barcode: String, name: String, sector: String, location: String, quantity: Int) -> Unit,
    onFinishAuditAndGeneratePdf: (Context, (File) -> Unit) -> Unit,
    onDismissPdfDialog: () -> Unit,
    onDeleteReport: (AuditSession) -> Unit = {},
    onOpenUrgentMarkdownWizard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedScreenTab by remember { mutableStateOf(AuditScreenTab.VALIDITY) }
    var showOperatorDialog by remember { mutableStateOf(false) }

    // Sync screen tab with ViewModel's active type
    LaunchedEffect(currentAuditType) {
        selectedScreenTab = when (currentAuditType) {
            AuditType.VALIDITY -> AuditScreenTab.VALIDITY
            AuditType.PRESENCE -> AuditScreenTab.PRESENCE
            AuditType.STOCK -> AuditScreenTab.STOCK
            AuditType.MARKDOWN_URGENCY -> AuditScreenTab.HISTORY
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // --- Header com operador e seletor de abas ---
        Surface(
            color = DarkSurfaceContainer,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                // Operator and Audit Status Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Central de Auditoria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showOperatorDialog = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "Operador: $operatorName",
                                style = MaterialTheme.typography.bodySmall,
                                color = BlueExpressive,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Alterar operador",
                                tint = BlueExpressive,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (activeSession != null && selectedScreenTab != AuditScreenTab.HISTORY) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BlueExpressiveContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(EmeraldSafe, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Sessão Ativa (${activeItems.size})",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable/Fixed Tab Row with 4 options
                ScrollableTabRow(
                    selectedTabIndex = selectedScreenTab.ordinal,
                    containerColor = DarkSurfaceContainer,
                    contentColor = BlueExpressive,
                    edgePadding = 12.dp,
                    divider = {}
                ) {
                    AuditScreenTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedScreenTab == tab,
                            onClick = {
                                selectedScreenTab = tab
                                when (tab) {
                                    AuditScreenTab.VALIDITY -> onSelectAuditType(AuditType.VALIDITY)
                                    AuditScreenTab.PRESENCE -> onSelectAuditType(AuditType.PRESENCE)
                                    AuditScreenTab.STOCK -> onSelectAuditType(AuditType.STOCK)
                                    AuditScreenTab.HISTORY -> {}
                                }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    tab.icon()
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (selectedScreenTab == tab) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            selectedContentColor = BlueExpressive,
                            unselectedContentColor = DarkTextSecondary
                        )
                    }
                }
            }
        }

        // --- Main Content Area by Tab ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedScreenTab) {
                AuditScreenTab.VALIDITY -> {
                    ValidityAuditView(
                        activeSession = activeSession,
                        activeItems = activeItems,
                        allProducts = allProducts,
                        isGeneratingPdf = isGeneratingPdf,
                        onRecordItem = onRecordValidityItem,
                        onFinishAndGeneratePdf = { onFinishAuditAndGeneratePdf(context, {}) }
                    )
                }

                AuditScreenTab.PRESENCE -> {
                    PresenceAuditView(
                        activeSession = activeSession,
                        activeItems = activeItems,
                        isGeneratingPdf = isGeneratingPdf,
                        onScanBarcode = onScanPresenceBarcode,
                        onMarkItem = onMarkPresenceItem,
                        onUnmarkItem = onUnmarkPresenceItem,
                        onFinishAndGeneratePdf = { onFinishAuditAndGeneratePdf(context, {}) }
                    )
                }

                AuditScreenTab.STOCK -> {
                    StockAuditView(
                        activeSession = activeSession,
                        activeItems = activeItems,
                        allProducts = allProducts,
                        isGeneratingPdf = isGeneratingPdf,
                        onRecordItem = onRecordStockItem,
                        onFinishAndGeneratePdf = { onFinishAuditAndGeneratePdf(context, {}) }
                    )
                }

                AuditScreenTab.HISTORY -> {
                    AuditHistoryView(
                        completedAudits = completedAudits,
                        onOpenPdf = { file -> AuditPdfGenerator.openPdf(context, file) },
                        onSharePdf = { file -> AuditPdfGenerator.sharePdf(context, file) },
                        onDeleteReport = onDeleteReport,
                        onOpenUrgentMarkdownWizard = onOpenUrgentMarkdownWizard
                    )
                }
            }
        }
    }

    // --- Dialog to edit Operator Name ---
    if (showOperatorDialog) {
        var tempName by remember { mutableStateOf(operatorName) }
        AlertDialog(
            onDismissRequest = { showOperatorDialog = false },
            containerColor = DarkSurfaceContainerHigh,
            title = { Text("Nome do Operador", color = DarkTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Operador Responsável") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            onSetOperatorName(tempName.trim())
                        }
                        showOperatorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
                ) {
                    Text("Salvar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOperatorDialog = false }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            }
        )
    }

    // --- Dialog when PDF is generated ---
    if (lastGeneratedPdf != null) {
        AlertDialog(
            onDismissRequest = onDismissPdfDialog,
            containerColor = DarkSurfaceContainerHigh,
            icon = {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = RedExpressive,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Relatório PDF Concluído!",
                    color = DarkTextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "A auditoria foi finalizada com sucesso e o documento oficial foi gerado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastGeneratedPdf.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AuditPdfGenerator.openPdf(context, lastGeneratedPdf)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir PDF", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            AuditPdfGenerator.sharePdf(context, lastGeneratedPdf)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartilhar", color = DarkTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = onDismissPdfDialog) {
                        Text("Fechar", color = DarkTextSecondary)
                    }
                }
            }
        )
    }
}

// =========================================================================
// 1. AUDITORIA DE VALIDADE
// =========================================================================

@Composable
fun ValidityAuditView(
    activeSession: AuditSession?,
    activeItems: List<AuditItem>,
    allProducts: List<Product>,
    isGeneratingPdf: Boolean,
    onRecordItem: (barcode: String, name: String, code: String, sector: String, batch: String, expiryDate: Long, qtySales: Int, qtyStock: Int) -> Unit,
    onFinishAndGeneratePdf: () -> Unit
) {
    val context = LocalContext.current

    var barcode by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var internalCode by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var expiryDateMillis by remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
        mutableLongStateOf(cal.timeInMillis)
    }
    var qtySales by remember { mutableIntStateOf(1) }
    var qtyStock by remember { mutableIntStateOf(0) }

    fun lookupProduct(code: String) {
        val clean = code.trim()
        val found = allProducts.find { it.barcode.equals(clean, ignoreCase = true) }
        if (found != null) {
            productName = found.name
            internalCode = found.internalCode
            sector = found.location
            batch = found.batchCode.ifBlank { batch }
            if (found.expiryDate > 0) {
                expiryDateMillis = found.expiryDate
            }
            qtySales = found.quantity
        }
    }

    fun scanWithGoogle() {
        BarcodeScannerHelper.startScan(
            context = context,
            onSuccess = { scanned ->
                barcode = scanned
                lookupProduct(scanned)
            }
        )
    }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Suporte a sensores de coletores industriais na Auditoria de Validade
    CollectorBroadcastScannerEffect { scanned ->
        barcode = scanned
        lookupProduct(scanned)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("validity_audit_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Scanner Banner Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auditoria de Validade & Lote",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = CircleShape,
                            color = BlueExpressiveContainer
                        ) {
                            IconButton(onClick = { scanWithGoogle() }, modifier = Modifier.size(38.dp)) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Bipar Produto",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Bipe o código de barras para carregar o item. Informe o lote, data de validade e as quantidades em área de vendas e estoque.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Barcode field + Scanner shortcut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = {
                                barcode = it
                                lookupProduct(it)
                            },
                            label = { Text("Código de Barras") },
                            placeholder = { Text("789...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = BlueExpressive,
                                unfocusedBorderColor = DarkBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("audit_barcode_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { scanWithGoogle() },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("btn_audit_scan_barcode")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bipar")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Product Name
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Nome do Produto") },
                        placeholder = { Text("Ex: Iogurte Natural 170g") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lote & Validade
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = batch,
                            onValueChange = { batch = it },
                            label = { Text("Lote") },
                            placeholder = { Text("Ex: L102A") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = BlueExpressive,
                                unfocusedBorderColor = DarkBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        // Expiry Date Field with DatePickerDialog
                        Box(modifier = Modifier.weight(1.2f)) {
                            OutlinedTextField(
                                value = dateFormatter.format(Date(expiryDateMillis)),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Data de Validade") },
                                trailingIcon = {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = BlueExpressive)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = DarkTextPrimary,
                                    focusedBorderColor = BlueExpressive,
                                    unfocusedBorderColor = DarkBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val cal = Calendar.getInstance().apply { timeInMillis = expiryDateMillis }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val selected = Calendar.getInstance().apply { set(y, m, d) }
                                                expiryDateMillis = selected.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                            )
                        }
                    }

                    // Quick date presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(7 to "+7d", 15 to "+15d", 30 to "+30d", 60 to "+60d", 90 to "+90d").forEach { (days, label) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceContainerHigh,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
                                        expiryDateMillis = c.timeInMillis
                                    }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = BlueExpressive,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quantities: Área de Vendas e Estoque
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Vendas
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Área de Vendas", fontSize = 11.sp, color = DarkTextSecondary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { if (qtySales > 0) qtySales-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = DarkTextPrimary)
                                    }
                                    Text(
                                        text = "$qtySales un",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = { qtySales++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = BlueExpressive)
                                    }
                                }
                            }
                        }

                        // Estoque
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Depósito / Estoque", fontSize = 11.sp, color = DarkTextSecondary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { if (qtyStock > 0) qtyStock-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = DarkTextPrimary)
                                    }
                                    Text(
                                        text = "$qtyStock un",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = { qtyStock++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = BlueExpressive)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Button Add to Audit
                    Button(
                        onClick = {
                            if (barcode.isNotBlank()) {
                                val nameToSave = productName.ifBlank { "Produto $barcode" }
                                onRecordItem(
                                    barcode.trim(),
                                    nameToSave,
                                    internalCode.trim(),
                                    sector.trim(),
                                    batch.trim().ifBlank { "S/L" },
                                    expiryDateMillis,
                                    qtySales,
                                    qtyStock
                                )
                                // Clear input for next bip
                                barcode = ""
                                productName = ""
                                batch = ""
                                qtySales = 1
                                qtyStock = 0
                            }
                        },
                        enabled = barcode.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_validity_item")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Item na Auditoria", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Audited Items in this session
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Itens Auditados na Sessão (${activeItems.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (activeItems.isNotEmpty()) {
                    Button(
                        onClick = onFinishAndGeneratePdf,
                        enabled = !isGeneratingPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = RedExpressive),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_finish_validity_audit_pdf")
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finalizar & Gerar PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (activeItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nenhum produto auditado nesta sessão ainda.\nBipe o primeiro código de barras acima para começar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(28.dp)
                    )
                }
            }
        } else {
            items(activeItems, key = { it.id }) { item ->
                AuditItemCard(item = item, auditType = AuditType.VALIDITY)
            }
        }
    }
}

// =========================================================================
// 2. AUDITORIA DE PRESENÇA
// =========================================================================

@Composable
fun PresenceAuditView(
    activeSession: AuditSession?,
    activeItems: List<AuditItem>,
    isGeneratingPdf: Boolean,
    onScanBarcode: (String) -> Unit,
    onMarkItem: (Long) -> Unit,
    onUnmarkItem: (Long) -> Unit,
    onFinishAndGeneratePdf: () -> Unit
) {
    val context = LocalContext.current
    var scanBarcodeQuery by remember { mutableStateOf("") }
    var presenceTab by remember { mutableIntStateOf(0) } // 0 = Pendentes, 1 = Bipados

    val pendingItems = remember(activeItems) { activeItems.filter { !it.isPresent } }
    val scannedItems = remember(activeItems) { activeItems.filter { it.isPresent } }

    val total = activeItems.size
    val present = scannedItems.size
    val progress = if (total > 0) present.toFloat() / total.toFloat() else 0f

    fun scanWithGoogle() {
        BarcodeScannerHelper.startScan(
            context = context,
            onSuccess = { scanned ->
                onScanBarcode(scanned)
            }
        )
    }

    // Suporte a sensores de coletores industriais na Auditoria de Presença
    CollectorBroadcastScannerEffect { scanned ->
        onScanBarcode(scanned)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("presence_audit_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Instructions & Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auditoria de Presença",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = CircleShape,
                            color = BlueExpressiveContainer
                        ) {
                            IconButton(onClick = { scanWithGoogle() }, modifier = Modifier.size(38.dp)) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Bipar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Bipe produto por produto. Conforme for bipado, o produto é marcado e sai da lista de pendentes. Ao finalizar, o relatório em PDF é gerado e a lista é resetada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Bip Bar with textfield + button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = scanBarcodeQuery,
                            onValueChange = { scanBarcodeQuery = it },
                            label = { Text("Bipar / Digitar Código") },
                            placeholder = { Text("789...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (scanBarcodeQuery.isNotBlank()) {
                                    onScanBarcode(scanBarcodeQuery)
                                    scanBarcodeQuery = ""
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = BlueExpressive,
                                unfocusedBorderColor = DarkBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("presence_barcode_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (scanBarcodeQuery.isNotBlank()) {
                                    onScanBarcode(scanBarcodeQuery)
                                    scanBarcodeQuery = ""
                                } else {
                                    scanWithGoogle()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("btn_presence_bipar")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bipar")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Progresso da Conferência", fontSize = 12.sp, color = DarkTextSecondary)
                        Text(
                            text = "${(progress * 100).toInt()}% ($present de $total)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BlueExpressive
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldSafe,
                        trackColor = DarkSurfaceContainerHigh
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Finish and PDF Button
                    Button(
                        onClick = onFinishAndGeneratePdf,
                        enabled = !isGeneratingPdf && activeItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedExpressive),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_finish_presence_audit_pdf")
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finalizar Auditoria & Gerar Relatório PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sub-tabs: Pendentes vs Bipados
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = presenceTab == 0,
                    onClick = { presenceTab = 0 },
                    label = { Text("Pendentes a Bipar (${pendingItems.size})", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RedExpressive.copy(alpha = 0.2f),
                        selectedLabelColor = RedExpressive,
                        containerColor = DarkSurfaceContainer,
                        labelColor = DarkTextSecondary
                    ),
                    modifier = Modifier.testTag("chip_presence_pending")
                )

                FilterChip(
                    selected = presenceTab == 1,
                    onClick = { presenceTab = 1 },
                    label = { Text("Já Bipados (${scannedItems.size})", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldSafe.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldSafe,
                        containerColor = DarkSurfaceContainer,
                        labelColor = DarkTextSecondary
                    ),
                    modifier = Modifier.testTag("chip_presence_scanned")
                )
            }
        }

        // Items List
        val currentList = if (presenceTab == 0) pendingItems else scannedItems

        if (currentList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (presenceTab == 0) "🎉 Todos os produtos foram bipados e estão presentes!"
                        else "Nenhum produto bipado ainda.\nUtilize o leitor para bipar os itens da gôndola.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(28.dp)
                    )
                }
            }
        } else {
            items(currentList, key = { it.id }) { item ->
                PresenceItemCard(
                    item = item,
                    onTogglePresence = {
                        if (item.isPresent) {
                            onUnmarkItem(item.id)
                        } else {
                            onMarkItem(item.id)
                        }
                    }
                )
            }
        }
    }
}

// =========================================================================
// 3. AUDITORIA DE ESTOQUE
// =========================================================================

@Composable
fun StockAuditView(
    activeSession: AuditSession?,
    activeItems: List<AuditItem>,
    allProducts: List<Product>,
    isGeneratingPdf: Boolean,
    onRecordItem: (barcode: String, name: String, sector: String, location: String, quantity: Int) -> Unit,
    onFinishAndGeneratePdf: () -> Unit
) {
    val context = LocalContext.current

    var barcode by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("Geral") }
    var selectedLocation by remember { mutableStateOf("Área de Vendas") } // "Área de Vendas" ou "Depósito"
    var quantity by remember { mutableIntStateOf(1) }

    fun lookupProduct(code: String) {
        val clean = code.trim()
        val found = allProducts.find { it.barcode.equals(clean, ignoreCase = true) }
        if (found != null) {
            productName = found.name
            sector = found.location.ifBlank { "Geral" }
        }
    }

    fun scanWithGoogle() {
        BarcodeScannerHelper.startScan(
            context = context,
            onSuccess = { scanned ->
                barcode = scanned
                lookupProduct(scanned)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stock_audit_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auditoria de Estoque Físico",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = CircleShape,
                            color = BlueExpressiveContainer
                        ) {
                            IconButton(onClick = { scanWithGoogle() }, modifier = Modifier.size(38.dp)) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Bipar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Bipe o produto e informe a quantidade apurada e o local onde ele se encontra (Área de Vendas ou Depósito).",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Barcode input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = {
                                barcode = it
                                lookupProduct(it)
                            },
                            label = { Text("Código de Barras") },
                            placeholder = { Text("789...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = BlueExpressive,
                                unfocusedBorderColor = DarkBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stock_barcode_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { scanWithGoogle() },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("btn_stock_scan")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bipar")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Product Name
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Nome do Produto") },
                        placeholder = { Text("Ex: Feijão Carioca 1kg") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Local: Área de Vendas vs Depósito
                    Text(
                        text = "Local Auditado:",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedLocation == "Área de Vendas",
                            onClick = { selectedLocation = "Área de Vendas" },
                            label = { Text("🏪 Área de Vendas", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueExpressive,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceContainerHigh,
                                labelColor = DarkTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedLocation == "Depósito",
                            onClick = { selectedLocation = "Depósito" },
                            label = { Text("🏢 Depósito / Estoque", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueExpressive,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceContainerHigh,
                                labelColor = DarkTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quantidade contada
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Quantidade Contada", fontSize = 12.sp, color = DarkTextSecondary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { if (quantity > 10) quantity -= 10 else quantity = 0 },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("-10", fontSize = 11.sp, color = DarkTextSecondary, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { if (quantity > 0) quantity-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = DarkTextPrimary)
                                    }
                                }

                                Text(
                                    text = "$quantity un",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { quantity++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = BlueExpressive)
                                    }
                                    IconButton(
                                        onClick = { quantity += 10 },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("+10", fontSize = 11.sp, color = BlueExpressive, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Record Button
                    Button(
                        onClick = {
                            if (barcode.isNotBlank()) {
                                val nameToSave = productName.ifBlank { "Produto $barcode" }
                                onRecordItem(
                                    barcode.trim(),
                                    nameToSave,
                                    sector.trim(),
                                    selectedLocation,
                                    quantity
                                )
                                barcode = ""
                                productName = ""
                                quantity = 1
                            }
                        },
                        enabled = barcode.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_stock_item")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Contagem", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Summary and Items
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contagens da Sessão (${activeItems.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (activeItems.isNotEmpty()) {
                    Button(
                        onClick = onFinishAndGeneratePdf,
                        enabled = !isGeneratingPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = RedExpressive),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_finish_stock_audit_pdf")
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finalizar & Gerar PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (activeItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nenhuma contagem realizada nesta sessão.\nBipe os produtos acima para registrar o estoque físico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(28.dp)
                    )
                }
            }
        } else {
            items(activeItems, key = { it.id }) { item ->
                AuditItemCard(item = item, auditType = AuditType.STOCK)
            }
        }
    }
}

// =========================================================================
// 4. HISTÓRICO DE RELATÓRIOS CONCLUÍDOS
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditHistoryView(
    completedAudits: List<AuditSession>,
    onOpenPdf: (File) -> Unit,
    onSharePdf: (File) -> Unit,
    onDeleteReport: (AuditSession) -> Unit = {},
    onOpenUrgentMarkdownWizard: () -> Unit = {}
) {
    var sessionToDelete by remember { mutableStateOf<AuditSession?>(null) }

    // Dialog de confirmação para exclusão de relatório
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    text = "Excluir Relatório?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Deseja realmente excluir permanentemente este relatório PDF do histórico? O arquivo e os registros serão removidos.",
                    color = DarkTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = session
                        sessionToDelete = null
                        onDeleteReport(toDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audit_history_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Histórico de Auditorias Concluídas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Visualize, compartilhe ou arraste para o lado para excluir relatórios em PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = DarkTextSecondary
            )
        }

        // Banner especial: Emissão de Relatório de Rebaixa Urgente (Prioridade de Vencimento)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Relatório de Rebaixa Urgente",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Produtos em ordem de prioridade de vencimento com preenchimento manual de preços card por card.",
                            color = DarkTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenUrgentMarkdownWizard,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Emitir", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        if (completedAudits.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhum relatório PDF gerado ainda.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Ao finalizar qualquer auditoria ou emitir o relatório de rebaixa, o PDF oficial aparecerá aqui.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(completedAudits, key = { it.id }) { session ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            sessionToDelete = session
                            false
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val isEndToStart = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(RedExpressive)
                                .padding(horizontal = 20.dp),
                            contentAlignment = if (isEndToStart) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Excluir Relatório",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                ) {
                    CompletedAuditCard(
                        session = session,
                        onOpenPdf = onOpenPdf,
                        onSharePdf = onSharePdf,
                        onDeleteClick = { sessionToDelete = session }
                    )
                }
            }
        }
    }
}

// =========================================================================
// HELPER CARDS
// =========================================================================

@Composable
fun AuditItemCard(item: AuditItem, auditType: AuditType) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Cód: ${item.barcode} ${if (!item.batch.isNullOrBlank()) "• Lote: ${item.batch}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary,
                    fontSize = 11.sp
                )
                if (auditType == AuditType.VALIDITY && item.expiryDate != null) {
                    Text(
                        text = "Validade: ${item.getFormattedExpiryDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BlueExpressive,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            // Quantities pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceContainerHigh,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (auditType == AuditType.VALIDITY) {
                        Text(
                            text = "Vendas: ${item.quantitySalesArea} un",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Estoque: ${item.quantityStockArea} un",
                            fontSize = 10.sp,
                            color = DarkTextSecondary
                        )
                    } else if (auditType == AuditType.STOCK) {
                        Text(
                            text = "${item.location ?: "Vendas"}",
                            fontSize = 10.sp,
                            color = BlueExpressive,
                            fontWeight = FontWeight.Bold
                        )
                        val q = if (item.location == "Área de Vendas") item.quantitySalesArea else item.quantityStockArea
                        Text(
                            text = "$q un",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresenceItemCard(
    item: AuditItem,
    onTogglePresence: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPresent) DarkSurfaceContainer else DarkSurfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isPresent) EmeraldSafe.copy(alpha = 0.4f) else DarkBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isPresent) Color.White else DarkTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Barras: ${item.barcode} • Setor: ${item.sector ?: "Geral"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary,
                    fontSize = 11.sp
                )
                if (item.isPresent && item.scannedAt != null) {
                    Text(
                        text = "Bipado às ${item.getFormattedScannedTime()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSafe,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }

            // Presence action button
            if (item.isPresent) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSafe.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTogglePresence() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSafe,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PRESENTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSafe
                        )
                    }
                }
            } else {
                Button(
                    onClick = onTogglePresence,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressiveContainer),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Marcar", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CompletedAuditCard(
    session: AuditSession,
    onOpenPdf: (File) -> Unit,
    onSharePdf: (File) -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    val typeTitle = when (session.auditType) {
        AuditType.VALIDITY.code -> "Auditoria de Validade"
        AuditType.PRESENCE.code -> "Auditoria de Presença"
        AuditType.STOCK.code -> "Auditoria de Estoque"
        AuditType.MARKDOWN_URGENCY.code -> "Relatório de Rebaixa Urgente"
        else -> "Auditoria Geral"
    }

    val file = remember(session.pdfFilePath) {
        session.pdfFilePath?.let { File(it) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = if (session.auditType == AuditType.MARKDOWN_URGENCY.code) Color(0xFFF59E0B) else RedExpressive,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = typeTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Concluída em: ${session.getFormattedCompletedTime()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BlueExpressiveContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${session.totalItems} itens",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Operador: ${session.operatorName}",
                style = MaterialTheme.typography.bodySmall,
                color = DarkTextSecondary
            )

            if (session.auditType == AuditType.PRESENCE.code) {
                Text(
                    text = "Presentes: ${session.presentCount} | Faltantes: ${session.missingCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (session.missingCount == 0) EmeraldSafe else RedExpressive,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Relatório",
                        tint = RedExpressive.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (file != null && file.exists()) {
                        OutlinedButton(
                            onClick = { onSharePdf(file) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartilhar", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onOpenPdf(file) },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Abrir PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Arquivo PDF não disponível",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkTextSecondary
                        )
                    }
                }
            }
        }
    }
}
