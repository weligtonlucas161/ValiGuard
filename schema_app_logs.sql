-- ============================================================================
-- VALIGUARD: SCRIPT SQL PARA CRIAÇÃO DA TABELA DE AUDITORIA E LOGS ('app_logs')
-- ============================================================================
-- Importante: Os campos de dados sensíveis ('usuario_nome' e 'detalhes') são
-- criptografados no aplicativo via AES-256-GCM antes de serem enviados ao banco
-- de dados. No banco, os dados são armazenados como strings cifradas (prefixo ENC_v1:).
-- ============================================================================

-- 1. Criação da tabela app_logs
CREATE TABLE IF NOT EXISTS public.app_logs (
    id TEXT PRIMARY KEY,
    usuario_matricula TEXT NOT NULL,
    usuario_nome TEXT NOT NULL,         -- Armazenado com criptografia AES-256-GCM
    loja_id TEXT NOT NULL DEFAULT 'default',
    acao TEXT NOT NULL,                 -- 'LOGIN', 'LOGOUT', 'CADASTRO_PRODUTO', 'EXCLUSAO_PRODUTO', 'AUDITORIA_PRESENCA', 'AUDITORIA_INVENTARIO', 'AUDITORIA_VALIDADE', 'STATUS_USUARIO', 'EDICAO_USUARIO', etc.
    detalhes TEXT NOT NULL,             -- Armazenado com criptografia AES-256-GCM
    timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Índices de performance para busca rápida por matrícula, loja e data
CREATE INDEX IF NOT EXISTS idx_app_logs_usuario_matricula ON public.app_logs (usuario_matricula);
CREATE INDEX IF NOT EXISTS idx_app_logs_loja_id ON public.app_logs (loja_id);
CREATE INDEX IF NOT EXISTS idx_app_logs_timestamp ON public.app_logs (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_app_logs_acao ON public.app_logs (acao);

-- 3. Habilitação de Segurança por Nível de Linha (Row Level Security - RLS)
ALTER TABLE public.app_logs ENABLE ROW LEVEL SECURITY;

-- 4. Política para inserção (qualquer usuário autenticado ou chave anônima da loja pode registrar seus logs)
CREATE POLICY "Permitir insercao de logs por usuarios" 
ON public.app_logs 
FOR INSERT 
TO anon, authenticated 
WITH CHECK (true);

-- 5. Política para leitura (leitura de logs liberada para perfis autorizados / anon para sincronização)
CREATE POLICY "Permitir leitura de logs" 
ON public.app_logs 
FOR SELECT 
TO anon, authenticated 
USING (true);

-- ============================================================================
-- COMENTÁRIOS E DOCUMENTAÇÃO
-- ============================================================================
COMMENT ON TABLE public.app_logs IS 'Trilha de auditoria criptografada dos usuários da loja no sistema ValiGuard';
COMMENT ON COLUMN public.app_logs.usuario_nome IS 'Nome do usuário criptografado em AES-256-GCM';
COMMENT ON COLUMN public.app_logs.detalhes IS 'Detalhes e metadados da ação criptografados em AES-256-GCM';
