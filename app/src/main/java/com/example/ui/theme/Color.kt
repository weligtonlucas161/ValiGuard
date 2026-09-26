package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Material 3 Expressive - Dark Palette with Vibrant Blue & Red Accents
// =========================================================================

// Expressive Blue Accents (Primary / Ações / Navegação / Auditoria)
val BlueExpressive = Color(0xFF3B82F6) // Electric Blue
val BlueExpressiveLight = Color(0xFF60A5FA)
val BlueExpressiveDark = Color(0xFF1D4ED8)
val BlueExpressiveContainer = Color(0xFF1E3A8A)
val BlueOnExpressive = Color(0xFFFFFFFF)
val BlueOnExpressiveContainer = Color(0xFFDBEAFE)

// Expressive Red Accents (Secondary / Alertas Críticos / Vencimento / Retirada)
val RedExpressive = Color(0xFFEF4444) // Vibrant Crimson Red
val RedExpressiveLight = Color(0xFFF87171)
val RedExpressiveDark = Color(0xFFDC2626)
val RedExpressiveContainer = Color(0xFF450A0A)
val RedOnExpressive = Color(0xFFFFFFFF)
val RedOnExpressiveContainer = Color(0xFFFEE2E2)

// Rebaixa Sunset Tangerine (≤15 dias)
val OrangeMarkdown = Color(0xFFF97316)
val OrangeMarkdownLight = Color(0xFFFB923C)
val OrangeMarkdownContainer = Color(0xFF431407)
val OrangeOnMarkdown = Color(0xFFFFF7ED)
val OrangeMarkdownBorder = Color(0xFFEA580C)

// Amber / Attention (16-30 dias)
val AmberAttention = Color(0xFFF59E0B)
val AmberAttentionContainer = Color(0xFF451A03)
val AmberOnAttention = Color(0xFFFEF3C7)

// Green / Safe (>30 dias)
val EmeraldSafe = Color(0xFF10B981)
val EmeraldSafeContainer = Color(0xFF064E3B)
val EmeraldOnSafe = Color(0xFFD1FAE5)

// Purple Urgency (≤1 dia - Retirar da Área de Venda)
val PurpleUrgent1Day = Color(0xFFA855F7) // Vibrant Purple
val PurpleUrgentLight = Color(0xFFC084FC)
val PurpleUrgentContainer = Color(0xFF3B0764)
val PurpleOnUrgent = Color(0xFFFAF5FF)
val PurpleUrgentBorder = Color(0xFF9333EA)

// Legacy aliases for components
val EmeraldPrimary = BlueExpressive
val EmeraldOnPrimary = Color(0xFFFFFFFF)
val EmeraldPrimaryContainer = BlueExpressiveContainer
val EmeraldOnPrimaryContainer = BlueOnExpressiveContainer
val BlueAudit = BlueExpressive
val BlueAuditContainer = BlueExpressiveContainer
val BlueOnAudit = BlueOnExpressiveContainer
val RedDanger = RedExpressive
val RedDangerContainer = RedExpressiveContainer
val RedOnDanger = RedOnExpressive
val RedOnDangerContainer = RedOnExpressiveContainer
val AmberSecondary = AmberAttention
val AmberSecondaryContainer = AmberAttentionContainer
val AmberOnSecondary = AmberOnAttention
val AmberOnSecondaryContainer = AmberOnAttention

// Deep Dark Expressive Surfaces (High contrast, fluid)
val DarkBackground = Color(0xFF080D1A) // Deep Cosmic Dark Slate
val DarkSurface = Color(0xFF0F172A) // Slate 900
val DarkSurfaceContainerLow = Color(0xFF131D33)
val DarkSurfaceContainer = Color(0xFF1A2642)
val DarkSurfaceContainerHigh = Color(0xFF223154)
val DarkSurfaceContainerHighest = Color(0xFF2B3D66)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkBorder = Color(0xFF2A3B5C)
val DarkBorderSubtle = Color(0xFF1E2D48)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextTertiary = Color(0xFF64748B)

// Neutral fallback aliases for components
val NeutralBackground = DarkBackground
val NeutralSurface = DarkSurface
val NeutralSurfaceVariant = DarkSurfaceVariant
val NeutralSurfaceContainerLow = DarkSurfaceContainerLow
val NeutralSurfaceContainer = DarkSurfaceContainer
val NeutralBorder = DarkBorder
val NeutralBorderSubtle = DarkBorderSubtle
val NeutralTextPrimary = DarkTextPrimary
val NeutralTextSecondary = DarkTextSecondary
val NeutralTextTertiary = DarkTextTertiary
