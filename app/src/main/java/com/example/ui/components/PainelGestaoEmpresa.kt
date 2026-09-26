package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CargoManager
import com.example.data.FeedbackManager
import com.example.data.SectorManager
import com.example.data.supabase.AppLog
import com.example.data.supabase.LogManager
import com.example.data.supabase.NovoUsuario
import com.example.data.supabase.Produto
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.UserFeedback
import com.example.data.supabase.Usuario
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import com.example.util.CryptoUtils
import com.example.util.decrypted
import com.example.util.encrypted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FuncionarioDesempenho(
    val usuario: Usuario,
    val itensAuditadosSemana: Int,
    val conferenciasSemana: Int,
    val ultimaConferenciaMs: Long?,
    val nivelDesempenho: String, // "Excelente", "Bom", "Regular", "Pendente"
    val realizouConferenciaSemanal: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelGestaoEmpresa(
    produtos: List<Produto>,
    masterLojaId: String,
    masterLojaNome: String,
    currentUser: Usuario,
    onRefreshProdutos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }

    // Abas: 0 = Desempenho Conferências Semanais, 1 = Funcionários da Empresa, 2 = Feedbacks da Loja
    var selectedTab by remember { mutableIntStateOf(0) }

    var usuariosLoja by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var logsLoja by remember { mutableStateOf<List<AppLog>>(emptyList()) }
    var feedbacksLoja by remember { mutableStateOf<List<UserFeedback>>(emptyList()) }
    var isLoadingDados by remember { mutableStateOf(true) }

    var showAddFuncionarioDialog by remember { mutableStateOf(false) }
    var showGerenciarCargosDialog by remember { mutableStateOf(false) }
    var showGerenciarSetoresDialog by remember { mutableStateOf(false) }
    var showFeedbacksDialog by remember { mutableStateOf(false) }
    var showRelatoriosEmpresaDialog by remember { mutableStateOf(false) }
    var usuarioParaEditar by remember { mutableStateOf<Usuario?>(null) }
    var usuarioParaExcluir by remember { mutableStateOf<Usuario?>(null) }

    val isMaster = SessionHolder.isMaster || com.example.data.CargoManager.isPerfilMaster(currentUser, context)

    // Carrega usuários da loja, feedbacks e logs semanais
    fun carregarDadosEmpresa() {
        isLoadingDados = true
        coroutineScope.launch {
            try {
                // 1. Usuários vinculados a esta empresa
                val users = supabase.from("usuarios")
                    .select { eq("loja_id", masterLojaId) }
                    .decodeList<Usuario>()
                    .map { it.decrypted() }
                usuariosLoja = users

                // 2. Logs da loja dos últimos 14 dias para cálculo semanal
                val logsRemotos = try {
                    supabase.from("app_logs")
                        .select {
                            eq("loja_id", masterLojaId)
                            order("timestamp", ascending = false)
                            limit(300)
                        }
                        .decodeList<AppLog>()
                } catch (_: Exception) {
                    emptyList()
                }

                // Unir com logs em memória do LogManager
                val logsMemoria = LogManager.logs.value.filter { it.loja_id == masterLojaId }
                val todosLogs = (logsRemotos + logsMemoria).distinctBy { it.id }
                logsLoja = todosLogs

                // 3. Feedbacks destinados à loja/master
                val fbs = try {
                    FeedbackManager.carregarFeedbacksComSupabase(context)
                    FeedbackManager.getFeedbacksParaMaster(context, masterLojaId)
                } catch (_: Exception) {
                    FeedbackManager.getFeedbacksParaMaster(context, masterLojaId)
                }
                feedbacksLoja = fbs
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar dados da empresa: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingDados = false
            }
        }
    }

    LaunchedEffect(masterLojaId) {
        carregarDadosEmpresa()
    }

    val feedbacksPendentesMaster = remember(feedbacksLoja, showFeedbacksDialog) {
        feedbacksLoja.count { it.status.equals("PENDENTE", ignoreCase = true) }
    }

    // =========================================================================
    // CÁLCULOS DO DESEMPENHO DAS CONFERÊNCIAS SEMANAIS DOS FUNCIONÁRIOS
    // =========================================================================
    val agora = System.currentTimeMillis()
    val inicioSemanaMs = agora - (7L * 24 * 3600 * 1000)

    val logsSemana = remember(logsLoja) {
        logsLoja.filter { it.timestamp >= inicioSemanaMs }
    }

    val desempenhoFuncionarios = remember(usuariosLoja, logsSemana) {
        usuariosLoja.map { user ->
            val logsUserSemana = logsSemana.filter { it.usuario_matricula == user.matricula }
            // Conta itens conferidos nos logs de auditoria
            val bipsPresenca = logsUserSemana.count { it.acao.contains("BIP", ignoreCase = true) }
            val conferencias = logsUserSemana.count {
                it.acao in listOf("AUDITORIA_PRESENCA_FIM", "AUDITORIA_PRESENCA_START", "CONFERENCIA_SEGUNDA", "AUDITORIA_INVENTARIO", "AUDITORIA_VALIDADE")
            }
            val totalItens = if (bipsPresenca > 0) bipsPresenca else {
                // Fallback estimativo baseado em sessões concluídas
                conferencias * 12
            }

            val ultimoLog = logsUserSemana.maxByOrNull { it.timestamp }
            val ultimaConferencia = ultimoLog?.timestamp

            val nivel = when {
                totalItens >= 40 -> "Excelente"
                totalItens >= 15 -> "Bom"
                totalItens > 0 -> "Regular"
                else -> "Pendente"
            }

            FuncionarioDesempenho(
                usuario = user,
                itensAuditadosSemana = totalItens,
                conferenciasSemana = conferencias,
                ultimaConferenciaMs = ultimaConferencia,
                nivelDesempenho = nivel,
                realizouConferenciaSemanal = totalItens > 0 || conferencias > 0
            )
        }.sortedByDescending { it.itensAuditadosSemana }
    }

    val totalConferenciasSemana = remember(desempenhoFuncionarios) {
        desempenhoFuncionarios.sumOf { it.conferenciasSemana }
    }
    val totalItensAuditadosSemana = remember(desempenhoFuncionarios) {
        desempenhoFuncionarios.sumOf { it.itensAuditadosSemana }
    }
    val funcionariosAtivosSemana = remember(desempenhoFuncionarios) {
        desempenhoFuncionarios.count { it.realizouConferenciaSemanal }
    }

    // Modal para adicionar funcionário pelo Master (com vínculo automático à empresa)
    if (showAddFuncionarioDialog && isMaster) {
        DialogAdicionarFuncionarioMaster(
            masterLojaId = masterLojaId,
            masterLojaNome = masterLojaNome,
            onDismiss = { showAddFuncionarioDialog = false },
            onFuncionarioAdicionado = {
                showAddFuncionarioDialog = false
                carregarDadosEmpresa()
            }
        )
    }

    // Diálogo de Gerenciar Cargos
    if (showGerenciarCargosDialog) {
        GerenciarCargosDialog(
            lojaId = masterLojaId,
            onDismiss = { showGerenciarCargosDialog = false }
        )
    }

    // Diálogo de Gerenciar Setores
    if (showGerenciarSetoresDialog) {
        GerenciarSetoresDialog(
            onDismiss = { showGerenciarSetoresDialog = false }
        )
    }

    // Modal de Relatórios Oficiais da Empresa (Exclusivo Master)
    if (showRelatoriosEmpresaDialog) {
        RelatoriosEmpresaMasterDialog(
            onDismiss = { showRelatoriosEmpresaDialog = false }
        )
    }

    // Modal de Feedbacks da Loja
    if (showFeedbacksDialog) {
        MasterFeedbacksDialog(
            masterLojaId = masterLojaId,
            masterLojaNome = masterLojaNome,
            onDismiss = {
                showFeedbacksDialog = false
                carregarDadosEmpresa()
            }
        )
    }

    // Confirmação de exclusão de funcionário
    usuarioParaExcluir?.let { user ->
        AlertDialog(
            onDismissRequest = { usuarioParaExcluir = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(32.dp))
            },
            title = { Text("Excluir Colaborador?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    text = "Deseja remover o colaborador \"${user.nome}\" (Matrícula: ${user.matricula}) da empresa?",
                    color = DarkTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = user
                        usuarioParaExcluir = null
                        coroutineScope.launch {
                            try {
                                supabase.from("usuarios").delete { eq("matricula", toDel.matricula) }
                                usuariosLoja = usuariosLoja.filter { it.matricula != toDel.matricula }
                                LogManager.recordLog(
                                    usuarioMatricula = currentUser.matricula,
                                    usuarioNome = currentUser.nome,
                                    lojaId = masterLojaId,
                                    acao = "EXCLUSAO_USUARIO",
                                    detalhes = "Excluiu colaborador '${toDel.nome}' (Matrícula: ${toDel.matricula})"
                                )
                                Toast.makeText(context, "Colaborador excluído da empresa.", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { usuarioParaExcluir = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // =====================================================================
        // HEADER DA EMPRESA
        // =====================================================================
        Card(
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF581C87).copy(alpha = 0.4f),
                            border = BorderStroke(1.5.dp, Color(0xFFA855F7)),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = masterLojaNome,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF581C87)
                                ) {
                                    Text(
                                        text = "GESTOR MASTER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE9D5FF),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Painel Administrativo da Empresa • Gestão & Validades",
                                fontSize = 11.sp,
                                color = DarkTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = {
                        carregarDadosEmpresa()
                        onRefreshProdutos()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar Dados", tint = Color(0xFFC084FC))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // BOTÕES DE AÇÃO DO GESTOR MASTER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // BOTÃO ADICIONAR FUNCIONÁRIO (Vínculo automático com a loja)
                    Button(
                        onClick = { showAddFuncionarioDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("btn_painel_empresa_add_funcionario")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Adicionar Funcionário",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // BOTÃO VER FEEDBACKS DA LOJA
                    Button(
                        onClick = { showFeedbacksDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("btn_feedbacks_loja_gestao")
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Feedbacks da Loja",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (feedbacksPendentesMaster > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$feedbacksPendentesMaster",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Botão Cargos
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceContainerHigh,
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showGerenciarCargosDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cargos", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Botão Setores
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceContainerHigh,
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showGerenciarSetoresDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Setores", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Botão Relatórios da Empresa (Acesso Unificado para qualquer perfil Master)
                    Button(
                        onClick = { showRelatoriosEmpresaDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("btn_relatorios_empresa_master")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Relatórios da Empresa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // =====================================================================
        // TAB ROW DO PAINEL DA EMPRESA (Validade removida conforme solicitação)
        // =====================================================================
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceContainer,
            contentColor = Color(0xFFA855F7),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFFA855F7),
                    height = 3.dp
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Conferências Semanais", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                selectedContentColor = Color.White,
                unselectedContentColor = DarkTextSecondary
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Equipe (${usuariosLoja.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                selectedContentColor = Color.White,
                unselectedContentColor = DarkTextSecondary
            )

            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Feedbacks da Loja", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (feedbacksPendentesMaster > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$feedbacksPendentesMaster",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                selectedContentColor = Color.White,
                unselectedContentColor = DarkTextSecondary
            )
        }

        // =====================================================================
        // CONTEÚDO DAS ABAS
        // =====================================================================
        when (selectedTab) {
            // =================================================================
            // 0. DESEMPENHO DAS CONFERÊNCIAS SEMANAIS DOS FUNCIONÁRIOS
            // =================================================================
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Resumo Semanal da Equipe
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Desempenho das Conferências Semanais",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Auditorias de presença e conferências realizadas nos últimos 7 dias",
                                    fontSize = 11.sp,
                                    color = DarkTextSecondary
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricPill(label = "Itens Auditados", value = "$totalItensAuditadosSemana", color = Color(0xFF38BDF8))
                                    MetricPill(label = "Conferências", value = "$totalConferenciasSemana", color = Color(0xFFA855F7))
                                    MetricPill(label = "Equipe Ativa", value = "$funcionariosAtivosSemana/${usuariosLoja.size}", color = EmeraldSafe)
                                }
                            }
                        }
                    }

                    // Título da Tabela de Funcionários
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Produtividade Individual da Equipe:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Semana Atual",
                                fontSize = 11.sp,
                                color = Color(0xFFC084FC),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (desempenhoFuncionarios.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Nenhum funcionário cadastrado nesta empresa.", color = DarkTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(desempenhoFuncionarios, key = { it.usuario.matricula }) { func ->
                            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                            val dataFormatada = remember(func.ultimaConferenciaMs) {
                                func.ultimaConferenciaMs?.let { dateFormat.format(Date(it)) } ?: "Nenhuma conferência"
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(
                                    1.dp,
                                    if (func.realizouConferenciaSemanal) EmeraldSafe.copy(alpha = 0.4f) else DarkBorder
                                ),
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
                                                color = if (func.realizouConferenciaSemanal) EmeraldSafe.copy(alpha = 0.2f) else Color(0xFF334155),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (func.realizouConferenciaSemanal) Icons.Default.CheckCircle else Icons.Default.FactCheck,
                                                        contentDescription = null,
                                                        tint = if (func.realizouConferenciaSemanal) EmeraldSafe else DarkTextSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = func.usuario.nome,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Matrícula: ${func.usuario.matricula}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF93C5FD)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("•", fontSize = 10.sp, color = DarkTextSecondary)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    val cargoDisplay = CargoManager.getCargoCustomizado(context, func.usuario.matricula) ?: func.usuario.cargo
                                                    Text(
                                                        text = cargoDisplay.uppercase(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFC084FC)
                                                    )
                                                }
                                            }
                                        }

                                        // Badge de Status Semanal
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (func.realizouConferenciaSemanal) EmeraldSafe.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, if (func.realizouConferenciaSemanal) EmeraldSafe else Color(0xFFF59E0B))
                                        ) {
                                            Text(
                                                text = if (func.realizouConferenciaSemanal) "CONFERIDO" else "PENDENTE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (func.realizouConferenciaSemanal) EmeraldSafe else Color(0xFFFBBF24),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Indicadores de Desempenho do Funcionário
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = "Itens Conferidos", fontSize = 10.sp, color = DarkTextSecondary)
                                                Text(
                                                    text = "${func.itensAuditadosSemana} itens",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            Column {
                                                Text(text = "Nível Semanal", fontSize = 10.sp, color = DarkTextSecondary)
                                                Text(
                                                    text = func.nivelDesempenho,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (func.nivelDesempenho) {
                                                        "Excelente" -> EmeraldSafe
                                                        "Bom" -> Color(0xFF38BDF8)
                                                        "Regular" -> Color(0xFFFBBF24)
                                                        else -> Color(0xFFF87171)
                                                    }
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(text = "Última Auditoria", fontSize = 10.sp, color = DarkTextSecondary)
                                                Text(
                                                    text = dataFormatada,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF93C5FD),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 1. GESTÃO DA EQUIPE DE FUNCIONÁRIOS DA EMPRESA
            // =================================================================
            1 -> {
                var searchQuery by remember { mutableStateOf("") }

                val funcionariosFiltrados = remember(usuariosLoja, searchQuery) {
                    if (searchQuery.isBlank()) usuariosLoja
                    else usuariosLoja.filter {
                        it.nome.contains(searchQuery, ignoreCase = true) ||
                        it.matricula.contains(searchQuery, ignoreCase = true) ||
                        it.cargo.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Barra de Busca
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Buscar funcionário por nome, matrícula ou cargo") },
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
                    }

                    if (isLoadingDados) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFA855F7))
                            }
                        }
                    } else if (funcionariosFiltrados.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Nenhum funcionário encontrado.", color = DarkTextSecondary)
                                }
                            }
                        }
                    } else {
                        items(funcionariosFiltrados, key = { it.matricula }) { func ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(1.dp, if (func.ativo) DarkBorder else RedExpressive.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (func.cargo.equals("master", ignoreCase = true)) Color(0xFF581C87) else Color(0xFF1E3A8A),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (func.cargo.equals("master", ignoreCase = true)) Icons.Default.Star else Icons.Default.Group,
                                                        contentDescription = null,
                                                        tint = if (func.cargo.equals("master", ignoreCase = true)) Color(0xFFC084FC) else Color(0xFF93C5FD),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = func.nome,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF581C87).copy(alpha = 0.5f)
                                                    ) {
                                                        Text(
                                                            text = func.cargo.uppercase(),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFE9D5FF),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "Matrícula: ${func.matricula} • Setor: ${func.setor ?: "Geral"}",
                                                    fontSize = 11.sp,
                                                    color = DarkTextSecondary
                                                )
                                            }
                                        }

                                        // Status
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (func.ativo) EmeraldSafe.copy(alpha = 0.2f) else RedExpressive.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (func.ativo) "Ativo" else "Inativo",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (func.ativo) EmeraldSafe else RedExpressive,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Ações do Master para o Funcionário
                                    if (isMaster) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    val novoStatus = !func.ativo
                                                    coroutineScope.launch {
                                                        try {
                                                            supabase.from("usuarios")
                                                                .update(mapOf("ativo" to novoStatus)) { eq("matricula", func.matricula) }
                                                            usuariosLoja = usuariosLoja.map {
                                                                if (it.matricula == func.matricula) it.copy(ativo = novoStatus) else it
                                                            }
                                                            Toast.makeText(context, "Status alterado.", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = if (func.ativo) "Desativar" else "Ativar",
                                                    color = if (func.ativo) RedExpressive else EmeraldSafe,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            IconButton(
                                                onClick = { usuarioParaExcluir = func },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RedExpressive, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 2. FEEDBACKS E SUGESTÕES DA LOJA (EXCLUSIVO DO MASTER)
            // =================================================================
            2 -> {
                var filtroFeedback by remember { mutableStateOf("TODOS") }

                val feedbacksFiltrados = remember(feedbacksLoja, filtroFeedback) {
                    when (filtroFeedback) {
                        "PENDENTES" -> feedbacksLoja.filter { it.status.equals("PENDENTE", ignoreCase = true) }
                        "RESOLVIDOS" -> feedbacksLoja.filter { it.status.equals("RESOLVIDO", ignoreCase = true) }
                        else -> feedbacksLoja
                    }
                }

                val mediaEstrelas = remember(feedbacksLoja) {
                    if (feedbacksLoja.isEmpty()) 5.0
                    else feedbacksLoja.map { it.estrelas }.average()
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card Resumo de Feedbacks da Loja
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.5.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF78350F).copy(alpha = 0.4f),
                                            border = BorderStroke(1.dp, Color(0xFFD97706)),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Feedbacks & Sugestões da Loja",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Canal anônimo de melhorias para a equipe de $masterLojaNome",
                                                fontSize = 11.sp,
                                                color = DarkTextSecondary
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { showFeedbacksDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Gerenciar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricPill(label = "Total Recebidos", value = "${feedbacksLoja.size}", color = Color(0xFFFBBF24))
                                    MetricPill(label = "Pendentes", value = "$feedbacksPendentesMaster", color = if (feedbacksPendentesMaster > 0) Color(0xFFEF4444) else EmeraldSafe)
                                    MetricPill(label = "Avaliação Média", value = String.format(Locale.US, "%.1f ★", mediaEstrelas), color = Color(0xFFF59E0B))
                                }
                            }
                        }
                    }

                    // Filtros
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("TODOS", "PENDENTES", "RESOLVIDOS").forEach { f ->
                                val sel = filtroFeedback == f
                                FilterChip(
                                    selected = sel,
                                    onClick = { filtroFeedback = f },
                                    label = { Text(f, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFD97706),
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkSurfaceContainer,
                                        labelColor = DarkTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    if (feedbacksFiltrados.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                                    Text("Nenhum feedback encontrado nesta categoria.", color = DarkTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(feedbacksFiltrados, key = { it.id }) { fb ->
                            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                            val dataFormatada = remember(fb.timestamp) { dateFormat.format(Date(fb.timestamp)) }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(
                                    1.dp,
                                    if (fb.status.equals("PENDENTE", ignoreCase = true)) Color(0xFFF59E0B).copy(alpha = 0.5f) else DarkBorder
                                ),
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
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF581C87).copy(alpha = 0.4f),
                                                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = "Cargo: ${fb.usuario_cargo.uppercase()} (Anônimo)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE9D5FF),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = DarkSurfaceContainerHigh
                                            ) {
                                                Text(
                                                    text = fb.categoria,
                                                    fontSize = 10.sp,
                                                    color = DarkTextSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        // Status
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (fb.status.equals("RESOLVIDO", ignoreCase = true)) EmeraldSafe.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = fb.status.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (fb.status.equals("RESOLVIDO", ignoreCase = true)) EmeraldSafe else Color(0xFFFBBF24),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Estrelas
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(5) { idx ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (idx < fb.estrelas) Color(0xFFFBBF24) else Color(0xFF475569),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = dataFormatada, fontSize = 10.sp, color = DarkTextSecondary)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "\"${fb.mensagem}\"",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    if (!fb.resposta_adm.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF065F46).copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "✓ Resposta da Gestão Master:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldSafe
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = fb.resposta_adm ?: "",
                                                    fontSize = 11.sp,
                                                    color = DarkTextPrimary
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { showFeedbacksDialog = true }) {
                                                Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFC084FC))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Responder no Modal", fontSize = 11.sp, color = Color(0xFFC084FC))
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
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(text = label, fontSize = 10.sp, color = DarkTextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogAdicionarFuncionarioMaster(
    masterLojaId: String,
    masterLojaNome: String,
    onDismiss: () -> Unit,
    onFuncionarioAdicionado: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }

    val cargosDisponiveis = remember {
        CargoManager.getCargos(context, masterLojaId)
    }

    var novaMatricula by remember { mutableStateOf("") }
    var novoNome by remember { mutableStateOf("") }
    var cargoSelecionado by remember { mutableStateOf(cargosDisponiveis.firstOrNull()?.nome ?: "Operador Geral") }
    val cargoObj = cargosDisponiveis.find { it.nome == cargoSelecionado }
    var setorAtribuido by remember(cargoSelecionado) { mutableStateOf(cargoObj?.setor ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var erroValidacao by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFF581C87).copy(alpha = 0.4f),
                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Cadastrar Novo Funcionário",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vínculo Automático com a Empresa:",
                    fontSize = 11.sp,
                    color = DarkTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF581C87).copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = masterLojaNome,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE9D5FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = novaMatricula,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            novaMatricula = input
                        }
                    },
                    label = { Text("Matrícula Funcional (6 dígitos)") },
                    placeholder = { Text("Ex: 102030") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_nova_matricula_empresa")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = novoNome,
                    onValueChange = { novoNome = it },
                    label = { Text("Nome Completo do Funcionário") },
                    placeholder = { Text("Ex: Mariana Souza") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_novo_nome_empresa")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Cargo / Função na Loja:", fontSize = 11.sp, color = DarkTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(cargosDisponiveis) { c ->
                        FilterChip(
                            selected = cargoSelecionado == c.nome,
                            onClick = {
                                cargoSelecionado = c.nome
                                setorAtribuido = c.setor ?: ""
                            },
                            label = { Text(c.nome, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7E22CE),
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceContainerHigh,
                                labelColor = DarkTextSecondary
                            )
                        )
                    }
                }

                if (erroValidacao != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = erroValidacao!!, color = RedExpressive, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (novaMatricula.length != 6) {
                        erroValidacao = "A matrícula deve ter exatamente 6 dígitos numéricos."
                        return@Button
                    }
                    if (novoNome.isBlank()) {
                        erroValidacao = "Informe o nome completo do colaborador."
                        return@Button
                    }

                    isSaving = true
                    erroValidacao = null
                    coroutineScope.launch {
                        try {
                            val setorFinal = setorAtribuido.trim().ifBlank { null }
                            val cargoNoBanco = CargoManager.normalizarCargoParaBanco(cargoSelecionado)
                            CargoManager.salvarAtribuicaoUsuario(
                                context = context,
                                matricula = novaMatricula,
                                cargoNome = cargoSelecionado,
                                setor = setorFinal
                            )
                            val cleanNome = novoNome.trim()
                            val novoUsuario = NovoUsuario(
                                matricula = novaMatricula,
                                nome = cleanNome,
                                cargo = cargoNoBanco,
                                loja_id = masterLojaId, // Vínculo estrito com a empresa do Master
                                ativo = true,
                                setor = setorFinal
                            )
                            supabase.from("usuarios").insert(novoUsuario.encrypted())
                            LogManager.recordLog(
                                usuarioMatricula = SessionHolder.currentUser?.matricula ?: "MASTER",
                                usuarioNome = SessionHolder.currentUser?.nome ?: "Master",
                                lojaId = masterLojaId,
                                acao = "CADASTRO_FUNCIONARIO",
                                detalhes = "Cadastrou funcionário '$cleanNome' (Matrícula: $novaMatricula, Função: $cargoSelecionado) com vínculo automático à empresa"
                            )
                            Toast.makeText(context, "Funcionário cadastrado e vinculado à empresa!", Toast.LENGTH_SHORT).show()
                            onFuncionarioAdicionado()
                        } catch (e: Exception) {
                            erroValidacao = "Erro ao cadastrar: ${e.message}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                modifier = Modifier.testTag("btn_salvar_novo_funcionario_empresa")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text("Cadastrar Funcionário", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar", color = DarkTextSecondary)
            }
        },
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(20.dp)
    )
}
