package com.warungsenja.kasirterangbulan

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungsenja.kasirterangbulan.domain.model.OwnerTab
import com.warungsenja.kasirterangbulan.domain.model.UserRole
import com.warungsenja.kasirterangbulan.presentation.components.RoleHeader
import com.warungsenja.kasirterangbulan.presentation.components.RoleSwitchDialog
import com.warungsenja.kasirterangbulan.presentation.screens.DashboardScreen
import com.warungsenja.kasirterangbulan.presentation.screens.GoogleSheetScreen
import com.warungsenja.kasirterangbulan.presentation.screens.HistoryScreen
import com.warungsenja.kasirterangbulan.presentation.screens.InventoryScreen
import com.warungsenja.kasirterangbulan.presentation.screens.KasirScreen
import com.warungsenja.kasirterangbulan.presentation.screens.LoginScreen
import com.warungsenja.kasirterangbulan.presentation.screens.SettingsScreen
import com.warungsenja.kasirterangbulan.presentation.theme.KasirTerangBulanTheme
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc950
import com.warungsenja.kasirterangbulan.presentation.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as KasirApplication).container

        setContent {
            KasirTerangBulanTheme {
                val posViewModel: PosViewModel = viewModel(
                    factory = PosViewModel.provideFactory(appContainer)
                )
                KasirTerangBulanApp(viewModel = posViewModel)
            }
        }
    }
}

@Composable
fun KasirTerangBulanApp(viewModel: PosViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.scanPairedBluetoothDevices()
    }

    val requestBluetoothPermissions: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            viewModel.scanPairedBluetoothDevices()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc950)
            .systemBarsPadding()
    ) {
        val currentUser = uiState.currentUser
        if (currentUser == null) {
            LoginScreen(
                settings = uiState.settings,
                onLogin = { role, pin -> viewModel.login(role, pin) }
            )
        } else {
            var showRoleSwitchDialog by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxSize()) {
                RoleHeader(
                    currentUser = currentUser,
                    activeTab = if (currentUser.role == UserRole.OWNER) uiState.activeOwnerTab else null,
                    onSelectTab = { viewModel.selectOwnerTab(it) },
                    onOpenRoleSwitch = { showRoleSwitchDialog = true },
                    onLogout = { viewModel.logout() }
                )

                if (showRoleSwitchDialog) {
                    val targetRole = if (currentUser.role == UserRole.OWNER) UserRole.KASIR else UserRole.OWNER
                    RoleSwitchDialog(
                        targetRole = targetRole,
                        settings = uiState.settings,
                        onDismiss = { showRoleSwitchDialog = false },
                        onSuccessSwitch = { role, pin ->
                            viewModel.login(role, pin)
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (currentUser.role == UserRole.KASIR) {
                        KasirScreen(
                            products = uiState.products,
                            settings = uiState.settings,
                            onCheckout = { cart, cust, note, cb ->
                                viewModel.checkoutCart(activity, cart, cust, note, cb)
                            },
                            onPrintReceipt = { trx ->
                                viewModel.printTransaction(activity, trx)
                            }
                        )
                    } else {
                        when (uiState.activeOwnerTab) {
                            OwnerTab.DASHBOARD -> {
                                DashboardScreen(
                                    stats = viewModel.getDashboardStats(),
                                    settings = uiState.settings,
                                    transactions = uiState.transactions,
                                    getTransactionModal = { viewModel.getTransactionModal(it) },
                                    onNavigateTab = { viewModel.selectOwnerTab(it) }
                                )
                            }
                            OwnerTab.KASIR -> {
                                KasirScreen(
                                    products = uiState.products,
                                    settings = uiState.settings,
                                    onCheckout = { cart, cust, note, cb ->
                                        viewModel.checkoutCart(activity, cart, cust, note, cb)
                                    },
                                    onPrintReceipt = { trx ->
                                        viewModel.printTransaction(activity, trx)
                                    }
                                )
                            }
                            OwnerTab.INVENTORY -> {
                                InventoryScreen(
                                    products = uiState.products,
                                    onUpdateProducts = { viewModel.updateProducts(it) }
                                )
                            }
                            OwnerTab.HISTORY -> {
                                HistoryScreen(
                                    transactions = uiState.transactions,
                                    getTransactionModal = { viewModel.getTransactionModal(it) },
                                    onPrintReceipt = { act, trx -> viewModel.printTransaction(act, trx) },
                                    onRetrySync = { trx, cb -> viewModel.retrySyncTransaction(trx, cb) }
                                )
                            }
                            OwnerTab.SHEETS -> {
                                GoogleSheetScreen(
                                    config = uiState.sheetConfig,
                                    isBusy = uiState.isBusy,
                                    onSaveConfig = { viewModel.saveSheetConfig(it) },
                                    onPingConnection = { url, cb -> viewModel.pingGoogleSheet(url, cb) },
                                    onPushStock = { url, cb -> viewModel.pushAllStockToSheet(url, cb) },
                                    onPullStock = { url, cb -> viewModel.pullStockFromSheet(url, cb) }
                                )
                            }
                            OwnerTab.SETTINGS -> {
                                SettingsScreen(
                                    settings = uiState.settings,
                                    pairedDevices = uiState.pairedBluetoothDevices,
                                    onSaveSettings = { viewModel.saveSettings(it) },
                                    onScanPairedDevices = { viewModel.scanPairedBluetoothDevices() },
                                    onConnectBluetooth = { viewModel.connectBluetoothPrinter(it) },
                                    onDisconnectBluetooth = { viewModel.disconnectBluetoothPrinter() },
                                    onPrintTestReceipt = { act -> viewModel.printTestReceipt(act) },
                                    onExportBackupJson = { viewModel.exportBackupJson() },
                                    onRestoreBackupJson = { viewModel.restoreBackupJson(it) },
                                    onResetAllData = { viewModel.resetAllData() },
                                    onRequestBluetoothPermission = requestBluetoothPermissions
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
