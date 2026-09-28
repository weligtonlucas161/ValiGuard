package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FeedbackManager
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.UserFeedback
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnviarFeedbackDialog(
    onDismiss: () -> Unit,
    onFeedbackEnviado: () -> Unit = {}
) {
    val context = LocalContext.current
    val usuario = SessionHolder.currentUser
    val loja = SessionHolder.currentLoja

    // Destino: "ADM" (Melhorias no App) ou "MASTER" (Melhorias na Empresa/Loja)
    var destinoSelecionado by remember { mutableStateOf("ADM") }
    var estrelas by remember { mutableIntStateOf(5) }
    var categoriaSelecionada by remember { mutableStateOf("Sugestão") }
    var mensagem by remember { mutableStateOf("") }
    var isEnviando by remember { mutableStateOf(false) }

    val isParaAdm = destinoSelecionado == "ADM"
    val corTema = if (isParaAdm) Color(0xFFF59E0B) else Color(0xFFA855F7)
    val corBotao = if (isParaAdm) Color(0xFFD97706) else Color(0xFF9333EA)

    val categorias = listOf(
        "Sugestão" to "💡 Sugestão",
        "Elogio" to "⭐ Elogio",
        "Problema" to "⚠️ Problema",
        "Dúvida" to "❓ Dúvida",
        "Outro" to "💬 Outro"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            border = BorderStroke(1.5.dp, corTema.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("dialog_enviar_feedback")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Topo com Título e Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = corTema.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, corTema),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isParaAdm) Icons.Default.PhoneAndroid else Icons.Default.Business,
                                    contentDescription = null,
                                    tint = corTema,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Enviar Feedback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isParaAdm) "Direcionado ao Administrador (ADM)" else "Direcionado ao Perfil Master da Loja",
                                fontSize = 11.sp,
                                color = corTema
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card de Identificação Anônima do Remetente
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceContainerHigh,
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Envio 100% Anônimo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Seu nome e matrícula NÃO serão divulgados. Apenas seu cargo e mensagem serão exibidos.",
                                fontSize = 10.sp,
                                color = DarkTextSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = corTema.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = (usuario?.cargo ?: "operador").uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = corTema,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SELETOR DE DESTINO: MELHORIAS NO APP vs MELHORIA NA EMPRESA
                Text(
                    text = "Para quem é o seu feedback?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Opção 1: Melhorias no App -> ADM
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isParaAdm) Color(0xFFF59E0B).copy(alpha = 0.2f) else DarkSurfaceContainerHigh,
                        border = BorderStroke(
                            1.5.dp,
                            if (isParaAdm) Color(0xFFF59E0B) else DarkBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { destinoSelecionado = "ADM" }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (isParaAdm) Color(0xFFFBBF24) else DarkTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Melhorias no App",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isParaAdm) Color(0xFFFDE68A) else DarkTextPrimary
                            )
                            Text(
                                text = "Vai para o ADM",
                                fontSize = 10.sp,
                                color = if (isParaAdm) Color(0xFFFBBF24) else DarkTextSecondary
                            )
                        }
                    }

                    // Opção 2: Melhorias na Empresa -> MASTER
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isParaAdm) Color(0xFFA855F7).copy(alpha = 0.2f) else DarkSurfaceContainerHigh,
                        border = BorderStroke(
                            1.5.dp,
                            if (!isParaAdm) Color(0xFFA855F7) else DarkBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { destinoSelecionado = "MASTER" }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = if (!isParaAdm) Color(0xFFA855F7) else DarkTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Melhoria na Empresa",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isParaAdm) Color(0xFFE9D5FF) else DarkTextPrimary
                            )
                            Text(
                                text = "Vai para o Master",
                                fontSize = 10.sp,
                                color = if (!isParaAdm) Color(0xFFA855F7) else DarkTextSecondary
                            )
                        }
                    }
                }

                // Dica explicativa do destino
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isParaAdm) {
                        "💡 Sugestões sobre o aplicativo Synk, novas funções, correções e suporte técnico."
                    } else {
                        "🏢 Sugestões para o dia a dia da loja, rotinas operacionais, organização e processos."
                    },
                    fontSize = 11.sp,
                    color = DarkTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Avaliação por Estrelas Interativa
                Text(
                    text = "Sua Avaliação:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { starIndex ->
                        val isSelected = starIndex <= estrelas
                        IconButton(
                            onClick = { estrelas = starIndex },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$starIndex Estrelas",
                                tint = if (isSelected) Color(0xFFFBBF24) else DarkTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Categorias
                Text(
                    text = "Tipo de Mensagem:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categorias.forEach { (chave, rotulo) ->
                        val selecionado = categoriaSelecionada == chave
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selecionado) corTema.copy(alpha = 0.25f) else DarkSurfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                if (selecionado) corTema else DarkBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { categoriaSelecionada = chave }
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

                // Campo de Texto da Mensagem
                OutlinedTextField(
                    value = mensagem,
                    onValueChange = { mensagem = it },
                    label = {
                        Text(
                            if (isParaAdm) "Sugestão ou problema no App"
                            else "Sugestão de melhoria na Empresa/Loja"
                        )
                    },
                    placeholder = {
                        Text(
                            if (isParaAdm) "Conte o que gostaria de ver no aplicativo Synk ou reporte um problema..."
                            else "Conte ideias para melhorar o ambiente de trabalho, rotinas ou infraestrutura da loja..."
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = corTema,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_mensagem_feedback")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botão Enviar
                Button(
                    onClick = {
                        val textoLimpo = mensagem.trim()
                        if (textoLimpo.isEmpty()) {
                            Toast.makeText(context, "Por favor, escreva uma mensagem antes de enviar.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isEnviando = true
                        val tipoSugestao = if (isParaAdm) "APP" else "EMPRESA"
                        val novoFeedback = UserFeedback(
                            id = UUID.randomUUID().toString(),
                            usuario_matricula = "ANONIMO",
                            usuario_nome = "Colaborador Anônimo",
                            usuario_cargo = usuario?.cargo ?: "operador",
                            loja_id = usuario?.loja_id ?: loja?.id ?: "",
                            loja_nome = loja?.nome_loja ?: "Loja Principal",
                            estrelas = estrelas,
                            categoria = categoriaSelecionada,
                            mensagem = textoLimpo,
                            status = "PENDENTE",
                            timestamp = System.currentTimeMillis(),
                            destino = destinoSelecionado,
                            tipo_sugestao = tipoSugestao
                        )
                        FeedbackManager.enviarFeedback(context, novoFeedback) {
                            isEnviando = false
                            val destinoNome = if (isParaAdm) "ao Administrador (ADM)" else "ao Gestor Master da Loja"
                            Toast.makeText(context, "Feedback enviado com sucesso $destinoNome!", Toast.LENGTH_LONG).show()
                            onFeedbackEnviado()
                            onDismiss()
                        }
                    },
                    enabled = !isEnviando,
                    colors = ButtonDefaults.buttonColors(containerColor = corBotao),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_enviar_feedback_submit")
                ) {
                    if (isEnviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isParaAdm) "Enviando ao ADM..." else "Enviando ao Master...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isParaAdm) "Enviar Feedback para o ADM" else "Enviar Sugestão para o Master",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
