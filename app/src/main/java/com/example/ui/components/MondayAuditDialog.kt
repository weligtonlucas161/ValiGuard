package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.theme.ExpressiveButtonShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressiveDialogShape
import com.example.ui.theme.OrangeMarkdown
import com.example.ui.theme.RedExpressive

@Composable
fun MondayAuditDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onFinishAudit: (List<Pair<Product, Int>>) -> Unit
) {
    // Map of product id -> verified count
    val verifiedCounts = remember {
        mutableStateMapOf<Long, Int>().apply {
            products.forEach { put(it.id, it.quantity) }
        }
    }
    // Set of product ids marked as checked
    val checkedProductIds = remember { mutableStateMapOf<Long, Boolean>() }

    val checkedCount = checkedProductIds.count { it.value }
    val totalCount = products.size
    val progress = if (totalCount > 0) checkedCount.toFloat() / totalCount.toFloat() else 0f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp)
                .testTag("monday_audit_dialog"),
            shape = ExpressiveDialogShape,
            color = DarkSurface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                            color = BlueExpressiveContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = BlueExpressive,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Conferência de Segunda-feira",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkTextPrimary
                            )
                            Text(
                                text = "Auditoria manual pré-expediente",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("button_close_audit")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = DarkTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar and Counter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceContainer,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progresso da Auditoria",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                            Text(
                                text = "$checkedCount de $totalCount itens conferidos",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (checkedCount == totalCount && totalCount > 0) EmeraldSafe else BlueExpressive
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (checkedCount == totalCount && totalCount > 0) EmeraldSafe else BlueExpressive,
                            trackColor = DarkSurfaceContainerHigh
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Products Checklist
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val isChecked = checkedProductIds[product.id] == true
                        val currentQty = verifiedCounts[product.id] ?: product.quantity
                        val daysRemaining = product.getDaysRemaining()

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) DarkSurfaceContainerHigh else DarkSurfaceContainer
                            ),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isChecked -> EmeraldSafe.copy(alpha = 0.6f)
                                    daysRemaining in 0..1 -> RedExpressive
                                    daysRemaining <= 15 -> OrangeMarkdown
                                    else -> DarkBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("audit_item_${product.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product info
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = BlueExpressiveContainer,
                                            shape = ExpressiveChipShape
                                        ) {
                                            Text(
                                                text = product.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF93C5FD),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Days remaining
                                        Text(
                                            text = when {
                                                daysRemaining < 0 -> "Vencido (${-daysRemaining}d)"
                                                daysRemaining <= 1 -> "🚨 Retirar (≤1d)"
                                                daysRemaining <= 15 -> "🏷️ Rebaixa (${daysRemaining}d)"
                                                else -> "${daysRemaining}d"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                daysRemaining <= 1 -> RedExpressive
                                                daysRemaining <= 15 -> OrangeMarkdown
                                                else -> Color(0xFF93C5FD)
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTextPrimary,
                                        maxLines = 1
                                    )

                                    if (product.internalCode.isNotBlank() || product.barcode.isNotBlank()) {
                                        Text(
                                            text = if (product.internalCode.isNotBlank()) "Cód: ${product.internalCode}" else "Bar: ${product.barcode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Text(
                                        text = "Val: ${product.getFormattedExpiryDate()} • Local: ${product.location.ifBlank { "Gôndola" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Quantity adjustment + Check Button
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = DarkSurfaceContainerHigh,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (currentQty > 0) verifiedCounts[product.id] = currentQty - 1
                                            }
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Menos", tint = DarkTextPrimary, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Text(
                                        text = "$currentQty",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DarkTextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = DarkSurfaceContainerHigh,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                verifiedCounts[product.id] = currentQty + 1
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Mais", tint = DarkTextPrimary, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Confirm Check Button
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isChecked) EmeraldSafe else DarkSurfaceContainerHigh,
                                        border = if (!isChecked) BorderStroke(1.dp, DarkBorder) else null,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                checkedProductIds[product.id] = !isChecked
                                            },
                                            modifier = Modifier.testTag("btn_check_product_${product.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Conferido",
                                                tint = if (isChecked) Color.White else DarkTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = ExpressiveButtonShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fechar")
                    }

                    Button(
                        onClick = {
                            val verifiedList = products.map { p ->
                                val qty = verifiedCounts[p.id] ?: p.quantity
                                Pair(p, qty)
                            }
                            onFinishAudit(verifiedList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                        shape = ExpressiveButtonShape,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_complete_audit")
                    ) {
                        Text("Concluir Conferência", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
