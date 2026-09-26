package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.ui.theme.AmberAttention
import com.example.ui.theme.AmberAttentionContainer
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.OrangeMarkdown
import com.example.ui.theme.OrangeMarkdownContainer
import com.example.ui.theme.PurpleUrgent1Day
import com.example.ui.theme.PurpleUrgentContainer
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer
import java.util.Locale

@Composable
fun UrgentMarkdownReportDialog(
    productsToReview: List<Product>,
    operatorName: String,
    operatorRegistration: String,
    onDismiss: () -> Unit,
    onCompleteAndGeneratePdf: (List<Product>) -> Unit,
    onCompleteAndExportCsv: ((List<Product>) -> Unit)? = null
) {
    // Sort strictly by expiry priority: <= 1 day, then <= 15 days, then expired, then upcoming
    val sortedProducts = remember(productsToReview) {
        productsToReview.sortedWith(
            compareBy<Product> {
                val days = it.getDaysRemaining()
                when {
                    days in 0..1 -> 0 // Maior prioridade: vence hoje ou amanhã
                    days in 2..15 -> 1 // Prioridade de rebaixa urgente
                    days < 0 -> 2 // Já vencido
                    else -> 3
                }
            }.thenBy { it.expiryDate }
        )
    }

    if (sortedProducts.isEmpty()) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberAttention,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum Produto Selecionado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Não há produtos cadastrados ou elegíveis para emissão do relatório de rebaixa no momento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
        return
    }

    // State for user-entered prices per product ID
    val priceMap = remember {
        mutableStateMapOf<Long, String>().apply {
            sortedProducts.forEach { p ->
                if (p.regularPrice > 0) {
                    put(p.id, String.format(Locale.US, "%.2f", p.regularPrice))
                }
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentProduct = sortedProducts[currentIndex]
    val totalCount = sortedProducts.size
    val progress = (currentIndex + 1).toFloat() / totalCount

    var currentPriceText by remember(currentIndex) {
        mutableStateOf(priceMap[currentProduct.id] ?: "")
    }

    fun getUpdatedProducts(): List<Product> {
        val parsed = currentPriceText.replace(",", ".").trim()
        if (parsed.isNotBlank()) {
            priceMap[currentProduct.id] = parsed
        }
        return sortedProducts.map { p ->
            val newPriceStr = priceMap[p.id]?.replace(",", ".")?.trim()
            val newPrice = newPriceStr?.toDoubleOrNull() ?: p.regularPrice
            p.copy(regularPrice = newPrice)
        }
    }

    fun saveCurrentPriceAndAdvance(exportCsvInstead: Boolean = false) {
        val parsed = currentPriceText.replace(",", ".").trim()
        if (parsed.isNotBlank()) {
            priceMap[currentProduct.id] = parsed
        }
        if (currentIndex < totalCount - 1) {
            currentIndex++
        } else {
            // Finished all products! Compile updated list and invoke callback
            val updatedProducts = getUpdatedProducts()
            if (exportCsvInstead && onCompleteAndExportCsv != null) {
                onCompleteAndExportCsv(updatedProducts)
            } else {
                onCompleteAndGeneratePdf(updatedProducts)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkBackground,
            border = BorderStroke(1.2.dp, DarkBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("urgent_markdown_wizard_dialog")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OrangeMarkdownContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = OrangeMarkdown,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Conferência de Preço de Gôndola",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                            Text(
                                text = "Preenchimento manual card a card para o Relatório",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary
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

                Spacer(modifier = Modifier.height(14.dp))

                // Stepper & Progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Produto ${currentIndex + 1} de $totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OrangeMarkdown
                    )
                    Text(
                        text = "${((progress) * 100).toInt()}% concluído",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = OrangeMarkdown,
                    trackColor = DarkSurfaceContainerHigh,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Product Card to find in sales area
                val daysRemaining = currentProduct.getDaysRemaining()
                val isUrgent1d = daysRemaining in 0..1 && !currentProduct.isRemovedFromSales
                val isMarkdown15d = daysRemaining in 2..15 && !currentProduct.isRemovedFromSales

                val cardBorderColor = when {
                    isUrgent1d -> PurpleUrgent1Day
                    isMarkdown15d -> AmberAttention
                    daysRemaining < 0 -> RedExpressive
                    else -> DarkBorder
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.5.dp, cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Priority Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when {
                                isUrgent1d -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PurpleUrgentContainer
                                    ) {
                                        Text(
                                            text = "🚨 PRIORIDADE MÁXIMA: Vence em ≤1 dia",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PurpleUrgent1Day,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                isMarkdown15d -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AmberAttentionContainer
                                    ) {
                                        Text(
                                            text = "🏷️ REBAIXA URGENTE: Vence em ≤15 dias",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberAttention,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                daysRemaining < 0 -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RedExpressiveContainer
                                    ) {
                                        Text(
                                            text = "⛔ PRODUTO VENCIDO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RedExpressive,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceContainer
                                    ) {
                                        Text(
                                            text = "📦 Vence em $daysRemaining dias",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DarkTextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Stock quantity tag
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = BlueExpressive,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${currentProduct.quantity} ${currentProduct.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Product Title / Name
                        Text(
                            text = currentProduct.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Codes row (EAN and PLU)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = DarkTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "CÓDIGO EAN",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = DarkTextSecondary
                                        )
                                        Text(
                                            text = currentProduct.barcode.ifBlank { "Sem código" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkTextPrimary
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = DarkTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "CÓDIGO PLU",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = DarkTextSecondary
                                        )
                                        Text(
                                            text = currentProduct.internalCode.ifBlank { "-" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sector, Section and Location
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = DarkTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val sectionText = if (currentProduct.section.isNotBlank()) " • Seção: ${currentProduct.section}" else ""
                            Text(
                                text = "Setor: ${currentProduct.category}$sectionText",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary
                            )
                        }

                        if (currentProduct.location.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = OrangeMarkdown,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Localização: ${currentProduct.location}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = DarkTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Entrada: ${currentProduct.getFormattedCreatedAt()}  |  Validade: ${currentProduct.getFormattedExpiryDate()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Price Input Section
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceContainer,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = OrangeMarkdown,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preço Atual do Produto na Gôndola:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = currentPriceText,
                            onValueChange = { currentPriceText = it },
                            placeholder = { Text("0,00", color = DarkTextSecondary) },
                            prefix = {
                                Text(
                                    text = "R$ ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = if (currentIndex == totalCount - 1) ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { saveCurrentPriceAndAdvance() },
                                onDone = { saveCurrentPriceAndAdvance() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeMarkdown,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = DarkTextPrimary,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("urgent_report_price_input")
                        )

                        // Quick suggestion: discount preview
                        val enteredPrice = currentPriceText.replace(",", ".").toDoubleOrNull() ?: currentProduct.regularPrice
                        if (enteredPrice > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val suggestedPrice = enteredPrice * 0.70 // 30% desconto sugerido
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sugestão Rebaixa (-30%):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkTextSecondary
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "R$ %.2f", suggestedPrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSafe
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentIndex > 0) {
                        OutlinedButton(
                            onClick = {
                                val parsed = currentPriceText.replace(",", ".").trim()
                                if (parsed.isNotBlank()) priceMap[currentProduct.id] = parsed
                                currentIndex--
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anterior")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (currentIndex < totalCount - 1) {
                                currentIndex++
                            } else {
                                saveCurrentPriceAndAdvance()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary)
                    ) {
                        Text("Pular")
                    }

                    if (currentIndex == totalCount - 1 && onCompleteAndExportCsv != null) {
                        OutlinedButton(
                            onClick = { saveCurrentPriceAndAdvance(exportCsvInstead = true) },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("urgent_report_csv_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSafe),
                            border = BorderStroke(1.dp, EmeraldSafe)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = EmeraldSafe
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gerar CSV", color = EmeraldSafe)
                        }
                    }

                    Button(
                        onClick = { saveCurrentPriceAndAdvance(exportCsvInstead = false) },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("urgent_report_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeMarkdown)
                    ) {
                        if (currentIndex == totalCount - 1) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gerar PDF")
                        } else {
                            Text("Confirmar")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
