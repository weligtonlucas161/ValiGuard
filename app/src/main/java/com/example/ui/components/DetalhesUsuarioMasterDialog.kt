package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material.icons.filled.SwitchRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.supabase.AppLog
import com.example.data.supabase.AtualizarUsuario
import com.example.data.supabase.LogManager
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import com.example.util.encrypted
import com.example.util.decrypted
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesUsuarioMasterDialog(
    usuario: Usuario,
    onDismiss: () -> Unit,
    onUsuarioUpdated: (Usuario) -> Unit,
    onUsuarioDeleted: (Usuario) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }
    val currentMaster = SessionHolder.currentUser

    var currentUsuarioState by remember { mutableStateOf(usuario) }
    var isEditing by remember { mutableStateOf(false) }
    var editNome by remember { mutableStateOf(usuario.nome) }
    var editCargo by remember { mutableStateOf(usuario.cargo) }
    var editSetor by remember { mutableStateOf(usuario.setor ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Detalhes & Ações, 1: Histórico de Logs

    var userLogs by remember { mutableStateOf<List<AppLog>>(emptyList()) }
    var isLoadingLogs by remember { mutableStateOf(true) }

    fun carregarLogs() {
        isLoadingLogs = true
        coroutineScope.launch {
            val logs = LogManager.getLogsForUser(usuario.matricula)
            userLogs = logs
            isLoadingLogs = false
        }
    }

    LaunchedEffect(usuario.matricula) {
        carregarLogs()
    }

    fun alternarStatusAtivo(novoStatus: Boolean) {
        isSaving = true
        coroutineScope.launch {
            try {
                val updateObj = AtualizarUsuario(
                    nome = currentUsuarioState.nome,
                    cargo = currentUsuarioState.cargo,
                    ativo = novoStatus
                )
                supabase.from("usuarios").update(updateObj.encrypted()) {
                    eq("matricula", currentUsuarioState.matricula)
                }

                val updated = currentUsuarioState.copy(ativo = novoStatus)
                currentUsuarioState = updated
                onUsuarioUpdated(updated)

                if (currentMaster != null) {
                    LogManager.recordLog(
                        usuarioMatricula = currentMaster.matricula,
                        usuarioNome = currentMaster.nome,
                        lojaId = currentMaster.loja_id,
                        acao = "STATUS_USUARIO",
                        detalhes = "Alterou status do funcionário '${updated.nome}' (Matrícula: ${updated.matricula}) para ${if (novoStatus) "ATIVO" else "INATIVO"}"
                    )
                }

                Toast.makeText(
                    context,
                    if (novoStatus) "Usuário ativado com sucesso!" else "Usuário desativado!",
                    Toast.LENGTH_SHORT
                ).show()
                carregarLogs()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao alterar status: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
            }
        }
    }

    fun salvarEdicao() {
        if (editNome.isBlank()) {
            Toast.makeText(context, "O nome não pode ficar vazio.", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        coroutineScope.launch {
            try {
                val setorFinal = editSetor.trim().ifBlank { null }
                val updateObj = AtualizarUsuario(
                    nome = editNome.trim(),
                    cargo = editCargo,
                    ativo = currentUsuarioState.ativo,
                    setor = setorFinal
                )
                supabase.from("usuarios").update(updateObj.encrypted()) {
                    eq("matricula", currentUsuarioState.matricula)
                }

                val updated = currentUsuarioState.copy(
                    nome = editNome.trim(),
                    cargo = editCargo,
                    setor = setorFinal
                )
                currentUsuarioState = updated
                onUsuarioUpdated(updated)
                isEditing = false

                if (currentMaster != null) {
                    LogManager.recordLog(
                        usuarioMatricula = currentMaster.matricula,
                        usuarioNome = currentMaster.nome,
                        lojaId = currentMaster.loja_id,
                        acao = "EDICAO_USUARIO",
                        detalhes = "Atualizou dados do colaborador '${updated.nome}' (Função: ${updated.cargo}, Setor: ${setorFinal ?: "Geral"})"
                    )
                }

                Toast.makeText(context, "Dados do usuário atualizados!", Toast.LENGTH_SHORT).show()
                carregarLogs()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao salvar alterações: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
            }
        }
    }

    fun excluirUsuario() {
        isSaving = true
        coroutineScope.launch {
            try {
                val deleted = supabase.from("usuarios").delete {
                    eq("matricula", currentUsuarioState.matricula)
                }
                if (deleted) {
                    if (currentMaster != null) {
                        LogManager.recordLog(
                            usuarioMatricula = currentMaster.matricula,
                            usuarioNome = currentMaster.nome,
                            lojaId = currentMaster.loja_id,
                            acao = "EXCLUSAO_USUARIO",
                            detalhes = "Removeu o usuário '${currentUsuarioState.nome}' (Matrícula: ${currentUsuarioState.matricula}) da loja"
                        )
                    }
                    Toast.makeText(context, "Usuário excluído com sucesso!", Toast.LENGTH_SHORT).show()
                    onUsuarioDeleted(currentUsuarioState)
                    onDismiss()
                } else {
                    Toast.makeText(context, "Não foi possível excluir o usuário no Supabase.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
                showDeleteConfirm = false
            }
        }
    }

    // Modal de Confirmação de Exclusão
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showDeleteConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RedExpressive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir Usuário", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    text = "Deseja realmente remover o colaborador '${currentUsuarioState.nome}' (Matrícula: ${currentUsuarioState.matricula})?\n\nEsta ação é irreversível no banco de dados da loja.",
                    color = DarkTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { excluirUsuario() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Sim, Excluir", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !isSaving
                ) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Surface(
                    color = DarkSurfaceContainer,
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
                                    color = if (currentUsuarioState.ativo) Color(0xFF581C87).copy(alpha = 0.4f) else Color(0xFF334155),
                                    border = BorderStroke(1.dp, if (currentUsuarioState.ativo) Color(0xFFA855F7) else DarkBorder),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (currentUsuarioState.ativo) Color(0xFFC084FC) else DarkTextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = currentUsuarioState.nome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Matrícula: ${currentUsuarioState.matricula}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF93C5FD)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "•", fontSize = 10.sp, color = DarkTextSecondary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = currentUsuarioState.cargo.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC084FC)
                                        )
                                    }
                                }
                            }

                            // Status Tag
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentUsuarioState.ativo) EmeraldSafe.copy(alpha = 0.15f) else RedExpressive.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (currentUsuarioState.ativo) EmeraldSafe else RedExpressive)
                            ) {
                                Text(
                                    text = if (currentUsuarioState.ativo) "ATIVO" else "INATIVO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentUsuarioState.ativo) EmeraldSafe else RedExpressive,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tab selector
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFA855F7),
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = Color(0xFFA855F7),
                                    height = 3.dp
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Dados & Gestão", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Logs & Tempo Ativo (${userLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                // Conteúdo da Tab Selecionada
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // TAB 0: DETALHES & AÇÕES (EDITAR, ATIVAR/DESATIVAR, EXCLUIR)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Card de Tempo Ativo & Última Sessão
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1E3A8A).copy(alpha = 0.3f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Tempo Ativo no App",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        val sessionDesc = if (currentUsuarioState.ativo) {
                                            val lastLog = userLogs.firstOrNull()
                                            if (lastLog != null) {
                                                val diffMinutes = ((System.currentTimeMillis() - lastLog.timestamp) / (1000 * 60)).coerceAtLeast(1)
                                                "Última atividade: há ${diffMinutes}m • Sessão em turno ativo"
                                            } else {
                                                "Usuário ativo e habilitado para operar hoje"
                                            }
                                        } else {
                                            "Conta inativa • Acesso ao app temporariamente bloqueado"
                                        }
                                        Text(
                                            text = sessionDesc,
                                            fontSize = 11.sp,
                                            color = DarkTextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Card de Controle de Ativação / Desativação
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(1.dp, if (currentUsuarioState.ativo) EmeraldSafe.copy(alpha = 0.4f) else RedExpressive.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Status da Conta:",
                                            fontSize = 11.sp,
                                            color = DarkTextSecondary
                                        )
                                        Text(
                                            text = if (currentUsuarioState.ativo) "Usuário Habilitado (Ativo)" else "Usuário Desativado (Inativo)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentUsuarioState.ativo) EmeraldSafe else RedExpressive
                                        )
                                        Text(
                                            text = if (currentUsuarioState.ativo) "Pode logar com a matrícula e operar o sistema" else "Login impedido na tela inicial de matrícula",
                                            fontSize = 10.sp,
                                            color = DarkTextSecondary
                                        )
                                    }

                                    Switch(
                                        checked = currentUsuarioState.ativo,
                                        onCheckedChange = { alternarStatusAtivo(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = EmeraldSafe,
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF334155)
                                        ),
                                        enabled = !isSaving
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Formulário de Edição
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                border = BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Dados Cadastrais",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )

                                        if (!isEditing) {
                                            TextButton(onClick = { isEditing = true }) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFA855F7))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Editar", color = Color(0xFFA855F7), fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editNome,
                                            onValueChange = { editNome = it },
                                            label = { Text("Nome Completo") },
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

                                        Text(text = "Cargo / Função (por Setor):", fontSize = 11.sp, color = DarkTextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        val availableCargos = remember {
                                            com.example.data.CargoManager.getCargos(context, currentUsuarioState.loja_id)
                                        }
                                        androidx.compose.foundation.lazy.LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(availableCargos) { c ->
                                                FilterChip(
                                                    selected = editCargo == c.nome,
                                                    onClick = {
                                                        editCargo = c.nome
                                                        editSetor = c.setor ?: ""
                                                    },
                                                    label = {
                                                        Column {
                                                            Text(c.nome, fontSize = 11.sp)
                                                            if (!c.setor.isNullOrBlank()) {
                                                                Text("Setor: ${c.setor}", fontSize = 9.sp, color = if (editCargo == c.nome) Color(0xFFF3E8FF) else Color(0xFFA855F7))
                                                            }
                                                        }
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(0xFF7E22CE),
                                                        selectedLabelColor = Color.White,
                                                        containerColor = DarkSurfaceContainerHigh,
                                                        labelColor = DarkTextSecondary
                                                    )
                                                )
                                            }
                                        }

                                        if (editSetor.isNotBlank()) {
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
                                                        text = "Ações limitadas ao setor '$editSetor'.",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFE9D5FF)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = {
                                                isEditing = false
                                                editNome = currentUsuarioState.nome
                                                editCargo = currentUsuarioState.cargo
                                                editSetor = currentUsuarioState.setor ?: ""
                                            }) {
                                                Text("Cancelar", color = DarkTextSecondary)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { salvarEdicao() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                                enabled = !isSaving
                                            ) {
                                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Salvar", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Text(text = "Nome: ${currentUsuarioState.nome}", color = DarkTextPrimary, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Cargo: ${currentUsuarioState.cargo.uppercase()}", color = DarkTextSecondary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val sectorRes = com.example.data.CargoManager.resolveUserSector(currentUsuarioState)
                                        if (sectorRes != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = "Setor Restrito: $sectorRes", color = Color(0xFFC084FC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Text(text = "Permissão: Acesso Geral a Todos os Setores", color = EmeraldSafe, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Matrícula: ${currentUsuarioState.matricula}", color = Color(0xFF93C5FD), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Proteção: Dados Pessoais Criptografados", color = EmeraldSafe, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Botão de Excluir Usuário
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedExpressive),
                                border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Excluir Usuário do Sistema", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // TAB 1: HISTÓRICO DE LOGS DO USUÁRIO
                        if (isLoadingLogs) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFA855F7))
                            }
                        } else if (userLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = DarkTextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Nenhum log registrado para este usuário.",
                                        color = DarkTextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Ações como login, cadastros, conferências e auditorias aparecerão aqui.",
                                        color = DarkTextSecondary.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(userLogs, key = { it.id }) { log ->
                                    val logVisual = getLogVisual(log.acao)
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                                        border = BorderStroke(1.dp, DarkBorder),
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
                                                color = logVisual.color.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, logVisual.color.copy(alpha = 0.6f)),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = logVisual.icon,
                                                        contentDescription = null,
                                                        tint = logVisual.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = logVisual.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = formatTimestamp(log.timestamp),
                                                        fontSize = 10.sp,
                                                        color = DarkTextSecondary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(
                                                    text = log.detalhes,
                                                    fontSize = 11.sp,
                                                    color = DarkTextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Close Bar
                Surface(
                    color = DarkSurfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("Fechar")
                        }
                    }
                }
            }
        }
    }
}

