package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import com.example.data.TelemetryManager
import com.example.ui.components.EditarProdutoDialog
import com.example.ui.components.EnviarFeedbackDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StockLockManager
import com.example.data.supabase.Loja
import com.example.data.supabase.Produto
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import com.example.ui.components.AddProdutoDialog
import com.example.ui.components.AuditoriaHubScreen
import com.example.ui.components.DetalhesProdutoDialog
import com.example.ui.components.GerenciarSetoresDialog
import com.example.ui.components.GestaoEquipeScreen
import com.example.ui.components.MasterUsuariosTab
import com.example.ui.components.MonitoramentoAcoesDialog
import com.example.ui.components.PlanilhaImportDialog
import com.example.data.RebaixaManager
import com.example.data.StatusRebaixa
import com.example.data.supabase.LogManager
import com.example.util.CryptoMigrationManager
import com.example.util.CryptoUtils
import com.example.util.decrypted
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import com.example.util.BarcodeScannerHelper
import com.example.util.SoundFeedbackHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class OperadorMenu(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    MONITORAMENTO("Monitoramento", Icons.Default.HourglassBottom),
    ESTOQUE("Estoque", Icons.Default.Inventory2),
    AUDITORIA("Auditoria", Icons.Default.FactCheck),
    EQUIPE("Equipe", Icons.Default.Group)
}

enum class ExpiryStatus(val title: String, val color: Color, val icon: ImageVector) {
    VENCIDO("Vencido", Color(0xFFEF4444), Icons.Default.Error),
    RETIRAR_AREA("Retirar da Área", Color(0xFFF97316), Icons.Default.RemoveShoppingCart),
    SOLICITAR_REBAIXA("Solicitar Rebaixa", Color(0xFFEAB308), Icons.Default.PriceCheck),
    NO_PRAZO("No Prazo", Color(0xFF10B981), Icons.Default.CheckCircle)
}

fun calculateDaysRemaining(dataVencimento: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val expiry = sdf.parse(dataVencimento.trim()) ?: return Long.MAX_VALUE
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calExpiry = Calendar.getInstance().apply {
            time = expiry
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = calExpiry.timeInMillis - calToday.timeInMillis
        TimeUnit.MILLISECONDS.toDays(diffMillis)
    } catch (e: Exception) {
        Long.MAX_VALUE
    }
}

