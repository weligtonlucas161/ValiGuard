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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CargoManager
import com.example.data.SectorManager
import com.example.data.supabase.Cargo
import com.example.data.supabase.LogManager
import com.example.data.supabase.SessionHolder
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarCargosDialog(
    lojaId: String?,
    onDismiss: () -> Unit,
    onCargoCreatedOrUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    var cargos by remember { mutableStateOf(CargoManager.getCargos(context, lojaId)) }
    val setoresDisponiveis: List<String> = remember { SectorManager.getSetores(context).map { it.nome } }

    var isCreatingNew by remember { mutableStateOf(false) }
    var cargoEmEdicao by remember { mutableStateOf<Cargo?>(null) }
    var cargoParaExcluir by remember { mutableStateOf<Cargo?>(null) }

    var formNomeCargo by remember { mutableStateOf("") }
    var formSetorCargo by remember { mutableStateOf("Todos") } // "Todos" ou setor específico
    var formDescricao by remember { mutableStateOf("") }
    var erroMensagem by remember { mutableStateOf<String?>(null) }

    fun iniciarCriacao() {
        cargoEmEdicao = null
        formNomeCargo = ""
        formSetorCargo = "Todos"
        formDescricao = ""
        erroMensagem = null
        isCreatingNew = true
    }

    fun iniciarEdicao(cargo: Cargo) {
        cargoEmEdicao = cargo
        formNomeCargo = cargo.nome
        formSetorCargo = cargo.setor ?: "Todos"
        formDescricao = cargo.descricao
        erroMensagem = null
        isCreatingNew = true
    }

    fun refreshCargos() {
        cargos = CargoManager.getCargos(context, lojaId)
        onCargoCreatedOrUpdated()
    }

    // Diálogo de confirmação de exclusão de cargo
    cargoParaExcluir?.let { cargo ->
        AlertDialog(
            onDismissRequest = { cargoParaExcluir = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Excluir Cargo", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "Tem certeza que deseja excluir o cargo \"${cargo.nome}\"? Os funcionários atribuídos a este cargo poderão necessitar de uma nova função.",
                    color = DarkTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        CargoManager.excluirCargo(context, cargo.id, lojaId)
                        val user = SessionHolder.currentUser
                        if (user != null) {
                            LogManager.recordLog(
                                usuarioMatricula = user.matricula,
                                usuarioNome = user.nome,
                                lojaId = user.loja_id,
                                acao = "EXCLUSAO_CARGO",
                                detalhes = "Excluiu cargo '${cargo.nome}'"
                            )
                        }
                        cargoParaExcluir = null
                        refreshCargos()
                        Toast.makeText(context, "Cargo removido com sucesso", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Sim, Excluir", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { cargoParaExcluir = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFF7E22CE).copy(alpha = 0.3f),
                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Cargos e Permissões por Setor",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "Defina cargos específicos por setor para os operadores",
                    fontSize = 12.sp,
                    color = DarkTextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // Banner Explicativo da Regra de Negócio
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E3A8A).copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Operadores atribuídos a um setor específico só poderão cadastrar, editar, rebaixar e auditar itens do seu próprio setor.",
                            fontSize = 11.sp,
                            color = Color(0xFFBFDBFE),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isCreatingNew) {
                    // Botão para abrir criação de novo cargo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { iniciarCriacao() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Novo Cargo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                CargoManager.restaurarCargosPadrao(context, lojaId)
                                refreshCargos()
                                Toast.makeText(context, "Cargos padrão restaurados", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                        ) {
                            Text("Restaurar", color = Color(0xFFC084FC), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Cargos Ativos na Loja (${cargos.size}):",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cargos, key = { it.id }) { cargo ->
                            val temSetorRestrito = !cargo.setor.isNullOrBlank()

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                                border = BorderStroke(
                                    1.dp,
                                    if (temSetorRestrito) Color(0xFFA855F7).copy(alpha = 0.5f) else DarkBorder
                                ),
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
                                        color = if (temSetorRestrito) Color(0xFF7E22CE).copy(alpha = 0.3f) else Color(0xFF2563EB).copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (temSetorRestrito) Icons.Default.Lock else Icons.Default.Work,
                                                contentDescription = null,
                                                tint = if (temSetorRestrito) Color(0xFFC084FC) else Color(0xFF60A5FA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cargo.nome,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (temSetorRestrito) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFA855F7).copy(alpha = 0.25f)
                                                ) {
                                                    Text(
                                                        text = "Restrito: ${cargo.setor}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFE9D5FF),
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = EmeraldSafe.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "Acesso Geral (Todos os Setores)",
                                                        fontSize = 10.sp,
                                                        color = EmeraldSafe,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (cargo.descricao.isNotBlank()) {
                                            Text(
                                                text = cargo.descricao,
                                                fontSize = 10.sp,
                                                color = DarkTextSecondary,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    // Ações de Editar e Excluir
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { iniciarEdicao(cargo) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar Cargo",
                                                tint = Color(0xFF93C5FD),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { cargoParaExcluir = cargo },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir Cargo",
                                                tint = RedExpressive,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Formulário de Criação ou Edição de Cargo
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                        border = BorderStroke(1.dp, Color(0xFFA855F7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (cargoEmEdicao != null) "Editar Cargo / Função" else "Novo Cargo / Função",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC084FC),
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { isCreatingNew = false; cargoEmEdicao = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = formNomeCargo,
                                onValueChange = { formNomeCargo = it },
                                label = { Text("Nome do Cargo") },
                                placeholder = { Text("Ex: Operador de Frios, Conferente Hortifruti", fontSize = 11.sp) },
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

                            Text(
                                text = "Setor Permitido de Atuação:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTextSecondary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = formSetorCargo == "Todos",
                                        onClick = { formSetorCargo = "Todos" },
                                        label = { Text("Todos os Setores", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldSafe,
                                            selectedLabelColor = Color.White,
                                            containerColor = DarkSurfaceContainer,
                                            labelColor = DarkTextSecondary
                                        )
                                    )
                                }
                                items(setoresDisponiveis) { setor ->
                                    FilterChip(
                                        selected = formSetorCargo == setor,
                                        onClick = { formSetorCargo = setor },
                                        label = { Text(setor, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF7E22CE),
                                            selectedLabelColor = Color.White,
                                            containerColor = DarkSurfaceContainer,
                                            labelColor = DarkTextSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = formDescricao,
                                onValueChange = { formDescricao = it },
                                label = { Text("Descrição / Tarefas (Opcional)") },
                                placeholder = { Text("Ex: Responsável por etiquetar e rebaixar produtos do setor") },
                                singleLine = false,
                                maxLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = DarkTextPrimary,
                                    focusedBorderColor = Color(0xFFA855F7),
                                    unfocusedBorderColor = DarkBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (erroMensagem != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = erroMensagem!!, color = RedExpressive, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isCreatingNew = false; cargoEmEdicao = null }) {
                                    Text("Cancelar", color = DarkTextSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val cleanNome = formNomeCargo.trim()
                                        if (cleanNome.isBlank()) {
                                            erroMensagem = "Informe o nome do cargo."
                                            return@Button
                                        }

                                        val setorFinal = if (formSetorCargo == "Todos") null else formSetorCargo

                                        val cargoParaSalvar = cargoEmEdicao?.copy(
                                            nome = cleanNome,
                                            setor = setorFinal,
                                            descricao = formDescricao.trim()
                                        ) ?: Cargo(
                                            nome = cleanNome,
                                            setor = setorFinal,
                                            loja_id = lojaId,
                                            descricao = formDescricao.trim()
                                        )

                                        CargoManager.salvarCargo(context, cargoParaSalvar, lojaId)

                                        // Registra log para auditoria
                                        val user = SessionHolder.currentUser
                                        if (user != null) {
                                            LogManager.recordLog(
                                                usuarioMatricula = user.matricula,
                                                usuarioNome = user.nome,
                                                lojaId = user.loja_id,
                                                acao = if (cargoEmEdicao != null) "EDICAO_CARGO" else "CRIACAO_CARGO",
                                                detalhes = "Cargo ${if (cargoEmEdicao != null) "editado" else "criado"}: $cleanNome (Setor: ${setorFinal ?: "Todos"})"
                                            )
                                        }

                                        val msg = if (cargoEmEdicao != null) "Cargo atualizado!" else "Cargo criado!"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        formNomeCargo = ""
                                        formDescricao = ""
                                        formSetorCargo = "Todos"
                                        erroMensagem = null
                                        cargoEmEdicao = null
                                        isCreatingNew = false
                                        refreshCargos()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
                                ) {
                                    Text(if (cargoEmEdicao != null) "Salvar Alterações" else "Salvar Cargo", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(20.dp)
    )
}
