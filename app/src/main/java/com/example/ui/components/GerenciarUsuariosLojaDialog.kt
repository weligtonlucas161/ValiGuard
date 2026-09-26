package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.supabase.Loja
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun GerenciarUsuariosLojaDialog(
    loja: Loja,
    todasLojas: List<Loja>,
    onDismiss: () -> Unit,
    onListaAlterada: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }

    var usuariosDaLoja by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var usuarioParaEditar by remember { mutableStateOf<Usuario?>(null) }
    var usuarioParaExcluir by remember { mutableStateOf<Usuario?>(null) }
    var isExcluindo by remember { mutableStateOf(false) }

    fun carregarUsuarios() {
        isLoading = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    val res = supabase.from("usuarios")
                        .select { eq("loja_id", loja.id) }
                        .decodeList<Usuario>()
                    usuariosDaLoja = res
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar colaboradores: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(loja.id) {
        carregarUsuarios()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("dialog_gerenciar_usuarios_loja")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Topo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BlueExpressive.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = BlueExpressive, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Colaboradores da Loja",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${loja.nome_loja} (${usuariosDaLoja.size} usuários)",
                                fontSize = 11.sp,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BlueExpressive)
                    }
                } else if (usuariosDaLoja.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum usuário cadastrado nesta loja.",
                            fontSize = 13.sp,
                            color = DarkTextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(usuariosDaLoja, key = { it.matricula }) { user ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                                border = BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when (user.cargo.lowercase()) {
                                                "master" -> Color(0xFF7E22CE)
                                                "adm" -> Color(0xFFD97706)
                                                else -> Color(0xFF1E3A8A)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = user.nome.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.nome,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (user.ativo) EmeraldSafe.copy(alpha = 0.2f) else RedExpressive.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = if (user.ativo) "ATIVO" else "INATIVO",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (user.ativo) EmeraldSafe else RedExpressive,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Mat: ${user.matricula} • ${user.cargo.uppercase()}",
                                                fontSize = 11.sp,
                                                color = DarkTextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Ações: Editar e Excluir
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { usuarioParaEditar = user },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = BlueExpressive, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = { usuarioParaExcluir = user },
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

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainerHigh),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar", color = Color.White)
                }
            }
        }
    }

    // Modal de Edição
    usuarioParaEditar?.let { user ->
        EditarUsuarioAdmDialog(
            usuario = user,
            lojasList = todasLojas,
            onDismiss = { usuarioParaEditar = null },
            onUsuarioSalvo = {
                usuarioParaEditar = null
                carregarUsuarios()
                onListaAlterada()
            }
        )
    }

    // Confirmação de Exclusão
    usuarioParaExcluir?.let { user ->
        AlertDialog(
            onDismissRequest = { usuarioParaExcluir = null },
            title = {
                Text(
                    text = "Excluir Usuário?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Deseja excluir permanentemente o usuário \"${user.nome}\" (Matrícula: ${user.matricula})?",
                    color = DarkTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isExcluindo = true
                        coroutineScope.launch {
                            try {
                                withTimeout(8000) {
                                    supabase.from("usuarios")
                                        .delete {
                                            eq("matricula", user.matricula)
                                        }
                                    Toast.makeText(context, "Usuário ${user.nome} excluído com sucesso.", Toast.LENGTH_SHORT).show()
                                    usuarioParaExcluir = null
                                    carregarUsuarios()
                                    onListaAlterada()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isExcluindo = false
                            }
                        }
                    },
                    enabled = !isExcluindo,
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    if (isExcluindo) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Excluir Usuário", color = Color.White, fontWeight = FontWeight.Bold)
                    }
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
}
