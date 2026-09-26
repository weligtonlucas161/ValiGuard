package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AuditItemScanned
import com.example.data.BipResult
import com.example.data.PresenceAuditReportManager
import com.example.data.PresenceAuditSession
import com.example.data.PresenceAuditSessionManager
import com.example.data.ProdutoAusenteResumo
import com.example.data.RelatorioAuditoriaPresenca
import com.example.data.RelatorioAuditoriaInventario
import com.example.data.RelatorioAuditoriaValidade
import com.example.data.ItemInventarioResumo
import com.example.data.ItemValidadeAreaResumo
import com.example.util.AuditPdfGenerator
import com.example.data.supabase.Usuario
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SectorManager
import com.example.data.SetorComSubsetores
import com.example.data.supabase.AtualizarProdutoQtd
import com.example.data.supabase.AtualizarProdutoValidade
import com.example.data.supabase.LogManager
import com.example.data.supabase.Produto
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.ui.screens.calculateDaysRemaining
import com.example.ui.screens.getExpiryStatus
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import com.example.util.BarcodeScannerHelper
import com.example.util.SoundFeedbackHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ItemAuditadoValidade(
    val id: String,
    val produtoNome: String,
    val codigoBarras: String,
    val plu: String?,
    val setor: String,
    val validadeAnterior: String,
    val validadeNova: String,
    val qtdArea: Int,
    val timestamp: Long
)

