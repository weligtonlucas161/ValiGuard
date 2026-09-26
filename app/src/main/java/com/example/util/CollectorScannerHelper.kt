package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Utilitário para escutar bipagem originada de coletores de dados profissionais
 * (Zebra / Motorola, Honeywell / Intermec, Datalogic, Sunmi, Urovo, Newland).
 *
 * Suporta:
 * 1. Broadcast Intents (DataWedge / Honeywell Scan / Broadcast Scanner)
 * 2. Emulação de teclado (Hardware Scanner KeyEvents / Keystrokes terminados em ENTER)
 */
object CollectorScannerHelper {

    // Ações comuns de broadcast para coletores de dados
    val BROADCAST_ACTIONS = listOf(
        "com.symbol.datawedge.api.RESULT_ACTION",
        "com.motorolasolutions.emdk.datawedge.api.RESULT_ACTION",
        "com.honeywell.decode.intent.action.EDIT_DATA",
        "android.intent.ACTION_DECODE_DATA",
        "com.datalogic.decodewedge.decode_action",
        "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED",
        "urovo.rcv.action",
        "nlscan.action.SCANNER_RESULT"
    )

    // Chaves extras de dados onde o código de barras pode vir
    val DATA_KEYS = listOf(
        "com.symbol.datawedge.data_string",
        "data_string",
        "barcode_data",
        "barcode_string",
        "barcode",
        "scannerdata",
        "barcode_value",
        "value",
        "data"
    )

    fun extractBarcodeFromIntent(intent: Intent): String? {
        for (key in DATA_KEYS) {
            val str = intent.getStringExtra(key)
            if (!str.isNullOrBlank()) return str.trim()

            val bytes = intent.getByteArrayExtra(key)
            if (bytes != null && bytes.isNotEmpty()) {
                val decoded = String(bytes).trim()
                if (decoded.isNotBlank()) return decoded
            }
        }
        return null
    }
}

/**
 * Composable que registra BroadcastReceivers para coletores de dados e repassa o código lido.
 */
@Composable
fun CollectorBroadcastScannerEffect(
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                val barcode = CollectorScannerHelper.extractBarcodeFromIntent(intent)
                if (!barcode.isNullOrBlank()) {
                    onBarcodeScanned(barcode)
                }
            }
        }

        val filter = IntentFilter().apply {
            CollectorScannerHelper.BROADCAST_ACTIONS.forEach { addAction(it) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }
}
