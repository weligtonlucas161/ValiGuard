package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OnlineUserInfo
import com.example.data.TelemetryManager
import com.example.data.TelemetrySnapshot
import com.example.data.supabase.Loja
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

@Composable
fun AdmTelemetriaTab(
    lojasList: List<Loja>,
    usuariosList: List<Usuario> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var snapshot by remember { mutableStateOf<TelemetrySnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun atualizarTelemetria() {
        isLoading = true
        coroutineScope.launch {
            val dados = TelemetryManager.coletarTelemetria(context, lojasList, usuariosList)
            snapshot = dados
            isLoading = false
        }
    }

    LaunchedEffect(lojasList, usuariosList) {
        atualizarTelemetria()
    }

    // Animação de pulso para indicador de online
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("adm_telemetria_tab")
    ) {
        // Cabeçalho da Telemetria
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldSafe.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = EmeraldSafe,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Dashboard de Telemetria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Monitoramento do servidor e tráfego simultâneo",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                }
            }

            IconButton(onClick = { atualizarTelemetria() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = EmeraldSafe)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading && snapshot == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldSafe)
            }
        } else {
            val dados = snapshot ?: return

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card Hero: Usuários em Simultâneo
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                        border = BorderStroke(2.dp, EmeraldSafe.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSafe)
                                            .alpha(pulseAlpha)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AO VIVO NO SERVIDOR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSafe,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldSafe.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${dados.latenciaServidorMs} ms • ${dados.statusConexao}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = EmeraldSafe,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${dados.usuariosSimultaneos}",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    lineHeight = 44.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                    Text(
                                        text = if (dados.usuariosSimultaneos == 1) "Usuário Conectado Simultaneamente" else "Usuários Conectados Simultaneamente",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA7F3D0)
                                    )
                                    Text(
                                        text = "Com atividade ou presença nos últimos 10 minutos",
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Grid de Métricas Auxiliares
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Lojas Conectadas (Ligadas vs Desligadas)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = BlueExpressive,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Lojas Ativas", fontSize = 11.sp, color = DarkTextSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${dados.totalLojasAtivas}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSafe
                                    )
                                    Text(
                                        text = " / ${dados.totalLojasAtivas + dados.totalLojasInativas}",
                                        fontSize = 13.sp,
                                        color = DarkTextSecondary,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                if (dados.totalLojasInativas > 0) {
                                    Text(
                                        text = "${dados.totalLojasInativas} desligada(s)",
                                        fontSize = 10.sp,
                                        color = RedExpressive,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text("100% das lojas ligadas", fontSize = 10.sp, color = EmeraldSafe)
                                }
                            }
                        }

                        // Card 2: Total de Usuários Cadastrados
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Total Cadastros", fontSize = 11.sp, color = DarkTextSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${dados.totalUsuariosCadastrados}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${dados.requisicoesRecentes} logs no banco",
                                    fontSize = 10.sp,
                                    color = DarkTextSecondary
                                )
                            }
                        }
                    }
                }

                // Lista de Usuários Online Agora
                item {
                    Text(
                        text = "Colaboradores Ativos Agora (${dados.usuariosOnline.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                }

                if (dados.usuariosOnline.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum usuário com atividade recente registrada nos últimos minutos.",
                                    fontSize = 12.sp,
                                    color = DarkTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(dados.usuariosOnline, key = { it.matricula }) { user ->
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (user.cargo.equals("master", true)) Color(0xFF7E22CE) else Color(0xFF1E3A8A),
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
                                                color = DarkSurfaceContainerHigh
                                            ) {
                                                Text(
                                                    text = user.cargo.uppercase(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (user.cargo.equals("master", true)) Color(0xFFC084FC) else Color(0xFF93C5FD),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${user.lojaNome} • Mat: ${user.matricula}",
                                            fontSize = 10.sp,
                                            color = DarkTextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSafe)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (user.minutosAtras == 0L) "Online agora" else "Há ${user.minutosAtras} min",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (user.minutosAtras == 0L) EmeraldSafe else DarkTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Distribuição por Loja
                item {
                    Text(
                        text = "Distribuição de Usuários por Loja",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val maxUsers = dados.distribuicaoPorLoja.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                            dados.distribuicaoPorLoja.forEach { (nomeLoja, count) ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(nomeLoja, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text("$count colaboradores", fontSize = 11.sp, color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { count.toFloat() / maxUsers },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = BlueExpressive,
                                        trackColor = DarkSurfaceContainerHigh,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