enum class AuditoriaTipo(val titulo: String, val subtitulo: String) {
    PRESENCA("Presença", "Bipar produtos em área"),
    INVENTARIO("Inventário", "Contagem física (Master)"),
    VALIDADE("Validade", "Conferência por seção"),
    RELATORIOS("Relatórios", "Histórico e download em PDF")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AuditoriaHubScreen(
    produtos: List<Produto>,
    onRefreshProdutos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = SessionHolder.currentUser
    val isMaster = SessionHolder.isMaster || com.example.data.CargoManager.isPerfilMaster(currentUser, context)

    var currentTab by remember { mutableStateOf(AuditoriaTipo.PRESENCA) }

    // Inicializa o gerenciador de presença compartilhada e relatórios
    val currentLojaId = currentUser?.loja_id ?: "default"
    androidx.compose.runtime.LaunchedEffect(currentLojaId) {
        PresenceAuditSessionManager.init(context)
        PresenceAuditReportManager.init(context, currentLojaId)
        PresenceAuditSessionManager.startRealtimeSync(context, currentLojaId, this)
    }

    // Controle para garantir que a auditoria SÓ comece quando o usuário clicar explicitamente em Iniciar
    var auditoriaIniciadaLocalmente by remember { mutableStateOf(false) }
    var selectedRelatorioForModal by remember { mutableStateOf<RelatorioAuditoriaPresenca?>(null) }
    var selectedInventarioForModal by remember { mutableStateOf<RelatorioAuditoriaInventario?>(null) }
    var selectedValidadeForModal by remember { mutableStateOf<RelatorioAuditoriaValidade?>(null) }
    var filtroTipoRelatorios by remember { mutableStateOf("TODOS") } // "TODOS", "PRESENCA", "INVENTARIO", "VALIDADE"

    // Estado em Tempo Real da Auditoria de Presença Compartilhada
    val activePresenceSession by PresenceAuditSessionManager.currentSession.collectAsStateWithLifecycle()
    var setorPresencaSelecionado by remember {
        mutableStateOf(
            if (!currentUser?.setor.isNullOrBlank() && currentUser?.setor != "Todos") {
                currentUser.setor!!
            } else {
                "Todos"
            }
        )
    }
    var bipDuplicadoAlerta by remember { mutableStateOf<AuditItemScanned?>(null) }
    var showConcluirPresencaConfirm by remember { mutableStateOf(false) }

    var inputCodigoPresenca by remember { mutableStateOf("") }
    var presencaSubTab by remember { mutableIntStateOf(0) } // 0 = Pendentes (a bipar), 1 = Confirmados em Área
    var ultimoProdutoBipado by remember { mutableStateOf<Produto?>(null) }

    // Estado da Auditoria de Inventário (Master)
    var filtroSetorInventario by remember { mutableStateOf("Todos") }
    var buscaInventario by remember { mutableStateOf("") }
    val contagensFisicas = remember { mutableStateMapOf<String, Int>() }
    var produtoParaAjustar by remember { mutableStateOf<Produto?>(null) }
    var novaQtdAjuste by remember { mutableIntStateOf(0) }
    var isSavingAjuste by remember { mutableStateOf(false) }

    // Estado da Auditoria de Validade (Por Seção / Interface Travada)
    var isValidadeLocked by remember { mutableStateOf(false) }
    var momentoInicioValidade by remember { mutableStateOf(0L) }
    var momentoFimValidade by remember { mutableStateOf(0L) }
    var setorValidadeSelecionado by remember { mutableStateOf("Frios") }
    var secaoValidadeSelecionada by remember { mutableStateOf("Iogurtes") }
    var produtoValidadeBipado by remember { mutableStateOf<Produto?>(null) }
    var inputCodigoValidade by remember { mutableStateOf("") }
    var inputNovaDataValidade by remember { mutableStateOf("") }
    var inputQtdValidadeArea by remember { mutableIntStateOf(1) }
    var isSalvandoValidadeItem by remember { mutableStateOf(false) }
    val itensAuditadosValidade = remember { mutableStateListOf<ItemAuditadoValidade>() }
    var showResumoFinalizacaoValidade by remember { mutableStateOf(false) }
    var showConfirmarFinalizacaoValidade by remember { mutableStateOf(false) }

    val setoresDisponiveis = remember { SectorManager.getSetores(context) }

    // Função de bipar/confirmar presença do produto com bloqueio de bip duplo em tempo real
    fun processarBipagemPresenca(codigo: String) {
        val cleanCode = codigo.trim()
        if (cleanCode.isEmpty()) return

        val session = activePresenceSession
        if (session == null) {
            Toast.makeText(context, "Inicie a auditoria clicando no Play antes de bipar.", Toast.LENGTH_SHORT).show()
            return
        }

        // Procurar produto no estoque por EAN ou por PLU ou código exato
        val produtoEncontrado = produtos.find { p ->
            p.codigo_barras.equals(cleanCode, ignoreCase = true) ||
            (p.plu != null && p.plu.equals(cleanCode, ignoreCase = true)) ||
            p.nome.contains(cleanCode, ignoreCase = true)
        }

        if (produtoEncontrado != null) {
            val userObj = currentUser ?: Usuario(
                matricula = "000000",
                nome = "Operador",
                cargo = "operador",
                loja_id = session.lojaId
            )

            coroutineScope.launch {
                val result = PresenceAuditSessionManager.processBip(context, produtoEncontrado, userObj)
                when (result) {
                    is BipResult.Success -> {
                        ultimoProdutoBipado = produtoEncontrado
                        SoundFeedbackHelper.playSuccessBeep(context)
                        Toast.makeText(context, "✅ '${produtoEncontrado.nome}' confirmado em área!", Toast.LENGTH_SHORT).show()
                        inputCodigoPresenca = ""
                    }
                    is BipResult.Duplicate -> {
                        SoundFeedbackHelper.playAlertBeep(context)
                        bipDuplicadoAlerta = result.existingItem
                        inputCodigoPresenca = ""
                    }
                    is BipResult.NotFound -> {
                        SoundFeedbackHelper.playAlertBeep(context)
                        Toast.makeText(context, "⚠️ Produto não localizado nesta auditoria.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            SoundFeedbackHelper.playAlertBeep(context)
            Toast.makeText(context, "⚠️ Produto não localizado no estoque: $cleanCode", Toast.LENGTH_SHORT).show()
        }
    }

    // Modal de confirmação de ajuste de inventário (Master)
    if (produtoParaAjustar != null) {
        val prod = produtoParaAjustar!!
        val diferenca = novaQtdAjuste - prod.quantidade
        AlertDialog(
            onDismissRequest = { if (!isSavingAjuste) produtoParaAjustar = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajustar Estoque Físico", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column {
                    Text("Produto: ${prod.nome}", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("EAN: ${prod.codigo_barras}", fontSize = 12.sp, color = DarkTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Estoque Sistema: ${prod.quantidade} un.", fontSize = 13.sp, color = DarkTextPrimary)
                    Text("Contagem Física: $novaQtdAjuste un.", fontSize = 13.sp, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Divergência: ${if (diferenca > 0) "+$diferenca" else "$diferenca"} un.",
                        fontWeight = FontWeight.Bold,
                        color = if (diferenca == 0) EmeraldSafe else if (diferenca > 0) Color(0xFF38BDF8) else RedExpressive
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSavingAjuste = true
                        coroutineScope.launch {
                            try {
                                SupabaseClient.instance.from("produtos")
                                    .update(AtualizarProdutoQtd(novaQtdAjuste)) {
                                        eq("id", prod.id)
                                    }

                                if (currentUser != null) {
                                    LogManager.recordLog(
                                        usuarioMatricula = currentUser.matricula,
                                        usuarioNome = currentUser.nome,
                                        lojaId = currentUser.loja_id,
                                        acao = "AUDITORIA_INVENTARIO",
                                        detalhes = "Ajustou estoque de '${prod.nome}' de ${prod.quantidade} para $novaQtdAjuste un. (Divergência: $diferenca)"
                                    )
                                }

                                Toast.makeText(context, "Estoque ajustado com sucesso!", Toast.LENGTH_SHORT).show()
                                produtoParaAjustar = null
                                onRefreshProdutos()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao ajustar: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSavingAjuste = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                    enabled = !isSavingAjuste
                ) {
                    if (isSavingAjuste) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Confirmar Ajuste", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { produtoParaAjustar = null },
                    enabled = !isSavingAjuste
                ) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal de Confirmação para Finalizar Seção de Validade
    if (showConfirmarFinalizacaoValidade) {
        val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        val horaInicioStr = remember(momentoInicioValidade) { timeFormat.format(Date(momentoInicioValidade)) }
        AlertDialog(
            onDismissRequest = { showConfirmarFinalizacaoValidade = false },
            icon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Finalizar Seção de Validade?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        text = "Seção: $secaoValidadeSelecionada ($setorValidadeSelecionado)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Iniciada às: $horaInicioStr", fontSize = 12.sp, color = DarkTextSecondary)
                    Text("Itens auditados nesta sessão: ${itensAuditadosValidade.size}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ao confirmar, a interface será destravada e o momento exato de início e término será gravado no log do sistema.",
                        fontSize = 12.sp,
                        color = DarkTextPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        momentoFimValidade = System.currentTimeMillis()
                        val horaFimStr = timeFormat.format(Date(momentoFimValidade))
                        val duracaoSeg = ((momentoFimValidade - momentoInicioValidade) / 1000).coerceAtLeast(0)
                        val minutos = duracaoSeg / 60
                        val segundos = duracaoSeg % 60
                        val duracaoStr = "${minutos}m ${segundos}s"

                        if (currentUser != null) {
                            LogManager.recordLog(
                                usuarioMatricula = currentUser.matricula,
                                usuarioNome = currentUser.nome,
                                lojaId = currentUser.loja_id,
                                acao = "AUDITORIA_VALIDADE_SECAO_CONCLUIDA",
                                detalhes = "Finalizou seção '$secaoValidadeSelecionada' ($setorValidadeSelecionado). Início: $horaInicioStr | Fim: $horaFimStr | Duração: $duracaoStr | Total auditado: ${itensAuditadosValidade.size} itens"
                            )
                        }

                        // Salvar relatório oficial de validade em área de vendas
                        val novoRelatorioValidade = RelatorioAuditoriaValidade(
                            lojaId = currentUser?.loja_id ?: "default",
                            setor = setorValidadeSelecionado,
                            secao = secaoValidadeSelecionada,
                            responsavelNome = currentUser?.nome ?: "Operador",
                            responsavelMatricula = currentUser?.matricula ?: "-",
                            dataInicioMs = momentoInicioValidade,
                            dataFimMs = momentoFimValidade,
                            totalItensAuditados = itensAuditadosValidade.size,
                            totalPecasAreaVendas = itensAuditadosValidade.sumOf { it.qtdArea },
                            itens = itensAuditadosValidade.map {
                                ItemValidadeAreaResumo(
                                    id = it.id,
                                    produtoNome = it.produtoNome,
                                    codigoBarras = it.codigoBarras,
                                    plu = it.plu,
                                    setor = it.setor,
                                    secao = secaoValidadeSelecionada,
                                    validadeAnterior = it.validadeAnterior,
                                    validadeNova = it.validadeNova,
                                    quantidadeAreaVendas = it.qtdArea,
                                    timestamp = it.timestamp
                                )
                            }
                        )
                        PresenceAuditReportManager.salvarRelatorioValidade(context, novoRelatorioValidade)

                        isValidadeLocked = false
                        showConfirmarFinalizacaoValidade = false
                        showResumoFinalizacaoValidade = true
                        onRefreshProdutos()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Sim, Finalizar Seção", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmarFinalizacaoValidade = false }) {
                    Text("Continuar Auditando", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal de Resumo de Conclusão da Seção de Validade
    if (showResumoFinalizacaoValidade) {
        val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        val inicioStr = timeFormat.format(Date(momentoInicioValidade))
        val fimStr = timeFormat.format(Date(momentoFimValidade))
        val duracaoSeg = ((momentoFimValidade - momentoInicioValidade) / 1000).coerceAtLeast(0)
        val minutos = duracaoSeg / 60
        val segundos = duracaoSeg % 60

        AlertDialog(
            onDismissRequest = { showResumoFinalizacaoValidade = false },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Seção Concluída com Sucesso!", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceContainerHigh,
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Setor: $setorValidadeSelecionado", fontSize = 12.sp, color = DarkTextSecondary)
                            Text("Seção: $secaoValidadeSelecionada", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Início: $inicioStr", fontSize = 11.sp, color = Color(0xFF93C5FD), fontFamily = FontFamily.Monospace)
                                Text("Fim: $fimStr", fontSize = 11.sp, color = Color(0xFF93C5FD), fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Duração total: ${minutos} min e ${segundos} seg", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmeraldSafe)
                            Text("Produtos auditados: ${itensAuditadosValidade.size} itens", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Todos os itens e horários foram devidamente registrados nos logs de auditoria.", fontSize = 11.sp, color = DarkTextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showResumoFinalizacaoValidade = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe)
                ) {
                    Text("OK, Concluído", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab de tipos de auditoria
        TabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = DarkSurfaceContainer,
            contentColor = Color(0xFFA855F7),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                    color = when (currentTab) {
                        AuditoriaTipo.PRESENCA -> Color(0xFFF59E0B)
                        AuditoriaTipo.INVENTARIO -> Color(0xFFA855F7)
                        AuditoriaTipo.VALIDADE -> Color(0xFF38BDF8)
                        AuditoriaTipo.RELATORIOS -> Color(0xFF10B981)
                    },
                    height = 3.dp
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            AuditoriaTipo.values().forEach { tipo ->
                Tab(
                    selected = currentTab == tipo,
                    onClick = {
                        if (isValidadeLocked && tipo != AuditoriaTipo.VALIDADE) {
                            Toast.makeText(context, "Sessão travada! Finalize a seção de validade antes de mudar de aba.", Toast.LENGTH_LONG).show()
                        } else {
                            currentTab = tipo
                        }
                    },
                    text = {
                        Text(
                            text = tipo.titulo,
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == tipo) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        when (tipo) {
                            AuditoriaTipo.PRESENCA -> Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            AuditoriaTipo.INVENTARIO -> Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            AuditoriaTipo.VALIDADE -> Icon(if (isValidadeLocked) Icons.Default.Lock else Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            AuditoriaTipo.RELATORIOS -> Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Conteúdo conforme o tipo de auditoria selecionado
        when (currentTab) {
            // =========================================================================
            // 1. AUDITORIA DE PRESENÇA EM REALTIME (Sessão Compartilhada & Play Button)
            // =========================================================================
            AuditoriaTipo.PRESENCA -> {
                val session = activePresenceSession

                if (!auditoriaIniciadaLocalmente || session == null) {
                    // -------------------------------------------------------------
                    // TELA DE ENTRADA: AUDITORIA NÃO INICIADA (BOTÃO INICIAR AUDITORIA EM DESTAQUE)
                    // -------------------------------------------------------------
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Auditoria de Presença em Tempo Real",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Sessão compartilhada por setor com prevenção de bip duplo",
                                            fontSize = 11.sp,
                                            color = DarkTextSecondary
                                        )
                                    }
                                }

                                if (session != null) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("⚡", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Sessão ativa detectada no setor '${session.setor}'",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFFDE68A)
                                                )
                                                Text(
                                                    text = "Iniciada por ${session.iniciadaPorNome} com ${session.itensBipados.size} itens já confirmados.",
                                                    fontSize = 11.sp,
                                                    color = DarkTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Seleção do Setor da Auditoria
                                Text(
                                    text = "1. Setor da Auditoria:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val setoresLoja = remember {
                                    listOf("Todos") + SectorManager.getSetores(context).map { it.nome }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    setoresLoja.forEach { s ->
                                        val isSel = setorPresencaSelecionado.equals(s, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) Color(0xFFB45309).copy(alpha = 0.4f) else DarkSurfaceContainerHigh,
                                            border = BorderStroke(1.dp, if (isSel) Color(0xFFF59E0B) else DarkBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { setorPresencaSelecionado = s }
                                        ) {
                                            Text(
                                                text = s,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else DarkTextSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // BOTÃO PARA INICIAR A AUDITORIA (SOMENTE QUANDO CLICADO)
                                Button(
                                    onClick = {
                                        auditoriaIniciadaLocalmente = true
                                        val lojaId = currentUser?.loja_id ?: "default"
                                        val userObj = currentUser ?: Usuario(
                                            matricula = "000000",
                                            nome = "Operador",
                                            cargo = "operador",
                                            loja_id = lojaId
                                        )
                                        PresenceAuditSessionManager.startOrJoinSession(
                                            context = context,
                                            lojaId = lojaId,
                                            setor = setorPresencaSelecionado,
                                            currentUser = userObj,
                                            scope = coroutineScope
                                        )
                                        Toast.makeText(context, "Auditoria iniciada com sucesso!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("btn_play_iniciar_auditoria_presenca")
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (session != null) "ENTRAR NA AUDITORIA DE PRESENÇA" else "INICIAR AUDITORIA DE PRESENÇA",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        // Cards informativos sobre a mecânica em tempo real
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Como funciona a sincronização:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("⚡", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Ao clicar em Play, outros operadores do mesmo setor ou Master que também iniciarem entram automaticamente na mesma sessão ao vivo.",
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("🚫", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bloqueio de bipagem dupla: o produto bipado por qualquer usuário sai instantaneamente da listagem de pendentes para todos.",
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("👥", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Se alguém tentar bipar um produto já registrado, o sistema avisa quem bipou e o horário exato.",
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // -------------------------------------------------------------
                    // TELA DE SESSÃO ATIVA (EM TEMPO REAL)
                    // -------------------------------------------------------------
                    val produtosAuditoria = remember(produtos, session) {
                        if (session.setor.equals("Todos", ignoreCase = true)) {
                            produtos
                        } else {
                            produtos.filter { p ->
                                p.setor.contains(session.setor, ignoreCase = true)
                            }
                        }
                    }

                    val confirmadosMap = session.itensBipados
                    val confirmados = produtosAuditoria.filter { confirmadosMap.containsKey(it.id) }
                    val pendentes = produtosAuditoria.filter { !confirmadosMap.containsKey(it.id) }
                    val totalEstoque = produtosAuditoria.size
                    val progresso = if (totalEstoque > 0) confirmados.size.toFloat() / totalEstoque else 0f

                    // Barra superior da Sessão Ativa
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldSafe,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AO VIVO • SINCRONIZADO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = EmeraldSafe
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFB45309).copy(alpha = 0.3f)
                                    ) {
                                        Text(
                                            text = "Setor: ${session.setor}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { showConcluirPresencaConfirm = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Finalizar", fontSize = 11.sp, color = RedExpressive)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Participantes Conectados
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Equipe na sessão: ${session.participantes.joinToString(", ")}",
                                    fontSize = 10.sp,
                                    color = DarkTextSecondary,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Métricas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricBadge(
                                    label = "Pendentes",
                                    value = "${pendentes.size}",
                                    color = Color(0xFFF59E0B)
                                )
                                MetricBadge(
                                    label = "Em Área",
                                    value = "${confirmados.size}",
                                    color = EmeraldSafe
                                )
                                MetricBadge(
                                    label = "Total Setor",
                                    value = "$totalEstoque",
                                    color = Color(0xFF93C5FD)
                                )
                                MetricBadge(
                                    label = "Progresso",
                                    value = "${(progresso * 100).toInt()}%",
                                    color = if (progresso >= 1f) EmeraldSafe else Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { progresso },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = if (progresso >= 1f) EmeraldSafe else Color(0xFFF59E0B),
                                trackColor = Color(0xFF334155),
                            )

                            // Banner do último bipado em tempo real
                            if (session.ultimoBip != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF065F46).copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Último bip: ${session.ultimoBip.nome} (${session.ultimoBip.operadorNome} às ${session.ultimoBip.getFormattedTime()})",
                                            fontSize = 11.sp,
                                            color = Color(0xFFA7F3D0),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bipagem Antifraude Exclusiva pela Câmera (Sem digitação manual)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                BarcodeScannerHelper.startScan(
                                    context = context,
                                    onSuccess = { barcode ->
                                        processarBipagemPresenca(barcode)
                                    },
                                    onError = {
                                        Toast.makeText(context, "Erro de scanner: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            .testTag("btn_bipar_camera_antifraude")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Câmera de Bipagem",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Bipar com a Câmera",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "🔒 Exclusivo via câmera física para evitar fraudes",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFDE68A),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    BarcodeScannerHelper.startScan(
                                        context = context,
                                        onSuccess = { barcode ->
                                            processarBipagemPresenca(barcode)
                                        },
                                        onError = {
                                            Toast.makeText(context, "Erro de scanner: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Escanear",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sub-tabs: "Pendentes (A Bipar)" vs "Em Área (Confirmados)"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = presencaSubTab == 0,
                            onClick = { presencaSubTab = 0 },
                            label = { Text("Pendentes a Bipar (${pendentes.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFB45309),
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceContainerHigh,
                                labelColor = DarkTextSecondary
                            )
                        )

                        FilterChip(
                            selected = presencaSubTab == 1,
                            onClick = { presencaSubTab = 1 },
                            label = { Text("Confirmados em Área (${confirmados.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF047857),
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceContainerHigh,
                                labelColor = DarkTextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lista de produtos
                    val listaExibicao = if (presencaSubTab == 0) pendentes else confirmados

                    if (listaExibicao.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (presencaSubTab == 0) Icons.Default.CheckCircle else Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (presencaSubTab == 0) EmeraldSafe else DarkTextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (presencaSubTab == 0) "Todos os produtos foram auditados em área!" else "Nenhum produto bipado ainda.",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (presencaSubTab == 0) "Parabéns! Lista de pendências zerada." else "Bipe os produtos para confirmar a presença.",
                                    color = DarkTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listaExibicao, key = { it.id }) { prod ->
                                val scannedItem = confirmadosMap[prod.id]
                                val isJaAuditado = scannedItem != null

                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                    border = BorderStroke(1.dp, if (isJaAuditado) EmeraldSafe.copy(alpha = 0.4f) else DarkBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isJaAuditado) EmeraldSafe.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isJaAuditado) Icons.Default.Check else Icons.Default.QrCodeScanner,
                                                    contentDescription = null,
                                                    tint = if (isJaAuditado) EmeraldSafe else DarkTextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.nome,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "EAN: ${prod.codigo_barras}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF93C5FD)
                                                )
                                                if (!prod.plu.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(text = "•", fontSize = 10.sp, color = DarkTextSecondary)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "PLU: ${prod.plu}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFFFBBF24)
                                                    )
                                                }
                                            }

                                            if (isJaAuditado) {
                                                Text(
                                                    text = "✅ Bipado por ${scannedItem.operadorNome} às ${scannedItem.getFormattedTime()}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = EmeraldSafe
                                                )
                                            } else {
                                                Text(
                                                    text = "Setor: ${prod.setor} • Estoque: ${prod.quantidade} un.",
                                                    fontSize = 10.sp,
                                                    color = DarkTextSecondary
                                                )
                                            }
                                        }

                                        if (isJaAuditado) {
                                            IconButton(
                                                onClick = { coroutineScope.launch { PresenceAuditSessionManager.undoBip(context, prod.id) } },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Undo, contentDescription = "Desfazer", tint = DarkTextSecondary, modifier = Modifier.size(18.dp))
                                            }
                                        } else {
                                            Button(
                                                onClick = { processarBipagemPresenca(prod.codigo_barras) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Validar", fontSize = 11.sp, color = Color(0xFF93C5FD))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modal de Alerta de Bip Duplo Evitado
                bipDuplicadoAlerta?.let { dup ->
                    AlertDialog(
                        onDismissRequest = { bipDuplicadoAlerta = null },
                        icon = {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(32.dp))
                        },
                        title = {
                            Text("Bip Duplo Evitado!", fontWeight = FontWeight.Bold, color = Color.White)
                        },
                        text = {
                            Column {
                                Text(
                                    text = "O produto \"${dup.nome}\" já foi confirmado nesta auditoria:",
                                    fontSize = 13.sp,
                                    color = DarkTextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("EAN: ${dup.codigoBarras}", fontSize = 12.sp, color = Color(0xFF93C5FD), fontFamily = FontFamily.Monospace)
                                        Text("Bipado por: ${dup.operadorNome}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Horário: ${dup.getFormattedTime()}", fontSize = 11.sp, color = Color(0xFFF59E0B))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "A sincronização em tempo real preveniu a duplicação do item na área.",
                                    fontSize = 11.sp,
                                    color = EmeraldSafe
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { bipDuplicadoAlerta = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                            ) {
                                Text("Entendido", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Modal de Confirmação para Finalizar Auditoria
                if (showConcluirPresencaConfirm && session != null) {
                    val confirmadosCount = session.itensBipados.size
                    AlertDialog(
                        onDismissRequest = { showConcluirPresencaConfirm = false },
                        icon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(32.dp))
                        },
                        title = {
                            Text("Finalizar Auditoria de Presença?", fontWeight = FontWeight.Bold, color = Color.White)
                        },
                        text = {
                            Text(
                                text = "Deseja concluir a auditoria do setor '${session.setor}'? Foram confirmados $confirmadosCount produtos em área. Todos os operadores da sessão serão desconectados.",
                                fontSize = 13.sp,
                                color = DarkTextPrimary
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val userObj = currentUser ?: Usuario(
                                        matricula = "000000",
                                        nome = "Operador",
                                        cargo = "operador",
                                        loja_id = session.lojaId
                                    )

                                    // Calcular métricas oficiais do setor para o relatório em PDF
                                    val produtosSetor = if (session.setor.equals("Todos", ignoreCase = true)) {
                                        produtos
                                    } else {
                                        produtos.filter { it.setor.equals(session.setor, ignoreCase = true) }
                                    }
                                    val totalEsperado = produtosSetor.size.coerceAtLeast(session.itensBipados.size)
                                    val totalBipados = session.itensBipados.size
                                    val totalFaltantes = (totalEsperado - totalBipados).coerceAtLeast(0)
                                    val taxa = if (totalEsperado > 0) (totalBipados * 100 / totalEsperado) else 100

                                    val bipadosIds = session.itensBipados.keys
                                    val produtosAusentes = produtosSetor.filter { !bipadosIds.contains(it.id) }.map {
                                        ProdutoAusenteResumo(
                                            id = it.id,
                                            nome = it.nome,
                                            codigoBarras = it.codigo_barras,
                                            setor = it.setor,
                                            quantidadeEstoque = it.quantidade,
                                            quantidadeEsperada = it.quantidade
                                        )
                                    }

                                    val novoRelatorio = RelatorioAuditoriaPresenca(
                                        sessionId = session.sessionId,
                                        lojaId = session.lojaId,
                                        setor = session.setor,
                                        iniciadaPorNome = session.iniciadaPorNome,
                                        iniciadaPorMatricula = session.iniciadaPorMatricula,
                                        finalizadaPorNome = userObj.nome,
                                        finalizadaPorMatricula = userObj.matricula,
                                        dataInicioMs = session.iniciadaEmMs,
                                        dataFimMs = System.currentTimeMillis(),
                                        totalEsperado = totalEsperado,
                                        totalBipados = totalBipados,
                                        totalNaoLocalizados = totalFaltantes,
                                        taxaPresenca = taxa,
                                        itensConfirmados = session.itensBipados.values.toList(),
                                        produtosNaoBipados = produtosAusentes
                                    )

                                    PresenceAuditReportManager.salvarRelatorio(context, novoRelatorio)

                                    coroutineScope.launch {
                                        PresenceAuditSessionManager.completeSession(context, userObj)
                                    }

                                    auditoriaIniciadaLocalmente = false
                                    showConcluirPresencaConfirm = false
                                    currentTab = AuditoriaTipo.RELATORIOS
                                    Toast.makeText(context, "Auditoria concluída! Relatório salvo na 4ª aba com opção de baixar em PDF.", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe)
                            ) {
                                Text("Finalizar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConcluirPresencaConfirm = false }) {
                                Text("Continuar Auditando", color = DarkTextSecondary)
                            }
                        }
                    )
                }
            }

            // =========================================================================
            // 2. AUDITORIA DE INVENTÁRIO (SOMENTE PARA MASTER)
            // =========================================================================
            AuditoriaTipo.INVENTARIO -> {
                if (!isMaster) {
                    // Bloqueio de acesso para operadores comuns
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedExpressive.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, RedExpressive),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Acesso Restrito ao Master",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "A auditoria de inventário físico e acerto contábil de estoque é restrita a usuários com perfil Master ou Administrador.",
                                    fontSize = 12.sp,
                                    color = DarkTextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Conteúdo exclusivo para perfil Master
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Inventário Geral de Estoque", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF581C87).copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, Color(0xFFA855F7))
                                ) {
                                    Text("Master Exclusivo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                text = "Compare o estoque registrado no sistema com a contagem física das prateleiras.",
                                fontSize = 11.sp,
                                color = DarkTextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Campo de Busca
                    OutlinedTextField(
                        value = buscaInventario,
                        onValueChange = { buscaInventario = it },
                        placeholder = { Text("Buscar por nome, EAN ou PLU...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botão para emitir e salvar o relatório de inventário oficial
                    Button(
                        onClick = {
                            val itensResumo = produtos.map { prod ->
                                val contado = contagensFisicas[prod.id] ?: prod.quantidade
                                ItemInventarioResumo(
                                    produtoId = prod.id,
                                    produtoNome = prod.nome,
                                    codigoBarras = prod.codigo_barras,
                                    setor = prod.setor,
                                    quantidadeEstoque = prod.quantidade,
                                    quantidadeContada = contado,
                                    divergencia = contado - prod.quantidade
                                )
                            }
                            val divergencias = itensResumo.count { it.divergencia != 0 }
                            val relatorioInv = RelatorioAuditoriaInventario(
                                lojaId = currentUser?.loja_id ?: "default",
                                setor = filtroSetorInventario,
                                responsavelNome = currentUser?.nome ?: "Gestor Master",
                                responsavelMatricula = currentUser?.matricula ?: "-",
                                dataHoraMs = System.currentTimeMillis(),
                                totalItensAuditados = itensResumo.size,
                                totalDivergencias = divergencias,
                                totalEstoqueSistema = itensResumo.sumOf { it.quantidadeEstoque },
                                totalContagemFisica = itensResumo.sumOf { it.quantidadeContada },
                                itens = itensResumo
                            )
                            PresenceAuditReportManager.salvarRelatorioInventario(context, relatorioInv)
                            if (currentUser != null) {
                                LogManager.recordLog(
                                    usuarioMatricula = currentUser.matricula,
                                    usuarioNome = currentUser.nome,
                                    lojaId = currentUser.loja_id,
                                    acao = "AUDITORIA_INVENTARIO_CONCLUIDA",
                                    detalhes = "Gerou relatório de inventário do setor '$filtroSetorInventario'. Total itens: ${itensResumo.size} | Divergências: $divergencias"
                                )
                            }
                            Toast.makeText(context, "Relatório de Inventário salvo! Acesse a aba Relatórios.", Toast.LENGTH_LONG).show()
                            currentTab = AuditoriaTipo.RELATORIOS
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Concluir e Salvar Relatório de Inventário", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val produtosFiltrados = produtos.filter { p ->
                        (filtroSetorInventario == "Todos" || p.setor.equals(filtroSetorInventario, ignoreCase = true)) &&
                        (buscaInventario.isBlank() ||
                        p.nome.contains(buscaInventario, ignoreCase = true) ||
                        p.codigo_barras.contains(buscaInventario, ignoreCase = true) ||
                        (p.plu != null && p.plu.contains(buscaInventario, ignoreCase = true)))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(produtosFiltrados, key = { it.id }) { prod ->
                            val contagem = contagensFisicas[prod.id] ?: prod.quantidade
                            val diferenca = contagem - prod.quantidade

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(1.dp, if (diferenca != 0) Color(0xFFA855F7).copy(alpha = 0.5f) else DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.nome, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1)
                                            Text("EAN: ${prod.codigo_barras}${if (!prod.plu.isNullOrBlank()) " • PLU: ${prod.plu}" else ""}", fontSize = 10.sp, color = DarkTextSecondary)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DarkSurfaceContainerHigh,
                                            border = BorderStroke(1.dp, DarkBorder)
                                        ) {
                                            Text(
                                                text = "Sistema: ${prod.quantidade} un.",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF93C5FD),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Controles de Contagem Física
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Físico:", fontSize = 11.sp, color = DarkTextSecondary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    if (contagem > 0) contagensFisicas[prod.id] = contagem - 1
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Text("-", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF581C87).copy(alpha = 0.3f),
                                                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    text = "$contagem",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    contagensFisicas[prod.id] = contagem + 1
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (diferenca != 0) {
                                                Text(
                                                    text = if (diferenca > 0) "+$diferenca un." else "$diferenca un.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (diferenca > 0) Color(0xFF38BDF8) else RedExpressive,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )

                                                Button(
                                                    onClick = {
                                                        produtoParaAjustar = prod
                                                        novaQtdAjuste = contagem
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Ajustar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Text("Conferido", fontSize = 11.sp, color = EmeraldSafe, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. AUDITORIA DE VALIDADE (POR SEÇÃO / INTERFACE TRAVADA)
            // =========================================================================
            AuditoriaTipo.VALIDADE -> {
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

                if (!isValidadeLocked) {
                    // TELA 1: SELEÇÃO DE SEÇÃO E PREPARAÇÃO PARA INICIAR AUDITORIA
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auditoria de Validade por Seção", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            }
                            Text(
                                text = "A conferência de validades é feita por seção. Ao iniciar, a interface ficará travada para garantir foco e registrará o momento exato de início e fim no log de auditoria.",
                                fontSize = 11.sp,
                                color = DarkTextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seleção do Setor
                    Text("1. Selecione o Setor:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(setoresDisponiveis, key = { it.nome }) { setor ->
                            val isSelected = setor.nome == setorValidadeSelecionado
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    setorValidadeSelecionado = setor.nome
                                    // Seleciona a primeira seção disponível do setor
                                    val subsetores = SectorManager.getSubsetoresDoSetor(context, setor.nome)
                                    secaoValidadeSelecionada = subsetores.firstOrNull() ?: "Geral"
                                },
                                label = { Text(setor.nome, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceContainerHigh,
                                    labelColor = DarkTextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Seleção da Seção (Subsetor)
                    val subsetoresDoSetor = remember(setorValidadeSelecionado) {
                        SectorManager.getSubsetoresDoSetor(context, setorValidadeSelecionado)
                    }

                    Text("2. Selecione a Seção do Setor ($setorValidadeSelecionado):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subsetoresDoSetor) { sub ->
                            val isSelected = sub == secaoValidadeSelecionada
                            FilterChip(
                                selected = isSelected,
                                onClick = { secaoValidadeSelecionada = sub },
                                label = { Text(sub, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF38BDF8),
                                    selectedLabelColor = Color(0xFF0F172A),
                                    containerColor = DarkSurfaceContainerHigh,
                                    labelColor = DarkTextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Card informativo da seção escolhida
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceContainerHigh,
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pronto para iniciar a Seção", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Setor: $setorValidadeSelecionado", fontSize = 12.sp, color = DarkTextSecondary)
                            Text("Seção a auditar: $secaoValidadeSelecionada", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ao clicar no botão abaixo, a interface ficará travada exclusivamente nesta seção até você finalizá-la.",
                                fontSize = 11.sp,
                                color = DarkTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            momentoInicioValidade = System.currentTimeMillis()
                            isValidadeLocked = true
                            itensAuditadosValidade.clear()
                            produtoValidadeBipado = null
                            inputCodigoValidade = ""

                            val horaStr = timeFormat.format(Date(momentoInicioValidade))
                            if (currentUser != null) {
                                LogManager.recordLog(
                                    usuarioMatricula = currentUser.matricula,
                                    usuarioNome = currentUser.nome,
                                    lojaId = currentUser.loja_id,
                                    acao = "INICIO_AUDITORIA_VALIDADE_SECAO",
                                    detalhes = "Iniciou auditoria de validade na seção '$secaoValidadeSelecionada' ($setorValidadeSelecionado) às $horaStr"
                                )
                            }
                            Toast.makeText(context, "🔒 Interface travada para a seção '$secaoValidadeSelecionada'!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Auditoria da Seção", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    // TELA 2: INTERFACE TRAVADA - EM ANDAMENTO
                    // Header de interface travada com cronômetro/horário de início
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF082F49)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0284C7),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🔒 SESSÃO TRAVADA EM ANDAMENTO", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                                Text("Seção: $secaoValidadeSelecionada ($setorValidadeSelecionado)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text("Iniciada às: ${timeFormat.format(Date(momentoInicioValidade))} • Conferidos: ${itensAuditadosValidade.size} itens", fontSize = 10.sp, color = Color(0xFFBAE6FD))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Campo de bipagem do produto (Puxa automaticamente todas as informações: nome, PLU, etc.)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputCodigoValidade,
                            onValueChange = { novoCodigo ->
                                inputCodigoValidade = novoCodigo
                                val clean = novoCodigo.trim()
                                if (clean.isNotEmpty()) {
                                    val encontrado = produtos.find { p ->
                                        p.codigo_barras.equals(clean, ignoreCase = true) ||
                                        (p.plu != null && p.plu.equals(clean, ignoreCase = true))
                                    }
                                    if (encontrado != null) {
                                        produtoValidadeBipado = encontrado
                                        inputNovaDataValidade = encontrado.data_vencimento
                                        inputQtdValidadeArea = encontrado.quantidade
                                        SoundFeedbackHelper.playSuccessBeep(context)
                                    }
                                }
                            },
                            label = { Text("Bipar EAN ou PLU do produto", fontSize = 12.sp) },
                            placeholder = { Text("Ex: 7891000... ou 1042", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF38BDF8)) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    BarcodeScannerHelper.startScan(
                                        context = context,
                                        onSuccess = { barcode ->
                                            SoundFeedbackHelper.playSuccessBeep(context)
                                            inputCodigoValidade = barcode.trim()
                                            val encontrado = produtos.find { p ->
                                                p.codigo_barras.equals(barcode.trim(), ignoreCase = true) ||
                                                (p.plu != null && p.plu.equals(barcode.trim(), ignoreCase = true))
                                            }
                                            if (encontrado != null) {
                                                produtoValidadeBipado = encontrado
                                                inputNovaDataValidade = encontrado.data_vencimento
                                                inputQtdValidadeArea = encontrado.quantidade
                                            } else {
                                                Toast.makeText(context, "Produto não encontrado: $barcode", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onError = {
                                            Toast.makeText(context, "Erro no scanner: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear Câmera", tint = Color(0xFF38BDF8))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = DarkBorder
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                val clean = inputCodigoValidade.trim()
                                val encontrado = produtos.find { p ->
                                    p.codigo_barras.equals(clean, ignoreCase = true) ||
                                    (p.plu != null && p.plu.equals(clean, ignoreCase = true)) ||
                                    p.nome.contains(clean, ignoreCase = true)
                                }
                                if (encontrado != null) {
                                    produtoValidadeBipado = encontrado
                                    inputNovaDataValidade = encontrado.data_vencimento
                                    inputQtdValidadeArea = encontrado.quantidade
                                    SoundFeedbackHelper.playSuccessBeep(context)
                                } else {
                                    Toast.makeText(context, "Nenhum produto localizado para: $clean", Toast.LENGTH_SHORT).show()
                                }
                            }),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                val clean = inputCodigoValidade.trim()
                                val encontrado = produtos.find { p ->
                                    p.codigo_barras.equals(clean, ignoreCase = true) ||
                                    (p.plu != null && p.plu.equals(clean, ignoreCase = true)) ||
                                    p.nome.contains(clean, ignoreCase = true)
                                }
                                if (encontrado != null) {
                                    produtoValidadeBipado = encontrado
                                    inputNovaDataValidade = encontrado.data_vencimento
                                    inputQtdValidadeArea = encontrado.quantidade
                                    SoundFeedbackHelper.playSuccessBeep(context)
                                } else {
                                    Toast.makeText(context, "Nenhum produto localizado.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF0284C7), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // CARD DO PRODUTO PUXADO (Informações automáticas: Nome, PLU, Estoque atual + inputs do operador)
                    if (produtoValidadeBipado != null) {
                        val prod = produtoValidadeBipado!!
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(prod.nome, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { produtoValidadeBipado = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Cancelar", tint = DarkTextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "PLU: ${prod.plu?.ifBlank { "N/D" } ?: "N/D"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, DarkBorder)
                                    ) {
                                        Text(
                                            text = "EAN: ${prod.codigo_barras}",
                                            fontSize = 11.sp,
                                            color = DarkTextPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Validade atual no sistema: ${prod.data_vencimento} • Estoque sistema: ${prod.quantidade} un.", fontSize = 11.sp, color = DarkTextSecondary)

                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(DarkBorder))

                                // Inputs do operador: Data e Quantidade em Área
                                Text("Conferência Física em Área:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inputNovaDataValidade,
                                        onValueChange = { inputNovaDataValidade = it },
                                        label = { Text("Validade Física (AAAA-MM-DD)", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Qtd em Área", fontSize = 10.sp, color = DarkTextSecondary)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (inputQtdValidadeArea > 0) inputQtdValidadeArea-- },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                                            }
                                            Text("$inputQtdValidadeArea", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF38BDF8))
                                            IconButton(
                                                onClick = { inputQtdValidadeArea++ },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        isSalvandoValidadeItem = true
                                        val dataTrim = inputNovaDataValidade.trim()
                                        val qtdSalvar = inputQtdValidadeArea

                                        coroutineScope.launch {
                                            try {
                                                SupabaseClient.instance.from("produtos")
                                                    .update(AtualizarProdutoValidade(dataTrim)) {
                                                        eq("id", prod.id)
                                                    }
                                                SupabaseClient.instance.from("produtos")
                                                    .update(AtualizarProdutoQtd(qtdSalvar)) {
                                                        eq("id", prod.id)
                                                    }

                                                // Registrar em log detalhado
                                                if (currentUser != null) {
                                                    LogManager.recordLog(
                                                        usuarioMatricula = currentUser.matricula,
                                                        usuarioNome = currentUser.nome,
                                                        lojaId = currentUser.loja_id,
                                                        acao = "AUDITORIA_VALIDADE_ITEM",
                                                        detalhes = "Seção: $secaoValidadeSelecionada | Produto: '${prod.nome}' | EAN: ${prod.codigo_barras} | PLU: ${prod.plu ?: "-"} | Nova Validade: $dataTrim | Qtd em Área: $qtdSalvar un."
                                                    )
                                                }

                                                SoundFeedbackHelper.playSuccessBeep(context)
                                                itensAuditadosValidade.add(
                                                    ItemAuditadoValidade(
                                                        id = prod.id,
                                                        produtoNome = prod.nome,
                                                        codigoBarras = prod.codigo_barras,
                                                        plu = prod.plu,
                                                        setor = prod.setor,
                                                        validadeAnterior = prod.data_vencimento,
                                                        validadeNova = dataTrim,
                                                        qtdArea = qtdSalvar,
                                                        timestamp = System.currentTimeMillis()
                                                    )
                                                )

                                                Toast.makeText(context, "✅ Lote registrado no log!", Toast.LENGTH_SHORT).show()
                                                produtoValidadeBipado = null
                                                inputCodigoValidade = ""
                                                onRefreshProdutos()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Erro ao gravar: ${e.message}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isSalvandoValidadeItem = false
                                            }
                                        }
                                    },
                                    enabled = !isSalvandoValidadeItem && inputNovaDataValidade.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSalvandoValidadeItem) {
                                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Confirmar & Gravar no Log", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // LISTA DOS ITENS AUDITADOS NESTA SESSÃO
                    Text(
                        text = "Itens Auditados nesta Seção (${itensAuditadosValidade.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (itensAuditadosValidade.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Nenhum produto bipado nesta seção ainda.", fontSize = 12.sp, color = DarkTextSecondary)
                                    Text("Bipe ou digite o código/PLU acima para começar.", fontSize = 11.sp, color = DarkTextSecondary.copy(alpha = 0.7f))
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(itensAuditadosValidade.reversed(), key = { "${it.id}_${it.timestamp}" }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurfaceContainer,
                                    border = BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldSafe.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, EmeraldSafe),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.produtoNome, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1)
                                            Text("PLU: ${item.plu ?: "-"} • EAN: ${item.codigoBarras}", fontSize = 10.sp, color = DarkTextSecondary)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Validade: ${item.validadeNova}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))
                                                Text("Área: ${item.qtdArea} un.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldSafe)
                                            }
                                        }

                                        Text(
                                            text = timeFormat.format(Date(item.timestamp)),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = DarkTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BOTÃO PARA FINALIZAR A SEÇÃO (DESTRAVAR A INTERFACE)
                    Button(
                        onClick = { showConfirmarFinalizacaoValidade = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedExpressive),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizar Seção de Validade", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // =========================================================================
            // 4. QUARTA ABA: RELATÓRIOS OFICIAIS & DOWNLOAD EM PDF (SOMENTE MASTER)
            // =========================================================================
            AuditoriaTipo.RELATORIOS -> {
                if (!isMaster) {
                    // Acesso estritamente bloqueado para operadores comuns
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedExpressive.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, RedExpressive),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Acesso Restrito ao Perfil Master",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "A visualização de relatórios oficiais de auditoria e a emissão de arquivos em PDF é restrita exclusivamente a usuários com perfil Master ou Administrador.",
                                    fontSize = 12.sp,
                                    color = DarkTextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Visualização completa exclusiva para gestores Master
                    val relatoriosPresenca by PresenceAuditReportManager.relatorios.collectAsStateWithLifecycle()
                    val relatoriosInventario by PresenceAuditReportManager.relatoriosInventario.collectAsStateWithLifecycle()
                    val relatoriosValidade by PresenceAuditReportManager.relatoriosValidade.collectAsStateWithLifecycle()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header Informativo
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Relatórios de Auditoria",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF065F46),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                "Master Autorizado",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Histórico de auditorias finalizadas com visualização detalhada e exportação oficial em PDF",
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary
                                    )
                                }
                            }
                        }

                        // Filtros por Tipo de Relatório
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = filtroTipoRelatorios == "TODOS",
                                onClick = { filtroTipoRelatorios = "TODOS" },
                                label = { Text("Todos", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceContainerHigh,
                                    labelColor = DarkTextPrimary
                                )
                            )
                            FilterChip(
                                selected = filtroTipoRelatorios == "PRESENCA",
                                onClick = { filtroTipoRelatorios = "PRESENCA" },
                                label = { Text("Presença (${relatoriosPresenca.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF59E0B),
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceContainerHigh,
                                    labelColor = DarkTextPrimary
                                )
                            )
                            FilterChip(
                                selected = filtroTipoRelatorios == "INVENTARIO",
                                onClick = { filtroTipoRelatorios = "INVENTARIO" },
                                label = { Text("Inventário (${relatoriosInventario.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFA855F7),
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceContainerHigh,
                                    labelColor = DarkTextPrimary
                                )
                            )
                            FilterChip(
                                selected = filtroTipoRelatorios == "VALIDADE",
                                onClick = { filtroTipoRelatorios = "VALIDADE" },
                                label = { Text("Validade (${relatoriosValidade.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceContainerHigh,
                                    labelColor = DarkTextPrimary
                                )
                            )
                        }

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
                                    Icon(
                                        Icons.Default.FactCheck,
                                        contentDescription = null,
                                        tint = DarkTextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Nenhum relatório de auditoria gerado ainda.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Inicie uma conferência de presença, inventário ou seção de validade e finalize para que os relatórios oficiais fiquem disponíveis aqui.",
                                        fontSize = 12.sp,
                                        color = DarkTextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // 1. LISTA DE RELATÓRIOS DE PRESENÇA
                            if (filtroTipoRelatorios in listOf("TODOS", "PRESENCA") && relatoriosPresenca.isNotEmpty()) {
                                Text(
                                    text = "Auditorias de Presença (${relatoriosPresenca.size}):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )

                                relatoriosPresenca.forEach { relatorio ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                        border = BorderStroke(1.dp, DarkBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRelatorioForModal = relatorio }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            text = "PRESENÇA • ${relatorio.setor.uppercase()}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFF59E0B),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = relatorio.getDataFormatada(),
                                                        fontSize = 11.sp,
                                                        color = DarkTextSecondary
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (relatorio.taxaPresenca >= 80) EmeraldSafe.copy(alpha = 0.2f) else RedExpressive.copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, if (relatorio.taxaPresenca >= 80) EmeraldSafe else RedExpressive)
                                                ) {
                                                    Text(
                                                        text = "${relatorio.taxaPresenca}% Presença",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (relatorio.taxaPresenca >= 80) EmeraldSafe else RedExpressive,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                MetricBadge(label = "Esperados", value = "${relatorio.totalEsperado}", color = Color.White)
                                                MetricBadge(label = "Não Bipados", value = "${relatorio.totalNaoLocalizados}", color = if (relatorio.totalNaoLocalizados > 0) RedExpressive else Color.Gray)
                                                MetricBadge(label = "Bipados", value = "${relatorio.totalBipados}", color = EmeraldSafe)
                                                MetricBadge(label = "Duração", value = relatorio.getDuracaoFormatada(), color = Color(0xFF38BDF8))
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Resp: ${relatorio.finalizadaPorNome}",
                                                    fontSize = 11.sp,
                                                    color = DarkTextSecondary
                                                )

                                                Button(
                                                    onClick = { selectedRelatorioForModal = relatorio },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Ver Relatório / PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. LISTA DE RELATÓRIOS DE INVENTÁRIO (COMPARA CONTAGEM COM ESTOQUE)
                            if (filtroTipoRelatorios in listOf("TODOS", "INVENTARIO") && relatoriosInventario.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Auditorias de Inventário (${relatoriosInventario.size}):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA855F7)
                                )

                                relatoriosInventario.forEach { inv ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedInventarioForModal = inv }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFA855F7).copy(alpha = 0.2f),
                                                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            text = "INVENTÁRIO • ${inv.setor.uppercase()}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFC084FC),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = inv.getDataFormatada(),
                                                        fontSize = 11.sp,
                                                        color = DarkTextSecondary
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (inv.totalDivergencias == 0) EmeraldSafe.copy(alpha = 0.2f) else RedExpressive.copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, if (inv.totalDivergencias == 0) EmeraldSafe else RedExpressive)
                                                ) {
                                                    Text(
                                                        text = if (inv.totalDivergencias == 0) "100% Acurado" else "${inv.totalDivergencias} Divergências",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (inv.totalDivergencias == 0) EmeraldSafe else RedExpressive,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                MetricBadge(label = "Auditados", value = "${inv.totalItensAuditados}", color = Color.White)
                                                MetricBadge(label = "Estoque Sist.", value = "${inv.totalEstoqueSistema}", color = Color(0xFF38BDF8))
                                                MetricBadge(label = "Contado", value = "${inv.totalContagemFisica}", color = EmeraldSafe)
                                                MetricBadge(label = "Divergência", value = "${inv.totalDivergencias}", color = if (inv.totalDivergencias > 0) RedExpressive else Color.Gray)
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Resp: ${inv.responsavelNome}",
                                                    fontSize = 11.sp,
                                                    color = DarkTextSecondary
                                                )

                                                Button(
                                                    onClick = { selectedInventarioForModal = inv },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Ver Relatório / PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. LISTA DE RELATÓRIOS DE VALIDADE EM ÁREA DE VENDAS
                            if (filtroTipoRelatorios in listOf("TODOS", "VALIDADE") && relatoriosValidade.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Auditorias de Validade em Área de Vendas (${relatoriosValidade.size}):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )

                                relatoriosValidade.forEach { valRep ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedValidadeForModal = valRep }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            text = "VALIDADE • ${valRep.secao.uppercase()}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF38BDF8),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = valRep.getDataFormatada(),
                                                        fontSize = 11.sp,
                                                        color = DarkTextSecondary
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, Color(0xFF0284C7))
                                                ) {
                                                    Text(
                                                        text = "${valRep.totalPecasAreaVendas} Peças Vendas",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF38BDF8),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                MetricBadge(label = "Itens Auditados", value = "${valRep.totalItensAuditados}", color = Color.White)
                                                MetricBadge(label = "Área de Vendas", value = "${valRep.totalPecasAreaVendas} un", color = EmeraldSafe)
                                                MetricBadge(label = "Duração", value = valRep.getDuracaoFormatada(), color = Color(0xFF38BDF8))
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Resp: ${valRep.responsavelNome}",
                                                    fontSize = 11.sp,
                                                    color = DarkTextSecondary
                                                )

                                                Button(
                                                    onClick = { selectedValidadeForModal = valRep },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Ver Relatório / PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

        // =========================================================================
        // 1. MODAL DE RELATÓRIO DE PRESENÇA (PRIORIDADE: NÃO BIPADOS PRIMEIRO)
        // =========================================================================
        selectedRelatorioForModal?.let { relatorio ->
            AlertDialog(
                onDismissRequest = { selectedRelatorioForModal = null },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(26.dp))
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Relatório de Auditoria de Presença",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Setor: ${relatorio.setor} • ${relatorio.getDataFormatada()}",
                            fontSize = 11.sp,
                            color = Color(0xFF93C5FD)
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cards de Métricas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetricBadge(label = "Esperados", value = "${relatorio.totalEsperado}", color = Color.White)
                            MetricBadge(label = "Não Bipados", value = "${relatorio.totalNaoLocalizados}", color = if (relatorio.totalNaoLocalizados > 0) RedExpressive else Color.Gray)
                            MetricBadge(label = "Bipados", value = "${relatorio.totalBipados}", color = EmeraldSafe)
                            MetricBadge(label = "Acurácia", value = "${relatorio.taxaPresenca}%", color = Color(0xFFF59E0B))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceContainerHigh,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Iniciada por: ${relatorio.iniciadaPorNome} (${relatorio.iniciadaPorMatricula})", fontSize = 11.sp, color = DarkTextSecondary)
                                Text("Finalizada por: ${relatorio.finalizadaPorNome} (${relatorio.finalizadaPorMatricula})", fontSize = 11.sp, color = DarkTextSecondary)
                                Text("Duração total: ${relatorio.getDuracaoFormatada()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))
                            }
                        }

                        // PRIORIDADE 1: PRODUTOS NÃO BIPADOS (FALTANTES) EXIBIDOS PRIMEIRO
                        if (relatorio.produtosNaoBipados.isNotEmpty()) {
                            Text(
                                text = "⚠️ PRIORIDADE: Produtos Não Bipados em Área (${relatorio.produtosNaoBipados.size}):",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = RedExpressive
                            )

                            relatorio.produtosNaoBipados.forEach { prod ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkSurfaceContainerHigh,
                                    border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.nome, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                            Text("EAN: ${prod.codigoBarras} • Setor: ${prod.setor}", fontSize = 9.sp, color = DarkTextSecondary)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = RedExpressive.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, RedExpressive)
                                            ) {
                                                Text(
                                                    "Estoque: ${prod.quantidadeEstoque} un",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RedExpressive,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text("NÃO BIPADO", fontSize = 8.sp, color = RedExpressive, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // SEGUIDO POR: PRODUTOS CONFIRMADOS / BIPADOS EM ÁREA
                        Text(
                            text = "✅ Produtos Confirmados em Área (${relatorio.itensConfirmados.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = EmeraldSafe
                        )

                        if (relatorio.itensConfirmados.isEmpty()) {
                            Text("Nenhum produto bipado nesta sessão.", fontSize = 11.sp, color = DarkTextSecondary)
                        } else {
                            relatorio.itensConfirmados.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkSurfaceContainerHigh,
                                    border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.nome, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                            Text("EAN: ${item.codigoBarras} • Op: ${item.operadorNome}", fontSize = 9.sp, color = DarkTextSecondary)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = EmeraldSafe.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, EmeraldSafe)
                                            ) {
                                                Text(
                                                    "Estoque: ${item.quantidadeEstoque} un",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldSafe,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(item.getFormattedTime(), fontSize = 9.sp, color = EmeraldSafe, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val pdfFile = AuditPdfGenerator.generatePresenceAuditPdf(context, relatorio)
                                AuditPdfGenerator.openPdf(context, pdfFile)
                                Toast.makeText(context, "PDF gerado: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar em PDF", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val pdfFile = AuditPdfGenerator.generatePresenceAuditPdf(context, relatorio)
                                    AuditPdfGenerator.sharePdf(context, pdfFile)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao compartilhar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartilhar", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { selectedRelatorioForModal = null }) {
                            Text("Fechar", color = DarkTextSecondary)
                        }
                    }
                },
                containerColor = DarkSurfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // =========================================================================
        // 2. MODAL DE RELATÓRIO DE INVENTÁRIO (COMPARA CONTADO VS ESTOQUE)
        // =========================================================================
        selectedInventarioForModal?.let { inv ->
            AlertDialog(
                onDismissRequest = { selectedInventarioForModal = null },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFA855F7).copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(26.dp))
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Relatório de Inventário Físico",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Setor: ${inv.setor} • ${inv.getDataFormatada()}",
                            fontSize = 11.sp,
                            color = Color(0xFFC084FC)
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetricBadge(label = "Auditados", value = "${inv.totalItensAuditados}", color = Color.White)
                            MetricBadge(label = "Estoque Sist.", value = "${inv.totalEstoqueSistema}", color = Color(0xFF38BDF8))
                            MetricBadge(label = "Contado", value = "${inv.totalContagemFisica}", color = EmeraldSafe)
                            MetricBadge(label = "Divergência", value = "${inv.totalDivergencias}", color = if (inv.totalDivergencias > 0) RedExpressive else Color.Gray)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceContainerHigh,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Responsável: ${inv.responsavelNome} (${inv.responsavelMatricula})", fontSize = 11.sp, color = DarkTextSecondary)
                                val statusInv = if (inv.totalDivergencias == 0) "Estoque 100% acurado sem divergências." else "${inv.totalDivergencias} produtos com divergência física."
                                Text(statusInv, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (inv.totalDivergencias == 0) EmeraldSafe else RedExpressive)
                            }
                        }

                        Text(
                            text = "Comparativo Contagem vs Estoque (${inv.itens.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFA855F7)
                        )

                        inv.itens.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceContainerHigh,
                                border = BorderStroke(1.dp, if (item.divergencia != 0) RedExpressive.copy(alpha = 0.4f) else DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.produtoNome, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                        Text("EAN: ${item.codigoBarras} • Setor: ${item.setor}", fontSize = 9.sp, color = DarkTextSecondary)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Estoque: ${item.quantidadeEstoque} | Contado: ${item.quantidadeContada}", fontSize = 10.sp, color = Color.White)
                                        if (item.divergencia > 0) {
                                            Text("+${item.divergencia} un (Sobra)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        } else if (item.divergencia < 0) {
                                            Text("${item.divergencia} un (Falta)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RedExpressive)
                                        } else {
                                            Text("0 un (Conferido)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldSafe)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val pdfFile = AuditPdfGenerator.generateInventoryAuditPdf(context, inv)
                                AuditPdfGenerator.openPdf(context, pdfFile)
                                Toast.makeText(context, "PDF gerado: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar em PDF", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val pdfFile = AuditPdfGenerator.generateInventoryAuditPdf(context, inv)
                                    AuditPdfGenerator.sharePdf(context, pdfFile)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao compartilhar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartilhar", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { selectedInventarioForModal = null }) {
                            Text("Fechar", color = DarkTextSecondary)
                        }
                    }
                },
                containerColor = DarkSurfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // =========================================================================
        // 3. MODAL DE RELATÓRIO DE VALIDADE EM ÁREA DE VENDAS
        // =========================================================================
        selectedValidadeForModal?.let { valRep ->
            AlertDialog(
                onDismissRequest = { selectedValidadeForModal = null },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(26.dp))
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Relatório de Validade em Área de Vendas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Seção: ${valRep.secao} (${valRep.setor}) • ${valRep.getDataFormatada()}",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetricBadge(label = "Auditados", value = "${valRep.totalItensAuditados}", color = Color.White)
                            MetricBadge(label = "Área de Vendas", value = "${valRep.totalPecasAreaVendas} un", color = EmeraldSafe)
                            MetricBadge(label = "Duração", value = valRep.getDuracaoFormatada(), color = Color(0xFF38BDF8))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceContainerHigh,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Responsável: ${valRep.responsavelNome} (${valRep.responsavelMatricula})", fontSize = 11.sp, color = DarkTextSecondary)
                                Text("Conferência por seção com foco em produtos expostos para venda.", fontSize = 11.sp, color = Color(0xFF38BDF8))
                            }
                        }

                        Text(
                            text = "Produtos Auditados em Área de Vendas (${valRep.itens.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7)
                        )

                        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

                        valRep.itens.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceContainerHigh,
                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.produtoNome, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                        Text("EAN: ${item.codigoBarras} • Seção: ${item.secao}", fontSize = 9.sp, color = DarkTextSecondary)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFF0284C7))
                                        ) {
                                            Text(
                                                "Val: ${item.validadeNova}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            "Área: ${item.quantidadeAreaVendas} un • ${timeFmt.format(Date(item.timestamp))}",
                                            fontSize = 9.sp,
                                            color = EmeraldSafe,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val pdfFile = AuditPdfGenerator.generateValidityAuditPdf(context, valRep)
                                AuditPdfGenerator.openPdf(context, pdfFile)
                                Toast.makeText(context, "PDF gerado: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar em PDF", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val pdfFile = AuditPdfGenerator.generateValidityAuditPdf(context, valRep)
                                    AuditPdfGenerator.sharePdf(context, pdfFile)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao compartilhar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartilhar", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { selectedValidadeForModal = null }) {
                            Text("Fechar", color = DarkTextSecondary)
                        }
                    }
                },
                containerColor = DarkSurfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceContainerHigh,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = DarkTextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
