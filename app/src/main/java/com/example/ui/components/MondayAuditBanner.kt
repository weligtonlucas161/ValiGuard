package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.MondayReminderScheduler
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.ExpressiveBannerShape
import com.example.ui.theme.ExpressiveButtonShape
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.ExpressiveSquircleShape

@Composable
fun MondayAuditBanner(
    onStartAuditClick: () -> Unit,
    onTestNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMonday = MondayReminderScheduler.isTodayMonday()
    val daysUntil = MondayReminderScheduler.getDaysUntilNextMonday()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monday_audit_banner"),
        shape = ExpressiveBannerShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isMonday) BlueExpressiveContainer.copy(alpha = 0.5f) else DarkSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isMonday) BlueExpressive else DarkBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMonday) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Pill Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = ExpressiveChipShape,
                    color = if (isMonday) BlueExpressive else BlueExpressiveContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMonday) Color.White else Color(0xFF93C5FD)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMonday) "HOJE É SEGUNDA-FEIRA" else "ROTINA SEMANAL (TODA SEGUNDA)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isMonday) Color.White else Color(0xFF93C5FD),
                            letterSpacing = 0.5.sp,
                            fontSize = 10.sp
                        )
                    }
                }

                // Notification Quick Trigger
                Surface(
                    shape = CircleShape,
                    color = BlueExpressiveContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(
                        onClick = onTestNotificationClick,
                        modifier = Modifier.testTag("button_test_notification")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Testar Notificação",
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Icon + Title + Description Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = ExpressiveSquircleShape,
                    color = BlueExpressive,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = "Conferência de Segunda-feira",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = if (isMonday) "Conferência Manual Pré-Expediente" else "Conferência Geral de Estoque",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isMonday) {
                            "Valide o estoque físico e os prazos antes de abrir o setor."
                        } else {
                            "Lembrete automático toda segunda às 07h (próxima em ${daysUntil}d)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row with Expressive Shapes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTestNotificationClick,
                    shape = ExpressiveButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary),
                    border = BorderStroke(1.dp, DarkBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Testar Lembrete",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onStartAuditClick,
                    shape = ExpressiveButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueExpressive
                    ),
                    modifier = Modifier.testTag("button_start_monday_audit"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Iniciar Conferência",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
