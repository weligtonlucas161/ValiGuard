package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PresenceAuditReportManager
import com.example.data.RelatorioAuditoriaInventario
import com.example.data.RelatorioAuditoriaPresenca
import com.example.data.RelatorioAuditoriaValidade
import com.example.ui.theme.*
import com.example.util.AuditPdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diálogo exclusivo para qualquer perfil Master da empresa consultar e baixar
 * os relatórios oficiais de auditoria (Presença, Inventário e Validade).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosEmpresaMasterDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val relatoriosPresenca by PresenceAuditReportManager.relatorios.collectAsStateWithLifecycle()
    val relatoriosInventario by PresenceAuditReportManager.relatoriosInventario.collectAsStateWithLifecycle()
    val relatoriosValidade by PresenceAuditReportManager.relatoriosValidade.collectAsStateWithLifecycle()

    var filtroTipo by remember { mutableStateOf("TODOS") }
    var selectedPresencaModal by remember { mutableStateOf<RelatorioAuditoriaPresenca?>(null) }
    var selectedInventarioModal by remember { mutableStateOf<RelatorioAuditoriaInventario?>(null) }
    var selectedValidadeModal by remember { mutableStateOf<RelatorioAuditoriaValidade?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Relatórios de Auditoria da Empresa", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF065F46)
                                ) {
                                    Text("Perfil Master", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text("Acesso unificado a todas as auditorias finalizadas da empresa", fontSize = 11.sp, color = DarkTextSecondary)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filtros por Tipo de Relatório
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = filtroTipo == "TODOS",
                        onClick = { filtroTipo = "TODOS" },
                        label = { Text("Todos", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceContainerHigh,
                            labelColor = DarkTextPrimary
                        )
                    )
                    FilterChip(
                        selected = filtroTipo == "PRESENCA",
                        onClick = { filtroTipo = "PRESENCA" },
                        label = { Text("Presença (${relatoriosPresenca.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B),
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceContainerHigh,
                            labelColor = DarkTextPrimary
                        )
                    )
                    FilterChip(
                        selected = filtroTipo == "INVENTARIO",
                        onClick = { filtroTipo = "INVENTARIO" },
                        label = { Text("Inventário (${relatoriosInventario.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFA855F7),
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceContainerHigh,
                            labelColor = DarkTextPrimary
                        )
                    )
                    FilterChip(
                        selected = filtroTipo == "VALIDADE",
                        onClick = { filtroTipo = "VALIDADE" },
                        label = { Text("Validade (${relatoriosValidade.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceContainerHigh,
                            labelColor = DarkTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val totalRelatorios = relatoriosPresenca.size + relatoriosInventario.size + relatoriosValidade.size

                if (totalRelatorios == 0) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Nenhum relatório de auditoria gerado ainda.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Os relatórios de presença, inventário e validade finalizados na empresa aparecerão aqui.", fontSize = 12.sp, color = DarkTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Presença
                        if (filtroTipo in listOf("TODOS", "PRESENCA") && relatoriosPresenca.isNotEmpty()) {
                            Text("Auditorias de Presença (${relatoriosPresenca.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            relatoriosPresenca.forEach { rel ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPresencaModal = rel }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("PRESENÇA • ${rel.setor.uppercase()}", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 11.sp)
                                            Text("${rel.taxaPresenca}% Presença", fontWeight = FontWeight.Bold, color = if (rel.taxaPresenca >= 80) EmeraldSafe else RedExpressive, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Faltantes: ${rel.totalNaoLocalizados} | Bipados: ${rel.totalBipados} | Resp: ${rel.finalizadaPorNome} | ${rel.getDataFormatada()}", fontSize = 10.sp, color = DarkTextSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            Button(
                                                onClick = { selectedPresencaModal = rel },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ver / PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Inventário
                        if (filtroTipo in listOf("TODOS", "INVENTARIO") && relatoriosInventario.isNotEmpty()) {
                            Text("Auditorias de Inventário (${relatoriosInventario.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                            relatoriosInventario.forEach { inv ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedInventarioModal = inv }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("INVENTÁRIO • ${inv.setor.uppercase()}", fontWeight = FontWeight.Bold, color = Color(0xFFC084FC), fontSize = 11.sp)
                                            Text(if (inv.totalDivergencias == 0) "100% Acurado" else "${inv.totalDivergencias} Divergências", fontWeight = FontWeight.Bold, color = if (inv.totalDivergencias == 0) EmeraldSafe else RedExpressive, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Estoque: ${inv.totalEstoqueSistema} | Físico: ${inv.totalContagemFisica} | Resp: ${inv.responsavelNome} | ${inv.getDataFormatada()}", fontSize = 10.sp, color = DarkTextSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            Button(
                                                onClick = { selectedInventarioModal = inv },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ver / PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Validade
                        if (filtroTipo in listOf("TODOS", "VALIDADE") && relatoriosValidade.isNotEmpty()) {
                            Text("Auditorias de Validade (${relatoriosValidade.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            relatoriosValidade.forEach { valRep ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedValidadeModal = valRep }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("VALIDADE • ${valRep.secao.uppercase()}", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 11.sp)
                                            Text("${valRep.totalPecasAreaVendas} Peças Vendas", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Auditados: ${valRep.totalItensAuditados} | Resp: ${valRep.responsavelNome} | ${valRep.getDataFormatada()}", fontSize = 10.sp, color = DarkTextSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            Button(
                                                onClick = { selectedValidadeModal = valRep },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ver / PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Presença
    selectedPresencaModal?.let { rel ->
        AlertDialog(
            onDismissRequest = { selectedPresencaModal = null },
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFF59E0B)) },
            title = { Text("Relatório de Presença - ${rel.setor}", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Taxa de Presença: ${rel.taxaPresenca}%", fontWeight = FontWeight.Bold, color = EmeraldSafe)
                    Text("Responsável: ${rel.finalizadaPorNome} (${rel.finalizadaPorMatricula})", fontSize = 11.sp, color = DarkTextSecondary)
                    Text("Data: ${rel.getDataFormatada()} | Duração: ${rel.getDuracaoFormatada()}", fontSize = 11.sp, color = DarkTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Não Bipados (Prioridade): ${rel.produtosNaoBipados.size}", fontWeight = FontWeight.Bold, color = RedExpressive, fontSize = 12.sp)
                    rel.produtosNaoBipados.take(10).forEach {
                        Text("• ${it.nome} - Estoque: ${it.quantidadeEstoque} un", fontSize = 11.sp, color = RedExpressive)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Bipados: ${rel.itensConfirmados.size}", fontWeight = FontWeight.Bold, color = EmeraldSafe, fontSize = 12.sp)
                    rel.itensConfirmados.take(10).forEach {
                        Text("• ${it.nome} - Estoque: ${it.quantidadeEstoque} un", fontSize = 11.sp, color = EmeraldSafe)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val file = AuditPdfGenerator.generatePresenceAuditPdf(context, rel)
                            AuditPdfGenerator.openPdf(context, file)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Baixar em PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPresencaModal = null }) {
                    Text("Fechar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Inventário
    selectedInventarioModal?.let { inv ->
        AlertDialog(
            onDismissRequest = { selectedInventarioModal = null },
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFA855F7)) },
            title = { Text("Relatório de Inventário - ${inv.setor}", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Divergências: ${inv.totalDivergencias}", fontWeight = FontWeight.Bold, color = if (inv.totalDivergencias > 0) RedExpressive else EmeraldSafe)
                    Text("Estoque Sistema: ${inv.totalEstoqueSistema} un | Contagem: ${inv.totalContagemFisica} un", fontSize = 11.sp, color = Color.White)
                    Text("Responsável: ${inv.responsavelNome} | ${inv.getDataFormatada()}", fontSize = 11.sp, color = DarkTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    inv.itens.take(15).forEach { item ->
                        Text("• ${item.produtoNome} (Est: ${item.quantidadeEstoque} | Cont: ${item.quantidadeContada} | Div: ${item.divergencia})", fontSize = 11.sp, color = if (item.divergencia != 0) RedExpressive else EmeraldSafe)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val file = AuditPdfGenerator.generateInventoryAuditPdf(context, inv)
                            AuditPdfGenerator.openPdf(context, file)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("Baixar em PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInventarioModal = null }) {
                    Text("Fechar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Validade
    selectedValidadeModal?.let { valRep ->
        AlertDialog(
            onDismissRequest = { selectedValidadeModal = null },
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF0284C7)) },
            title = { Text("Relatório de Validade - ${valRep.secao}", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Peças em Área de Vendas: ${valRep.totalPecasAreaVendas} un", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Text("Itens Auditados: ${valRep.totalItensAuditados} | Resp: ${valRep.responsavelNome}", fontSize = 11.sp, color = Color.White)
                    Text("Data: ${valRep.getDataFormatada()} | Duração: ${valRep.getDuracaoFormatada()}", fontSize = 11.sp, color = DarkTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    valRep.itens.take(15).forEach { item ->
                        Text("• ${item.produtoNome} (Val: ${item.validadeNova} | Área: ${item.quantidadeAreaVendas} un)", fontSize = 11.sp, color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val file = AuditPdfGenerator.generateValidityAuditPdf(context, valRep)
                            AuditPdfGenerator.openPdf(context, file)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Baixar em PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedValidadeModal = null }) {
                    Text("Fechar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