fun getExpiryStatus(days: Long): ExpiryStatus {
    return when {
        days < 0 -> ExpiryStatus.VENCIDO
        days <= 1 -> ExpiryStatus.RETIRAR_AREA
        days <= 15 -> ExpiryStatus.SOLICITAR_REBAIXA
        else -> ExpiryStatus.NO_PRAZO
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val supabase = remember { SupabaseClient.instance }

    val currentUser = SessionHolder.currentUser ?: return
    var currentLoja by remember { mutableStateOf(SessionHolder.currentLoja) }

    var currentTab by remember { mutableStateOf(OperadorMenu.DASHBOARD) }
    var produtos by remember { mutableStateOf<List<Produto>>(emptyList()) }
    var isLoadingProdutos by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    val userRestrictedSector = remember(currentUser) { com.example.data.CargoManager.resolveUserSector(currentUser) }
    var selectedSetor by remember { mutableStateOf(userRestrictedSector ?: "Todos") }
    var monitoramentoFiltro by remember { mutableStateOf("Alerta") } // "Alerta", "Retirar", "Rebaixa", "Vencidos", "Todos"

    var isAddDialogOpen by remember { mutableStateOf(false) }
    var showCreatedSuccessDialog by remember { mutableStateOf<Produto?>(null) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Carrega dados da loja vinculada se ainda não estiver em memória
    fun loadLojaDetails() {
        if (currentLoja != null) return
        coroutineScope.launch {
            try {
                withTimeout(6000) {
                    val loja = supabase.from("lojas")
                        .select { eq("id", currentUser.loja_id) }
                        .decodeSingleOrNull<Loja>()
                    val decLoja = loja?.decrypted()
                    currentLoja = decLoja
                    SessionHolder.currentLoja = decLoja
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    // Leitura do estoque: filtrar sempre por loja_id.eq(usuarioAtual.loja_id)
    fun loadProdutos() {
        isLoadingProdutos = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    val result = supabase.from("produtos")
                        .select { eq("loja_id", currentUser.loja_id) }
                        .decodeList<Produto>()
                    produtos = result.map { it.decrypted() }

                    // Verifica se existem dados sem criptografia para migração automática
                    val hasUnencrypted = result.any { !CryptoUtils.isEncrypted(it.nome) || !CryptoUtils.isEncrypted(it.codigo_barras) }
                    if (hasUnencrypted) {
                        CryptoMigrationManager.migrarTodosDadosNaoCriptografados(currentUser.loja_id)
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Falha ao carregar estoque: ${e.localizedMessage ?: "Erro"}")
            } finally {
                isLoadingProdutos = false
            }
        }
    }

    LaunchedEffect(currentUser) {
        loadLojaDetails()
        loadProdutos()
        // Inicia monitoramento em tempo real do bloqueio de produtos
        StockLockManager.startLockMonitoring(currentUser.loja_id, this)
        // Heartbeat periódico para telemetria em tempo real
        while (true) {
            try {
                TelemetryManager.recordHeartbeat(context, currentUser, currentLoja)
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(45000L)
        }
    }

    // Cálculos de métricas da dashboard
    val produtosWithDays = remember(produtos) {
        produtos.map { it to calculateDaysRemaining(it.data_vencimento) }
    }

    val vencidos = remember(produtosWithDays) {
        produtosWithDays.filter { it.second < 0 }
    }
    val retirarArea = remember(produtosWithDays) {
        produtosWithDays.filter { it.second in 0..1 }
    }
    val solicitarRebaixa = remember(produtosWithDays) {
        produtosWithDays.filter { it.second in 2..15 }
    }
    val aVencer15Dias = remember(produtosWithDays) {
        produtosWithDays.filter { it.second in 0..15 }
    }
    val noPrazo = remember(produtosWithDays) {
        produtosWithDays.filter { it.second > 15 }
    }

    val isMaster = SessionHolder.isMaster || com.example.data.CargoManager.isPerfilMaster(currentUser, context)
    val isAdm = SessionHolder.isAdm
    val roleAccentColor = when {
        isAdm -> Color(0xFFEAB308)     // Amarelo para ADM
        isMaster -> Color(0xFFA855F7)  // Roxo para Master
        else -> BlueExpressive         // Azul para Comum/Operador
    }

    var selectedProdutoForDetails by remember { mutableStateOf<Produto?>(null) }
    var isPlanilhaImportOpen by remember { mutableStateOf(false) }
    var showGerenciarSetores by remember { mutableStateOf(false) }

    fun deleteProduto(produto: Produto) {
        if (!com.example.data.CargoManager.podeRealizarAcaoNoSetor(currentUser, produto.setor)) {
            val setorPermitido = com.example.data.CargoManager.resolveUserSector(currentUser)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("⛔ Acesso negado: Seu usuário pode alterar produtos apenas do setor '$setorPermitido'.")
            }
            return
        }
        coroutineScope.launch {
            try {
                supabase.from("produtos").delete { eq("id", produto.id) }
                produtos = produtos.filter { it.id != produto.id }
                LogManager.recordLog(
                    usuarioMatricula = currentUser.matricula,
                    usuarioNome = currentUser.nome,
                    lojaId = currentUser.loja_id,
                    acao = "EXCLUSAO_PRODUTO",
                    detalhes = "Excluiu produto '${produto.nome}' (EAN: ${produto.codigo_barras}) do estoque"
                )
                snackbarHostState.showSnackbar("Produto \"${produto.nome}\" excluído do estoque.")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Erro ao excluir: ${e.localizedMessage ?: "Erro"}")
            }
        }
    }

    val storeBorderColor = try {
        Color(android.graphics.Color.parseColor(currentLoja?.cor_borda ?: "#2563EB"))
    } catch (e: Exception) {
        roleAccentColor
    }

    val navItems = remember(isMaster, isAdm) {
        buildList {
            add(OperadorMenu.DASHBOARD)
            add(OperadorMenu.MONITORAMENTO)
            add(OperadorMenu.ESTOQUE)
            add(OperadorMenu.AUDITORIA)
            if (isMaster || isAdm) {
                add(OperadorMenu.EQUIPE)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = roleAccentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.5.dp, roleAccentColor),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = roleAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentLoja?.nome_loja ?: "Supermercado",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = roleAccentColor.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, roleAccentColor.copy(alpha = 0.7f))
                                ) {
                                    Text(
                                        text = currentUser.cargo.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = roleAccentColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${currentUser.nome} • Matrícula: ${currentUser.matricula}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    if (isMaster || isAdm) {
                        IconButton(
                            onClick = { currentTab = OperadorMenu.EQUIPE },
                            modifier = Modifier.testTag("btn_gestao_equipe_topbar")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Gestão de Equipe e Logs",
                                tint = roleAccentColor
                            )
                        }
                    }
                    if (isMaster) {
                        IconButton(
                            onClick = { showGerenciarSetores = true },
                            modifier = Modifier.testTag("btn_gerenciar_setores_topbar")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Gerenciar Setores",
                                tint = Color(0xFFC084FC)
                            )
                        }
                    }
                    // Ícone de Estrela: Enviar Feedback ao ADM / Loja (Apenas para colaboradores abaixo de Master)
                    if (!isMaster) {
                        IconButton(
                            onClick = { showFeedbackDialog = true },
                            modifier = Modifier.testTag("btn_feedback_star")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Enviar Feedback",
                                tint = Color(0xFFFBBF24)
                            )
                        }
                    }
                    IconButton(
                        onClick = { loadProdutos() },
                        modifier = Modifier.testTag("btn_refresh_produtos")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = DarkTextSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            LogManager.recordLog(
                                usuarioMatricula = currentUser.matricula,
                                usuarioNome = currentUser.nome,
                                lojaId = currentUser.loja_id,
                                acao = "LOGOUT",
                                detalhes = "Usuário '${currentUser.nome}' encerrou a sessão no dispositivo"
                            )
                            SessionHolder.clearSession()
                            onLogout()
                        },
                        modifier = Modifier.testTag("btn_logout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sair",
                            tint = RedExpressive
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurfaceContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurfaceContainer,
                tonalElevation = 8.dp
            ) {
                navItems.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = roleAccentColor,
                            unselectedIconColor = DarkTextSecondary,
                            unselectedTextColor = DarkTextSecondary,
                            indicatorColor = roleAccentColor
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == OperadorMenu.ESTOQUE || currentTab == OperadorMenu.DASHBOARD) {
                ExtendedFloatingActionButton(
                    onClick = { isAddDialogOpen = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Novo Produto", fontWeight = FontWeight.Bold) },
                    containerColor = roleAccentColor,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_produto")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkSurface
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                OperadorMenu.DASHBOARD -> DashboardContent(
                    lojaNome = currentLoja?.nome_loja ?: "Loja",
                    produtos = produtos,
                    currentUser = currentUser,
                    onNavigateToMonitoramento = { filtro, setor ->
                        monitoramentoFiltro = filtro
                        if (setor != null) selectedSetor = setor
                        currentTab = OperadorMenu.MONITORAMENTO
                    },
                    onNavigateToEstoque = { setor ->
                        if (setor != null) selectedSetor = setor
                        currentTab = OperadorMenu.ESTOQUE
                    },
                    onAddProdutoClick = { isAddDialogOpen = true }
                )

                OperadorMenu.MONITORAMENTO -> MonitoramentoContent(
                    produtosWithDays = produtosWithDays,
                    filtro = monitoramentoFiltro,
                    onFiltroChange = { monitoramentoFiltro = it },
                    isLoading = isLoadingProdutos,
                    onRefresh = { loadProdutos() }
                )

                OperadorMenu.ESTOQUE -> EstoqueContent(
                    produtos = produtos,
                    isLoading = isLoadingProdutos,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedSetor = selectedSetor,
                    onSetorSelected = { selectedSetor = it },
                    isMaster = isMaster,
                    onOpenPlanilhaImport = { isPlanilhaImportOpen = true },
                    onSelectProduto = { produto -> selectedProdutoForDetails = produto },
                    onDeleteProduto = { produto -> deleteProduto(produto) },
                    onProdutoUpdated = { updated ->
                        produtos = produtos.map { if (it.id == updated.id) updated else it }
                    },
                    onScanBarcode = {
                        BarcodeScannerHelper.startScan(
                            context = context,
                            onSuccess = { scannedBarcode ->
                                searchQuery = scannedBarcode
                            }
                        )
                    },
                    onRefresh = { loadProdutos() }
                )

                OperadorMenu.AUDITORIA -> AuditoriaHubScreen(
                    produtos = produtos,
                    onRefreshProdutos = { loadProdutos() }
                )

                OperadorMenu.EQUIPE -> GestaoEquipeScreen(
                    lojaIdOverride = currentUser.loja_id,
                    lojaNomeOverride = currentLoja?.nome_loja
                )
            }
        }
    }

    // Detalhes do Produto Selecionado
    if (selectedProdutoForDetails != null) {
        DetalhesProdutoDialog(
            produto = selectedProdutoForDetails!!,
            onDismiss = { selectedProdutoForDetails = null },
            onDelete = { produto ->
                deleteProduto(produto)
                selectedProdutoForDetails = null
            },
            onProdutoUpdated = { updated ->
                produtos = produtos.map { if (it.id == updated.id) updated else it }
                selectedProdutoForDetails = updated
            }
        )
    }

    // Modal de Feedback para o Administrador (Estrela)
    if (showFeedbackDialog) {
        EnviarFeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onFeedbackEnviado = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Obrigado! Seu feedback foi enviado ao Administrador.")
                }
            }
        )
    }

    // Modal de Gerenciamento de Setores (Master)
    if (showGerenciarSetores) {
        GerenciarSetoresDialog(
            onDismiss = { showGerenciarSetores = false },
            onSetoresAtualizados = { loadProdutos() }
        )
    }

    // Modal de Importação via Planilha (Exclusivo Master)
    if (isPlanilhaImportOpen) {
        PlanilhaImportDialog(
            lojaId = currentUser.loja_id,
            lojaNome = currentLoja?.nome_loja ?: "Loja Vinculada",
            onDismiss = { isPlanilhaImportOpen = false },
            onImportSuccess = { count ->
                loadProdutos()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("$count produtos importados com sucesso!")
                }
            }
        )
    }

    // Modal de Cadastro de Produto
    if (isAddDialogOpen) {
        AddProdutoDialog(
            onDismiss = { isAddDialogOpen = false },
            onProductAdded = { novoProduto ->
                showCreatedSuccessDialog = novoProduto
                loadProdutos()
            }
        )
    }

    // Diálogo de confirmação com UUID do produto
    if (showCreatedSuccessDialog != null) {
        val produto = showCreatedSuccessDialog!!
        AlertDialog(
            onDismissRequest = { showCreatedSuccessDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSafe,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Produto Cadastrado!", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        text = "O produto foi salvo com sucesso no banco de dados Supabase.",
                        color = DarkTextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Nome: ${produto.nome}", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Setor: ${produto.setor} • Qtd: ${produto.quantidade}", color = DarkTextSecondary, fontSize = 12.sp)
                            Text(text = "Vencimento: ${produto.data_vencimento}", color = Color(0xFFFDE68A), fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCreatedSuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// =============================================================================
// 1. DASHBOARD DO OPERADOR / MASTER (VALIDADE E ESTOQUE)
// =============================================================================
@Composable
fun DashboardContent(
    lojaNome: String,
    produtos: List<Produto>,
    currentUser: Usuario,
    onNavigateToMonitoramento: (String, String?) -> Unit,
    onNavigateToEstoque: (String?) -> Unit,
    onAddProdutoClick: () -> Unit
) {
    val context = LocalContext.current
    val userRestrictedSector = remember(currentUser) { com.example.data.CargoManager.resolveUserSector(currentUser) }
    var selectedSectorFilter by remember(userRestrictedSector) { mutableStateOf(userRestrictedSector ?: "Todos") }

    // Lista de setores disponíveis a partir do catálogo ou lista padrão
    val availableSectors = remember(produtos) {
        val distinct = produtos.map { it.setor.trim() }.filter { it.isNotBlank() }.distinct()
        val defaultList = listOf("Laticínios", "Mercearia", "Hortifrúti", "Padaria", "Açougue", "Bebidas", "Fiambreria", "Congelados", "Higiene & Limpeza")
        (distinct + defaultList).distinct().sorted()
    }

    // Filtrar produtos de acordo com o setor selecionado
    val filteredProdutos = remember(produtos, selectedSectorFilter) {
        if (selectedSectorFilter == "Todos") produtos
        else produtos.filter { it.setor.equals(selectedSectorFilter, ignoreCase = true) }
    }

    val produtosWithDays = remember(filteredProdutos) {
        filteredProdutos.map { it to calculateDaysRemaining(it.data_vencimento) }
    }

    val vencidos = remember(produtosWithDays) { produtosWithDays.filter { it.second < 0 } }
    val retirarArea = remember(produtosWithDays) { produtosWithDays.filter { it.second in 0..1 } }
    val solicitarRebaixa = remember(produtosWithDays) { produtosWithDays.filter { it.second in 2..15 } }
    val aVencer15Dias = remember(produtosWithDays) { produtosWithDays.filter { it.second in 0..15 } }
    val noPrazo = remember(produtosWithDays) { produtosWithDays.filter { it.second > 15 } }

    val totalProdutos = filteredProdutos.size
    val vencidosCount = vencidos.size
    val retirarAreaCount = retirarArea.size
    val solicitarRebaixaCount = solicitarRebaixa.size
    val aVencer15DiasCount = aVencer15Dias.size
    val noPrazoCount = noPrazo.size

    val isMaster = SessionHolder.isMaster || com.example.data.CargoManager.isPerfilMaster(currentUser, context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Banner de Alerta Crítico se houver produtos vencendo ou vencidos
        val totalUrgente = vencidosCount + retirarAreaCount
        if (totalUrgente > 0) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                border = BorderStroke(1.5.dp, RedExpressive),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        onNavigateToMonitoramento(
                            "Retirar",
                            if (selectedSectorFilter != "Todos") selectedSectorFilter else null
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RedExpressive,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ação Imediata Necessária!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "$totalUrgente produto(s) requerem retirada imediata da área de vendas ou já estão vencidos.",
                            color = DarkTextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RedExpressive,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Título da Dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Resumo de Validades",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Controle dinâmico de perecíveis • $lojaNome",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceContainerHigh,
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Text(
                    text = "$totalProdutos Itens",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF93C5FD),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Badge / Seletor de Setor (Isolamento por Setor)
        if (userRestrictedSector != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFA855F7).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SETOR DE ATUAÇÃO VINCULADO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE9D5FF)
                        )
                        Text(
                            text = "Você visualiza e opera no setor: $userRestrictedSector",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            // Master / Operador Geral: seletor rápido de setores
            Text(
                text = "Filtrar Indicadores por Setor:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DarkTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSectorFilter == "Todos",
                        onClick = { selectedSectorFilter = "Todos" },
                        label = { Text("Todos os Setores (${produtos.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueExpressive,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceContainerHigh,
                            labelColor = DarkTextSecondary
                        )
                    )
                }
                items(availableSectors) { setor ->
                    val count = produtos.count { it.setor.equals(setor, ignoreCase = true) }
                    FilterChip(
                        selected = selectedSectorFilter == setor,
                        onClick = { selectedSectorFilter = setor },
                        label = { Text("$setor ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF9333EA),
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceContainerHigh,
                            labelColor = DarkTextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Card de Destaque: A Vencer (Próximos 15 dias)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onNavigateToMonitoramento(
                        "Rebaixa",
                        if (selectedSectorFilter != "Todos") selectedSectorFilter else null
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "A Vencer (Próximos 15 dias)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (selectedSectorFilter == "Todos") "Perecíveis para monitoramento preventivo" else "Em monitoramento no setor $selectedSectorFilter",
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Text(
                        text = "$aVencer15DiasCount",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid 2x2 com os 4 Cards de Resumo Solicitados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Vencidos
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Vencidos",
                count = vencidosCount,
                subtitle = "Prazo expirado",
                color = Color(0xFFEF4444),
                icon = Icons.Default.Error,
                onClick = {
                    onNavigateToMonitoramento(
                        "Vencidos",
                        if (selectedSectorFilter != "Todos") selectedSectorFilter else null
                    )
                }
            )

            // Card 2: Retirar da Área (≤ 1 dia)
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Retirar da Área",
                count = retirarAreaCount,
                subtitle = "Vence em até 24h",
                color = Color(0xFFF97316),
                icon = Icons.Default.RemoveShoppingCart,
                onClick = {
                    onNavigateToMonitoramento(
                        "Retirar",
                        if (selectedSectorFilter != "Todos") selectedSectorFilter else null
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: Solicitar Rebaixa (≤ 15 dias)
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Solicitar Rebaixa",
                count = solicitarRebaixaCount,
                subtitle = "Perecíveis ≤ 15 dias",
                color = Color(0xFFEAB308),
                icon = Icons.Default.PriceCheck,
                onClick = {
                    onNavigateToMonitoramento(
                        "Rebaixa",
                        if (selectedSectorFilter != "Todos") selectedSectorFilter else null
                    )
                }
            )

            // Card 4: No Prazo (> 15 dias)
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "No Prazo",
                count = noPrazoCount,
                subtitle = "Validade segura",
                color = Color(0xFF10B981),
                icon = Icons.Default.CheckCircle,
                onClick = {
                    onNavigateToMonitoramento(
                        "Todos",
                        if (selectedSectorFilter != "Todos") selectedSectorFilter else null
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Barra de Distribuição de Validade
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Distribuição do Estoque Perecível",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (totalProdutos > 0) {
                    val safeTotal = totalProdutos.toFloat()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        if (vencidosCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(vencidosCount / safeTotal)
                                    .background(Color(0xFFEF4444))
                            )
                        }
                        if (retirarAreaCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(retirarAreaCount / safeTotal)
                                    .background(Color(0xFFF97316))
                            )
                        }
                        if (solicitarRebaixaCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(solicitarRebaixaCount / safeTotal)
                                    .background(Color(0xFFEAB308))
                            )
                        }
                        if (noPrazoCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(noPrazoCount / safeTotal)
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem(color = Color(0xFFEF4444), label = "Vencido (${vencidosCount})")
                        LegendItem(color = Color(0xFFF97316), label = "Retirar (${retirarAreaCount})")
                        LegendItem(color = Color(0xFFEAB308), label = "Rebaixa (${solicitarRebaixaCount})")
                        LegendItem(color = Color(0xFF10B981), label = "No Prazo (${noPrazoCount})")
                    }
                } else {
                    Text(
                        text = "Nenhum produto cadastrado para este setor/loja.",
                        color = DarkTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Visão Geral de Setores (para Master ou Operadores que estão vendo 'Todos')
        if (selectedSectorFilter == "Todos" && produtos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visão por Setores",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Toque para filtrar",
                            fontSize = 11.sp,
                            color = DarkTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val sectorsWithProducts = produtos.groupBy { it.setor.ifBlank { "Sem Setor" } }
                    sectorsWithProducts.forEach { (sectorName, prods) ->
                        val sectorCriticos = prods.count {
                            val days = calculateDaysRemaining(it.data_vencimento)
                            days <= 15
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (userRestrictedSector == null) {
                                        selectedSectorFilter = sectorName
                                    } else {
                                        onNavigateToEstoque(sectorName)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = if (sectorCriticos > 0) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sectorName,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${prods.size} produtos cadastrados",
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary
                                    )
                                }

                                if (sectorCriticos > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "$sectorCriticos em risco",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = DarkTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ações Rápidas
        Text(
            text = "Ações Operacionais",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    onNavigateToMonitoramento(
                        "Alerta",
                        if (selectedSectorFilter != "Todos") selectedSectorFilter else null
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFFEAB308))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFEAB308))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Monitorar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Button(
                onClick = {
                    onNavigateToEstoque(if (selectedSectorFilter != "Todos") selectedSectorFilter else null)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, BlueExpressive)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = BlueExpressive)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ver Estoque", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Button(
                onClick = onAddProdutoClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, EmeraldSafe)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldSafe)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cadastrar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
            .clickable { onClick() }
            .testTag("metric_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "$count",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = DarkTextSecondary
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 9.sp, color = DarkTextSecondary)
    }
}

// =============================================================================
// 2. MONITORAMENTO DE VALIDADES
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoramentoContent(
    produtosWithDays: List<Pair<Produto, Long>>,
    filtro: String,
    onFiltroChange: (String) -> Unit,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var statusRefreshTrigger by remember { mutableStateOf(0) }
    var produtoSelecionadoParaAcoes by remember { mutableStateOf<Produto?>(null) }

    // Animação de pulso roxo para itens vencidos que NÃO foram retirados de área
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_purple")
    val purplePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val filteredList = remember(produtosWithDays, filtro, statusRefreshTrigger) {
        val list = when (filtro) {
            "Alerta" -> produtosWithDays.filter { it.second <= 15 }
            "Retirar" -> produtosWithDays.filter { it.second in 0..1 }
            "Rebaixa" -> produtosWithDays.filter { it.second in 2..15 }
            "Vencidos" -> produtosWithDays.filter { it.second < 0 }
            else -> produtosWithDays
        }
        // PRIORIDADE MÁXIMA: Produtos vencidos que NÃO foram retirados de área ficam no topo absoluto da lista!
        list.sortedWith(
            compareByDescending<Pair<Produto, Long>> { (prod, days) ->
                val info = RebaixaManager.getInfo(context, prod.id)
                RebaixaManager.isCriticoVencidoNaoRetirado(days, info.retiradoDeArea)
            }.thenBy { it.second }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Monitoramento de Validades",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Selecione qualquer produto para solicitar rebaixa, confirmar rebaixa ou retirar de área.",
            style = MaterialTheme.typography.bodySmall,
            color = DarkTextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Chips de Filtro Rápido
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Alerta" to "Com Alerta (≤ 15d)",
                "Retirar" to "Retirar da Área (≤ 1d)",
                "Rebaixa" to "Solicitar Rebaixa (2-15d)",
                "Vencidos" to "Vencidos",
                "Todos" to "Todos os Itens"
            ).forEach { (key, label) ->
                val selected = filtro == key
                FilterChip(
                    selected = selected,
                    onClick = { onFiltroChange(key) },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueExpressive,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurfaceContainer,
                        labelColor = DarkTextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selected) BlueExpressive else DarkBorder)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueExpressive)
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSafe,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum produto nesta condição",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Não há produtos correspondentes ao filtro '$filtro'.",
                        color = DarkTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                items(filteredList, key = { it.first.id }) { (produto, days) ->
                    val status = getExpiryStatus(days)
                    val rebaixaInfo = remember(produto.id, statusRefreshTrigger) {
                        RebaixaManager.getInfo(context, produto.id)
                    }
                    val isCritico = remember(days, rebaixaInfo.retiradoDeArea) {
                        RebaixaManager.isCriticoVencidoNaoRetirado(days, rebaixaInfo.retiradoDeArea)
                    }
                    val precisaAlertaRetirar = remember(days, rebaixaInfo.retiradoDeArea) {
                        RebaixaManager.precisaAlertaRetirarArea1Dia(days, rebaixaInfo.retiradoDeArea)
                    }
                    val alertaRebaixaHoje = remember(produto.id, statusRefreshTrigger) {
                        RebaixaManager.deveAlertarRebaixaHoje(context, produto.id)
                    }
                    val alertaNovaRebaixa2Dias = remember(produto.id, statusRefreshTrigger) {
                        RebaixaManager.devePedirNovaRebaixa2Dias(context, produto.id)
                    }

                    // Card estilizado com pulsante roxo se for vencido e NÃO retirado de área
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCritico) {
                                Color(0xFF3B0764).copy(alpha = 0.35f + 0.15f * purplePulseAlpha)
                            } else {
                                DarkSurfaceContainer
                            }
                        ),
                        border = BorderStroke(
                            width = if (isCritico) 2.5.dp else 1.2.dp,
                            color = if (isCritico) {
                                Color(0xFFA855F7).copy(alpha = purplePulseAlpha)
                            } else {
                                status.color.copy(alpha = 0.6f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { produtoSelecionadoParaAcoes = produto }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Banner de prioridade máxima piscando em roxo se vencido e NÃO retirado de área
                            if (isCritico) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFA855F7).copy(alpha = 0.25f * purplePulseAlpha),
                                    border = BorderStroke(1.2.dp, Color(0xFFA855F7).copy(alpha = purplePulseAlpha)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFE9D5FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PRIORIDADE: VENCIDO EM ÁREA • RETIRADA IMEDIATA!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFF5F3FF)
                                        )
                                    }
                                }
                            } else if (precisaAlertaRetirar) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF97316).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFF97316)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveShoppingCart,
                                            contentDescription = null,
                                            tint = Color(0xFFFB923C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ALERTA: FALTA 1 DIA PARA VENCER • RETIRAR DA ÁREA!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFED7AA)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCritico) Color(0xFFA855F7).copy(alpha = 0.25f) else status.color.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (isCritico) Color(0xFFA855F7) else status.color)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isCritico) Icons.Default.Warning else status.icon,
                                            contentDescription = null,
                                            tint = if (isCritico) Color(0xFFE9D5FF) else status.color,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCritico) "VENCIDO EM ÁREA" else status.title.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isCritico) Color(0xFFE9D5FF) else status.color
                                        )
                                    }
                                }

                                Text(
                                    text = when {
                                        days < 0 -> "Vencido há ${-days} dia(s)"
                                        days == 0L -> "VENCE HOJE!"
                                        days == 1L -> "VENCE AMANHÃ!"
                                        else -> "Faltam $days dias"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCritico) Color(0xFFE9D5FF) else status.color
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = produto.nome,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Setor: ${produto.setor} • Qtd: ${produto.quantidade} un.",
                                    fontSize = 12.sp,
                                    color = DarkTextSecondary
                                )
                                Text(
                                    text = "Validade: ${produto.data_vencimento}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFDE68A)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EAN: ${produto.codigo_barras}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF93C5FD)
                                )
                                if (!produto.plu.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "PLU: ${produto.plu}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            // Badges de Status de Rebaixa e Retirada
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (rebaixaInfo.retiradoDeArea) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldSafe.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, EmeraldSafe)
                                    ) {
                                        Text(
                                            text = "✓ Retirado de Área",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSafe,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                when (rebaixaInfo.status) {
                                    StatusRebaixa.PENDENTE -> {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEAB308).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFFEAB308))
                                        ) {
                                            Text(
                                                text = "⏳ Rebaixa Pendente",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFDE047),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    StatusRebaixa.CONFIRMADA -> {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFF3B82F6))
                                        ) {
                                            Text(
                                                text = "✓ Rebaixa Aplicada",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF93C5FD),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    StatusRebaixa.NAO_APLICADA -> {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = RedExpressive.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, RedExpressive)
                                        ) {
                                            Text(
                                                text = "✗ Rebaixa Não Aplicada",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RedExpressive,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    StatusRebaixa.NENHUMA -> {}
                                }

                                if (alertaRebaixaHoje) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFD97706).copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, Color(0xFFD97706))
                                    ) {
                                        Text(
                                            text = "🔔 Alerta Diário: Olhar se houve rebaixa",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (alertaNovaRebaixa2Dias) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFA855F7).copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, Color(0xFFA855F7))
                                    ) {
                                        Text(
                                            text = "🔄 Passaram 2 dias: Fazer nova rebaixa!",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE9D5FF),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Botão de Ação Direta
                            Button(
                                onClick = { produtoSelecionadoParaAcoes = produto },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCritico) Color(0xFF9333EA) else Color(0xFF2563EB)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCritico) "Gerenciar Retirada de Área / Rebaixa" else "Opções de Rebaixa e Retirada",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Ações de Monitoramento (Rebaixa, Retirada, Alertas)
    if (produtoSelecionadoParaAcoes != null) {
        MonitoramentoAcoesDialog(
            produto = produtoSelecionadoParaAcoes!!,
            onDismiss = { produtoSelecionadoParaAcoes = null },
            onStatusChanged = {
                statusRefreshTrigger++
                produtoSelecionadoParaAcoes = null
                onRefresh()
            }
        )
    }
}

