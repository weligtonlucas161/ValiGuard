package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.Loja
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseConfig
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
fun SupabaseConfigDialog(
    onDismiss: () -> Unit,
    onSaved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var url by remember { mutableStateOf(SupabaseConfig.supabaseUrl) }
    var anonKey by remember { mutableStateOf(SupabaseConfig.supabaseAnonKey) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }

    var isSeeding by remember { mutableStateOf(false) }
    var seedResult by remember { mutableStateOf<String?>(null) }
    var showSqlModal by remember { mutableStateOf(false) }

    val sqlScript = """
-- =========================================================================
-- TABELAS PARA AUDITORIA EM TEMPO REAL E BLOQUEIO DE EDIÇÃO DE ESTOQUE
-- Cole no SQL Editor do Supabase e clique em RUN
-- =========================================================================

-- 1. Bloqueio de Edição de Produtos (Evita Edição Dupla)
CREATE TABLE IF NOT EXISTS public.produto_bloqueios (
    produto_id TEXT PRIMARY KEY,
    loja_id TEXT NOT NULL,
    usuario_matricula TEXT NOT NULL,
    usuario_nome TEXT NOT NULL,
    timestamp BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.produto_bloqueios ENABLE ROW LEVEL SECURITY;
DO ${'$'}${'$'} BEGIN
    DROP POLICY IF EXISTS "Acesso produto_bloqueios" ON public.produto_bloqueios;
    CREATE POLICY "Acesso produto_bloqueios" ON public.produto_bloqueios FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN OTHERS THEN NULL;
END ${'$'}${'$'};

-- 2. Sessões de Auditoria de Presença Compartilhada
CREATE TABLE IF NOT EXISTS public.auditoria_presenca_sessoes (
    id TEXT PRIMARY KEY,
    loja_id TEXT NOT NULL,
    setor TEXT NOT NULL DEFAULT 'Todos',
    iniciada_por_nome TEXT NOT NULL,
    iniciada_por_matricula TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ATIVA',
    timestamp BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.auditoria_presenca_sessoes ENABLE ROW LEVEL SECURITY;
DO ${'$'}${'$'} BEGIN
    DROP POLICY IF EXISTS "Acesso auditoria_sessoes" ON public.auditoria_presenca_sessoes;
    CREATE POLICY "Acesso auditoria_sessoes" ON public.auditoria_presenca_sessoes FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN OTHERS THEN NULL;
END ${'$'}${'$'};

-- 3. Bips da Auditoria (Impede Bip Duplo via Chave Única)
CREATE TABLE IF NOT EXISTS public.auditoria_presenca_bips (
    id TEXT PRIMARY KEY,
    sessao_id TEXT NOT NULL REFERENCES public.auditoria_presenca_sessoes(id) ON DELETE CASCADE,
    loja_id TEXT NOT NULL,
    produto_id TEXT NOT NULL,
    codigo_barras TEXT NOT NULL,
    produto_nome TEXT NOT NULL,
    setor TEXT NOT NULL,
    operador_matricula TEXT NOT NULL,
    operador_nome TEXT NOT NULL,
    timestamp BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.auditoria_presenca_bips ENABLE ROW LEVEL SECURITY;
DO ${'$'}${'$'} BEGIN
    DROP POLICY IF EXISTS "Acesso auditoria_bips" ON public.auditoria_presenca_bips;
    CREATE POLICY "Acesso auditoria_bips" ON public.auditoria_presenca_bips FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN OTHERS THEN NULL;
END ${'$'}${'$'};

CREATE INDEX IF NOT EXISTS idx_produto_bloqueios_loja ON public.produto_bloqueios(loja_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_sessoes_loja ON public.auditoria_presenca_sessoes(loja_id, status);
CREATE INDEX IF NOT EXISTS idx_auditoria_bips_sessao ON public.auditoria_presenca_bips(sessao_id);
""".trimIndent()

    fun testConnection() {
        val cleanUrl = url.trim().removeSuffix("/")
        val cleanKey = anonKey.trim()

        if (!cleanUrl.startsWith("http")) {
            testSuccess = false
            testResult = "A URL deve começar com https:// (ex: https://xxxx.supabase.co)"
            return
        }
        if (cleanKey.isBlank()) {
            testSuccess = false
            testResult = "Informe a chave Anon pública do seu projeto Supabase."
            return
        }

        isTesting = true
        testResult = null
        coroutineScope.launch {
            try {
                withTimeout(10000) {
                    val client = SupabaseClient(baseUrl = cleanUrl, anonKey = cleanKey)
                    // Faz consulta real no Postgrest
                    val lojas = client.from("lojas").select().decodeList<Loja>()
                    testSuccess = true
                    testResult = "✅ Conexão bem-sucedida com o Supabase! Tabela 'lojas' lida (${lojas.size} registro(s))."
                }
            } catch (e: Exception) {
                testSuccess = false
                testResult = "❌ Erro ao conectar ao Supabase: ${e.localizedMessage ?: e.message}"
            } finally {
                isTesting = false
            }
        }
    }

    fun seedData() {
        val cleanUrl = url.trim().removeSuffix("/")
        val cleanKey = anonKey.trim()

        if (!cleanUrl.startsWith("http") || cleanKey.isBlank()) {
            seedResult = "Informe a URL e a Anon Key antes de criar os dados."
            return
        }

        isSeeding = true
        seedResult = null
        coroutineScope.launch {
            try {
                withTimeout(15000) {
                    val client = SupabaseClient(baseUrl = cleanUrl, anonKey = cleanKey)
                    val msg = client.seedInitialData()
                    seedResult = "✅ $msg"
                    // Atualiza credenciais ativas
                    SupabaseConfig.updateCredentials(context, cleanUrl, cleanKey)
                    SupabaseClient.instance.baseUrl = cleanUrl
                    SupabaseClient.instance.anonKey = cleanKey
                }
            } catch (e: Exception) {
                seedResult = "❌ Erro ao inserir dados iniciais: ${e.localizedMessage ?: e.message}"
            } finally {
                isSeeding = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = BlueExpressive,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Conexão Supabase Real", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Insira as credenciais do seu projeto Supabase para que todas as operações (lojas, usuários, produtos) sejam salvas diretamente no PostgreSQL do Supabase via Postgrest.",
                            fontSize = 11.sp,
                            color = DarkTextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Project URL (Supabase)") },
                    placeholder = { Text("https://seu-projeto.supabase.co") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = BlueExpressive) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_supabase_url")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    label = { Text("Project API Key (Anon / Public)") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6...") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = BlueExpressive) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = BlueExpressive,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_supabase_anon_key")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Botão Testar Conexão
                OutlinedButton(
                    onClick = { testConnection() },
                    enabled = !isTesting && !isSeeding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BlueExpressive, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Testar Conexão com o Supabase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (testResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (testSuccess) EmeraldSafe.copy(alpha = 0.15f) else RedExpressive.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (testSuccess) EmeraldSafe else RedExpressive
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testResult ?: "",
                            fontSize = 11.sp,
                            color = if (testSuccess) EmeraldSafe else RedExpressive,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Botão para Inicializar Dados no Banco do Supabase
                Column {
                    Text(
                        text = "Banco de Dados Vazio no Supabase?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Se suas tabelas 'lojas', 'usuarios' e 'produtos' estão vazias no Supabase, toque no botão abaixo para criar a loja matriz e os usuários iniciais (ADM 111111, Master 123456, Operador 654321) diretamente no seu banco de dados.",
                        fontSize = 11.sp,
                        color = DarkTextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    Button(
                        onClick = { seedData() },
                        enabled = !isSeeding && !isTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSeeding) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Criar Loja e Usuários Iniciais no Supabase", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (seedResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = seedResult ?: "",
                                fontSize = 11.sp,
                                color = if (seedResult?.startsWith("✅") == true) EmeraldSafe else RedExpressive,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Botão para Visualizar e Copiar Script SQL do Supabase
                    OutlinedButton(
                        onClick = { showSqlModal = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Script SQL das Novas Tabelas (Supabase)", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanUrl = url.trim().removeSuffix("/")
                    val cleanKey = anonKey.trim()
                    SupabaseConfig.updateCredentials(context, cleanUrl, cleanKey)
                    SupabaseClient.instance.baseUrl = cleanUrl
                    SupabaseClient.instance.anonKey = cleanKey
                    Toast.makeText(context, "Credenciais do Supabase salvas!", Toast.LENGTH_SHORT).show()
                    onSaved?.invoke()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlueExpressive)
            ) {
                Text("Salvar Credenciais", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = DarkTextSecondary)
            }
        },
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(16.dp)
    )

    if (showSqlModal) {
        AlertDialog(
            onDismissRequest = { showSqlModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Script SQL para o Supabase", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Cole o código abaixo no SQL Editor do console do Supabase para criar as tabelas de alta performance com prevenção de bip duplo e bloqueio de edição simultânea:",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sqlScript,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Supabase SQL", sqlScript)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Código SQL copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                        showSqlModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar Código SQL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSqlModal = false }) {
                    Text("Fechar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
