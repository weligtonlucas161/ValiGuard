package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
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
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.OrangeMarkdown
import com.example.ui.theme.OrangeMarkdownContainer
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer
import com.example.ui.viewmodel.MonitoringMetrics

enum class MonitoringFilter(val label: String) {
    ALL("Todos em Alerta"),
    MUST_REMOVE("🚨 Retirar ≤1d"),
    DAILY_CHECK("🔔 Lembrete Diário"),
    RENEW_3_DAYS("🔄 Lembrete 3 Dias"),
    MARKDOWN_PENDING("🏷️ Rebaixa Não Solicitada"),
    ALREADY_REMOVED("✅ Já Recolhidos")
}

@Composable
fun MonitoringScreen(
    monitoringProducts: List<Product>,
    metrics: MonitoringMetrics,
    onProductClick: (Product) -> Unit,
    onConfirmRemoval: (Product, String) -> Unit,
    onRequestMarkdown: (Product) -> Unit,
    onConfirmMarkdownRequested: (Product, Boolean) -> Unit = { _, _ -> },
    onConfirmMarkdownAcceptance: (Product, Boolean) -> Unit = { _, _ -> },
    onRequestRenewedMarkdown: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(MonitoringFilter.ALL) }
    var productToConfirmRemoval by remember { mutableStateOf<Product?>(null) }
    var removalReason by remember { mutableStateOf("Retirada obrigatória 1 dia antes do vencimento") }

    val now = System.currentTimeMillis()

    val filteredList = remember(monitoringProducts, selectedFilter) {
        when (selectedFilter) {
            MonitoringFilter.ALL -> monitoringProducts
            MonitoringFilter.MUST_REMOVE -> monitoringProducts.filter {
                val days = it.getDaysRemaining(now)
                !it.isRemovedFromSales && (days in 0..1 || days < 0)
            }
            MonitoringFilter.DAILY_CHECK -> monitoringProducts.filter {
                !it.isRemovedFromSales && it.isDueForDailyAcceptanceCheck()
            }
            MonitoringFilter.RENEW_3_DAYS -> monitoringProducts.filter {
                !it.isRemovedFromSales && it.isDueForNewMarkdownRequest(now) && it.markdownRequested
            }
            MonitoringFilter.MARKDOWN_PENDING -> monitoringProducts.filter {
                val days = it.getDaysRemaining(now)
                !it.isRemovedFromSales && days in 0..15 && (!it.markdownRequested || it.markdownStatus == "NOT_REQUESTED")
            }
            MonitoringFilter.ALREADY_REMOVED -> monitoringProducts.filter { it.isRemovedFromSales }
        }
    }

    // Infinite transition for urgent removal alert
    val infiniteTransition = rememberInfiniteTransition(label = "monitoring_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("monitoring_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header: Supermarket Sales Floor Removal Protocol
        item {
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                border = BorderStroke(1.5.dp, RedExpressive.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = RedExpressiveContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RemoveShoppingCart,
                                    contentDescription = null,
                                    tint = RedExpressive,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Controle de Monitoramento & Retirada",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                            Text(
                                text = "Regra de gôndola: retirar 1 dia antes da área de venda",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 KPI Cards (2x2 Grid)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Row: Urgent Removal & Daily Check Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Urgent Removal Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RedExpressiveContainer,
                                border = BorderStroke(1.dp, RedExpressive.copy(alpha = pulseAlpha)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(RedExpressive.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Retirar ≤1d",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCA5A5)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${metrics.urgentRemovalCount}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkTextPrimary
                                    )
                                    Text(
                                        text = "Itens na gôndola",
                                        fontSize = 10.sp,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }

                            // Daily Markdown Check Card (Lembrete Diário)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BlueExpressiveContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFF93C5FD),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Lembrete Diário",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF93C5FD)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${metrics.dailyCheckCount}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkTextPrimary
                                    )
                                    Text(
                                        text = "Verificar se aceita",
                                        fontSize = 10.sp,
                                        color = Color(0xFF93C5FD)
                                    )
                                }
                            }
                        }

                        // Bottom Row: 3-Day Renewal Cycle & Markdown Not Requested
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 3-Day Renewal Reminder Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmberAttentionContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, AmberAttention.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE047),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ciclo 3 Dias",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFDE047)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${metrics.renewIn3DaysCount}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkTextPrimary
                                    )
                                    Text(
                                        text = "Renovar rebaixa",
                                        fontSize = 10.sp,
                                        color = Color(0xFFFDE047)
                                    )
                                }
                            }

                            // Markdown Not Requested Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = OrangeMarkdownContainer,
                                border = BorderStroke(1.dp, OrangeMarkdown.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Não Solicitada",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDBA74)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${metrics.markdownPendingCount}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkTextPrimary
                                    )
                                    Text(
                                        text = "≤15d sem pedido",
                                        fontSize = 10.sp,
                                        color = Color(0xFFFDBA74)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips Row (Scrollable)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonitoringFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueExpressiveContainer,
                            selectedLabelColor = Color(0xFF93C5FD),
                            containerColor = DarkSurface,
                            labelColor = DarkTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedFilter == filter) BlueExpressive else DarkBorder,
                            enabled = true,
                            selected = selectedFilter == filter
                        )
                    )
                }
            }
        }

        // Empty state
        if (filteredList.isEmpty()) {
            item {
                Card(
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum produto nesta categoria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "Gôndolas e área de venda em conformidade com os prazos de validade.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Monitoring Cards
        items(filteredList, key = { it.id }) { product ->
            val daysRemaining = product.getDaysRemaining(now)
            val isUrgentRemoval = !product.isRemovedFromSales && (daysRemaining in 0..1 || daysRemaining < 0)

            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (product.isRemovedFromSales) DarkSurface.copy(alpha = 0.7f) else DarkSurface
                ),
                border = when {
                    product.isRemovedFromSales -> BorderStroke(1.dp, Color(0xFF64748B))
                    isUrgentRemoval -> BorderStroke(2.5.dp, RedExpressive.copy(alpha = pulseAlpha))
                    else -> BorderStroke(2.dp, OrangeMarkdown)
                },
                elevation = CardDefaults.cardElevation(defaultElevation = if (isUrgentRemoval) 6.dp else 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monitoring_card_${product.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Status
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
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF93C5FD),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Urgency Tag
                        when {
                            product.isRemovedFromSales -> {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = ExpressiveChipShape
                                ) {
                                    Text(
                                        text = "✅ FORA DA VENDA",
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            daysRemaining < 0 -> {
                                Surface(
                                    color = RedExpressiveContainer,
                                    shape = ExpressiveChipShape
                                ) {
                                    Text(
                                        text = "⛔ VENCIDO",
                                        color = RedExpressive,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            daysRemaining <= 1 -> {
                                Surface(
                                    color = RedExpressiveContainer,
                                    shape = ExpressiveChipShape
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(RedExpressive.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (daysRemaining == 0L) "RETIRAR HOJE" else "RETIRAR (FALTA 1 DIA)",
                                            color = Color(0xFFFCA5A5),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    color = OrangeMarkdownContainer,
                                    shape = ExpressiveChipShape
                                ) {
                                    Text(
                                        text = "Faltam ${daysRemaining}d",
                                        color = Color(0xFFFDBA74),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )

                    // Codes & Details
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (product.internalCode.isNotBlank()) {
                            Text(
                                text = "Cód. Interno: ${product.internalCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (product.barcode.isNotBlank()) {
                            Text(
                                text = "Barras: ${product.barcode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stock & Expiry Info Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (isUrgentRemoval) RedExpressive else OrangeMarkdown,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vencimento: ${product.getFormattedExpiryDate()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = DarkTextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Estoque: ${product.quantity} ${product.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (product.location.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = BlueExpressive,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Local: ${product.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Removal confirmation record info if already removed
                    if (product.isRemovedFromSales) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recolhido da área de venda em ${product.getFormattedRemovalTime() ?: "N/A"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    // Markdown (Rebaixa) Workflow Card (Confirmation, Daily Reminder & 3-Day Renewal)
                    if (!product.isRemovedFromSales && daysRemaining in 0..15) {
                        Spacer(modifier = Modifier.height(10.dp))
                        when {
                            // 1. Not requested yet
                            !product.markdownRequested || product.markdownStatus == "NOT_REQUESTED" -> {
                                Surface(
                                    color = OrangeMarkdownContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, OrangeMarkdown.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Rebaixa de Preço (≤15 dias)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFFFDBA74)
                                            )
                                            Text(
                                                text = "Ainda não solicitada",
                                                fontSize = 11.sp,
                                                color = DarkTextSecondary
                                            )
                                        }
                                        Button(
                                            onClick = { onConfirmMarkdownRequested(product, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = OrangeMarkdown),
                                            shape = ExpressiveChipShape,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("btn_request_markdown_${product.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Confirmar Solicitação",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Pending approval (Daily reminder to check with management)
                            product.markdownStatus == "PENDING_APPROVAL" -> {
                                Surface(
                                    color = BlueExpressiveContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = Color(0xFF93C5FD),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Lembrete Diário: A rebaixa foi aceita?",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF93C5FD)
                                                )
                                            }
                                            Surface(
                                                color = BlueExpressive,
                                                shape = ExpressiveChipShape
                                            ) {
                                                Text(
                                                    text = "Aguardando",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        val daysSince = product.getDaysSinceMarkdownRequest()
                                        Text(
                                            text = "Solicitado em ${product.getFormattedMarkdownRequestTime()} (há $daysSince ${if (daysSince == 1L) "dia" else "dias"}). Todo dia confira se a gerência aceitou.",
                                            fontSize = 11.sp,
                                            color = DarkTextPrimary,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                        )

                                        // Actions: Aceita / Não Aceita
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onConfirmMarkdownAcceptance(product, true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                                                shape = ExpressiveChipShape,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f).testTag("btn_accept_markdown_${product.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Foi Aceita", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { onConfirmMarkdownAcceptance(product, false) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                                                border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.5f)),
                                                shape = ExpressiveChipShape,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f).testTag("btn_reject_markdown_${product.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Não Aceita", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // 3-day reminder renewal if >= 3 days
                                        if (daysSince >= 3) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                color = AmberAttentionContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "⚠️ Sem resposta há $daysSince dias.",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFFDE047),
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    TextButton(
                                                        onClick = { onRequestRenewedMarkdown(product) },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFDE047),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Renovar (3d)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Rejected - Every 3 days prompt for a new request
                            product.markdownStatus == "REJECTED" -> {
                                val daysSince = product.getDaysSinceMarkdownRequest()
                                val isDueForRenewal = daysSince >= 3
                                Surface(
                                    color = if (isDueForRenewal) AmberAttentionContainer.copy(alpha = 0.7f) else DarkSurfaceContainerHigh,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (isDueForRenewal) AmberAttention else DarkBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isDueForRenewal) Icons.Default.Refresh else Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = if (isDueForRenewal) Color(0xFFFDE047) else RedExpressive,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isDueForRenewal) "Lembrete de 3 Dias: Solicitar Nova Rebaixa" else "Rebaixa Recusada",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDueForRenewal) Color(0xFFFDE047) else Color(0xFFFCA5A5)
                                                )
                                            }
                                            Surface(
                                                color = if (isDueForRenewal) AmberAttention else RedExpressiveContainer,
                                                shape = ExpressiveChipShape
                                            ) {
                                                Text(
                                                    text = if (isDueForRenewal) "Nova Tentativa" else "Recusada",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDueForRenewal) Color.Black else Color(0xFFFCA5A5),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (isDueForRenewal)
                                                "Já se passaram $daysSince dias desde a última tentativa. Solicite uma nova rebaixa à gerência para evitar descarte."
                                            else
                                                "A gerência não aprovou a última rebaixa. Um novo lembrete será exibido após 3 dias (${3 - daysSince}d restante(s)).",
                                            fontSize = 11.sp,
                                            color = DarkTextPrimary,
                                            modifier = Modifier.padding(top = 4.dp, bottom = if (isDueForRenewal) 8.dp else 0.dp)
                                        )

                                        if (isDueForRenewal) {
                                            Button(
                                                onClick = { onRequestRenewedMarkdown(product) },
                                                colors = ButtonDefaults.buttonColors(containerColor = AmberAttention),
                                                shape = ExpressiveChipShape,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("btn_renew_markdown_${product.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Solicitar Nova Rebaixa Agora",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Accepted
                            product.markdownStatus == "ACCEPTED" -> {
                                Surface(
                                    color = Color(0xFF064E3B),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF6EE7B7),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Rebaixa Aceita e Aplicada",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF6EE7B7)
                                            )
                                            Text(
                                                text = "Preço reduzido na gôndola para acelerar saída.",
                                                fontSize = 10.sp,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Actions Bar for this item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!product.isRemovedFromSales) {
                            // Primary Action: Confirm Removal from Sales Floor
                            Button(
                                onClick = {
                                    productToConfirmRemoval = product
                                    removalReason = if (daysRemaining <= 1)
                                        "Retirada preventiva 1 dia antes do vencimento"
                                    else
                                        "Retirada antecipada por controle de perdas"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isUrgentRemoval) RedExpressive else BlueExpressive
                                ),
                                shape = ExpressiveChipShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_remove_sales_${product.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Confirmar Retirada",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Ver Detalhes
                        OutlinedButton(
                            onClick = { onProductClick(product) },
                            shape = ExpressiveChipShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextPrimary),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = if (product.isRemovedFromSales) Modifier.fillMaxWidth() else Modifier
                        ) {
                            Text(
                                text = "Detalhes",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Confirm Removal from Sales Floor
    productToConfirmRemoval?.let { product ->
        AlertDialog(
            onDismissRequest = { productToConfirmRemoval = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.RemoveShoppingCart,
                    contentDescription = null,
                    tint = RedExpressive,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Confirmar Retirada da Área de Venda",
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Confirma a retirada física de \"${product.name}\" (${product.quantity} ${product.unit}) da gôndola/ilha de vendas?",
                        color = DarkTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Vencimento: ${product.getFormattedExpiryDate()} (Faltam ${product.getDaysRemaining(now)} dias)",
                        fontWeight = FontWeight.Bold,
                        color = if (product.getDaysRemaining(now) <= 1) RedExpressive else OrangeMarkdown,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = removalReason,
                        onValueChange = { removalReason = it },
                        label = { Text("Motivo / Destino da mercadoria") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmRemoval(product, removalReason)
                        productToConfirmRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Confirmar Retirada", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToConfirmRemoval = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainerHigh,
            shape = ExpressiveCardShape
        )
    }
}
