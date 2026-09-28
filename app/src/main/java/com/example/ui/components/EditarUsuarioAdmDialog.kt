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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.supabase.AtualizarUsuarioCompleto
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarUsuarioAdmDialog(
    usuario: Usuario,
    lojasList: List<Loja>,
    onDismiss: () -> Unit,
    onUsuarioSalvo: (Usuario) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }

    var nome by remember { mutableStateOf(usuario.nome) }
    var cargo by remember { mutableStateOf(usuario.cargo.lowercase()) }
    var lojaSelecionadaId by remember { mutableStateOf(usuario.loja_id) }
    var ativo by remember { mutableStateOf(usuario.ativo) }
    var isSalvando by remember { mutableStateOf(false) }

    var cargoDropdownExpanded by remember { mutableStateOf(false) }
    var lojaDropdownExpanded by remember { mutableStateOf(false) }

    val cargosDisponiveis = listOf("operador", "master", "adm")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("dialog_editar_usuario_adm")
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
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = BlueExpressive,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Editar Usuário",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Matrícula: ${usuario.matricula}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nome
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome Completo") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Seletor de Cargo
                ExposedDropdownMenuBox(
                    expanded = cargoDropdownExpanded,
                    onExpandedChange = { cargoDropdownExpanded = !cargoDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = cargo.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cargo / Permissão") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cargoDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = cargoDropdownExpanded,
                        onDismissRequest = { cargoDropdownExpanded = false },
                        modifier = Modifier.background(DarkSurfaceContainerHigh)
                    ) {
                        cargosDisponiveis.forEach { c ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (c) {
                                            "master" -> "Master (Gerente da Loja)"
                                            "operador" -> "Operador (Coletor de Dados)"
                                            "adm" -> "Administrador Geral (ADM)"
                                            else -> c.uppercase()
                                        },
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    cargo = c
                                    cargoDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Seletor de Loja
                val lojaAtual = lojasList.find { it.id == lojaSelecionadaId }
                ExposedDropdownMenuBox(
                    expanded = lojaDropdownExpanded,
                    onExpandedChange = { lojaDropdownExpanded = !lojaDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = lojaAtual?.nome_loja ?: "Selecione a Loja...",
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
                    )
                    ExposedDropdownMenu(
                        expanded = lojaDropdownExpanded,
                        onDismissRequest = { lojaDropdownExpanded = false },
                        modifier = Modifier.background(DarkSurfaceContainerHigh)
                    ) {
                        lojasList.forEach { l ->
                            DropdownMenuItem(
                                text = {
                                    Text(l.nome_loja, fontWeight = FontWeight.Medium, color = Color.White)
                                },
                                onClick = {
                                    lojaSelecionadaId = l.id
                                    lojaDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Ativo / Inativo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceContainerHigh, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (ativo) "Usuário Ativo" else "Usuário Inativo",
                            fontWeight = FontWeight.Bold,
                            color = if (ativo) EmeraldSafe else RedExpressive,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (ativo) "Pode realizar login normalmente" else "Acesso ao app bloqueado",
                            fontSize = 11.sp,
                            color = DarkTextSecondary
                        )
                    }
                    Switch(
                        checked = ativo,
                        onCheckedChange = { ativo = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldSafe,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = RedExpressive.copy(alpha = 0.4f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botão Salvar
                Button(
                    onClick = {
                        val cleanNome = nome.trim()
                        if (cleanNome.isEmpty()) {
                            Toast.makeText(context, "O nome não pode ficar vazio.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSalvando = true
                        coroutineScope.launch {
                            try {
                                withTimeout(8000) {
                                    val updatePayload = AtualizarUsuarioCompleto(
                                        nome = cleanNome,
                                        cargo = cargo,
                                        loja_id = lojaSelecionadaId,
                                        ativo = ativo
                                    )
                                    supabase.from("usuarios")
                                        .update(updatePayload) {
                                            eq("matricula", usuario.matricula)
                                        }

                                    val usuarioAtualizado = usuario.copy(
                                        nome = cleanNome,
                                        cargo = cargo,
                                        loja_id = lojaSelecionadaId,
                                        ativo = ativo
                                    )
                                    Toast.makeText(context, "Usuário atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                                    onUsuarioSalvo(usuarioAtualizado)
                                    onDismiss()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSalvando = false
                            }
                        }
                    },
                    enabled = !isSalvando,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSalvando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salvando...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salvar Alterações", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
