package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CargoManager
import com.example.data.StockLockManager
import com.example.data.supabase.Produto
import com.example.data.supabase.SessionHolder
import com.example.ui.screens.calculateDaysRemaining
import com.example.ui.screens.getExpiryStatus
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.RedExpressive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DetalhesProdutoDialog(
    produto: Produto,
    onDismiss: () -> Unit,
    onDelete: (Produto) -> Unit,
    onProdutoUpdated: (Produto) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser = SessionHolder.currentUser
    var produtoAtual by remember(produto) { mutableStateOf(produto) }
    var showEditarDialog by remember { mutableStateOf(false) }

    // Observa se o produto está em edição por outro operador
    val lockedMap by StockLockManager.lockedProducts.collectAsState()
    val lockByOther = remember(lockedMap, produtoAtual.id) {
        StockLockManager.getLockByOther(produtoAtual.id, currentUser?.matricula ?: "")
    }

    val daysRemaining = remember(produtoAtual.data_vencimento) {
        calculateDaysRemaining(produtoAtual.data_vencimento)
    }
    val status = remember(daysRemaining) {
        getExpiryStatus(daysRemaining)
    }

    val canOperate = CargoManager.podeRealizarAcaoNoSetor(currentUser, produtoAtual.setor) && lockByOther == null
    val userSector = CargoManager.resolveUserSector(currentUser)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = RedExpressive,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Confirmar Exclusão", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    text = "Deseja remover \"${produtoAtual.nome}\" do estoque? Esta ação não pode ser desfeita no Supabase.",
                    color = DarkTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(produtoAtual)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Sim, Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showEditarDialog) {
        EditarProdutoDialog(
            produto = produtoAtual,
            onDismiss = { showEditarDialog = false },
            onProdutoUpdated = { updated ->
                produtoAtual = updated
                onProdutoUpdated(updated)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = status.color.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, status.color),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = status.icon,
                            contentDescription = null,
                            tint = status.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Detalhes do Produto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Setor: ${produtoAtual.setor}",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (lockByOther != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Bloqueado",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PRODUTO EM MONITORAMENTO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                                Text(
                                    text = "Aberto por ${lockByOther.usuarioNome} (${lockByOther.usuarioMatricula}). Edições bloqueadas para evitar sobrescrita.",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Nome do Produto
                Text(
                    text = produtoAtual.nome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Card de Validade e Status
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                    border = BorderStroke(1.dp, status.color.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Condição de Validade:",
                                fontSize = 10.sp,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = status.title.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = status.color
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = status.color.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, status.color)
                        ) {
                            Text(
                                text = when {
                                    daysRemaining < 0 -> "Vencido há ${-daysRemaining} dia(s)"
                                    daysRemaining == 0L -> "Vence hoje!"
                                    daysRemaining == 1L -> "Vence amanhã!"
                                    else -> "Restam $daysRemaining dias"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = status.color,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detalhes em Grid
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceContainerHigh,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DetailItemRow(
                            icon = Icons.Default.QrCode,
                            label = "Código de Barras (EAN)",
                            value = produtoAtual.codigo_barras
                        )

                        if (!produtoAtual.plu.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailItemRow(
                                icon = Icons.Default.Info,
                                label = "Código PLU (Balança/FLV)",
                                value = produtoAtual.plu!!
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        DetailItemRow(
                            icon = Icons.Default.Inventory2,
                            label = "Quantidade em Estoque",
                            value = "${produtoAtual.quantidade} unidades"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DetailItemRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Data de Vencimento",
                            value = produtoAtual.data_vencimento
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DetailItemRow(
                            icon = Icons.Default.Store,
                            label = "Setor / Categoria",
                            value = produtoAtual.setor
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (canOperate) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showEditarDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA)),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_edit_produto_modal")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedExpressive),
                        border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_delete_produto_modal")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFA855F7).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Somente Leitura (Restrito)", fontSize = 11.sp, color = Color(0xFFE9D5FF))
                    }
                }
            }
        },
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DetailItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DarkTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = DarkTextSecondary)
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
