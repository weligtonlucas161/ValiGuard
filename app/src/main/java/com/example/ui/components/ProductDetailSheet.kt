package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.ProductHistory
import com.example.ui.theme.AmberAttention
import com.example.ui.theme.AmberAttentionContainer
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EmeraldSafeContainer
import com.example.ui.theme.ExpressiveButtonShape
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.OrangeMarkdown
import com.example.ui.theme.OrangeMarkdownBorder
import com.example.ui.theme.OrangeMarkdownContainer
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailSheet(
    product: Product?,
    historyList: List<ProductHistory>,
    onDismiss: () -> Unit,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit,
    onRequestMarkdown: (Product, Int, String) -> Unit,
    onAdjustStock: (Product, Int) -> Unit,
    onConfirmRemovalFromSales: (Product) -> Unit,
    onConfirmMarkdownRequested: (Product, Boolean) -> Unit = { _, _ -> },
    onConfirmMarkdownAcceptance: (Product, Boolean) -> Unit = { _, _ -> },
    onRequestRenewedMarkdown: (Product) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (product == null) return

    val context = LocalContext.current
    val daysRemaining = product.getDaysRemaining()
    val isMarkdownEligible = product.isMarkdownEligible()
    val isMustRemove1Day = daysRemaining in 0..1 && !product.isRemovedFromSales
    var selectedDiscount by remember { mutableIntStateOf(product.markdownDiscountPercent.coerceAtLeast(20)) }
    var markdownNotes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(DarkBorder)
            )
        },
        modifier = Modifier.testTag("product_detail_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Top Bar: Category Pill & Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = BlueExpressiveContainer,
                        shape = ExpressiveChipShape
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF93C5FD),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onEditClick(product) },
                            modifier = Modifier.testTag("detail_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = BlueExpressive
                            )
                        }

                        IconButton(
                            onClick = { onDeleteClick(product) },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = RedExpressive
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("detail_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = DarkTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Product Title
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkTextPrimary
                )

                // Subtitle: Internal code and Barcode
                if (product.internalCode.isNotBlank() || product.barcode.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            product.internalCode.isNotBlank() && product.barcode.isNotBlank() ->
                                "Código Interno: ${product.internalCode} • Barras: ${product.barcode}"
                            product.internalCode.isNotBlank() -> "Código Interno: ${product.internalCode}"
                            else -> "Código de Barras: ${product.barcode}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // CRITICAL PROTOCOL: RETIRADA DA ÁREA DE VENDA (1 DIA ANTES DO VENCIMENTO)
            item {
                if (product.isRemovedFromSales) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                        border = BorderStroke(1.dp, EmeraldSafe)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSafe,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PRODUTO RETIRADO DA ÁREA DE VENDA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldSafe
                                )
                                Text(
                                    text = "Recolhido da gôndola em ${product.getFormattedRemovalTime() ?: "N/A"} para evitar comercialização fora do prazo.",
                                    fontSize = 11.sp,
                                    color = DarkTextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else if (isMustRemove1Day) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("urgent_removal_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = RedExpressiveContainer),
                        border = BorderStroke(2.dp, RedExpressive)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = RedExpressive,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "RETIRADA OBRIGATÓRIA DA GÔNDOLA",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFFCA5A5)
                                    )
                                    Text(
                                        text = if (daysRemaining == 0L) "Vence hoje! Não pode permanecer exposto." else "Falta apenas 1 dia para o vencimento!",
                                        fontSize = 12.sp,
                                        color = DarkTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "A norma operacional do setor determina a remoção física dos produtos 1 dia antes da data de validade.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onConfirmRemovalFromSales(product) },
                                colors = ButtonDefaults.buttonColors(containerColor = RedExpressive),
                                shape = ExpressiveChipShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_detail_confirm_removal")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Confirmar Retirada da Área de Venda",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // CRITICAL SECTION: REBAIXA DE PREÇO (15 DIAS ANTES DO VENCIMENTO)
            item {
                if (isMarkdownEligible) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("markdown_alert_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (product.markdownRequested) BlueExpressiveContainer else OrangeMarkdownContainer
                        ),
                        border = BorderStroke(
                            2.dp,
                            if (product.markdownRequested) BlueExpressive else OrangeMarkdownBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (product.markdownRequested) BlueExpressive else OrangeMarkdown,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val iconVector = when (product.markdownStatus) {
                                            "ACCEPTED" -> Icons.Default.CheckCircle
                                            "PENDING_APPROVAL" -> Icons.Default.NotificationsActive
                                            "REJECTED" -> Icons.Default.Close
                                            else -> Icons.Default.LocalOffer
                                        }
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val statusTitle = when (product.markdownStatus) {
                                        "ACCEPTED" -> "REBAIXA ACEITA PELA GERÊNCIA"
                                        "PENDING_APPROVAL" -> "REBAIXA SOLICITADA (LEMBRETE DIÁRIO)"
                                        "REJECTED" -> "REBAIXA NÃO ACEITA PELA GERÊNCIA"
                                        else -> "REBAIXA DE PREÇO (≤15 DIAS)"
                                    }
                                    val statusColor = when (product.markdownStatus) {
                                        "ACCEPTED" -> Color(0xFF6EE7B7)
                                        "PENDING_APPROVAL" -> Color(0xFFFDE047)
                                        "REJECTED" -> Color(0xFFFCA5A5)
                                        else -> Color(0xFFFDBA74)
                                    }
                                    Text(
                                        text = statusTitle,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Faltam $daysRemaining dias para o vencimento.",
                                        fontSize = 12.sp,
                                        color = DarkTextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Markdown status explanation & actions
                            when {
                                !product.markdownRequested || product.markdownStatus == "NOT_REQUESTED" -> {
                                    Text(
                                        text = "O sistema de rebaixa funciona por confirmação: registre se a rebaixa foi solicitada para iniciar os lembretes automáticos de acompanhamento.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            onConfirmMarkdownRequested(product, true)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeMarkdown),
                                        shape = ExpressiveButtonShape,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("button_confirm_markdown_requested")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalOffer,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Confirmar: Rebaixa Solicitada", fontWeight = FontWeight.Bold)
                                    }
                                }

                                product.markdownStatus == "PENDING_APPROVAL" -> {
                                    val daysSince = product.getDaysSinceMarkdownRequest()
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = AmberAttentionContainer.copy(alpha = 0.45f)),
                                        border = BorderStroke(1.dp, AmberAttention.copy(alpha = 0.7f)),
                                        shape = ExpressiveCardShape,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = AmberAttention,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Lembrete Diário: Conferir Aceitação",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFDE047),
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Solicitada ${if (daysSince == 0L) "hoje" else "há $daysSince dia(s)"} (Pedido #${product.markdownRequestCount}). Verifique com a gerência se a rebaixa foi aceita ou não:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DarkTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onConfirmMarkdownAcceptance(product, true) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                                                    shape = ExpressiveButtonShape,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("button_markdown_accepted")
                                                ) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Aceita", fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { onConfirmMarkdownAcceptance(product, false) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive),
                                                    shape = ExpressiveButtonShape,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("button_markdown_rejected")
                                                ) {
                                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Não Aceita", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // Lembrete de 3 dias se já decorreram 3 dias sem resposta
                                    if (daysSince >= 3) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            color = OrangeMarkdownContainer,
                                            shape = ExpressiveCardShape,
                                            border = BorderStroke(1.dp, OrangeMarkdown)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "⚠️ Ciclo de 3 Dias Atingido",
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFDBA74),
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = "Já se passaram $daysSince dias. Solicite uma nova rebaixa de reforço.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = DarkTextPrimary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { onRequestRenewedMarkdown(product) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeMarkdown),
                                                    shape = ExpressiveChipShape
                                                ) {
                                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Nova Rebaixa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(
                                        onClick = { onConfirmMarkdownRequested(product, false) },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Desmarcar Solicitação", fontSize = 11.sp, color = DarkTextSecondary)
                                    }
                                }

                                product.markdownStatus == "ACCEPTED" -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = EmeraldSafeContainer.copy(alpha = 0.4f)),
                                        border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.6f)),
                                        shape = ExpressiveCardShape,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Rebaixa Aceita e em Vigor", fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7), fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "A gerência aprovou a rebaixa de preço. Item com etiqueta de desconto aplicada na gôndola.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DarkTextPrimary
                                            )
                                            if (product.getFormattedMarkdownCheckedDate() != null) {
                                                Text(
                                                    text = "Confirmada em: ${product.getFormattedMarkdownCheckedDate()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DarkTextSecondary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { onRequestRenewedMarkdown(product) },
                                        shape = ExpressiveButtonShape,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeMarkdown),
                                        border = BorderStroke(1.dp, OrangeMarkdown),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Solicitar Nova Rebaixa Adicional", fontWeight = FontWeight.Bold)
                                    }
                                }

                                product.markdownStatus == "REJECTED" -> {
                                    val daysSince = product.getDaysSinceMarkdownRequest()
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = RedExpressiveContainer.copy(alpha = 0.4f)),
                                        border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.6f)),
                                        shape = ExpressiveCardShape,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Rebaixa Não Aceita / Recusada", fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5), fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (daysSince >= 3) {
                                                    "Já se passaram $daysSince dias desde a última tentativa. Solicite uma nova rebaixa para tentar autorização da gerência e evitar perdas!"
                                                } else {
                                                    "A gerência não autorizou a rebaixa. O sistema lembrará automaticamente para solicitar uma nova rebaixa em ${3 - daysSince} dia(s)."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DarkTextPrimary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onRequestRenewedMarkdown(product) },
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeMarkdown),
                                        shape = ExpressiveButtonShape,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("button_request_renewed_markdown")
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (daysSince >= 3) "Solicitar Nova Rebaixa (Lembrete de 3 Dias)" else "Solicitar Nova Rebaixa Agora",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val statusDesc = when (product.markdownStatus) {
                                        "ACCEPTED" -> "ACEITA"
                                        "PENDING_APPROVAL" -> "AGUARDANDO APROVAÇÃO"
                                        "REJECTED" -> "NÃO ACEITA"
                                        else -> "PENDENTE DE SOLICITAÇÃO"
                                    }
                                    val textToCopy = """
                                        *SOLICITAÇÃO DE REBAIXA DE PREÇO*
                                        Produto: ${product.name}
                                        Cód. Interno: ${product.internalCode}
                                        Código de Barras: ${product.barcode}
                                        Lote: ${product.batchCode}
                                        Localização: ${product.location}
                                        Estoque Atual: ${product.quantity} ${product.unit}
                                        Data de Validade: ${product.getFormattedExpiryDate()} (faltam $daysRemaining dias)
                                        Status da Rebaixa: $statusDesc
                                        Tentativa de Solicitação: #${product.markdownRequestCount}
                                    """.trimIndent()
                                    val clip = ClipData.newPlainText("Rebaixa", textToCopy)
                                    clipboard.setPrimaryClip(clip)
                                },
                                shape = ExpressiveButtonShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD)),
                                border = BorderStroke(1.dp, BlueExpressive),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Dados para Enviar à Gerência", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // PRODUCT DETAILS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Dados do Produto & Validade",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkTextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        DetailRow(
                            icon = Icons.Default.CalendarMonth,
                            label = "Data de Validade",
                            value = "${product.getFormattedExpiryDate()} (${if (daysRemaining < 0) "Vencido há ${-daysRemaining}d" else if (daysRemaining == 0L) "Vence hoje" else "$daysRemaining dias restantes"})",
                            highlightColor = if (isMustRemove1Day) RedExpressive else if (isMarkdownEligible) OrangeMarkdown else BlueExpressive
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)

                        // Stock with quick +/- counter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = BlueExpressive,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Estoque Físico",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DarkTextSecondary
                                    )
                                    Text(
                                        text = "${product.quantity} ${product.unit}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkTextPrimary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = DarkSurfaceContainerHigh,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(
                                        onClick = { onAdjustStock(product, -1) },
                                        enabled = product.quantity > 0,
                                        modifier = Modifier.testTag("detail_qty_minus")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Diminuir 1", tint = DarkTextPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = BlueExpressiveContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(
                                        onClick = { onAdjustStock(product, 1) },
                                        modifier = Modifier.testTag("detail_qty_plus")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Aumentar 1", tint = Color(0xFF93C5FD), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (product.internalCode.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)
                            DetailRow(
                                icon = Icons.Default.QrCode,
                                label = "Código Interno / SKU",
                                value = product.internalCode
                            )
                        }

                        if (product.barcode.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)
                            DetailRow(
                                icon = Icons.Default.QrCode,
                                label = "Código de Barras / EAN",
                                value = product.barcode
                            )
                        }

                        if (product.batchCode.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)
                            DetailRow(
                                icon = Icons.Default.QrCode,
                                label = "Lote de Fabricação",
                                value = product.batchCode
                            )
                        }

                        if (product.location.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)
                            DetailRow(
                                icon = Icons.Default.LocationOn,
                                label = "Localização no Supermercado",
                                value = product.location
                            )
                        }

                        if (product.regularPrice > 0.0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)
                            DetailRow(
                                icon = Icons.Default.LocalOffer,
                                label = "Preço Regular",
                                value = "R$ ${"%.2f".format(product.regularPrice)}"
                            )
                        }

                        if (product.notes.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkBorder)
                            DetailRow(
                                icon = Icons.Default.History,
                                label = "Observações",
                                value = product.notes
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // HISTÓRICO COMPLETO DO PRODUTO
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BlueExpressive,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Histórico Completo do Produto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (historyList.isEmpty()) {
                item {
                    Surface(
                        color = DarkSurfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nenhum histórico registrado para este item até o momento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(historyList, key = { it.id }) { historyItem ->
                    ProductHistoryItemView(historyItem)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    highlightColor: Color = DarkTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BlueExpressive,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DarkTextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = highlightColor
            )
        }
    }
}

@Composable
private fun ProductHistoryItemView(history: ProductHistory) {
    Surface(
        color = DarkSurfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = when (history.actionType) {
                    "CONFERENCIA_SEGUNDA" -> BlueExpressiveContainer
                    "REBAIXA_SOLICITADA" -> OrangeMarkdownContainer
                    "RETIRADA_AREA_VENDA" -> RedExpressiveContainer
                    "ALERTA_VENCIMENTO" -> RedExpressiveContainer
                    else -> DarkSurfaceContainerHigh
                },
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (history.actionType) {
                            "CONFERENCIA_SEGUNDA" -> Icons.Default.CheckCircle
                            "REBAIXA_SOLICITADA" -> Icons.Default.LocalOffer
                            "RETIRADA_AREA_VENDA" -> Icons.Default.RemoveShoppingCart
                            "ALERTA_VENCIMENTO" -> Icons.Default.Warning
                            else -> Icons.Default.History
                        },
                        contentDescription = null,
                        tint = when (history.actionType) {
                            "CONFERENCIA_SEGUNDA" -> Color(0xFF93C5FD)
                            "REBAIXA_SOLICITADA" -> Color(0xFFFDBA74)
                            "RETIRADA_AREA_VENDA" -> RedExpressive
                            "ALERTA_VENCIMENTO" -> RedExpressive
                            else -> DarkTextSecondary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = history.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = DarkTextPrimary
                    )
                    Text(
                        text = history.getFormattedDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkTextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = history.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary
                )
            }
        }
    }
}
