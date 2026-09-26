package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SingleSessionManager
import com.example.data.SessionValidationResult
import com.example.data.supabase.Loja
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseException
import com.example.data.supabase.Usuario
import com.example.util.decrypted
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun MatriculaLoginScreen(
    onLoginSuccess: (Usuario) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }

    var matricula by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var alertTitle by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var detectedCargo by remember { mutableStateOf<String?>(null) }
    var detectedNome by remember { mutableStateOf<String?>(null) }

    // Detecção dinâmica de cargo para mudar detalhes visuais (Amarelo = ADM, Roxo = Master, Azul = Comum)
    LaunchedEffect(matricula) {
        if (matricula.length >= 3) {
            val quickCargo = when (matricula) {
                "111111" -> "adm"
                "123456" -> "master"
                "654321" -> "operador"
                else -> null
            }
            if (quickCargo != null) {
                detectedCargo = quickCargo
            } else if (matricula.length == 6) {
                try {
                    val u = supabase.from("usuarios")
                        .select { eq("matricula", matricula) }
                        .decodeSingleOrNull<Usuario>()
                        ?.decrypted()
                    detectedCargo = u?.cargo
                    detectedNome = u?.nome
                } catch (e: Exception) {
                    // ignora erro de prefetch
                }
            }
        } else {
            detectedCargo = null
            detectedNome = null
        }
    }

    val isAdm = detectedCargo.equals("adm", ignoreCase = true)
    val isMaster = detectedCargo.equals("master", ignoreCase = true)

    val accentColor = when {
        isAdm -> Color(0xFFF59E0B) // Amarelo para ADM
        isMaster -> Color(0xFFA855F7) // Roxo para Master
        else -> BlueExpressive // Normal (Azul) para comum / operador
    }

    val accentContainer = when {
        isAdm -> Color(0xFF78350F).copy(alpha = 0.35f)
        isMaster -> Color(0xFF581C87).copy(alpha = 0.35f)
        else -> BlueExpressiveContainer
    }

    val accentLight = when {
        isAdm -> Color(0xFFFDE68A)
        isMaster -> Color(0xFFE9D5FF)
        else -> Color(0xFF93C5FD)
    }

    fun processMatriculaSearch(digits: String) {
        if (digits.length != 6 || isSearching) return

        isSearching = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    // Consulta direta na tabela 'usuarios' via Postgrest
                    val usuario = supabase.from("usuarios")
                        .select { eq("matricula", digits) }
                        .decodeSingleOrNull<Usuario>()
                        ?.decrypted()

                    if (usuario == null) {
                        alertTitle = "Acesso Negado"
                        alertMessage = "Matrícula $digits não encontrada no banco de dados."
                    } else if (!usuario.ativo) {
                        alertTitle = "Usuário Desativado"
                        alertMessage = "O usuário \"${usuario.nome}\" (Matrícula: $digits) está inativo no sistema."
                    } else {
                        // Validação de Sessão Única: impede se já houver login ativo em outro aparelho
                        val sessionCheck = SingleSessionManager.checkCanLogin(usuario.matricula)
                        if (sessionCheck is SessionValidationResult.Blocked) {
                            alertTitle = "Login Bloqueado (Sessão Ativa)"
                            alertMessage = "O usuário \"${sessionCheck.usuarioNome}\" (Matrícula: $digits) já possui uma sessão ativa em outro dispositivo.\n\nPara a sua segurança operacional, não é permitido manter dois logins ativos ao mesmo tempo. Encerre o aplicativo no outro aparelho para poder acessar aqui."
                            matricula = ""
                            return@withTimeout
                        }

                        // Buscar loja vinculada se existir
                        val loja = try {
                            supabase.from("lojas")
                                .select { eq("id", usuario.loja_id) }
                                .decodeSingleOrNull<Loja>()
                                ?.decrypted()
                        } catch (e: Exception) {
                            null
                        }

                        // Inicia a sessão ativa única com heartbeat periódico
                        SingleSessionManager.startSession(usuario.matricula, usuario.nome, usuario.loja_id)

                        // Salvar na sessão local
                        SessionHolder.setSession(usuario, loja)
                        com.example.data.supabase.LogManager.recordLog(
                            usuarioMatricula = usuario.matricula,
                            usuarioNome = usuario.nome,
                            lojaId = usuario.loja_id,
                            acao = "LOGIN",
                            detalhes = "Acesso ao sistema via matrícula (${usuario.cargo.uppercase()}) [Sessão Única Ativa]"
                        )
                        onLoginSuccess(usuario)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                alertTitle = "Tempo Limite (8s)"
                alertMessage = "O servidor demorou mais de 8s para responder. Verifique sua conexão com a internet."
            } catch (e: SupabaseException) {
                alertTitle = "Erro do Banco"
                alertMessage = e.message ?: "Erro desconhecido retornado pelo servidor."
            } catch (e: Exception) {
                alertTitle = "Erro na Conexão"
                alertMessage = "Falha ao consultar usuário: ${e.localizedMessage ?: "Erro de rede"}"
            } finally {
                isSearching = false
            }
        }
    }

    // Trigger automático ao digitar exatamente 6 dígitos
    LaunchedEffect(matricula) {
        if (matricula.length == 6 && !isSearching) {
            processMatriculaSearch(matricula)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon - ValiGuard
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = DarkSurfaceContainerHigh,
                border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_valiguard_logo),
                        contentDescription = "Logo ValiGuard",
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ValiGuard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DarkTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Controle Inteligente de Validades, Estoque e Auditorias",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Main Card: Login por Matrícula
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                border = BorderStroke(1.dp, if (detectedCargo != null) accentColor.copy(alpha = 0.6f) else DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acesso por Matrícula",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Informe sua matrícula de 6 dígitos para autenticação automática.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                    )

                    // Campo de Matrícula (Único campo sem redundância)
                    OutlinedTextField(
                        value = matricula,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                matricula = input
                            }
                        },
                        label = { Text("Matrícula Funcional (6 dígitos)") },
                        placeholder = { Text("Ex: 123456", color = DarkTextSecondary) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (matricula.isNotEmpty()) accentColor else DarkTextSecondary
                            )
                        },
                        trailingIcon = {
                            if (matricula.isNotEmpty()) {
                                IconButton(onClick = { matricula = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpar",
                                        tint = DarkTextSecondary
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = accentColor,
                            cursorColor = accentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_matricula")
                    )

                    // Badge de identificação de cargo detectado (Amarelo para ADM, Roxo para Master, Azul para Comum)
                    if (detectedCargo != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentContainer,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        isAdm -> Icons.Default.AdminPanelSettings
                                        isMaster -> Icons.Default.Storefront
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = when {
                                            isAdm -> "Perfil Administrador Geral (ADM)"
                                            isMaster -> "Perfil Gerente da Loja (MASTER)"
                                            else -> "Perfil Operador da Loja"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = accentLight
                                    )
                                    if (!detectedNome.isNullOrBlank()) {
                                        Text(
                                            text = detectedNome ?: "",
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Loading State indicator
                    if (isSearching) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = accentColor,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Autenticando matrícula...",
                                color = accentLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Security note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EmeraldSafe,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ambiente Seguro • Acesso Criptografado",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }

    // Alerta na tela para usuário não encontrado, erro ou inativo
    if (alertMessage != null) {
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (alertTitle.contains("Erro")) RedExpressive else Color(0xFFF59E0B),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = alertTitle,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = alertMessage ?: "",
                    color = DarkTextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        alertMessage = null
                        matricula = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
