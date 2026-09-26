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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SectorManager
import com.example.data.SetorComSubsetores
import com.example.data.supabase.LogManager
import com.example.data.supabase.SessionHolder
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GerenciarSetoresDialog(
    onDismiss: () -> Unit,
    onSetoresAtualizados: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser = SessionHolder.currentUser
    var setores by remember { mutableStateOf(SectorManager.getSetores(context)) }

    var setorSelecionado by remember { mutableStateOf<SetorComSubsetores?>(setores.firstOrNull()) }
    var novoSetorNome by remember { mutableStateOf("") }
    var showAddSetorField by remember { mutableStateOf(false) }

    var novoSubsetorNome by remember { mutableStateOf("") }
    var setorEditandoNome by remember { mutableStateOf<String?>(null) }
    var nomeEditadoSetor by remember { mutableStateOf("") }

    // Estados de exclusão com confirmação
    var setorParaExcluir by remember { mutableStateOf<String?>(null) }
    var subsetorParaExcluir by remember { mutableStateOf<Pair<String, String>?>(null) } // Setor, Subsetor
    var subsetorParaEditar by remember { mutableStateOf<Pair<String, String>?>(null) } // Setor, SubsetorAntigo
    var nomeEditadoSubsetor by remember { mutableStateOf("") }

    // Diálogo de confirmação para excluir Setor
    setorParaExcluir?.let { nomeSetor ->
        AlertDialog(
            onDismissRequest = { setorParaExcluir = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Excluir Setor", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "Tem certeza que deseja apagar o setor '$nomeSetor' e todos os seus subsetores? Esta ação não pode ser desfeita.",
                    color = DarkTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SectorManager.excluirSetor(context, nomeSetor)
                        if (currentUser != null) {
                            LogManager.recordLog(
                                usuarioMatricula = currentUser.matricula,
                                usuarioNome = currentUser.nome,
                                lojaId = currentUser.loja_id,
                                acao = "EXCLUSAO_SETOR",
                                detalhes = "Excluiu setor '$nomeSetor'"
                            )
                        }
                        setores = SectorManager.getSetores(context)
                        setorSelecionado = setores.firstOrNull()
                        setorParaExcluir = null
                        onSetoresAtualizados()
                        Toast.makeText(context, "Setor excluído com sucesso", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Sim, Excluir", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { setorParaExcluir = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo de confirmação para excluir Subsetor
    subsetorParaExcluir?.let { (setorNome, subNome) ->
        AlertDialog(
            onDismissRequest = { subsetorParaExcluir = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Excluir Subsetor", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "Deseja remover o subsetor '$subNome' do setor '$setorNome'?",
                    color = DarkTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SectorManager.removerSubsetor(context, setorNome, subNome)
                        setores = SectorManager.getSetores(context)
                        setorSelecionado = setores.find { it.nome.equals(setorNome, ignoreCase = true) }
                        subsetorParaExcluir = null
                        onSetoresAtualizados()
                        Toast.makeText(context, "Subsetor removido", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { subsetorParaExcluir = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo para Editar Subsetor
    subsetorParaEditar?.let { (setorNome, subAntigo) ->
        AlertDialog(
            onDismissRequest = { subsetorParaEditar = null },
            icon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Editar Subsetor", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text("Setor: $setorNome", fontSize = 12.sp, color = DarkTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nomeEditadoSubsetor,
                        onValueChange = { nomeEditadoSubsetor = it },
                        label = { Text("Nome do Subsetor") },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nomeEditadoSubsetor.trim().isNotBlank()) {
                            SectorManager.editarSubsetor(context, setorNome, subAntigo, nomeEditadoSubsetor.trim())
                            setores = SectorManager.getSetores(context)
                            setorSelecionado = setores.find { it.nome.equals(setorNome, ignoreCase = true) }
                            subsetorParaEditar = null
                            onSetoresAtualizados()
                            Toast.makeText(context, "Subsetor atualizado!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("Salvar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { subsetorParaEditar = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }

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
                        color = Color(0xFF581C87).copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Setores & Subsetores", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text("Configuração exclusiva do Master", fontSize = 11.sp, color = DarkTextSecondary)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Adicionar Novo Setor
                if (showAddSetorField) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                        border = BorderStroke(1.dp, Color(0xFFA855F7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Criar Novo Setor", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = novoSetorNome,
                                onValueChange = { novoSetorNome = it },
                                placeholder = { Text("Nome do novo setor...", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = DarkTextPrimary,
                                    focusedBorderColor = Color(0xFFA855F7),
                                    unfocusedBorderColor = DarkBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showAddSetorField = false }) {
                                    Text("Cancelar", color = DarkTextSecondary, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (novoSetorNome.trim().isNotBlank()) {
                                            SectorManager.adicionarOuAtualizarSetor(context, null, novoSetorNome.trim())
                                            setores = SectorManager.getSetores(context)
                                            setorSelecionado = setores.find { it.nome.equals(novoSetorNome.trim(), ignoreCase = true) }
                                            if (currentUser != null) {
                                                LogManager.recordLog(
                                                    usuarioMatricula = currentUser.matricula,
                                                    usuarioNome = currentUser.nome,
                                                    lojaId = currentUser.loja_id,
                                                    acao = "CRIACAO_SETOR",
                                                    detalhes = "Criou novo setor '${novoSetorNome.trim()}'"
                                                )
                                            }
                                            novoSetorNome = ""
                                            showAddSetorField = false
                                            onSetoresAtualizados()
                                            Toast.makeText(context, "Setor criado com sucesso!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                                ) {
                                    Text("Salvar Setor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Setores Cadastrados", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFC084FC))
                        Button(
                            onClick = { showAddSetorField = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF581C87).copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, Color(0xFFA855F7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Novo Setor", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Lista horizontal de setores com chips selecionáveis
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    setores.forEach { setor ->
                        val isSelected = setorSelecionado?.nome == setor.nome
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF7E22CE) else DarkSurfaceContainerHigh,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFC084FC) else DarkBorder),
                            modifier = Modifier.clickable { setorSelecionado = setor }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else DarkTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = setor.nome,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DarkTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Área do Setor Selecionado e seus Subsetores
                setorSelecionado?.let { setor ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Cabeçalho do Setor
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(setor.nome, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            setorEditandoNome = setor.nome
                                            nomeEditadoSetor = setor.nome
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar nome do setor", tint = Color(0xFF93C5FD), modifier = Modifier.size(16.dp))
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            setorParaExcluir = setor.nome
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir setor", tint = RedExpressive, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Edição do nome do setor
                            if (setorEditandoNome == setor.nome) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = nomeEditadoSetor,
                                        onValueChange = { nomeEditadoSetor = it },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = DarkTextPrimary,
                                            focusedBorderColor = Color(0xFFA855F7),
                                            unfocusedBorderColor = DarkBorder
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            if (nomeEditadoSetor.isNotBlank()) {
                                                SectorManager.adicionarOuAtualizarSetor(context, setor.nome, nomeEditadoSetor.trim())
                                                setores = SectorManager.getSetores(context)
                                                setorSelecionado = setores.find { it.nome.equals(nomeEditadoSetor.trim(), ignoreCase = true) }
                                                setorEditandoNome = null
                                                onSetoresAtualizados()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                                    ) {
                                        Text("OK", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Subsetores de '${setor.nome}':",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Exibição dos subsetores existentes com opções de editar e excluir
                            if (setor.subsetores.isEmpty()) {
                                Text("Nenhum subsetor cadastrado para este setor.", fontSize = 11.sp, color = DarkTextSecondary)
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    setor.subsetores.forEach { sub ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF334155))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 8.dp, top = 3.dp, bottom = 3.dp, end = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(sub, fontSize = 11.sp, color = Color.White)
                                                Spacer(modifier = Modifier.width(4.dp))

                                                // Botão Editar Subsetor
                                                IconButton(
                                                    onClick = {
                                                        nomeEditadoSubsetor = sub
                                                        subsetorParaEditar = Pair(setor.nome, sub)
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF93C5FD), modifier = Modifier.size(11.dp))
                                                }

                                                // Botão Excluir Subsetor
                                                IconButton(
                                                    onClick = {
                                                        subsetorParaExcluir = Pair(setor.nome, sub)
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = RedExpressive, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Campo para adicionar novo subsetor
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = novoSubsetorNome,
                                    onValueChange = { novoSubsetorNome = it },
                                    placeholder = { Text("Novo subsetor (ex: Iogurtes)...", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = DarkTextPrimary,
                                        focusedBorderColor = Color(0xFFA855F7),
                                        unfocusedBorderColor = DarkBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (novoSubsetorNome.trim().isNotBlank()) {
                                            SectorManager.adicionarSubsetor(context, setor.nome, novoSubsetorNome.trim())
                                            if (currentUser != null) {
                                                LogManager.recordLog(
                                                    usuarioMatricula = currentUser.matricula,
                                                    usuarioNome = currentUser.nome,
                                                    lojaId = currentUser.loja_id,
                                                    acao = "CRIACAO_SUBSETOR",
                                                    detalhes = "Adicionou subsetor '${novoSubsetorNome.trim()}' ao setor '${setor.nome}'"
                                                )
                                            }
                                            setores = SectorManager.getSetores(context)
                                            setorSelecionado = setores.find { it.nome.equals(setor.nome, ignoreCase = true) }
                                            novoSubsetorNome = ""
                                            onSetoresAtualizados()
                                            Toast.makeText(context, "Subsetor adicionado!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Adicionar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF581C87))
            ) {
                Text("Concluir", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(16.dp)
    )
}
