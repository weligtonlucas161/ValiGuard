package com.example.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Utilitário para acionamento do Google Code Scanner nativo.
 * Lê códigos de barras de supermercado (EAN-13, EAN-8, CODE-128, UPC, QR Code)
 * diretamente via Google Play Services sem necessidade de gerenciar câmera manualmente.
 */
object BarcodeScannerHelper {

    fun startScan(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_QR_CODE
                )
                .enableAutoZoom()
                .build()

            val scanner = GmsBarcodeScanning.getClient(context, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val code = barcode.rawValue ?: barcode.displayValue ?: ""
                    if (code.isNotBlank()) {
                        SoundFeedbackHelper.playSuccessBeep(context)
                        onSuccess(code)
                    }
                }
                .addOnCanceledListener {
                    // Usuário cancelou o escaneamento
                }
                .addOnFailureListener { e ->
                    SoundFeedbackHelper.playAlertBeep(context)
                    onError(e)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
}
