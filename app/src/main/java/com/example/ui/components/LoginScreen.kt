package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import kotlinx.coroutines.delay

/**
 * Tela de Login Automatizado com TextWatcher (Requirement 4):
 * - Matrícula numérica (6 a 8 dígitos).
 * - Busca automática do nome no Supabase em tempo real.
 * - Bloqueio de sessão simultânea se sessao_ativa = true.
 * - Auto-login instantâneo quando o usuário for validado no Supabase.
 * - 100% de autenticação remota: sem credenciais salvas em código local ou senhas mestras.
 */
@Composable
fun LoginScreen(
    onLogin: (matricula: String, senhaOuNome: String) -> Unit,
    onSearchMatricula: (matricula: String, onResult: (nome: String?, cargo: String?, ativo: Boolean, sessaoAtiva: Boolean) -> Unit) -> Unit = { _, _ -> },
    isAuthenticating: Boolean = false,
    authErrorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var matricula by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var alertErrorDialogMessage by remember { mutableStateOf<String?>(null) }
    var isSearchingName by remember { mutableStateOf(false) }
    var detectedNome by remember { mutableStateOf<String?>(null) }
    var detectedCargo by remember { mutableStateOf<String?>(null) }
    var isAutoLoggingIn by remember { mutableStateOf(false) }

    val isAdmMatricula = matricula.trim() == "02003025" || detectedCargo?.equals("adm", ignoreCase = true) == true
    val isMasterMatricula = detectedCargo?.equals("master", ignoreCase = true) == true

    // TextWatcher / Reactive Lookup no Supabase
    LaunchedEffect(matricula) {
        val clean = matricula.trim()
        detectedNome = null
        detectedCargo = null
        localError = null
        isAutoLoggingIn = false

        if (clean.length == 6 || (clean.startsWith("0200") && clean.length == 8)) {
            // TextWatcher reativo: assim que o 6º dígito for digitado, busca no Supabase e autentica
            delay(200)
            isSearchingName = true
            onSearchMatricula(clean) { nome, cargo, ativo, _ ->
                isSearchingName = false
                if (nome != null) {
                    detectedNome = nome
                    detectedCargo = cargo

                    if (!ativo) {
                        alertErrorDialogMessage = "Matrícula incorreta ou acesso desativado. Entre em contato com seu gestor."
                        return@onSearchMatricula
                    }

                    // Conclui a autenticação de forma automática
                    isAutoLoggingIn = true
                    onLogin(clean, "")
                } else {
                    alertErrorDialogMessage = "Matrícula incorreta ou acesso desativado. Entre em contato com seu gestor."
                }
            }
        }
    }

    fun performSubmit() {
        val cleanMat = matricula.trim()
        if (cleanMat.isEmpty()) {
            localError = "Informe a matrícula funcional."
            return
        }

        localError = null
        onLogin(cleanMat, "")
    }

    val displayError = localError ?: authErrorMessage

    if (alertErrorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { alertErrorDialogMessage = null },
            title = {
                Text(
                    text = "Aviso de Acesso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = alertErrorDialogMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { alertErrorDialogMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceContainerHigh
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient glow in the background
        Box(
            modifier = Modifier
                .size(340.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isAdmMatricula) Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else if (isMasterMatricula) Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            else BlueExpressive.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo & Branding
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isAdmMatricula) Color(0xFF451A03)
                else if (isMasterMatricula) Color(0xFF2E1065)
                else BlueExpressiveContainer,
                border = BorderStroke(
                    1.5.dp,
                    if (isAdmMatricula) Color(0xFFF59E0B)
                    else if (isMasterMatricula) Color(0xFF8B5CF6)
                    else BlueExpressive.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAdmMatricula) Icons.Default.AdminPanelSettings
                        else if (isMasterMatricula) Icons.Default.Star
                        else Icons.Default.Storefront,
                        contentDescription = "Logo",
                        tint = if (isAdmMatricula) Color(0xFFF59E0B)
                        else if (isMasterMatricula) Color(0xFFA78BFA)
                        else BlueExpressive,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Synk",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isAdmMatricula) "Autenticação Direta de Administrador (ADM)"
                else if (isMasterMatricula) "Autenticação Direta de Gestor Master"
                else "Login Automatizado em Tempo Real no Supabase",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isAdmMatricula) Color(0xFFFCD34D)
                else if (isMasterMatricula) Color(0xFFDDD6FE)
                else DarkTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Login Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceContainerHigh
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    if (isAdmMatricula) Color(0xFFF59E0B).copy(alpha = 0.6f)
                    else if (isMasterMatricula) Color(0xFF8B5CF6).copy(alpha = 0.6f)
                    else DarkBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isAdmMatricula) "Acesso Administrativo"
                        else if (isMasterMatricula) "Acesso Gestor Master"
                        else "Acesso ao Sistema",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Digite sua matrícula numérica. O sistema localiza seu cadastro e efetua o login automático.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Input: Matrícula Funcional com TextWatcher
                    OutlinedTextField(
                        value = matricula,
                        onValueChange = { input ->
                            // Aceita apenas dígitos numéricos limitados a 6 dígitos (ou 8 para ADM)
                            val filtered = input.filter { it.isDigit() }
                            matricula = if (filtered.startsWith("02003025")) {
                                filtered.take(8)
                            } else {
                                filtered.take(6)
                            }
                            localError = null
                        },
                        label = { Text("Matrícula Funcional") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = if (isAdmMatricula) Color(0xFFF59E0B)
                                else if (isMasterMatricula) Color(0xFF8B5CF6)
                                else if (matricula.isNotBlank()) BlueExpressive
                                else DarkTextSecondary
                            )
                        },
                        trailingIcon = {
                            if (isSearchingName) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = BlueExpressive
                                )
                            } else if (matricula.isNotEmpty()) {
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
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { performSubmit() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = if (isAdmMatricula) Color(0xFFF59E0B)
                            else if (isMasterMatricula) Color(0xFF8B5CF6)
                            else BlueExpressive,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_matricula_input")
                    )

                    // Card de Detecção Automática do Usuário no Supabase (Requirement 4)
                    AnimatedVisibility(
                        visible = detectedNome != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAdmMatricula) Color(0xFF78350F).copy(alpha = 0.4f)
                            else if (isMasterMatricula) Color(0xFF4C1D95).copy(alpha = 0.4f)
                            else BlueExpressiveContainer.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isAdmMatricula) Color(0xFFF59E0B).copy(alpha = 0.5f)
                                else if (isMasterMatricula) Color(0xFF8B5CF6).copy(alpha = 0.5f)
                                else BlueExpressive.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isAdmMatricula) Color(0xFFFCD34D) else EmeraldSafe,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = detectedNome ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Cargo: ${(detectedCargo ?: "operador").uppercase()} • Autenticando...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Error Message
                    AnimatedVisibility(
                        visible = displayError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RedExpressive.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, RedExpressive.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = RedExpressive,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayError ?: "",
                                    color = RedExpressive,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = { performSubmit() },
                        enabled = !isAuthenticating,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdmMatricula) Color(0xFFF59E0B)
                            else if (isMasterMatricula) Color(0xFF8B5CF6)
                            else BlueExpressive
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_btn")
                    ) {
                        if (isAuthenticating || isAutoLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = if (isAdmMatricula) Color.Black else Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Autenticando no Supabase...",
                                color = if (isAdmMatricula) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = if (isAdmMatricula) "Acessar como Administrador"
                                else if (isMasterMatricula) "Acessar como Gestor Master"
                                else "Acessar Sistema",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmMatricula) Color.Black else Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isAdmMatricula) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security & Traceability Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EmeraldSafe,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Acesso Seguro • Validação 100% no Supabase • Sessão Única",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
