package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.data.supabase.AtualizarLojaStatus
import com.example.data.FeedbackManager
import com.example.ui.components.AdmFeedbacksTab
import com.example.ui.components.AdmTelemetriaTab
import com.example.ui.components.EditarUsuarioAdmDialog
import com.example.ui.components.GerenciarUsuariosLojaDialog
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.Loja
import com.example.data.supabase.NovaLoja
import com.example.data.supabase.NovoUsuario
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import com.example.ui.components.SupabaseConfigDialog
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val supabase = remember { SupabaseClient.instance }

    val currentUser = SessionHolder.currentUser

    // Listas do Postgrest
    var lojasList by remember { mutableStateOf<List<Loja>>(emptyList()) }
    var mastersList by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var todosUsuariosList by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var isLoadingLojas by remember { mutableStateOf(false) }
    var isLoadingUsuarios by remember { mutableStateOf(false) }

    // Tab de navegação no painel ADM (0 = Lojas, 1 = Usuários, 2 = Feedbacks, 3 = Telemetria)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Filtros e diálogos de gerenciamento de usuário
    var filterUsuarioCargo by remember { mutableStateOf("Todos") } // "Todos", "master", "operador"
    var searchUsuarioQuery by remember { mutableStateOf("") }
    var userToEdit by remember { mutableStateOf<Usuario?>(null) }
    var userToDelete by remember { mutableStateOf<Usuario?>(null) }
    var lojaParaGerenciarUsuarios by remember { mutableStateOf<Loja?>(null) }
    var feedbackCount by remember { mutableIntStateOf(0) }

    // Formulário Criação de Loja (Item 2)
    var nomeLojaInput by remember { mutableStateOf("") }
    var corBordaInput by remember { mutableStateOf("#2563EB") }
    var isCreatingLoja by remember { mutableStateOf(false) }
    var createdLojaConfirmDialog by remember { mutableStateOf<Loja?>(null) }

    // Formulário Criação de Usuário Master (Item 3)
    var selectedLojaParaMaster by remember { mutableStateOf<Loja?>(null) }
    var matriculaMasterInput by remember { mutableStateOf("") }
    var nomeMasterInput by remember { mutableStateOf("") }
    var isCreatingMaster by remember { mutableStateOf(false) }
    var createdMasterConfirmDialog by remember { mutableStateOf<Usuario?>(null) }
    var lojaDropdownExpanded by remember { mutableStateOf(false) }

    var showConfigDialog by remember { mutableStateOf(false) }

    // Função para carregar lista de lojas via Postgrest
    fun loadLojas() {
        isLoadingLojas = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    val result = supabase.from("lojas").select().decodeList<Loja>()
                    lojasList = result
                    if (selectedLojaParaMaster == null && result.isNotEmpty()) {
                        selectedLojaParaMaster = result.first()
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Aviso lojas: ${e.localizedMessage ?: "falha de leitura"}")
            } finally {
                isLoadingLojas = false
            }
        }
    }

    // Função para carregar todos os usuários
    fun loadTodosUsuarios() {
        isLoadingUsuarios = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    val result = supabase.from("usuarios")
                        .select()
                        .decodeList<Usuario>()
                    todosUsuariosList = result
                    mastersList = result.filter { it.cargo.equals("master", ignoreCase = true) }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Aviso usuários: ${e.localizedMessage ?: "falha de leitura"}")
            } finally {
                isLoadingUsuarios = false
            }
        }
    }

    // Alternar status da loja (Ligar / Desligar)
    fun alternarStatusLoja(loja: Loja, novoStatus: Boolean) {
        // Atualização otimista
        lojasList = lojasList.map { if (it.id == loja.id) it.copy(ativa = novoStatus) else it }
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    supabase.from("lojas")
                        .update(AtualizarLojaStatus(ativa = novoStatus)) {
                            eq("id", loja.id)
                        }
                    snackbarHostState.showSnackbar(
                        if (novoStatus) "Loja \"${loja.nome_loja}\" ATIVADA (Ligada)."
                        else "Loja \"${loja.nome_loja}\" DESATIVADA (Desligada)."
                    )
                }
            } catch (e: Exception) {
                // Reverter em caso de falha
                lojasList = lojasList.map { if (it.id == loja.id) it.copy(ativa = !novoStatus) else it }
                snackbarHostState.showSnackbar("Erro ao alterar status: ${e.localizedMessage}")
            }
        }
    }

    // Excluir usuário do Supabase
    fun excluirUsuario(usuario: Usuario) {
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    supabase.from("usuarios").delete {
                        eq("matricula", usuario.matricula)
                    }
                    todosUsuariosList = todosUsuariosList.filter { it.matricula != usuario.matricula }
                    mastersList = mastersList.filter { it.matricula != usuario.matricula }
                    snackbarHostState.showSnackbar("Usuário ${usuario.nome} excluído com sucesso.")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Erro ao excluir usuário: ${e.localizedMessage}")
            }
        }
    }

    fun loadMasters() {
        loadTodosUsuarios()
    }

    LaunchedEffect(Unit) {
        loadLojas()
        loadTodosUsuarios()
        feedbackCount = FeedbackManager.getFeedbacks(context).count { it.status.equals("pendente", ignoreCase = true) }
    }

    // Item 2: Criação de Loja pelo ADM
    fun criarLoja() {
        val cleanNome = nomeLojaInput.trim()
        val cleanCor = corBordaInput.trim()
        if (cleanNome.isEmpty()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Informe o nome da loja.") }
            return
        }
        if (cleanCor.isEmpty()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Informe a cor de borda.") }
            return
        }

        isCreatingLoja = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    // Inserção direta via Postgrest conforme especificado no item 2:
                    // .insert(NovaLoja(nome_loja, cor_borda)) { select() }.decodeSingle<Loja>()
                    val novaLoja = supabase.from("lojas")
                        .insert(NovaLoja(nome_loja = cleanNome, cor_borda = cleanCor)) { select() }
                        .decodeSingle<Loja>()

                    createdLojaConfirmDialog = novaLoja
                    nomeLojaInput = ""
                    // Atualiza a lista de lojas
                    loadLojas()
                }
            } catch (e: TimeoutCancellationException) {
                snackbarHostState.showSnackbar("Tempo limite de 8s excedido ao criar loja.")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Erro ao criar loja: ${e.localizedMessage ?: "Erro desconhecido"}")
            } finally {
                isCreatingLoja = false
            }
        }
    }

    // Item 3: Criação de Usuário Master pelo ADM
    fun criarUsuarioMaster() {
        val loja = selectedLojaParaMaster
        if (loja == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Selecione uma loja para vincular o Master.") }
            return
        }
        val cleanMatricula = matriculaMasterInput.trim()
        val cleanNome = nomeMasterInput.trim()

        if (cleanMatricula.length != 6 || !cleanMatricula.all { it.isDigit() }) {
            coroutineScope.launch { snackbarHostState.showSnackbar("A matrícula do Master deve ter exatamente 6 dígitos numéricos.") }
            return
        }
        if (cleanNome.isEmpty()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Informe o nome do Master.") }
            return
        }

        isCreatingMaster = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    // Inserir na tabela 'usuarios' com cargo = "master" e loja_id = UUID da loja selecionada
                    val novoMaster = supabase.from("usuarios")
                        .insert(
                            NovoUsuario(
                                matricula = cleanMatricula,
                                nome = cleanNome,
                                cargo = "master",
                                loja_id = loja.id,
                                ativo = true
                            )
                        ) { select() }
                        .decodeSingle<Usuario>()

                    createdMasterConfirmDialog = novoMaster
                    matriculaMasterInput = ""
                    nomeMasterInput = ""
                    // Recarrega a lista de masters
                    loadMasters()
                }
            } catch (e: TimeoutCancellationException) {
                snackbarHostState.showSnackbar("Tempo limite de 8s excedido ao cadastrar Master.")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Erro ao cadastrar Master: ${e.localizedMessage ?: "Erro desconhecido"}")
            } finally {
                isCreatingMaster = false
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("adm_screen"),
        containerColor = DarkSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Painel Administrativo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                            Text(
                                text = "${currentUser?.nome ?: "ADM"} • Mat: ${currentUser?.matricula ?: "111111"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.testTag("adm_config_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar Supabase",
                            tint = DarkTextSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            SessionHolder.clear()
                            onLogout()
                        },
                        modifier = Modifier.testTag("adm_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Encerrar Sessão",
                            tint = RedExpressive
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurfaceContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Material 3: 0 = Lojas, 1 = Usuários, 2 = Feedbacks, 3 = Telemetria
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurfaceContainer,
                contentColor = BlueExpressive,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BlueExpressive,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lojas (${lojasList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Usuários (${todosUsuariosList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFFBBF24))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Feedbacks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (feedbackCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = RedExpressive,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("$feedbackCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = DarkTextSecondary
                )

                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmeraldSafe)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Telemetria", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = DarkTextSecondary
                )
            }

            // Conteúdo da Aba 0: CRIAÇÃO E GESTÃO DE LOJAS
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Formulário Item 2
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AddBusiness,
                                        contentDescription = null,
                                        tint = BlueExpressive,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cadastrar Nova Loja (Item 2)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "Insere na tabela 'lojas' via Postgrest. O UUID é gerado automaticamente pelo banco.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                )

                                OutlinedTextField(
                                    value = nomeLojaInput,
                                    onValueChange = { nomeLojaInput = it },
                                    label = { Text("Nome da Loja") },
                                    placeholder = { Text("Ex: Supermercado Alvorada - Loja 2") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = DarkTextPrimary,
                                        focusedBorderColor = BlueExpressive,
                                        unfocusedBorderColor = DarkBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_nome_loja")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = corBordaInput,
                                    onValueChange = { corBordaInput = it },
                                    label = { Text("Cor da Borda (Hexadecimal)") },
                                    placeholder = { Text("#2563EB") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ColorLens,
                                            contentDescription = null,
                                            tint = parseHexColor(corBordaInput)
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = DarkTextPrimary,
                                        focusedBorderColor = BlueExpressive,
                                        unfocusedBorderColor = DarkBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_cor_borda")
                                )

                                // Presets de Cores
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val colorPresets = listOf(
                                        "#2563EB" to "Azul",
                                        "#10B981" to "Verde",
                                        "#F59E0B" to "Âmbar",
                                        "#8B5CF6" to "Roxo",
                                        "#EF4444" to "Vermelho"
                                    )
                                    colorPresets.forEach { (hex, _) ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(hex))
                                                .clickable { corBordaInput = hex }
                                                .border(
                                                    width = if (corBordaInput.equals(hex, true)) 2.5.dp else 1.dp,
                                                    color = if (corBordaInput.equals(hex, true)) Color.White else DarkBorder,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { criarLoja() },
                                    enabled = !isCreatingLoja,
                                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_criar_loja")
                                ) {
                                    if (isCreatingLoja) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Criando no Supabase...", color = Color.White, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Criar Loja (Timeout 8s)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Título da Lista de Lojas
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lojas Cadastradas no Banco (${lojasList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                            IconButton(onClick = { loadLojas() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Recarregar", tint = BlueExpressive)
                            }
                        }
                    }

                    // Lista de Lojas
                    if (lojasList.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Nenhuma loja cadastrada ainda.", color = DarkTextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(lojasList, key = { it.id }) { loja ->
                            val borderColor = parseHexColor(loja.cor_borda)
                            val usuariosDaLoja = todosUsuariosList.filter { it.loja_id == loja.id }
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(2.dp, borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(borderColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = borderColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = loja.nome_loja,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "UUID: ${loja.id}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = DarkTextSecondary,
                                                    maxLines = 1
                                                )
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(loja.id))
                                                        Toast.makeText(context, "UUID copiado!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copiar UUID",
                                                        tint = DarkTextSecondary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = borderColor.copy(alpha = 0.25f)
                                                ) {
                                                    Text(
                                                        text = "Cor: ${loja.cor_borda}",
                                                        fontSize = 10.sp,
                                                        color = borderColor,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                // Contagem de usuários na loja
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "👥 ${usuariosDaLoja.size} usuário(s)",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF93C5FD),
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                // Status Ativa / Inativa
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (loja.ativa) EmeraldSafe.copy(alpha = 0.2f) else RedExpressive.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = if (loja.ativa) "Ligada" else "Desligada",
                                                        fontSize = 10.sp,
                                                        color = if (loja.ativa) EmeraldSafe else RedExpressive,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Controle de Ligar / Desligar Loja
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DarkSurfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.PowerSettingsNew,
                                                    contentDescription = null,
                                                    tint = if (loja.ativa) EmeraldSafe else RedExpressive,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = if (loja.ativa) "Loja Ativa (Ligada)" else "Loja Inativa (Desligada)",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (loja.ativa) EmeraldSafe else RedExpressive
                                                    )
                                                    Text(
                                                        text = if (loja.ativa) "Acesso permitido aos operadores" else "Acesso suspenso aos operadores",
                                                        fontSize = 10.sp,
                                                        color = DarkTextSecondary
                                                    )
                                                }
                                            }

                                            Switch(
                                                checked = loja.ativa,
                                                onCheckedChange = { novoStatus ->
                                                    alternarStatusLoja(loja, novoStatus)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = EmeraldSafe,
                                                    uncheckedThumbColor = Color.LightGray,
                                                    uncheckedTrackColor = DarkSurfaceContainer
                                                ),
                                                modifier = Modifier.testTag("switch_loja_${loja.id}")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Botão de Gerenciar / Ver Usuários da Loja
                                    Button(
                                        onClick = { lojaParaGerenciarUsuarios = loja },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainerHigh),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, DarkBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_gerenciar_usuarios_${loja.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = null,
                                            tint = BlueExpressive,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Gerenciar / Excluir / Editar Usuários (${usuariosDaLoja.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Conteúdo da Aba 1: CRIAÇÃO E GESTÃO DE USUÁRIOS MASTERS (Item 3)
            if (selectedTab == 1) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Formulário Item 3
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cadastrar Usuário Master (Item 3)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "O ADM seleciona a Loja, define a matrícula de 6 dígitos e nome. O registro é salvo com cargo = 'master'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                )

                                // Seletor de Loja (Dropdown Exposed)
                                ExposedDropdownMenuBox(
                                    expanded = lojaDropdownExpanded,
                                    onExpandedChange = { lojaDropdownExpanded = !lojaDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedLojaParaMaster?.let { "${it.nome_loja} (${it.id.take(8)}...)" } ?: "Selecione uma loja...",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Loja Vinculada") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lojaDropdownExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = BlueExpressive,
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("dropdown_loja_master")
                                    )

                                    ExposedDropdownMenu(
                                        expanded = lojaDropdownExpanded,
                                        onDismissRequest = { lojaDropdownExpanded = false },
                                        modifier = Modifier.background(DarkSurfaceContainerHigh)
                                    ) {
                                        lojasList.forEach { loja ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(loja.nome_loja, fontWeight = FontWeight.Medium, color = Color.White)
                                                },
                                                onClick = {
                                                    selectedLojaParaMaster = loja
                                                    lojaDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = matriculaMasterInput,
                                    onValueChange = { input ->
                                        if (input.length <= 6 && input.all { it.isDigit() }) {
                                            matriculaMasterInput = input
                                        }
                                    },
                                    label = { Text("Matrícula (6 Dígitos Numéricos)") },
                                    placeholder = { Text("Ex: 123456") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = DarkTextPrimary,
                                        focusedBorderColor = BlueExpressive,
                                        unfocusedBorderColor = DarkBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_matricula_master")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = nomeMasterInput,
                                    onValueChange = { nomeMasterInput = it },
                                    label = { Text("Nome do Usuário Master") },
                                    placeholder = { Text("Ex: Carlos Gerente Master") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = DarkTextPrimary,
                                        focusedBorderColor = BlueExpressive,
                                        unfocusedBorderColor = DarkBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_nome_master")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { criarUsuarioMaster() },
                                    enabled = !isCreatingMaster,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_cadastrar_master")
                                ) {
                                    if (isCreatingMaster) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cadastrando Master...", color = Color.White, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cadastrar Usuário Master", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Título e Filtros da Lista de Usuários
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Usuários do Sistema (${todosUsuariosList.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary
                                )
                                IconButton(onClick = { loadTodosUsuarios() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Recarregar", tint = BlueExpressive)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Campo de Busca de Usuário
                            OutlinedTextField(
                                value = searchUsuarioQuery,
                                onValueChange = { searchUsuarioQuery = it },
                                placeholder = { Text("Buscar por nome ou matrícula...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = DarkTextPrimary,
                                    focusedBorderColor = BlueExpressive,
                                    unfocusedBorderColor = DarkBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Chips de Filtro de Cargo
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = filterUsuarioCargo == "Todos",
                                    onClick = { filterUsuarioCargo = "Todos" },
                                    label = { Text("Todos (${todosUsuariosList.size})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BlueExpressive,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = filterUsuarioCargo == "master",
                                    onClick = { filterUsuarioCargo = "master" },
                                    label = { Text("Masters (${mastersList.size})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF059669),
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = filterUsuarioCargo == "operador",
                                    onClick = { filterUsuarioCargo = "operador" },
                                    label = {
                                        val opCount = todosUsuariosList.count { it.cargo.equals("operador", true) }
                                        Text("Operadores ($opCount)", fontSize = 11.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2563EB),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Lista de Usuários Filtrados
                    val filteredUsuarios = todosUsuariosList.filter { user ->
                        val matchesCargo = when (filterUsuarioCargo) {
                            "master" -> user.cargo.equals("master", ignoreCase = true)
                            "operador" -> user.cargo.equals("operador", ignoreCase = true)
                            else -> true
                        }
                        val matchesSearch = searchUsuarioQuery.isBlank() ||
                                user.nome.contains(searchUsuarioQuery, ignoreCase = true) ||
                                user.matricula.contains(searchUsuarioQuery)
                        matchesCargo && matchesSearch
                    }

                    if (filteredUsuarios.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Nenhum usuário encontrado.", color = DarkTextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(filteredUsuarios, key = { it.matricula }) { user ->
                            val lojaVinculada = lojasList.firstOrNull { it.id == user.loja_id }
                            val isMasterRole = user.cargo.equals("master", true)
                            val isAdmRole = user.cargo.equals("adm", true)

                            val badgeBg = when {
                                isAdmRole -> Color(0xFF78350F)
                                isMasterRole -> Color(0xFF065F46)
                                else -> Color(0xFF1E3A8A)
                            }
                            val badgeColor = when {
                                isAdmRole -> Color(0xFFFDE68A)
                                isMasterRole -> Color(0xFFA7F3D0)
                                else -> Color(0xFFBFDBFE)
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(1.dp, if (!user.ativo) RedExpressive.copy(alpha = 0.5f) else DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = badgeBg,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = user.nome.take(1).uppercase(),
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.nome,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = badgeBg
                                            ) {
                                                Text(
                                                    text = user.cargo.uppercase(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (!user.ativo) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = RedExpressive.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "INATIVO",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RedExpressive,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Matrícula: ${user.matricula}",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF93C5FD),
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Text(
                                            text = "Loja: ${lojaVinculada?.nome_loja ?: user.loja_id}",
                                            fontSize = 11.sp,
                                            color = DarkTextSecondary
                                        )
                                    }

                                    // Botões de Ação: Editar e Excluir Usuário
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { userToEdit = user },
                                            modifier = Modifier.size(36.dp).testTag("btn_edit_user_${user.matricula}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar Usuário",
                                                tint = BlueExpressive,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { userToDelete = user },
                                            modifier = Modifier.size(36.dp).testTag("btn_delete_user_${user.matricula}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir Usuário",
                                                tint = RedExpressive,
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

            // Conteúdo da Aba 2: FEEDBACKS DOS USUÁRIOS
            if (selectedTab == 2) {
                AdmFeedbacksTab()
            }

            // Conteúdo da Aba 3: DASHBOARD DE TELEMETRIA
            if (selectedTab == 3) {
                AdmTelemetriaTab(
                    lojasList = lojasList,
                    usuariosList = todosUsuariosList
                )
            }
        }
    }

    // Modal de Edição de Usuário
    userToEdit?.let { usuario ->
        EditarUsuarioAdmDialog(
            usuario = usuario,
            lojasList = lojasList,
            onDismiss = { userToEdit = null },
            onUsuarioSalvo = {
                userToEdit = null
                loadTodosUsuarios()
            }
        )
    }

    // Confirmação de Exclusão de Usuário
    userToDelete?.let { usuario ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = RedExpressive,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Excluir Usuário?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tem certeza que deseja excluir permanentemente o usuário \"${usuario.nome}\"?",
                        color = DarkTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Matrícula: ${usuario.matricula} • Cargo: ${usuario.cargo.uppercase()}",
                        color = Color(0xFF93C5FD),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = usuario
                        userToDelete = null
                        excluirUsuario(toDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal de Gerenciamento de Usuários de uma Loja Específica
    lojaParaGerenciarUsuarios?.let { loja ->
        GerenciarUsuariosLojaDialog(
            loja = loja,
            todasLojas = lojasList,
            onDismiss = { lojaParaGerenciarUsuarios = null },
            onListaAlterada = {
                loadTodosUsuarios()
            }
        )
    }

    // Diálogo de confirmação de criação de loja com UUID (Item 2)
    createdLojaConfirmDialog?.let { loja ->
        AlertDialog(
            onDismissRequest = { createdLojaConfirmDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSafe,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Loja Criada com Sucesso!",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "A nova loja foi inserida na tabela 'lojas' do Postgrest.",
                        color = DarkTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Nome da Loja:", fontSize = 11.sp, color = DarkTextSecondary)
                            Text(loja.nome_loja, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("UUID Gerado pelo Banco:", fontSize = 11.sp, color = DarkTextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = loja.id,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF93C5FD),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(loja.id))
                                        Toast.makeText(context, "UUID copiado!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar UUID",
                                        tint = Color(0xFF93C5FD),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Cor da Borda: ${loja.cor_borda}", fontSize = 12.sp, color = parseHexColor(loja.cor_borda))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { createdLojaConfirmDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo de confirmação de criação de Master (Item 3)
    createdMasterConfirmDialog?.let { master ->
        AlertDialog(
            onDismissRequest = { createdMasterConfirmDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSafe,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Usuário Master Cadastrado!",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "O usuário Master foi cadastrado com sucesso e já pode fazer login pelo número de matrícula.",
                        color = DarkTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Nome:", fontSize = 11.sp, color = DarkTextSecondary)
                            Text(master.nome, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Matrícula de Login:", fontSize = 11.sp, color = DarkTextSecondary)
                            Text(master.matricula, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD), fontSize = 16.sp, fontFamily = FontFamily.Monospace)

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Cargo: ${master.cargo.uppercase()}", fontSize = 12.sp, color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(6.dp))

                            val lojaVinculadaNome = lojasList.find { it.id == master.loja_id }?.nome_loja ?: "Loja Vinculada"
                            Text("Loja Vinculada: $lojaVinculadaNome", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { createdMasterConfirmDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Concluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showConfigDialog) {
        SupabaseConfigDialog(onDismiss = { showConfigDialog = false })
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> 0xFF2563EB.toInt()
        }
        Color(colorInt)
    } catch (e: Exception) {
        BlueExpressive
    }
}
