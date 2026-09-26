package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.NovoUsuario
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import com.example.util.decrypted
import com.example.util.encrypted
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterUsuariosTab(
    masterLojaId: String,
    masterLojaNome: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }

    var usuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSetoresDialog by remember { mutableStateOf(false) }
    var showCargosDialog by remember { mutableStateOf(false) }
    var showFeedbacksDialog by remember { mutableStateOf(false) }
    var selectedUsuarioForDetails by remember { mutableStateOf<Usuario?>(null) }

    val feedbacksPendentesMaster = remember(showFeedbacksDialog, usuarios) {
        com.example.data.FeedbackManager.getFeedbacksParaMaster(context, masterLojaId)
            .count { it.status.equals("PENDENTE", ignoreCase = true) }
    }

    fun carregarUsuarios() {
        isLoading = true
        coroutineScope.launch {
            try {
                val list = supabase.from("usuarios")
                    .select { eq("loja_id", masterLojaId) }
                    .decodeList<Usuario>()
                usuarios = list.map { it.decrypted() }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar usuários da loja: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(masterLojaId) {
        carregarUsuarios()
    }

    // Modal de criação de novo usuário pelo Master
    if (showAddDialog) {
        val cargosDisponiveis = remember(showAddDialog, showCargosDialog) {
            com.example.data.CargoManager.getCargos(context, masterLojaId)
        }
        var novaMatricula by remember { mutableStateOf("") }
        var novoNome by remember { mutableStateOf("") }
        var cargoSelecionado by remember { mutableStateOf(cargosDisponiveis.firstOrNull()?.nome ?: "Operador Geral") }
        val cargoObj = cargosDisponiveis.find { it.nome == cargoSelecionado }
        var setorAtribuido by remember(cargoSelecionado) { mutableStateOf(cargoObj?.setor ?: "") }
        var isSaving by remember { mutableStateOf(false) }
        var erroValidacao by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF581C87).copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, Color(0xFFA855F7)),
                    modifier = Modifier.size(44.dp)
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
                    text = "Cadastrar Usuário da Loja",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Todo usuário criado pelo Master é automaticamente associado à sua loja vinculada.",
                        fontSize = 12.sp,
                        color = DarkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Loja Vinculada (Exibição fixa garantindo vínculo)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF581C87).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Loja de Vínculo Obrigatório:", fontSize = 10.sp, color = DarkTextSecondary)
                                Text(
                                    text = masterLojaNome,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE9D5FF)
                                )
                            }
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
                        placeholder = { Text("Ex: 102030", color = DarkTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                    OutlinedTextField(
                        value = novoNome,
                        onValueChange = { novoNome = it },
                        label = { Text("Nome Completo do Funcionário") },
                        placeholder = { Text("Ex: Carlos Silva", color = DarkTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Cargo / Função (por Setor):", fontSize = 11.sp, color = DarkTextSecondary)
                        Text(
                            text = "+ Gerenciar Cargos",
                            fontSize = 11.sp,
                            color = Color(0xFFC084FC),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showCargosDialog = true }
                        )
                    }

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
                                label = {
                                    Column {
                                        Text(c.nome, fontSize = 11.sp)
                                        if (!c.setor.isNullOrBlank()) {
                                            Text(
                                                text = "Setor: ${c.setor}",
                                                fontSize = 9.sp,
                                                color = if (cargoSelecionado == c.nome) Color(0xFFF3E8FF) else Color(0xFFA855F7)
                                            )
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF7E22CE),
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceContainerHigh,
                                    labelColor = DarkTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = cargoSelecionado == c.nome,
                                    borderColor = DarkBorder,
                                    selectedBorderColor = Color(0xFFA855F7)
                                )
                            )
                        }
                    }

                    if (setorAtribuido.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFA855F7).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Restrição de Acesso: Operador atuará apenas no setor '$setorAtribuido'.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE9D5FF)
                                )
                            }
                        }
                    }

                    if (erroValidacao != null) {
                        Spacer(modifier = Modifier.height(10.dp))
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
                            erroValidacao = "Informe o nome do funcionário."
                            return@Button
                        }

                        isSaving = true
                        erroValidacao = null
                        coroutineScope.launch {
                            try {
                                val setorFinal = setorAtribuido.trim().ifBlank { null }
                                val cargoNoBanco = com.example.data.CargoManager.normalizarCargoParaBanco(cargoSelecionado)
                                com.example.data.CargoManager.salvarAtribuicaoUsuario(
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
                                    loja_id = masterLojaId, // Vínculo estrito com a loja do master
                                    ativo = true,
                                    setor = setorFinal
                                )
                                supabase.from("usuarios").insert(novoUsuario.encrypted())
                                com.example.data.supabase.LogManager.recordLog(
                                    usuarioMatricula = SessionHolder.currentUser?.matricula ?: "MASTER",
                                    usuarioNome = SessionHolder.currentUser?.nome ?: "Master",
                                    lojaId = masterLojaId,
                                    acao = "CADASTRO_USUARIO",
                                    detalhes = "Cadastrou colaborador '$cleanNome' (Matrícula: $novaMatricula, Função: $cargoSelecionado, Setor: ${setorFinal ?: "Geral"})"
                                )
                                Toast.makeText(context, "Usuário cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                carregarUsuarios()
                            } catch (e: Exception) {
                                erroValidacao = "Erro ao cadastrar usuário: ${e.message}"
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                    enabled = !isSaving,
                    modifier = Modifier.testTag("btn_salvar_usuario_master")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Cadastrar Usuário", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Banner / Actions
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF581C87).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFFA855F7)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Equipe da Loja",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${usuarios.size} funcionários em $masterLojaNome",
                            fontSize = 12.sp,
                            color = Color(0xFFC084FC)
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { carregarUsuarios() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recarregar", tint = DarkTextSecondary)
                    }

                    Button(
                        onClick = { showFeedbacksDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_feedbacks_empresa_tab")
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Feedbacks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (feedbacksPendentesMaster > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
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

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { showCargosDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_gerenciar_cargos_tab")
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cargos", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { showSetoresDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_gerenciar_setores_tab")
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Setores", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_novo_usuario_master")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo Usuário", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFA855F7))
            }
        } else if (usuarios.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum usuário cadastrado nesta loja.",
                        color = DarkTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                    ) {
                        Text("Cadastrar Primeiro Usuário")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(usuarios, key = { it.matricula }) { usuario ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUsuarioForDetails = usuario }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (usuario.ativo) Color(0xFF581C87).copy(alpha = 0.3f) else Color(0xFF334155),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (usuario.ativo) Color(0xFFC084FC) else DarkTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = usuario.nome,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Matrícula: ${usuario.matricula}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF93C5FD)
                                    )
                                    Text(text = "•", fontSize = 10.sp, color = DarkTextSecondary)
                                    val cargoDisplay = com.example.data.CargoManager.getCargoCustomizado(context, usuario.matricula) ?: usuario.cargo
                                    Text(
                                        text = cargoDisplay.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFC084FC)
                                    )
                                }

                                val sectorRes = com.example.data.CargoManager.resolveUserSector(usuario)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (sectorRes != null) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFA855F7).copy(alpha = 0.2f),
                                        border = BorderStroke(0.8.dp, Color(0xFFA855F7).copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Acesso Restrito: $sectorRes",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE9D5FF)
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = EmeraldSafe.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Acesso Geral (Todos os Setores)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EmeraldSafe,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (usuario.ativo) EmeraldSafe.copy(alpha = 0.15f) else RedExpressive.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (usuario.ativo) EmeraldSafe.copy(alpha = 0.5f) else RedExpressive.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (usuario.ativo) "Ativo" else "Inativo",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (usuario.ativo) EmeraldSafe else RedExpressive,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Gerenciamento de Cargos e Permissões por Setor
    if (showCargosDialog) {
        GerenciarCargosDialog(
            lojaId = masterLojaId,
            onDismiss = { showCargosDialog = false },
            onCargoCreatedOrUpdated = { carregarUsuarios() }
        )
    }

    // Modal de Gerenciamento de Setores e Subsetores
    if (showSetoresDialog) {
        GerenciarSetoresDialog(
            onDismiss = { showSetoresDialog = false }
        )
    }

    // Modal de Feedbacks e Sugestões da Loja (Master)
    if (showFeedbacksDialog) {
        MasterFeedbacksDialog(
            masterLojaId = masterLojaId,
            masterLojaNome = masterLojaNome,
            onDismiss = { showFeedbacksDialog = false }
        )
    }

    // Modal de Detalhes do Usuário (Logs, Tempo Ativo, Editar, Ativar/Desativar, Excluir)
    selectedUsuarioForDetails?.let { user ->
        DetalhesUsuarioMasterDialog(
            usuario = user,
            onDismiss = { selectedUsuarioForDetails = null },
            onUsuarioUpdated = { updated ->
                usuarios = usuarios.map { if (it.matricula == updated.matricula) updated else it }
                selectedUsuarioForDetails = updated
            },
            onUsuarioDeleted = { deleted ->
                usuarios = usuarios.filter { it.matricula != deleted.matricula }
                selectedUsuarioForDetails = null
            }
        )
    }
}
