package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RebaixaInfo
import com.example.data.RebaixaManager
import com.example.data.StatusRebaixa
import com.example.data.supabase.LogManager
import com.example.data.supabase.Produto
import com.example.data.supabase.SessionHolder
import com.example.ui.screens.calculateDaysRemaining
import com.example.ui.screens.getExpiryStatus
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonitoramentoAcoesDialog(
    produto: Produto,
    onDismiss: () -> Unit,
    onStatusChanged: () -> Unit
) {
    val context = LocalContext.current
    val currentUser = SessionHolder.currentUser
    val daysRemaining = remember(produto.data_vencimento) {
        calculateDaysRemaining(produto.data_vencimento)
    }
    val status = remember(daysRemaining) {
        getExpiryStatus(daysRemaining)
    }

    var rebaixaInfo by remember {
        mutableStateOf(RebaixaManager.getInfo(context, produto.id))
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = status.color.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(status.icon, contentDescription = null, tint = status.color, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(produto.nome, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp, maxLines = 1)
                        Text(
                            text = "Validade: ${produto.data_vencimento} (${when {
                                daysRemaining < 0 -> "Vencido há ${-daysRemaining}d"
                                daysRemaining == 0L -> "Vence hoje"
                                daysRemaining == 1L -> "Falta 1 dia"
                                else -> "Faltam ${daysRemaining}d"
                            }})",
                            fontSize = 11.sp,
                            color = status.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Info do produto
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceContainerHigh,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("EAN: ${produto.codigo_barras}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF93C5FD))
                            if (!produto.plu.isNullOrBlank()) {
                                Text("PLU: ${produto.plu}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFBBF24))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Setor: ${produto.setor}", fontSize = 10.sp, color = DarkTextSecondary)
                            Text("Estoque: ${produto.quantidade} un.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Estado Atual da Rebaixa
                Text("Fluxo de Rebaixa de Preço", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEAB308))
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                    border = BorderStroke(
                        1.dp,
                        when (rebaixaInfo.status) {
                            StatusRebaixa.PENDENTE -> Color(0xFFEAB308)
                            StatusRebaixa.CONFIRMADA -> EmeraldSafe
                            StatusRebaixa.NAO_APLICADA -> RedExpressive
                            StatusRebaixa.NENHUMA -> DarkBorder
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status da Rebaixa:",
                                fontSize = 11.sp,
                                color = DarkTextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (rebaixaInfo.status) {
                                    StatusRebaixa.PENDENTE -> Color(0xFFEAB308).copy(alpha = 0.2f)
                                    StatusRebaixa.CONFIRMADA -> EmeraldSafe.copy(alpha = 0.2f)
                                    StatusRebaixa.NAO_APLICADA -> RedExpressive.copy(alpha = 0.2f)
                                    StatusRebaixa.NENHUMA -> Color(0xFF334155)
                                }
                            ) {
                                Text(
                                    text = when (rebaixaInfo.status) {
                                        StatusRebaixa.PENDENTE -> "PENDENTE DE CONFIRMAÇÃO"
                                        StatusRebaixa.CONFIRMADA -> "REBAIXA CONFIRMADA"
                                        StatusRebaixa.NAO_APLICADA -> "NÃO APLICADA"
                                        StatusRebaixa.NENHUMA -> "NENHUMA SOLICITAÇÃO"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (rebaixaInfo.status) {
                                        StatusRebaixa.PENDENTE -> Color(0xFFFDE68A)
                                        StatusRebaixa.CONFIRMADA -> EmeraldSafe
                                        StatusRebaixa.NAO_APLICADA -> RedExpressive
                                        StatusRebaixa.NENHUMA -> DarkTextSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (rebaixaInfo.status == StatusRebaixa.PENDENTE && rebaixaInfo.dataSolicitada > 0L) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Solicitada em: ${dateFormat.format(Date(rebaixaInfo.dataSolicitada))}",
                                fontSize = 10.sp,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = "⚠️ Atenção: Conferir hoje na gôndola/etiqueta se a rebaixa foi aplicada!",
                                fontSize = 10.sp,
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (rebaixaInfo.status == StatusRebaixa.CONFIRMADA && rebaixaInfo.dataConfirmada > 0L) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Última rebaixa confirmada em: ${dateFormat.format(Date(rebaixaInfo.dataConfirmada))}",
                                fontSize = 10.sp,
                                color = DarkTextSecondary
                            )
                            if (RebaixaManager.devePedirNovaRebaixa2Dias(context, produto.id)) {
                                Text(
                                    text = "🔄 Já se passaram 2 dias! Necessário solicitar nova rebaixa de preço.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFF97316),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botões de Ação para Rebaixa
                if (rebaixaInfo.status == StatusRebaixa.PENDENTE) {
                    Text("Confirmar aplicação da rebaixa na gôndola:", fontSize = 11.sp, color = DarkTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                RebaixaManager.confirmarRebaixa(context, produto.id, confirmou = true)
                                rebaixaInfo = RebaixaManager.getInfo(context, produto.id)
                                if (currentUser != null) {
                                    LogManager.recordLog(
                                        usuarioMatricula = currentUser.matricula,
                                        usuarioNome = currentUser.nome,
                                        lojaId = currentUser.loja_id,
                                        acao = "REBAIXA_CONFIRMADA",
                                        detalhes = "Confirmou que houve rebaixa para '${produto.nome}'"
                                    )
                                }
                                onStatusChanged()
                                Toast.makeText(context, "Rebaixa confirmada com sucesso!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Houve Rebaixa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                RebaixaManager.confirmarRebaixa(context, produto.id, confirmou = false)
                                rebaixaInfo = RebaixaManager.getInfo(context, produto.id)
                                if (currentUser != null) {
                                    LogManager.recordLog(
                                        usuarioMatricula = currentUser.matricula,
                                        usuarioNome = currentUser.nome,
                                        lojaId = currentUser.loja_id,
                                        acao = "REBAIXA_NAO_APLICADA",
                                        detalhes = "Registrou que não houve rebaixa para '${produto.nome}'"
                                    )
                                }
                                onStatusChanged()
                                Toast.makeText(context, "Registrado: Não houve rebaixa.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedExpressive),
                            border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Não Houve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            RebaixaManager.solicitarRebaixa(context, produto.id)
                            rebaixaInfo = RebaixaManager.getInfo(context, produto.id)
                            if (currentUser != null) {
                                LogManager.recordLog(
                                    usuarioMatricula = currentUser.matricula,
                                    usuarioNome = currentUser.nome,
                                    lojaId = currentUser.loja_id,
                                    acao = "REBAIXA_SOLICITADA",
                                    detalhes = "Solicitou rebaixa para '${produto.nome}' (Validade: ${produto.data_vencimento})"
                                )
                            }
                            onStatusChanged()
                            Toast.makeText(context, "Rebaixa solicitada! Ficará pendente até conferência.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (rebaixaInfo.status == StatusRebaixa.CONFIRMADA) "Solicitar Nova Rebaixa de Preço" else "Solicitar Rebaixa de Preço",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Área e Retirada
                Text("Controle de Área de Venda", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (rebaixaInfo.retiradoDeArea) "Item Retirado de Área" else "Item em Área de Venda",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (rebaixaInfo.retiradoDeArea) EmeraldSafe else Color(0xFFF97316)
                        )
                        Text(
                            text = if (rebaixaInfo.retiradoDeArea) "Produto removido das gôndolas" else "Disponível nas prateleiras",
                            fontSize = 10.sp,
                            color = DarkTextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            val novoEstado = !rebaixaInfo.retiradoDeArea
                            RebaixaManager.setRetiradoDeArea(context, produto.id, novoEstado)
                            rebaixaInfo = RebaixaManager.getInfo(context, produto.id)
                            if (currentUser != null) {
                                LogManager.recordLog(
                                    usuarioMatricula = currentUser.matricula,
                                    usuarioNome = currentUser.nome,
                                    lojaId = currentUser.loja_id,
                                    acao = if (novoEstado) "RETIRADA_AREA" else "DEVOLUCAO_AREA",
                                    detalhes = if (novoEstado) "Retirou '${produto.nome}' da área de venda" else "Devolveu '${produto.nome}' para a área de venda"
                                )
                            }
                            onStatusChanged()
                            Toast.makeText(context, if (novoEstado) "Produto retirado de área!" else "Produto retornado à área!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rebaixaInfo.retiradoDeArea) Color(0xFF334155) else Color(0xFFEA580C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (rebaixaInfo.retiradoDeArea) Icons.Default.Refresh else Icons.Default.RemoveShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (rebaixaInfo.retiradoDeArea) "Devolver à Área" else "Retirar de Área",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(16.dp)
    )
}
