package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EmeraldSafeContainer
import com.example.ui.theme.PurpleUrgent1Day
import com.example.ui.theme.PurpleUrgentContainer
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.StatusFilter

/**
 * Painel Dashboard Executivo com resumo de:
 * 1. Estoque vs Apresentado em Área de Vendas
 * 2. Vencimento inferior a 15 dias (<15 dias)
 * 3. Vencimento no próximo dia (≤1 dia)
 * 4. Vencidos sem baixa de retirada da área
 * 5. Rebaixa solicitada
 * 6. Aguardando rebaixa
 * 7. Rebaixa aprovada
 */
@Composable
fun DashboardControlPanel(
    metrics: DashboardMetrics,
    currentFilter: StatusFilter,
    onFilterSelected: (StatusFilter) -> Unit,
    isSyncing: Boolean = false,
    onUploadLocalToSupabase: (() -> Unit)? = null,
    onPullFromSupabase: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_metrics")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val purpleUrgent = PurpleUrgent1Day
    val yellowWarning = Color(0xFFFBBF24)
    val greenMarkdown = EmeraldSafe
    val redExpired = RedExpressive
    val blueStock = Color(0xFF60A5FA)
    val orangeAwaiting = Color(0xFFFB923C)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("dashboard_control_panel"),
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceContainer,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Cabeçalho do Painel de Controle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Dashboard do Setor",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkTextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Visão executiva e controle de validade",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botão Sincronizar / Baixar do Supabase
                    if (onPullFromSupabase != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onPullFromSupabase() }
                                .testTag("btn_pull_supabase")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                                    contentDescription = "Sincronizar Supabase",
                                    tint = if (isSyncing) Color(0xFF60A5FA) else EmeraldSafe,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Indicador Total Geral
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.clickable { onFilterSelected(StatusFilter.ALL) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${metrics.totalCount} itens",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }
                }
            }

            // Barra de Status de Sincronização com Supabase (Nuvem em tempo real)
            if (onUploadLocalToSupabase != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = if (isSyncing) Color(0xFF60A5FA) else EmeraldSafe,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "Sincronizando com Supabase..." else "Supabase Conectado • Conexão simultânea",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSyncing) Color(0xFF93C5FD) else Color(0xFF6EE7B7)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.clickable { onUploadLocalToSupabase() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Subir Todos",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Bloco de Resumo: Estoque Físico vs Apresentado em Área de Vendas
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceContainerHigh,
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Estoque
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = blueStock,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Em Estoque/Depósito",
                                fontSize = 10.sp,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = "${metrics.totalStockQuantity} un/kg",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .padding(vertical = 2.dp)
                    )

                    // Em Área de Vendas
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Exposto em Área",
                                fontSize = 10.sp,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = "${metrics.totalSalesQuantity} un/kg",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linha 1 de Métricas Críticas: Vencimento
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Vence Próximo Dia (≤1d / Vence Amanhã)
                CriticalCounterCard(
                    modifier = Modifier.weight(1f),
                    title = "Vence Próx. Dia",
                    subtitle = "≤ 1d (Retirar)",
                    count = metrics.expiringNextDayCount,
                    icon = Icons.Default.AccessTime,
                    accentColor = purpleUrgent,
                    containerColor = PurpleUrgentContainer,
                    isSelected = currentFilter == StatusFilter.EXPIRING_NEXT_DAY,
                    isPulsing = metrics.expiringNextDayCount > 0,
                    pulseAlpha = pulseAlpha,
                    testTag = "counter_expiring_next_day",
                    onClick = {
                        onFilterSelected(
                            if (currentFilter == StatusFilter.EXPIRING_NEXT_DAY) StatusFilter.ALL
                            else StatusFilter.EXPIRING_NEXT_DAY
                        )
                    }
                )

                // 2. Vencimento inferior a 15 dias (< 15 dias)
                CriticalCounterCard(
                    modifier = Modifier.weight(1f),
                    title = "Vence < 15 Dias",
                    subtitle = "Alerta Rebaixa",
                    count = metrics.expiringUnder15DaysCount,
                    icon = Icons.Default.Warning,
                    accentColor = yellowWarning,
                    containerColor = Color(0xFF422006),
                    isSelected = currentFilter == StatusFilter.EXPIRING_UNDER_15_DAYS,
                    isPulsing = metrics.expiringUnder15DaysCount > 0,
                    pulseAlpha = pulseAlpha,
                    testTag = "counter_expiring_under_15",
                    onClick = {
                        onFilterSelected(
                            if (currentFilter == StatusFilter.EXPIRING_UNDER_15_DAYS) StatusFilter.ALL
                            else StatusFilter.EXPIRING_UNDER_15_DAYS
                        )
                    }
                )

                // 3. Vencidos sem Baixa de Retirada
                CriticalCounterCard(
                    modifier = Modifier.weight(1f),
                    title = "Vencidos s/ Baixa",
                    subtitle = "Retirada Urgente",
                    count = metrics.expiredNotRemovedCount,
                    icon = Icons.Default.Warning,
                    accentColor = redExpired,
                    containerColor = RedExpressiveContainer,
                    isSelected = currentFilter == StatusFilter.EXPIRED_NOT_REMOVED,
                    isPulsing = metrics.expiredNotRemovedCount > 0,
                    pulseAlpha = pulseAlpha,
                    testTag = "counter_expired_not_removed",
                    onClick = {
                        onFilterSelected(
                            if (currentFilter == StatusFilter.EXPIRED_NOT_REMOVED) StatusFilter.ALL
                            else StatusFilter.EXPIRED_NOT_REMOVED
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Linha 2 de Métricas do Ciclo de Rebaixa: Solicitada | Aguarda | Aprovada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 4. Rebaixa Solicitada
                CriticalCounterCard(
                    modifier = Modifier.weight(1f),
                    title = "Rebaixa Pedida",
                    subtitle = "Solicitação enviada",
                    count = metrics.markdownRequestedCount,
                    icon = Icons.Default.Upload,
                    accentColor = Color(0xFF38BDF8),
                    containerColor = Color(0xFF0C4A6E),
                    isSelected = currentFilter == StatusFilter.MARKDOWN_REQUESTED,
                    isPulsing = false,
                    pulseAlpha = 1f,
                    testTag = "counter_markdown_requested",
                    onClick = {
                        onFilterSelected(
                            if (currentFilter == StatusFilter.MARKDOWN_REQUESTED) StatusFilter.ALL
                            else StatusFilter.MARKDOWN_REQUESTED
                        )
                    }
                )

                // 5. Aguarda Rebaixa
                CriticalCounterCard(
                    modifier = Modifier.weight(1f),
                    title = "Aguarda Rebaixa",
                    subtitle = "Em conferência",
                    count = metrics.awaitingMarkdownCount,
                    icon = Icons.Default.HourglassTop,
                    accentColor = orangeAwaiting,
                    containerColor = Color(0xFF431407),
                    isSelected = currentFilter == StatusFilter.AWAITING_REVIEW,
                    isPulsing = metrics.awaitingMarkdownCount > 0,
                    pulseAlpha = pulseAlpha,
                    testTag = "counter_awaiting_markdown",
                    onClick = {
                        onFilterSelected(
                            if (currentFilter == StatusFilter.AWAITING_REVIEW) StatusFilter.ALL
                            else StatusFilter.AWAITING_REVIEW
                        )
                    }
                )

                // 6. Rebaixa Aprovada
                CriticalCounterCard(
                    modifier = Modifier.weight(1f),
                    title = "Rebaixa Aprovada",
                    subtitle = "Preço aceito ativo",
                    count = metrics.markdownApprovedCount,
                    icon = Icons.Default.CheckCircle,
                    accentColor = greenMarkdown,
                    containerColor = EmeraldSafeContainer,
                    isSelected = currentFilter == StatusFilter.MARKDOWN_APPROVED,
                    isPulsing = false,
                    pulseAlpha = 1f,
                    testTag = "counter_markdown_approved",
                    onClick = {
                        onFilterSelected(
                            if (currentFilter == StatusFilter.MARKDOWN_APPROVED) StatusFilter.ALL
                            else StatusFilter.MARKDOWN_APPROVED
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CriticalCounterCard(
    title: String,
    subtitle: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    isSelected: Boolean,
    isPulsing: Boolean,
    pulseAlpha: Float,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        accentColor
    } else if (isPulsing && count > 0) {
        accentColor.copy(alpha = pulseAlpha)
    } else {
        DarkBorder
    }

    val borderWidth = if (isSelected || (isPulsing && count > 0)) 2.dp else 1.dp

    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                containerColor.copy(alpha = 0.5f)
            } else if (count > 0) {
                containerColor.copy(alpha = 0.22f)
            } else {
                DarkSurface
            }
        ),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) accentColor else containerColor.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contador Numérico
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (count > 0) accentColor else DarkTextSecondary
            )

            // Título do Contador
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else DarkTextPrimary,
                maxLines = 1,
                fontSize = 10.sp
            )

            // Subtítulo descritivo
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = DarkTextSecondary,
                maxLines = 1
            )
        }
    }
}
