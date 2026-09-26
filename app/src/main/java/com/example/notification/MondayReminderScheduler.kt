package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import java.util.Calendar

object MondayReminderScheduler {
    const val CHANNEL_ID = "supermarket_validade_alerts"
    const val CHANNEL_NAME = "Alertas de Validade e Conferência"
    const val NOTIFICATION_ID_MONDAY = 1001
    const val NOTIFICATION_ID_MARKDOWN = 1002
    const val NOTIFICATION_ID_DAILY_CHECK = 1003
    const val NOTIFICATION_ID_RENEW_3_DAYS = 1004

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Lembretes de conferência manual de segunda-feira e alertas de rebaixa de preço"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isTodayMonday(): Boolean {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
    }

    fun getDaysUntilNextMonday(): Int {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        return if (currentDay == Calendar.MONDAY) {
            0
        } else {
            var diff = Calendar.MONDAY - currentDay
            if (diff < 0) {
                diff += 7
            }
            diff
        }
    }

    fun scheduleMondayReminder(context: Context, hour: Int = 7, minute: Int = 0) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MONDAY_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY || timeInMillis <= System.currentTimeMillis()) {
                // Advance to next Monday
                val daysToAdd = getDaysUntilNextMonday().let { if (it == 0) 7 else it }
                add(Calendar.DAY_OF_YEAR, daysToAdd)
            }
        }

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Alarm permission fallback
        }
    }

    fun showMondayNotification(context: Context) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("📋 Conferência Manual de Estoque (Segunda-feira)")
            .setContentText("Antes do início do expediente: faça a conferência manual de validades e quantidades do setor.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Hoje é Segunda-feira! Antes de abrir o expediente, verifique as validades nas gôndolas e ilhas, identifique itens a vencer em até 15 dias para solicitar rebaixa de preço e retire os vencidos."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_MONDAY, builder.build())
    }

    fun showMarkdownAlertNotification(context: Context, productName: String, daysLeft: Long) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🏷️ Solicitar Rebaixa: $productName")
            .setContentText("Produto vence em $daysLeft dias (≤15 dias). Solicite rebaixa de preço imediata!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_MARKDOWN, builder.build())
    }

    /**
     * Lembrete Diário: Notifica para verificar se a rebaixa foi aceita ou não
     */
    fun showDailyMarkdownCheckNotification(context: Context, pendingCount: Int) {
        if (pendingCount <= 0) return
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 Lembrete Diário: Conferir Rebaixas")
            .setContentText("Você tem $pendingCount produto(s) aguardando resposta da gerência se a rebaixa foi aceita ou não.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Lembrete Diário: Verifique hoje com a gerência se as solicitações de rebaixa foram aceitas ou recusadas para $pendingCount produto(s) em monitoramento."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_DAILY_CHECK, builder.build())
    }

    /**
     * Lembrete de 3 Dias: Notifica para solicitar uma nova rebaixa
     */
    fun showRenewMarkdownReminderNotification(context: Context, renewCount: Int) {
        if (renewCount <= 0) return
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🔄 Ciclo de 3 Dias: Nova Rebaixa")
            .setContentText("Já se passaram 3 dias! Solicite uma nova rebaixa para $renewCount produto(s) a vencer.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Lembrete de 3 Dias: Há $renewCount produto(s) com rebaixa pendente há 3+ dias ou não aceita. Solicite uma nova rebaixa para acelerar a venda antes do vencimento!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_RENEW_3_DAYS, builder.build())
    }
}
