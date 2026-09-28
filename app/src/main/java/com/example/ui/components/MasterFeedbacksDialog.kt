package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FeedbackManager
import com.example.data.supabase.UserFeedback
import com.example.ui.theme.DarkBorder
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasterFeedbacksDialog(
    masterLojaId: String,
    masterLojaNome: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var feedbacksList by remember { mutableStateOf<List<UserFeedback>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filtroSelecionado by remember { mutableStateOf("TODOS") } // "TODOS", "PENDENTES", "RESOLVIDOS"

    var feedbackParaResponder by remember { mutableStateOf<UserFeedback?>(null) }
    var respostaTexto by remember { mutableStateOf("") }
    var feedbackParaExcluir by remember { mutableStateOf<UserFeedback?>(null) }

    fun recarregarFeedbacks() {
        isLoading = true
        coroutineScope.launch {
            FeedbackManager.carregarFeedbacksComSupabase(context)
            feedbacksList = FeedbackManager.getFeedbacksParaMaster(context, masterLojaId)
            isLoading = false
        }
    }

    LaunchedEffect(masterLojaId) {
        recarregarFeedbacks()
    }

    val feedbacksFiltrados = remember(feedbacksList, filtroSelecionado) {
        when (filtroSelecionado) {
            "PENDENTES" -> feedbacksList.filter { it.status.equals("PENDENTE", ignoreCase = true) }
            "RESOLVIDOS" -> feedbacksList.filter { it.status.equals("RESOLVIDO", ignoreCase = true) }
            else -> feedbacksList
        }
    }

    val totalPendentes = remember(feedbacksList) {
        feedbacksList.count { it.status.equals("PENDENTE", ignoreCase = true) }
    }

    // Modal de Resposta do Master
    if (feedbackParaResponder != null) {
        val fb = feedbackParaResponder!!
        AlertDialog(
            onDismissRequest = { feedbackParaResponder = null },
            icon = {
                Icon(Icons.Default.Reply, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Responder à Sugestão", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        text = "Cargo: ${fb.usuario_cargo.uppercase()} (Anônimo)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFC084FC)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${fb.mensagem}\"",
                            fontSize = 12.sp,
                            color = DarkTextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = respostaTexto,
                        onValueChange = { respostaTexto = it },
                        label = { Text("Resposta da Gestão Master") },
                        placeholder = { Text("Escreva uma resposta para o colaborador...") },
                        minLines = 3,
                        maxLines = 5,
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
                        val txt = respostaTexto.trim()
                        if (txt.isNotBlank()) {
                            FeedbackManager.atualizarStatusFeedback(
                                context,
                                fb.id,
                                "RESOLVIDO",
                                "Resposta do Master: $txt"
                            )
                            Toast.makeText(context, "Resposta enviada e sugestão resolvida!", Toast.LENGTH_SHORT).show()
                            feedbackParaResponder = null
                            recarregarFeedbacks()
                        } else {
                            Toast.makeText(context, "Digite uma resposta antes de confirmar.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("Salvar e Resolver", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { feedbackParaResponder = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal de Confirmação de Exclusão
    if (feedbackParaExcluir != null) {
        val fb = feedbackParaExcluir!!
        AlertDialog(
            onDismissRequest = { feedbackParaExcluir = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RedExpressive, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Excluir Sugestão?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    text = "Deseja remover esta sugestão enviada por colaborador (${fb.usuario_cargo.uppercase()})? Esta ação é permanente.",
                    color = DarkTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FeedbackManager.excluirFeedback(context, fb.id)
                        Toast.makeText(context, "Feedback excluído.", Toast.LENGTH_SHORT).show()
                        feedbackParaExcluir = null
                        recarregarFeedbacks()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Sim, Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { feedbackParaExcluir = null }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("dialog_master_feedbacks")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Topo com Título e Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF581C87).copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFFA855F7)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sugestões para a Empresa / Loja",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Caixa de Entrada do Master • $masterLojaNome",
                                fontSize = 11.sp,
                                color = Color(0xFFC084FC)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { recarregarFeedbacks() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recarregar", tint = DarkTextSecondary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resumo do Master
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Recebidos", fontSize = 11.sp, color = DarkTextSecondary)
                            Text(
                                text = "${feedbacksList.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Pendentes", fontSize = 11.sp, color = DarkTextSecondary)
                            Text(
                                text = "$totalPendentes",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPendentes > 0) Color(0xFFFBBF24) else EmeraldSafe
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Resolvidos", fontSize = 11.sp, color = DarkTextSecondary)
                            Text(
                                text = "${feedbacksList.count { it.status.equals("RESOLVIDO", ignoreCase = true) }}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSafe
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filtros rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("TODOS", "PENDENTES", "RESOLVIDOS").forEach { f ->
                        val isSelected = filtroSelecionado == f
                        FilterChip(
                            selected = isSelected,
                            onClick = { filtroSelecionado = f },
                            label = {
                                Text(
                                    text = when (f) {
                                        "PENDENTES" -> "Pendentes ($totalPendentes)"
                                        "RESOLVIDOS" -> "Resolvidos"
                                        else -> "Todos (${feedbacksList.size})"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
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

                // Lista de Feedbacks
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFA855F7))
                    }
                } else if (feedbacksFiltrados.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = DarkTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nenhuma sugestão encontrada para a empresa.",
                                color = DarkTextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Sugestões de melhoria enviadas pelos operadores da loja aparecerão aqui.",
                                color = DarkTextSecondary.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(feedbacksFiltrados, key = { it.id }) { fb ->
                            val isPendente = fb.status.equals("PENDENTE", ignoreCase = true)
                            val isResolvido = fb.status.equals("RESOLVIDO", ignoreCase = true)

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPendente) Color(0xFFFBBF24).copy(alpha = 0.5f) else DarkBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Cabeçalho do Card
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF581C87).copy(alpha = 0.5f),
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Security,
                                                        contentDescription = null,
                                                        tint = Color(0xFFC084FC),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF581C87).copy(alpha = 0.4f),
                                                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = "Cargo: ${fb.usuario_cargo.uppercase()}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE9D5FF),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        // Status
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when {
                                                isResolvido -> EmeraldSafe.copy(alpha = 0.15f)
                                                isPendente -> Color(0xFFFBBF24).copy(alpha = 0.15f)
                                                else -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = fb.status.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isResolvido -> EmeraldSafe
                                                    isPendente -> Color(0xFFFBBF24)
                                                    else -> Color(0xFF60A5FA)
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Estrelas e Categoria
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        (1..5).forEach { starIndex ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (starIndex <= fb.estrelas) Color(0xFFFBBF24) else DarkTextSecondary.copy(alpha = 0.3f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${fb.categoria}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFDE68A)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        val dataFormatada = remember(fb.timestamp) {
                                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fb.timestamp))
                                        }
                                        Text(
                                            text = dataFormatada,
                                            fontSize = 10.sp,
                                            color = DarkTextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Conteúdo da Mensagem
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = fb.mensagem,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    // Resposta se houver
                                    if (!fb.resposta_adm.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF581C87).copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "Resposta da Gestão:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC084FC)
                                                )
                                                Text(
                                                    text = fb.resposta_adm,
                                                    fontSize = 11.sp,
                                                    color = DarkTextPrimary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Ações do Master
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { feedbackParaExcluir = fb },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RedExpressive, modifier = Modifier.size(16.dp))
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        if (isPendente) {
                                            OutlinedButton(
                                                onClick = {
                                                    FeedbackManager.atualizarStatusFeedback(context, fb.id, "LIDO")
                                                    recarregarFeedbacks()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA))
                                            ) {
                                                Text("Marcar Lido", fontSize = 11.sp)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Button(
                                            onClick = {
                                                respostaTexto = fb.resposta_adm ?: ""
                                                feedbackParaResponder = fb
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                                        ) {
                                            Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isResolvido) "Editar Resposta" else "Responder / Resolver", fontSize = 11.sp)
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
