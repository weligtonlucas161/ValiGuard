package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Utilitário de feedback sonoro e tátil para conferência e bipagem de produtos.
 * Emite som característico de bipe de checkout/coletor de dados e vibração rápida.
 */
object SoundFeedbackHelper {

    private var soundPool: SoundPool? = null
    private var beepSoundId: Int = 0
    private var isLoaded: Boolean = false

    fun init(context: Context) {
        if (soundPool != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build().apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0 && sampleId == beepSoundId) {
                        isLoaded = true
                    }
                }
            }
    }

    private var toneGeneratorMusic: android.media.ToneGenerator? = null

    private fun getToneGen(): android.media.ToneGenerator? {
        if (toneGeneratorMusic == null) {
            try {
                toneGeneratorMusic = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
            } catch (_: Exception) {}
        }
        return toneGeneratorMusic
    }

    /**
     * Toca um bipe sonoro de leitor de código de barras (estilo caixa de supermercado/coletor)
     * e aciona vibração rápida de confirmação.
     */
    fun playSuccessBeep(context: Context) {
        try {
            val tg = getToneGen() ?: android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {
            try {
                val fallbackTg = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
                fallbackTg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (_: Exception) {
                // Silencioso em caso de restrição de áudio do dispositivo
            }
        }

        // Haptic Feedback (Vibração curta de 50ms)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            }
        } catch (_: Exception) {
            // Permissão ou hardware de vibração indisponível
        }
    }

    /**
     * Bipe de alerta/atenção para itens que não foram encontrados.
     */
    fun playAlertBeep(context: Context) {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_NACK, 200)
        } catch (_: Exception) {
            // Silencioso
        }
    }
}
