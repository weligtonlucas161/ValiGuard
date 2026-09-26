package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.AddSectorDialog
import com.example.ui.components.AuditScreen
import com.example.ui.components.DashboardControlPanel
import com.example.ui.components.LoginScreen
import com.example.ui.components.MondayAuditBanner
import com.example.ui.components.MondayAuditDialog
import com.example.ui.components.MonitoringScreen
import com.example.ui.components.ProductDetailSheet
import com.example.ui.components.ProductGridCard
import com.example.ui.components.StatusSummaryCards
import com.example.ui.components.UrgentMarkdownReportDialog
import com.example.data.supabase.SessionHolder
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseConfig
import com.example.ui.screens.AdmScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.MatriculaLoginScreen
import com.example.ui.theme.BlueExpressive
import com.example.ui.theme.BlueExpressiveContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.ExpressiveChipShape
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangeMarkdown
import com.example.ui.theme.RedExpressive
import com.example.ui.theme.RedExpressiveContainer
import com.example.ui.viewmodel.AppScreenTab
import com.example.ui.viewmodel.ProductViewModel
import com.example.ui.viewmodel.StatusFilter
import com.example.util.BarcodeScannerHelper
import com.example.util.CollectorBroadcastScannerEffect
import com.example.util.SoundFeedbackHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ProductViewModel by viewModels()

    // Suporte a coletores de dados e leitores de código de barras físicos que emitem KeyEvents (HID / Teclado)
    private val barcodeBuffer = StringBuilder()
    private var lastKeyTimestamp = 0L

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            // Leitores de código de barras digitam muito rápido (< 60ms entre caracteres)
            if (now - lastKeyTimestamp > 250) {
                barcodeBuffer.setLength(0)
            }
            lastKeyTimestamp = now

            if (event.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                val scanned = barcodeBuffer.toString().trim()
                barcodeBuffer.setLength(0)
                if (scanned.length >= 3) {
                    processHardwareBarcode(scanned)
                    return true
                }
            } else {
                val unicodeChar = event.keyCharacterMap.get(event.keyCode, event.metaState)
                if (unicodeChar != 0 && !Character.isISOControl(unicodeChar)) {
                    barcodeBuffer.append(unicodeChar.toChar())
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun processHardwareBarcode(barcode: String) {
        SoundFeedbackHelper.playSuccessBeep(this)
        val all = viewModel.allProductsRaw.value
        val found = all.firstOrNull { it.barcode.equals(barcode, ignoreCase = true) }
        if (found != null) {
            viewModel.selectProduct(found)
            viewModel.showSnackbar("Coletor: Produto \"${found.name}\" identificado!")
        } else {
            viewModel.openAddProduct(prefilledBarcode = barcode)
            viewModel.showSnackbar("Coletor: Código $barcode lido. Abrindo cadastro...")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseConfig.init(this)
        SupabaseClient.instance.baseUrl = SupabaseConfig.supabaseUrl
        SupabaseClient.instance.anonKey = SupabaseConfig.supabaseAnonKey
        setContent {
            MyApplicationTheme {
                AppNavigationRoot()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.data.SingleSessionManager.terminateSession()
        SessionHolder.clear()
    }
}

@Composable
fun AppNavigationRoot() {
    val currentUser by SessionHolder.currentUserState.collectAsStateWithLifecycle()

    when {
        currentUser == null -> {
            MatriculaLoginScreen(
                onLoginSuccess = { /* SessionHolder is already updated */ }
            )
        }
        currentUser?.cargo.equals("adm", ignoreCase = true) -> {
            AdmScreen(
                onLogout = {
                    com.example.data.SingleSessionManager.terminateSession()
                    SessionHolder.clear()
                }
            )
        }
        else -> {
            MainAppScreen(
                onLogout = {
                    com.example.data.SingleSessionManager.terminateSession()
                    SessionHolder.clear()
                }
            )
        }
    }
}