private data class LogVisual(
    val title: String,
    val color: Color,
    val icon: ImageVector
)

private fun getLogVisual(acao: String): LogVisual {
    return when (acao) {
        "LOGIN" -> LogVisual("Acesso ao Sistema", Color(0xFF10B981), Icons.Default.Key)
        "CADASTRO_PRODUTO" -> LogVisual("Cadastro de Produto", Color(0xFF3B82F6), Icons.Default.Inventory2)
        "EXCLUSAO_PRODUTO" -> LogVisual("Exclusão de Estoque", Color(0xFFEF4444), Icons.Default.Delete)
        "AUDITORIA_PRESENCA" -> LogVisual("Auditoria de Presença", Color(0xFFF59E0B), Icons.Default.FactCheck)
        "AUDITORIA_INVENTARIO" -> LogVisual("Auditoria de Inventário", Color(0xFFA855F7), Icons.Default.Inventory2)
        "AUDITORIA_VALIDADE" -> LogVisual("Auditoria de Validade", Color(0xFF06B6D4), Icons.Default.CheckCircle)
        "REBAIXA_SOLICITADA" -> LogVisual("Rebaixa de Preço", Color(0xFFEAB308), Icons.Default.PriceCheck)
        "STATUS_USUARIO" -> LogVisual("Alteração de Status", Color(0xFFF97316), Icons.Default.SwitchRight)
        "EDICAO_USUARIO" -> LogVisual("Edição de Cadastro", Color(0xFF8B5CF6), Icons.Default.Edit)
        "EXCLUSAO_USUARIO" -> LogVisual("Remoção de Usuário", Color(0xFFDC2626), Icons.Default.Delete)
        else -> LogVisual(acao.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, Color(0xFF94A3B8), Icons.Default.History)
    }
}

private fun formatTimestamp(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}