// =============================================================================
// 3. ESTOQUE COMPLETO
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstoqueContent(
    produtos: List<Produto>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSetor: String,
    onSetorSelected: (String) -> Unit,
    isMaster: Boolean = false,
    onOpenPlanilhaImport: () -> Unit = {},
    onSelectProduto: (Produto) -> Unit = {},
    onDeleteProduto: (Produto) -> Unit = {},
    onProdutoUpdated: (Produto) -> Unit = {},
    onScanBarcode: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val currentUser = SessionHolder.currentUser
    val lockedMap by StockLockManager.lockedProducts.collectAsState()

    var produtoParaExcluir by remember { mutableStateOf<Produto?>(null) }
    var produtoParaEditar by remember { mutableStateOf<Produto?>(null) }

    // Filtros de setores estritamente dinâmicos baseados nos produtos presentes no estoque da loja
    val setoresPresentes = remember(produtos) {
        val setoresEncontrados = produtos
            .map { it.setor.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("Todos") + setoresEncontrados
    }

    // Se o setor anteriormente selecionado não existir mais entre os produtos em estoque, reseta para "Todos"
    LaunchedEffect(setoresPresentes, selectedSetor) {
        if (selectedSetor != "Todos" && !setoresPresentes.any { it.equals(selectedSetor, ignoreCase = true) }) {
            onSetorSelected("Todos")
        }
    }

    val filteredList = remember(produtos, searchQuery, selectedSetor) {
        produtos.filter { produto ->
            val matchesQuery = searchQuery.isBlank() ||
                    produto.nome.contains(searchQuery, ignoreCase = true) ||
                    produto.codigo_barras.contains(searchQuery)
            val matchesSetor = selectedSetor.equals("Todos", ignoreCase = true) ||
                    produto.setor.trim().equals(selectedSetor.trim(), ignoreCase = true)
            matchesQuery && matchesSetor
        }
    }

    val totalUnidades = remember(filteredList) {
        filteredList.sumOf { it.quantidade }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Estoque da Loja",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Total: ${filteredList.size} itens (${totalUnidades} unidades)",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (isMaster) {
                Button(
                    onClick = onOpenPlanilhaImport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9333EA)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Importar Planilha",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barra de busca e leitor
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Buscar por nome ou EAN...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = DarkTextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = DarkTextPrimary,
                    focusedBorderColor = BlueExpressive,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurfaceContainer,
                    unfocusedContainerColor = DarkSurfaceContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_search_estoque")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onScanBarcode,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BlueExpressiveContainer)
                    .border(1.dp, BlueExpressive, RoundedCornerShape(12.dp))
                    .testTag("btn_scan_barcode")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear Código de Barras",
                    tint = Color(0xFF93C5FD)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filtro de Setores Dinâmico (Apenas setores com produtos presentes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            setoresPresentes.forEach { setor ->
                val selected = selectedSetor.equals(setor, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = { onSetorSelected(setor) },
                    label = { Text(setor, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueExpressive,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurfaceContainer,
                        labelColor = DarkTextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selected) BlueExpressive else DarkBorder)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueExpressive)
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nenhum produto encontrado",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Cadastre novos produtos através do botão (+).",
                        color = DarkTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 76.dp)
            ) {
                items(filteredList, key = { it.id }) { produto ->
                    val days = calculateDaysRemaining(produto.data_vencimento)
                    val status = getExpiryStatus(days)

                    val lockByOther = remember(lockedMap, produto.id) {
                        StockLockManager.getLockByOther(produto.id, currentUser?.matricula ?: "")
                    }

                    // Animação de pulso/piscar quando o produto está sendo monitorado por outro operador
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_${produto.id}")
                    val blinkAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(650, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "blinkAlpha_${produto.id}"
                    )

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                if (lockByOther != null) {
                                    Toast.makeText(
                                        context,
                                        "🔒 Em edição por ${lockByOther.usuarioNome}. Bloqueado para exclusão.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    false
                                } else {
                                    produtoParaExcluir = produto
                                    false
                                }
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val isEndToStart = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                            val alignment = if (isEndToStart) Alignment.CenterEnd else Alignment.CenterStart
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFDC2626))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Excluir",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (lockByOther != null) {
                                    Color(0xFF450A0A).copy(alpha = 0.45f * blinkAlpha)
                                } else {
                                    DarkSurfaceContainer
                                }
                            ),
                            border = if (lockByOther != null) {
                                BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = blinkAlpha))
                            } else {
                                BorderStroke(1.dp, DarkBorder)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (lockByOther != null) {
                                        Toast.makeText(
                                            context,
                                            "🔒 Produto indisponível para edição! ${lockByOther.usuarioNome} (${lockByOther.usuarioMatricula}) está editando no momento.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        onSelectProduto(produto)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (lockByOther != null) Color(0xFFEF4444).copy(alpha = 0.25f * blinkAlpha) else status.color.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (lockByOther != null) Color(0xFFEF4444).copy(alpha = blinkAlpha) else status.color.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (lockByOther != null) Icons.Default.Lock else status.icon,
                                            contentDescription = null,
                                            tint = if (lockByOther != null) Color(0xFFFCA5A5) else status.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = produto.nome,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    if (lockByOther != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF7F1D1D),
                                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = blinkAlpha))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFCA5A5),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "EM EDIÇÃO POR ${lockByOther.usuarioNome.uppercase()}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Setor: ${produto.setor} • EAN: ${produto.codigo_barras}",
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Validade: ${produto.data_vencimento} (${status.title})",
                                        fontSize = 11.sp,
                                        color = status.color
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceContainerHigh,
                                        border = BorderStroke(1.dp, DarkBorder)
                                    ) {
                                        Text(
                                            text = "${produto.quantidade} un.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF93C5FD),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            if (lockByOther != null) {
                                                Toast.makeText(
                                                    context,
                                                    "🔒 Produto indisponível para edição! ${lockByOther.usuarioNome} está editando agora.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                produtoParaEditar = produto
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("btn_quick_edit_${produto.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar Produto",
                                            tint = if (lockByOther != null) Color.Gray.copy(alpha = 0.5f) else Color(0xFF60A5FA),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (lockByOther != null) {
                                                Toast.makeText(
                                                    context,
                                                    "🔒 Bloqueado: ${lockByOther.usuarioNome} está editando este produto.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                produtoParaExcluir = produto
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("btn_quick_delete_${produto.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir Produto",
                                            tint = if (lockByOther != null) Color.Gray.copy(alpha = 0.5f) else Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
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

    if (produtoParaExcluir != null) {
        val p = produtoParaExcluir!!
        AlertDialog(
            onDismissRequest = { produtoParaExcluir = null },
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
                Column {
                    Text(
                        text = "Tem certeza de que deseja excluir este produto do estoque?",
                        color = DarkTextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceContainerHigh,
                        border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = p.nome,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "EAN: ${p.codigo_barras} • Qtd: ${p.quantidade} un.",
                                fontSize = 11.sp,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = "Setor: ${p.setor} • Vence: ${p.data_vencimento}",
                                fontSize = 11.sp,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Esta ação removerá o produto permanentemente do estoque no sistema.",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = p
                        produtoParaExcluir = null
                        onDeleteProduto(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Sim, Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { produtoParaExcluir = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (produtoParaEditar != null) {
        EditarProdutoDialog(
            produto = produtoParaEditar!!,
            onDismiss = { produtoParaEditar = null },
            onProdutoUpdated = { updated ->
                onProdutoUpdated(updated)
                produtoParaEditar = null
            }
        )
    }
}

// =============================================================================
// 4. AUDITORIA (SEM FUNÇÃO POR ENQUANTO)
// =============================================================================
@Composable
fun AuditoriaContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.5.dp, Color(0xFF64748B)),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FactCheck,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Módulo de Auditoria",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Conferência cega e inventário rotativo",
            style = MaterialTheme.typography.bodySmall,
            color = DarkTextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
        )

        // Card Informativo
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = Color(0xFFEAB308),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Funcionalidade em Preparação",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE68A),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "O módulo de auditoria de gôndola, conferência física por amostragem e apuração de divergências será disponibilizado em breve.",
                    fontSize = 12.sp,
                    color = DarkTextPrimary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Recursos Previstos",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuditoriaPlaceholderCard(
            title = "Auditoria de Ruptura e Gôndola",
            description = "Validação física de presença do item e identificação de falta na área de vendas."
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuditoriaPlaceholderCard(
            title = "Conferência Cega de Validades",
            description = "Contagem independente de lotes perecíveis sem exibir a quantidade do sistema."
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuditoriaPlaceholderCard(
            title = "Registro de Descarte e Avarias",
            description = "Lançamento oficial de perdas por validade vencida para baixa contábil."
        )
    }
}

@Composable
fun AuditoriaPlaceholderCard(
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, fontSize = 11.sp, color = DarkTextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF334155)
            ) {
                Text(
                    text = "Em breve",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
