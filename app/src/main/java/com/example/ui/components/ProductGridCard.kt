package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.EmeraldSafeContainer
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.PurpleUrgent1Day
import com.example.ui.theme.PurpleUrgentContainer
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer

@Composable
fun ProductGridCard(
    product: Product,
    isSelected: Boolean = false,
    onSelectCard: () -> Unit,
    onCardClick: () -> Unit,
    onRequestMarkdown: (Product) -> Unit,
    onConfirmMarkdownAcceptance: (Product, Boolean) -> Unit,
    onConfirmRemoval: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysRemaining = product.getDaysRemaining()
    val isExpired = daysRemaining < 0
    val isRemoved = product.isRemovedFromSales

    // 1. Faltando 1 dia: pisca em roxo
    val isPurpleBlink1Day = daysRemaining in 0..1 && !isRemoved

    // 2. Rebaixa aceita: fica verde sólido (pois não tem mais o que fazer)
    val isGreenAccepted = product.markdownStatus == "ACCEPTED" && !isRemoved

    // 3. Faltando 15 dias até ser aceita OU rebaixa clicada aguardando aceite (lembrando todo dia): pisca em amarelo
    val isYellowBlink = !isRemoved && !isGreenAccepted && !isPurpleBlink1Day && !isExpired &&
            (daysRemaining in 2..15 || (product.markdownRequested && product.markdownStatus == "PENDING_APPROVAL"))

    // Infinite transition for pulsing states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_card_border")
    val pulseBorderWidth by infiniteTransition.animateFloat(
        initialValue = 2.0f,
        targetValue = 3.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_width"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Vibrant colors according to user specification
    val yellowPulseColor = Color(0xFFEAB308) // Vibrant Yellow

    // Card border calculated by state
    val cardBorder = when {
        isRemoved -> {
            BorderStroke(1.2.dp, Color(0xFF64748B).copy(alpha = 0.6f))
        }
        isExpired -> {
            BorderStroke(2.dp, RedExpressive.copy(alpha = 0.95f))
        }
        isPurpleBlink1Day -> {
            // "quando tiver faltando um dia para vencer o produto passar a ficar piscando em roxo"
            BorderStroke(pulseBorderWidth.dp, PurpleUrgent1Day.copy(alpha = pulseAlpha))
        }
        isGreenAccepted -> {
            // "passa a ficar verde pois o produto entrou em rebaixa e não tem mais o que fazer"
            BorderStroke(2.5.dp, EmeraldSafe)
        }
        isYellowBlink -> {
            // "quando clicar em rebaixa deverá ficar lembrando todos os dias de conferir o produto e ele ficará piscando em amarelo por causa disso até o operador voltar e dizer se foi aceita ou não"
            // "e quando falta 15 dias para vencer o produto pisca em amarelo até a rebaixa ser aprovada e o operador sinalizar"
            BorderStroke(pulseBorderWidth.dp, yellowPulseColor.copy(alpha = pulseAlpha))
        }
        daysRemaining <= 30 -> {
            BorderStroke(1.5.dp, AmberAttention.copy(alpha = 0.7f))
        }
        else -> {
            BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.35f))
        }
    }

    // Card background color tinting
    val cardBg = when {
        isRemoved -> DarkSurfaceContainer.copy(alpha = 0.7f)
        isGreenAccepted -> Color(0xFF064E3B).copy(alpha = 0.35f) // Subtle green tint
        isPurpleBlink1Day -> PurpleUrgentContainer.copy(alpha = 0.30f) // Subtle purple tint
        isYellowBlink -> Color(0xFF713F12).copy(alpha = 0.25f) // Subtle yellow glow
        else -> DarkSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clickable(onClick = onSelectCard),
        shape = ExpressiveCardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPurpleBlink1Day || isYellowBlink || isGreenAccepted) 6.dp else 2.dp
        ),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Top Row: Setor/Seção & Life-State Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Setor & Seção Chip
                Surface(
                    color = BlueExpressiveContainer.copy(alpha = 0.7f),
                    shape = ExpressiveChipShape
                ) {
                    val sectorSectionLabel = if (product.section.isNotBlank()) {
                        "${product.category} • ${product.section}"
                    } else {
                        product.category
                    }
                    Text(
                        text = sectorSectionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expiry Life-State Badge
                when {
                    isRemoved -> {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = ExpressiveChipShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "RECOLHIDO",
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    isExpired -> {
                        Surface(
                            color = RedExpressiveContainer,
                            shape = ExpressiveChipShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Vencido",
                                    tint = RedExpressive,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "VENCIDO",
                                    color = RedExpressive,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    isPurpleBlink1Day -> {
                        Surface(
                            color = PurpleUrgentContainer,
                            shape = ExpressiveChipShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(PurpleUrgent1Day.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "RETIRAR (1d)",
                                    color = Color(0xFFE9D5FF),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    isGreenAccepted -> {
                        Surface(
                            color = EmeraldSafeContainer,
                            shape = ExpressiveChipShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldSafe,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "REBAIXADO",
                                    color = EmeraldSafe,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    isYellowBlink -> {
                        Surface(
                            color = Color(0xFF451A03),
                            shape = ExpressiveChipShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(yellowPulseColor.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val label = if (product.markdownRequested && product.markdownStatus == "PENDING_APPROVAL") {
                                    "CONFERIR DIARIAMENTE"
                                } else {
                                    "${daysRemaining}d (REBAIXA)"
                                }
                                Text(
                                    text = label,
                                    color = Color(0xFFFDE047),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    daysRemaining <= 30 -> {
                        Surface(
                            color = AmberAttentionContainer,
                            shape = ExpressiveChipShape
                        ) {
                            Text(
                                text = "${daysRemaining}d p/ vencer",
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    else -> {
                        Surface(
                            color = BlueExpressiveContainer.copy(alpha = 0.5f),
                            shape = ExpressiveChipShape
                        ) {
                            Text(
                                text = "${daysRemaining}d",
                                color = Color(0xFF93C5FD),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            // Product Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            // PLU / Barcode subtitle
            if (product.internalCode.isNotBlank() || product.barcode.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = BlueExpressive.copy(alpha = 0.8f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = when {
                            product.internalCode.isNotBlank() && product.barcode.isNotBlank() ->
                                "PLU: ${product.internalCode} • EAN: ${product.barcode}"
                            product.internalCode.isNotBlank() -> "PLU: ${product.internalCode}"
                            else -> "EAN: ${product.barcode}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pulsing Status Banners
            if (isPurpleBlink1Day) {
                Surface(
                    color = PurpleUrgentContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = PurpleUrgent1Day,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Piscando Roxo: Retirar em ≤1d da venda!",
                            color = Color(0xFFE9D5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
            } else if (isGreenAccepted) {
                Surface(
                    color = EmeraldSafeContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSafe,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Rebaixa Aceita (Verde - nada a fazer)",
                            color = EmeraldSafe,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
            } else if (isYellowBlink) {
                Surface(
                    color = Color(0xFF451A03),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationImportant,
                            contentDescription = null,
                            tint = yellowPulseColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        val bannerText = if (product.markdownRequested && product.markdownStatus == "PENDING_APPROVAL") {
                            "Piscando Amarelo: Lembrete diário de conferência!"
                        } else {
                            "Piscando Amarelo: Solicitar rebaixa (≤15d)!"
                        }
                        Text(
                            text = bannerText,
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
            }

            // Expiry Date and Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = when {
                            isExpired -> RedExpressive
                            isPurpleBlink1Day -> PurpleUrgent1Day
                            isYellowBlink -> yellowPulseColor
                            isGreenAccepted -> EmeraldSafe
                            else -> DarkTextSecondary
                        },
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = product.getFormattedExpiryDate(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkTextPrimary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${product.quantity} ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary,
                        fontSize = 11.sp
                    )
                }
            }

            // Location
            if (product.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = BlueExpressive.copy(alpha = 0.8f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = product.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // =========================================================================
            // Action Panel when card is selected: Rebaixar Preço & Retirada
            // "nos cards quando selecionados coloque as opções de rebaixar preço retirada"
            // =========================================================================
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceContainerHigh,
                        border = BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Ações Rápidas:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // 1. REBAIXA DE PREÇO (com fluxo de conferência se foi aceita ou não)
                            if (product.markdownStatus == "PENDING_APPROVAL") {
                                // Lembrete diário: operador volta para sinalizar se foi aceita
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "A rebaixa foi aceita pela gerência?",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = yellowPulseColor,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { onConfirmMarkdownAcceptance(product, true) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Sim, Aceita", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { onConfirmMarkdownAcceptance(product, false) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedExpressive)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Recusada", fontSize = 11.sp)
                                        }
                                    }
                                }
                            } else if (product.markdownStatus == "ACCEPTED") {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldSafeContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Rebaixa Aceita (Desconto aplicado)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSafe
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onRequestMarkdown(product) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = yellowPulseColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rebaixar Preço",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2. RETIRADA DA ÁREA DE VENDA
                            if (!isRemoved) {
                                Button(
                                    onClick = { onConfirmRemoval(product) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleUrgent1Day)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Outbox,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Retirada da Gôndola",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // 3. ABRIR FICHA COMPLETA
                            OutlinedButton(
                                onClick = onCardClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ver Detalhes e Ficha Completa", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
