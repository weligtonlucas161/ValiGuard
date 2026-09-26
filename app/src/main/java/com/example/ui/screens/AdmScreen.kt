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
import com.example.util.decrypted
import com.example.util.encrypted
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
    var isLoadingLojas by remember { mutableStateOf(false) }

    // Tab de navegação no painel ADM (0 = Lojas, 1 = Feedbacks, 2 = Telemetria)
    var selectedTab by remember { mutableIntStateOf(0) }
    var feedbackCount by remember { mutableIntStateOf(0) }

    // Formulário Criação de Loja (Item 2)
    var nomeLojaInput by remember { mutableStateOf("") }
    var corBordaInput by remember { mutableStateOf("#2563EB") }
    var isCreatingLoja by remember { mutableStateOf(false) }
    var createdLojaConfirmDialog by remember { mutableStateOf<Loja?>(null) }

    var showConfigDialog by remember { mutableStateOf(false) }

    // Função para carregar lista de lojas via Postgrest
    fun loadLojas() {
        isLoadingLojas = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    val result = supabase.from("lojas").select().decodeList<Loja>()
                    lojasList = result.map { it.decrypted() }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Aviso lojas: ${e.localizedMessage ?: "falha de leitura"}")
            } finally {
                isLoadingLojas = false
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

    LaunchedEffect(Unit) {
        loadLojas()
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
                        .insert(NovaLoja(nome_loja = cleanNome, cor_borda = cleanCor).encrypted()) { select() }
                        .decodeSingle<Loja>()
                        .decrypted()

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
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFFBBF24))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Melhorias & Feedbacks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmeraldSafe)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Telemetria do App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

                                }
                            }
                        }
                    }
                }
            }

            // Conteúdo da Aba 1: MELHORIAS E FEEDBACKS DO APP
            if (selectedTab == 1) {
                AdmFeedbacksTab()
            }

            // Conteúdo da Aba 2: DASHBOARD DE TELEMETRIA DO APP
            if (selectedTab == 2) {
                AdmTelemetriaTab(
                    lojasList = lojasList
                )
            }
        }
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
