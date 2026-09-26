package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAttention
import com.example.ui.theme.AmberAttentionContainer
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.OrangeMarkdown
import com.example.ui.theme.OrangeMarkdownContainer
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.StatusFilter

@Composable
fun StatusSummaryCards(
    metrics: DashboardMetrics,
    currentFilter: StatusFilter,
    onFilterSelected: (StatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Retirar da Área de Venda (<= 1 dia) - Red Expressive
        ExpressiveMetricCard(
            modifier = Modifier.weight(1f),
            title = "Retirar",
            subtitle = "≤ 1 dia",
            count = metrics.urgentRemovalCount,
            icon = Icons.Default.RemoveShoppingCart,
            accentColor = RedExpressive,
            containerColor = RedExpressiveContainer,
            isSelected = currentFilter == StatusFilter.MUST_REMOVE_1_DAY,
            testTag = "metric_must_remove",
            onClick = {
                onFilterSelected(
                    if (currentFilter == StatusFilter.MUST_REMOVE_1_DAY) StatusFilter.ALL
                    else StatusFilter.MUST_REMOVE_1_DAY
                )
            }
        )

        // 2. Rebaixa de Preço (<= 15d) - Orange Markdown
        ExpressiveMetricCard(
            modifier = Modifier.weight(1f),
            title = "Rebaixa",
            subtitle = "≤ 15 dias",
            count = metrics.markdownNeededCount,
            icon = Icons.Default.LocalOffer,
            accentColor = OrangeMarkdown,
            containerColor = OrangeMarkdownContainer,
            isSelected = currentFilter == StatusFilter.MARKDOWN_REBAIXA,
            testTag = "metric_markdown",
            onClick = {
                onFilterSelected(
                    if (currentFilter == StatusFilter.MARKDOWN_REBAIXA) StatusFilter.ALL
                    else StatusFilter.MARKDOWN_REBAIXA
                )
            }
        )

        // 3. Vencidos - Dark Red
        ExpressiveMetricCard(
            modifier = Modifier.weight(1f),
            title = "Vencidos",
            subtitle = "Retirados",
            count = metrics.expiredCount,
            icon = Icons.Default.Error,
            accentColor = if (metrics.expiredCount > 0) RedExpressive else DarkTextSecondary,
            containerColor = if (metrics.expiredCount > 0) RedExpressiveContainer else DarkSurface,
            isSelected = currentFilter == StatusFilter.EXPIRED,
            testTag = "metric_expired",
            onClick = {
                onFilterSelected(
                    if (currentFilter == StatusFilter.EXPIRED) StatusFilter.ALL
                    else StatusFilter.EXPIRED
                )
            }
        )

        // 4. Total do Setor - Blue Expressive
        ExpressiveMetricCard(
            modifier = Modifier.weight(1f),
            title = "Total",
            subtitle = "Setor",
            count = metrics.totalCount,
            icon = Icons.Default.Inventory2,
            accentColor = BlueExpressive,
            containerColor = BlueExpressiveContainer,
            isSelected = currentFilter == StatusFilter.ALL,
            testTag = "metric_total",
            onClick = { onFilterSelected(StatusFilter.ALL) }
        )
    }
}

@Composable
private fun ExpressiveMetricCard(
    title: String,
    subtitle: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (isSelected) {
        BorderStroke(2.dp, accentColor)
    } else {
        BorderStroke(1.dp, DarkBorder)
    }

    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = ExpressiveCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) containerColor.copy(alpha = 0.35f) else DarkSurface
        ),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) accentColor else containerColor.copy(alpha = 0.5f),
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

            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) accentColor else DarkTextPrimary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else DarkTextPrimary,
                maxLines = 1,
                fontSize = 11.sp
            )

            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = DarkTextSecondary,
                maxLines = 1
            )
        }
    }
}
