package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CargoManager
import com.example.data.SectorManager
import com.example.data.supabase.AppLog
import com.example.data.supabase.AtualizarUsuario
import com.example.data.supabase.LogManager
import com.example.data.supabase.NovoUsuario
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.Usuario
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedExpressive
import com.example.util.decrypted
import com.example.util.encrypted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestaoEquipeScreen(
    modifier: Modifier = Modifier,
    lojaIdOverride: String? = null,
    lojaNomeOverride: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }

    val currentUser = SessionHolder.currentUser
    val currentLojaId = remember(lojaIdOverride, currentUser) {
        lojaIdOverride?.takeIf { it.isNotBlank() }
            ?: SessionHolder.currentUser?.loja_id.orEmpty()
    }
    val currentLojaNome = remember(lojaNomeOverride) {
        lojaNomeOverride?.takeIf { it.isNotBlank() }
            ?: SessionHolder.currentLoja?.nome_loja
            ?: "Empresa Vinculada"
    }

    // Estado do formulário de adição de usuário (Auto-vínculo de Loja)
    var nomeInput by remember { mutableStateOf("") }
    var matriculaInput by remember { mutableStateOf("") }
    var cargoInput by remember { mutableStateOf("operador") }
    var setorInput by remember { mutableStateOf("Geral") }
    var isSavingUser by remember { mutableStateOf(false) }
    var formExpanded by remember { mutableStateOf(true) }

    // Lista de usuários da equipe
    var equipeList by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var isLoadingEquipe by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TODOS") } // "TODOS", "ATIVOS", "INATIVOS"

    // Usuário selecionado para o Modal/Sheet de Detalhes e Logs
    var selectedUsuario by remember { mutableStateOf<Usuario?>(null) }
    var showGerenciarCargos by remember { mutableStateOf(false) }
    var showGerenciarSetores by remember { mutableStateOf(false) }

    val availableSectors = remember(showGerenciarSetores) {
        listOf("Geral") + SectorManager.getSetores(context).map { it.nome }
    }

    val cargosPadrao = remember(showGerenciarCargos, currentLojaId) {
        val custom = CargoManager.getCargos(context, currentLojaId).map { it.nome }
        (listOf("operador", "master", "adm") + custom).distinct()
    }

    fun carregarEquipe() {
        if (currentLojaId.isBlank()) return
        isLoadingEquipe = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    val result = supabase.from("usuarios")
                        .select { eq("loja_id", currentLojaId) }
                        .decodeList<Usuario>()
                        .map { it.decrypted() }
                    equipeList = result.sortedBy { it.nome.lowercase() }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Erro ao carregar equipe: ${e.localizedMessage ?: "Falha de conexão"}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoadingEquipe = false
            }
        }
    }

    LaunchedEffect(currentLojaId) {
        carregarEquipe()
    }

    fun adicionarUsuario() {
        val cleanNome = nomeInput.trim()
        val cleanMatricula = matriculaInput.trim()
        val cleanCargo = cargoInput.trim().ifBlank { "operador" }
        val cleanSetor = setorInput.trim().takeIf { it.isNotBlank() && !it.equals("Geral", ignoreCase = true) }
        val targetLojaId = SessionHolder.currentUser?.loja_id?.takeIf { it.isNotBlank() } ?: currentLojaId

        if (cleanNome.isEmpty()) {
            Toast.makeText(context, "Informe o nome completo do colaborador.", Toast.LENGTH_SHORT).show()
            return
        }
        if (cleanMatricula.length != 6 || !cleanMatricula.all { it.isDigit() }) {
            Toast.makeText(context, "A matrícula deve conter exatamente 6 dígitos numéricos.", Toast.LENGTH_SHORT).show()
            return
        }
        if (targetLojaId.isBlank()) {
            Toast.makeText(context, "Loja vinculada não identificada na sessão atual.", Toast.LENGTH_SHORT).show()
            return
        }

        isSavingUser = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    val cargoBanco = CargoManager.normalizarCargoParaBanco(cleanCargo)
                    CargoManager.salvarAtribuicaoUsuario(
                        context = context,
                        matricula = cleanMatricula,
                        cargoNome = cleanCargo,
                        setor = cleanSetor
                    )

                    val novoUsuario = NovoUsuario(
                        matricula = cleanMatricula,
                        nome = cleanNome,
                        cargo = cargoBanco,
                        loja_id = targetLojaId,
                        ativo = true,
                        setor = cleanSetor
                    )

                    supabase.from("usuarios").insert(novoUsuario.encrypted())

                    LogManager.recordLog(
                        usuarioMatricula = currentUser?.matricula ?: "MASTER",
                        usuarioNome = currentUser?.nome ?: "Gestor",
                        lojaId = targetLojaId,
                        acao = "CADASTRO_USUARIO",
                        detalhes = "Cadastrou colaborador '$cleanNome' (Mat: $cleanMatricula, Cargo: $cleanCargo, Setor: ${cleanSetor ?: "Geral"})"
                    )

                    nomeInput = ""
                    matriculaInput = ""
                    Toast.makeText(context, "Usuário cadastrado e vinculado à loja!", Toast.LENGTH_SHORT).show()
                    carregarEquipe()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Erro ao salvar usuário: ${e.localizedMessage ?: "Falha"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isSavingUser = false
            }
        }
    }

    val filteredEquipe = remember(equipeList, searchQuery, statusFilter) {
        equipeList.filter { user ->
            val matchesStatus = when (statusFilter) {
                "ATIVOS" -> user.ativo
                "INATIVOS" -> !user.ativo
                else -> true
            }
            val resolvedSetor = user.setor ?: CargoManager.resolveUserSector(user) ?: "Geral"
            val matchesQuery = searchQuery.isBlank() ||
                user.nome.contains(searchQuery, ignoreCase = true) ||
                user.matricula.contains(searchQuery, ignoreCase = true) ||
                user.cargo.contains(searchQuery, ignoreCase = true) ||
                resolvedSetor.contains(searchQuery, ignoreCase = true)

            matchesStatus && matchesQuery
        }
    }

    val totalAtivos = remember(equipeList) { equipeList.count { it.ativo } }
    val totalInativos = remember(equipeList) { equipeList.count { !it.ativo } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
            .padding(horizontal = 16.dp)
            .testTag("gestao_equipe_screen"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabeçalho da Gestão da Empresa / Equipe
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF581C87).copy(alpha = 0.4f),
                                border = BorderStroke(1.5.dp, Color(0xFFA855F7)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gestão de Equipe & Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$currentLojaNome • ID: ${currentLojaId.take(8)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkTextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { carregarEquipe() },
                            modifier = Modifier.testTag("btn_refresh_equipe")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recarregar Equipe",
                                tint = Color(0xFFC084FC)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resumo de métricas da equipe
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EquipeStatPill(
                            modifier = Modifier.weight(1f),
                            label = "Total Equipe",
                            value = "${equipeList.size}",
                            color = BlueExpressive
                        )
                        EquipeStatPill(
                            modifier = Modifier.weight(1f),
                            label = "Ativos",
                            value = "$totalAtivos",
                            color = EmeraldSafe
                        )
                        EquipeStatPill(
                            modifier = Modifier.weight(1f),
                            label = "Inativos",
                            value = "$totalInativos",
                            color = RedExpressive
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Atalhos rápidos de Cargos e Setores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showGerenciarCargos = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cargos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { showGerenciarSetores = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Setores", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 1. Formulário de Adição de Usuário com Auto-vínculo de Loja
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { formExpanded = !formExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = BlueExpressive,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Adicionar Colaborador à Equipe",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Auto-vínculo com loja_id: ${currentLojaId.ifBlank { "sessão atual" }}",
                                    fontSize = 11.sp,
                                    color = DarkTextSecondary
                                )
                            }
                        }

                        TextButton(onClick = { formExpanded = !formExpanded }) {
                            Text(
                                text = if (formExpanded) "Ocultar" else "Expandir",
                                fontSize = 11.sp,
                                color = BlueExpressive
                            )
                        }
                    }

                    if (formExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = nomeInput,
                            onValueChange = { nomeInput = it },
                            label = { Text("Nome Completo") },
                            placeholder = { Text("Ex: Carlos Eduardo Silva") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = BlueExpressive,
                                unfocusedBorderColor = DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_equipe_nome")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = matriculaInput,
                            onValueChange = { value ->
                                if (value.length <= 6 && value.all { it.isDigit() }) {
                                    matriculaInput = value
                                }
                            },
                            label = { Text("Matrícula (6 dígitos numéricos)") },
                            placeholder = { Text("Ex: 456789") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = DarkTextPrimary,
                                focusedBorderColor = BlueExpressive,
                                unfocusedBorderColor = DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_equipe_matricula")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Cargo / Função:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cargosPadrao) { cargoOption ->
                                val selected = cargoInput.equals(cargoOption, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        cargoInput = cargoOption
                                        val cargoCustom = CargoManager.getCargos(context, currentLojaId)
                                            .find { it.nome.equals(cargoOption, ignoreCase = true) }
                                        if (!cargoCustom?.setor.isNullOrBlank()) {
                                            setorInput = cargoCustom?.setor ?: "Geral"
                                        }
                                    },
                                    label = { Text(cargoOption.uppercase(), fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF7E22CE),
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkSurfaceContainerHigh,
                                        labelColor = DarkTextSecondary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Setor de Atuação:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availableSectors) { setorOption ->
                                val selected = setorInput.equals(setorOption, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { setorInput = setorOption },
                                    label = { Text(setorOption, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BlueExpressive,
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkSurfaceContainerHigh,
                                        labelColor = DarkTextSecondary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { adicionarUsuario() },
                            enabled = !isSavingUser,
                            colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_salvar_usuario_equipe")
                        ) {
                            if (isSavingUser) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Salvando no Supabase...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cadastrar Colaborador", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Busca e Filtro da Equipe
        item {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar por nome, matrícula, cargo ou setor") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar", tint = DarkTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_equipe_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    listOf(
                        "TODOS" to "Todos (${equipeList.size})",
                        "ATIVOS" to "Ativos ($totalAtivos)",
                        "INATIVOS" to "Inativos ($totalInativos)"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueExpressive,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceContainer,
                                labelColor = DarkTextSecondary
                            )
                        )
                    }
                }
            }
        }

        // 3. Lista de Cards da Equipe
        if (isLoadingEquipe) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BlueExpressive)
                }
            }
        } else if (filteredEquipe.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum colaborador encontrado para esta loja.",
                            color = DarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(filteredEquipe, key = { it.matricula }) { usuario ->
                val cargoDisplay = CargoManager.getCargoCustomizado(context, usuario.matricula) ?: usuario.cargo
                val setorDisplay = usuario.setor
                    ?: CargoManager.resolveUserSector(usuario)
                    ?: "Geral"

                val roleColor = when {
                    usuario.cargo.equals("adm", ignoreCase = true) -> Color(0xFFEAB308)
                    usuario.cargo.equals("master", ignoreCase = true) -> Color(0xFFA855F7)
                    else -> BlueExpressive
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (usuario.ativo) DarkBorder else RedExpressive.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedUsuario = usuario }
                        .testTag("card_equipe_usuario_${usuario.matricula}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = roleColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, roleColor),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = usuario.nome.take(1).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = roleColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = usuario.nome,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = roleColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = cargoDisplay.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = roleColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Matrícula: ${usuario.matricula} • Setor: $setorDisplay",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = DarkTextSecondary
                                )
                            }
                        }

                        // Status Ativo / Inativo
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (usuario.ativo) EmeraldSafe.copy(alpha = 0.18f) else RedExpressive.copy(alpha = 0.18f),
                            border = BorderStroke(
                                1.dp,
                                if (usuario.ativo) EmeraldSafe.copy(alpha = 0.6f) else RedExpressive.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = if (usuario.ativo) "ATIVO" else "INATIVO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (usuario.ativo) EmeraldSafe else RedExpressive,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 4. ModalBottomSheet de Detalhes e Logs do Usuário
    selectedUsuario?.let { user ->
        UsuarioDetalhesLogsSheet(
            usuario = user,
            lojaNome = currentLojaNome,
            onDismiss = { selectedUsuario = null },
            onUsuarioUpdated = { updated ->
                equipeList = equipeList.map { if (it.matricula == updated.matricula) updated else it }
                selectedUsuario = updated
            },
            onUsuarioDeleted = { deleted ->
                equipeList = equipeList.filter { it.matricula != deleted.matricula }
                selectedUsuario = null
            }
        )
    }

    if (showGerenciarCargos) {
        GerenciarCargosDialog(
            lojaId = currentLojaId,
            onDismiss = { showGerenciarCargos = false }
        )
    }

    if (showGerenciarSetores) {
        GerenciarSetoresDialog(
            onDismiss = { showGerenciarSetores = false }
        )
    }
}

@Composable
private fun EquipeStatPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceContainerHigh,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = DarkTextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsuarioDetalhesLogsSheet(
    usuario: Usuario,
    lojaNome: String,
    onDismiss: () -> Unit,
    onUsuarioUpdated: (Usuario) -> Unit,
    onUsuarioDeleted: (Usuario) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabase = remember { SupabaseClient.instance }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentUsuario by remember(usuario) { mutableStateOf(usuario) }
    var isUpdatingStatus by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    var logsUsuario by remember { mutableStateOf<List<AppLog>>(emptyList()) }
    var isLoadingLogs by remember { mutableStateOf(true) }

    val cargoDisplay = remember(currentUsuario) {
        CargoManager.getCargoCustomizado(context, currentUsuario.matricula) ?: currentUsuario.cargo
    }
    val setorDisplay = remember(currentUsuario) {
        currentUsuario.setor ?: CargoManager.resolveUserSector(currentUsuario) ?: "Geral"
    }

    fun carregarLogsDoUsuario() {
        isLoadingLogs = true
        coroutineScope.launch {
            try {
                val remoteLogs = try {
                    withTimeout(8000) {
                        supabase.from("app_logs")
                            .select {
                                eq("usuario_matricula", currentUsuario.matricula)
                                order("timestamp", ascending = false)
                            }
                            .decodeList<AppLog>()
                            .map { it.decrypted() }
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                val memoryLogs = LogManager.logs.value.filter {
                    it.usuario_matricula == currentUsuario.matricula
                }
                logsUsuario = (remoteLogs + memoryLogs)
                    .distinctBy { it.id }
                    .sortedByDescending { it.timestamp }
            } finally {
                isLoadingLogs = false
            }
        }
    }

    LaunchedEffect(currentUsuario.matricula) {
        carregarLogsDoUsuario()
    }

    fun alternarStatusAtivo(novoStatus: Boolean) {
        isUpdatingStatus = true
        coroutineScope.launch {
            try {
                withTimeout(8000) {
                    val payload = AtualizarUsuario(
                        nome = currentUsuario.nome,
                        cargo = currentUsuario.cargo,
                        ativo = novoStatus,
                        setor = currentUsuario.setor
                    )
                    supabase.from("usuarios")
                        .update(payload.encrypted()) {
                            eq("matricula", currentUsuario.matricula)
                        }

                    val atualizado = currentUsuario.copy(ativo = novoStatus)
                    currentUsuario = atualizado
                    onUsuarioUpdated(atualizado)

                    val executor = SessionHolder.currentUser
                    LogManager.recordLog(
                        usuarioMatricula = executor?.matricula ?: "MASTER",
                        usuarioNome = executor?.nome ?: "Gestor",
                        lojaId = currentUsuario.loja_id,
                        acao = "STATUS_USUARIO",
                        detalhes = "Alterou status de '${currentUsuario.nome}' (${currentUsuario.matricula}) para ${if (novoStatus) "ATIVO" else "INATIVO"}"
                    )

                    Toast.makeText(
                        context,
                        if (novoStatus) "Colaborador ativado!" else "Colaborador desativado!",
                        Toast.LENGTH_SHORT
                    ).show()
                    carregarLogsDoUsuario()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Erro ao atualizar status: ${e.localizedMessage ?: "Falha"}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isUpdatingStatus = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceContainer,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .testTag("sheet_detalhes_usuario")
        ) {
            // Cabeçalho do Usuário
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = BlueExpressive.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, BlueExpressive),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = BlueExpressive,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = currentUsuario.nome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Matrícula: ${currentUsuario.matricula} • $lojaNome",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF93C5FD)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = DarkTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dados Completos do Usuário
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Cargo / Permissão", fontSize = 10.sp, color = DarkTextSecondary)
                            Text(
                                text = cargoDisplay.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFC084FC)
                            )
                        }
                        Column {
                            Text("Setor Vinculado", fontSize = 10.sp, color = DarkTextSecondary)
                            Text(
                                text = setorDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Loja ID", fontSize = 10.sp, color = DarkTextSecondary)
                            Text(
                                text = currentUsuario.loja_id.take(8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DarkTextPrimary
                            )
                        }
                    }

                    if (!currentUsuario.ultimo_ping.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Último ping de atividade: ${currentUsuario.ultimo_ping}",
                            fontSize = 11.sp,
                            color = DarkTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão / Switch para Alternar Status Ativo / Inativo
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (currentUsuario.ativo) EmeraldSafe.copy(alpha = 0.5f) else RedExpressive.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (currentUsuario.ativo) EmeraldSafe else RedExpressive,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentUsuario.ativo) "Status: Usuário Ativo" else "Status: Usuário Inativo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (currentUsuario.ativo) EmeraldSafe else RedExpressive
                            )
                            Text(
                                text = if (currentUsuario.ativo) "Acesso liberado no sistema" else "Login bloqueado para esta matrícula",
                                fontSize = 11.sp,
                                color = DarkTextSecondary
                            )
                        }
                    }

                    if (isUpdatingStatus) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = BlueExpressive,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Switch(
                            checked = currentUsuario.ativo,
                            onCheckedChange = { novo -> alternarStatusAtivo(novo) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldSafe,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = RedExpressive.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("switch_ativo_usuario_${currentUsuario.matricula}")
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { showConfirmDelete = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir Usuário",
                            tint = RedExpressive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DarkBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Cabeçalho da Lista de Logs do Usuário
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Histórico de Logs (${logsUsuario.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = { carregarLogsDoUsuario() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar Logs",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingLogs) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BlueExpressive)
                }
            } else if (logsUsuario.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum registro encontrado em 'app_logs' para a matrícula ${currentUsuario.matricula}.",
                        color = DarkTextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else {
                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(logsUsuario, key = { it.id }) { logItem ->
                        val formattedDate = remember(logItem.timestamp) {
                            dateFormat.format(Date(logItem.timestamp))
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainerHigh),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BlueExpressive.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = logItem.acao,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF93C5FD),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = DarkTextSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formattedDate,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = DarkTextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = logItem.detalhes,
                                    fontSize = 12.sp,
                                    color = DarkTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = {
                Text("Excluir Colaborador?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    text = "Deseja remover permanentemente \"${currentUsuario.nome}\" (Matrícula: ${currentUsuario.matricula})?",
                    color = DarkTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        coroutineScope.launch {
                            try {
                                withTimeout(8000) {
                                    supabase.from("usuarios").delete {
                                        eq("matricula", currentUsuario.matricula)
                                    }
                                    Toast.makeText(context, "Colaborador removido.", Toast.LENGTH_SHORT).show()
                                    onUsuarioDeleted(currentUsuario)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpressive)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
