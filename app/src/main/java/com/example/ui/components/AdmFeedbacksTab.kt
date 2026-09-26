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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FeedbackManager
import com.example.data.supabase.UserFeedback
import com.example.ui.theme.BlueExpressive
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
fun AdmFeedbacksTab(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var feedbacksList by remember { mutableStateOf<List<UserFeedback>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filtroSelecionado by remember { mutableStateOf("TODOS") } // "TODOS", "PENDENTES", "ELOGIOS", "SUGESTOES", "PROBLEMAS"

    var feedbackParaResponder by remember { mutableStateOf<UserFeedback?>(null) }
    var respostaTexto by remember { mutableStateOf("") }
    var feedbackParaExcluir by remember { mutableStateOf<UserFeedback?>(null) }

    fun recarregarFeedbacks() {
        isLoading = true
        coroutineScope.launch {
            val lista = FeedbackManager.carregarFeedbacksComSupabase(context)
            // No painel administrativo, exibe exclusivamente feedbacks de melhorias para o APP
            feedbacksList = lista.filter {
                it.destino.equals("ADM", ignoreCase = true) || it.tipo_sugestao.equals("APP", ignoreCase = true)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        recarregarFeedbacks()
    }

    val feedbacksFiltrados = remember(feedbacksList, filtroSelecionado) {
        when (filtroSelecionado) {
            "PENDENTES" -> feedbacksList.filter { it.status == "PENDENTE" }
            "ELOGIOS" -> feedbacksList.filter { it.categoria.contains("Elogio", ignoreCase = true) }
            "SUGESTOES" -> feedbacksList.filter { it.categoria.contains("Sugestão", ignoreCase = true) }
            "PROBLEMAS" -> feedbacksList.filter { it.categoria.contains("Problema", ignoreCase = true) }
            else -> feedbacksList
        }
    }

    val totalPendentes = remember(feedbacksList) { feedbacksList.count { it.status == "PENDENTE" } }
    val mediaEstrelas = remember(feedbacksList) {
        if (feedbacksList.isEmpty()) 5.0
        else feedbacksList.map { it.estrelas }.average()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("adm_feedbacks_tab")
    ) {
        // Card Resumo no Topo
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Central de Feedbacks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recebidos de colaboradores e gerentes de todas as lojas",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", mediaEstrelas),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFFDE68A)
                            )
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${feedbacksList.size} recebidos (${totalPendentes} pendentes)",
                            fontSize = 10.sp,
                            color = DarkTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { recarregarFeedbacks() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recarregar", tint = Color(0xFFFBBF24))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Banner Exclusivo: Apenas Melhorias do App no Painel ADM
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF78350F).copy(alpha = 0.25f),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exclusivo: Sugestões e melhorias técnicas do Aplicativo.",
                    fontSize = 11.sp,
                    color = Color(0xFFFDE68A),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filtros Rápidos em Chips
        val filtros = listOf(
            "TODOS" to "Todos (${feedbacksList.size})",
            "PENDENTES" to "Pendentes ($totalPendentes)",
            "ELOGIOS" to "⭐ Elogios",
            "SUGESTOES" to "💡 Sugestões",
            "PROBLEMAS" to "⚠️ Problemas"
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            filtros.forEach { (chave, rotulo) ->
                val selecionado = filtroSelecionado == chave
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selecionado) Color(0xFFD97706) else DarkSurfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        if (selecionado) Color(0xFFFBBF24) else DarkBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { filtroSelecionado = chave }
                ) {
                    Text(
                        text = rotulo,
                        fontSize = 11.sp,
                        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                        color = if (selecionado) Color.White else DarkTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lista de Feedbacks
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFBBF24))
            }
        } else if (feedbacksFiltrados.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = null,
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum feedback encontrado neste filtro.",
                            color = DarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(feedbacksFiltrados, key = { it.id }) { feedback ->
                    CardFeedbackItem(
                        feedback = feedback,
                        onMarcarLido = {
                            FeedbackManager.atualizarStatusFeedback(context, feedback.id, "LIDO")
                            recarregarFeedbacks()
                        },
                        onMarcarResolvido = {
                            FeedbackManager.atualizarStatusFeedback(context, feedback.id, "RESOLVIDO")
                            recarregarFeedbacks()
                        },
                        onResponder = {
                            feedbackParaResponder = feedback
                            respostaTexto = feedback.resposta_adm ?: ""
                        },
                        onExcluir = {
                            feedbackParaExcluir = feedback
                        }
                    )
                }
            }
        }
    }

    // Modal de Resposta do ADM
    feedbackParaResponder?.let { fb ->
        AlertDialog(
            onDismissRequest = { feedbackParaResponder = null },
            title = {
                Text(
                    text = "Responder ao Feedback",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Cargo: ${fb.usuario_cargo.uppercase()} (Anônimo)",
                        fontSize = 12.sp,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "\"${fb.mensagem}\"",
                        fontSize = 12.sp,
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = respostaTexto,
                        onValueChange = { respostaTexto = it },
                        label = { Text("Resposta Oficial do ADM") },
                        placeholder = { Text("Ex: Agradecemos o feedback! A funcionalidade foi aprimorada.") },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = BlueExpressive,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val resp = respostaTexto.trim()
                        FeedbackManager.atualizarStatusFeedback(
                            context = context,
                            feedbackId = fb.id,
                            novoStatus = "RESOLVIDO",
                            respostaAdm = if (resp.isNotBlank()) resp else null
                        )
                        Toast.makeText(context, "Resposta registrada e status atualizado para Resolvido!", Toast.LENGTH_SHORT).show()
                        feedbackParaResponder = null
                        recarregarFeedbacks()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe)
                ) {
                    Text("Salvar Resposta", color = Color.White, fontWeight = FontWeight.Bold)
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

    // Modal de Exclusão
    feedbackParaExcluir?.let { fb ->
        AlertDialog(
            onDismissRequest = { feedbackParaExcluir = null },
            title = {
                Text("Excluir Feedback?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    text = "Deseja remover permanentemente esta sugestão enviada por colaborador (${fb.usuario_cargo.uppercase()})?",
                    color = DarkTextSecondary,
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
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
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
}

@Composable
private fun CardFeedbackItem(
    feedback: UserFeedback,
    onMarcarLido: () -> Unit,
    onMarcarResolvido: () -> Unit,
    onResponder: () -> Unit,
    onExcluir: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()) }
    val dataFormatada = remember(feedback.timestamp) { dateFormat.format(Date(feedback.timestamp)) }

    val corCategoria = when (feedback.categoria.lowercase()) {
        "elogio" -> EmeraldSafe
        "problema" -> RedExpressive
        "sugestão", "sugestao" -> Color(0xFFF59E0B)
        else -> BlueExpressive
    }

    val corStatus = when (feedback.status) {
        "PENDENTE" -> Color(0xFFF59E0B)
        "LIDO" -> BlueExpressive
        "RESOLVIDO" -> EmeraldSafe
        else -> DarkTextSecondary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
        border = BorderStroke(1.dp, if (feedback.status == "PENDENTE") Color(0xFFF59E0B).copy(alpha = 0.6f) else DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabeçalho: Autor + Estrelas + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Feedback Anônimo",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF78350F).copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Cargo: ${feedback.usuario_cargo.uppercase()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE68A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${feedback.loja_nome.ifBlank { "Loja vinculada" }} • $dataFormatada",
                            fontSize = 10.sp,
                            color = DarkTextSecondary
                        )
                    }
                }

                // Estrelas
                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..5).forEach { i ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= feedback.estrelas) Color(0xFFFBBF24) else DarkBorder,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges: Categoria + Status + Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = corCategoria.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = feedback.categoria,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = corCategoria,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF78350F).copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "📱 APP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = corStatus.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = feedback.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = corStatus,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = dataFormatada,
                    fontSize = 10.sp,
                    color = DarkTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mensagem do Usuário
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feedback.mensagem,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Resposta do ADM (se houver)
            if (!feedback.resposta_adm.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSafe.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Resposta do ADM:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSafe
                        )
                        Text(
                            text = feedback.resposta_adm,
                            fontSize = 12.sp,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ações do ADM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExcluir, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RedExpressive.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedButton(
                    onClick = onResponder,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BlueExpressive.copy(alpha = 0.6f)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Reply, contentDescription = null, tint = BlueExpressive, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Responder", fontSize = 11.sp, color = BlueExpressive)
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (feedback.status != "RESOLVIDO") {
                    Button(
                        onClick = onMarcarResolvido,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resolver", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldSafe.copy(alpha = 0.2f),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resolvido", fontSize = 11.sp, color = EmeraldSafe, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
