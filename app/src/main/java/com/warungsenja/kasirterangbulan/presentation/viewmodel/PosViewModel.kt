package com.warungsenja.kasirterangbulan.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.warungsenja.kasirterangbulan.di.AppContainer
import com.warungsenja.kasirterangbulan.domain.model.BluetoothDeviceItem
import com.warungsenja.kasirterangbulan.domain.model.CartItem
import com.warungsenja.kasirterangbulan.domain.model.DashboardStats
import com.warungsenja.kasirterangbulan.domain.model.GoogleSheetConfig
import com.warungsenja.kasirterangbulan.domain.model.OwnerTab
import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.SyncStatus
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.domain.model.TransactionType
import com.warungsenja.kasirterangbulan.domain.model.UserProfile
import com.warungsenja.kasirterangbulan.domain.model.UserRole
import com.warungsenja.kasirterangbulan.presentation.util.Formatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PosUiState(
    val products: List<ProductItem> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val settings: StoreSettings = StoreSettings(),
    val sheetConfig: GoogleSheetConfig = GoogleSheetConfig(),
    val currentUser: UserProfile? = null,
    val activeOwnerTab: OwnerTab = OwnerTab.DASHBOARD,
    val pairedBluetoothDevices: List<BluetoothDeviceItem> = emptyList(),
    val isBusy: Boolean = false
)

class PosViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.repository
    private val printerManager = container.printerManager
    private val calculateStatsUseCase = container.calculateDashboardStatsUseCase
    private val checkoutUseCase = container.checkoutTransactionUseCase
    private val resetAllDataUseCase = container.resetAllDataUseCase
    private val backupRestoreUseCase = container.backupRestoreUseCase

    private val _uiState = MutableStateFlow(
        PosUiState(
            products = repository.getProducts(),
            transactions = repository.getTransactions(),
            settings = repository.getSettings(),
            sheetConfig = repository.getSheetConfig(),
            currentUser = repository.getCurrentUser(),
            pairedBluetoothDevices = printerManager.getPairedDevices()
        )
    )
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    fun getDashboardStats(): DashboardStats {
        val s = _uiState.value
        return calculateStatsUseCase(s.products, s.transactions)
    }

    fun getTransactionModal(trx: TransactionRecord): Long {
        return calculateStatsUseCase.getTransactionCost(trx, _uiState.value.products)
    }

    fun selectOwnerTab(tab: OwnerTab) {
        _uiState.update { it.copy(activeOwnerTab = tab) }
    }

    fun login(role: UserRole, pin: String): Boolean {
        val currentSettings = _uiState.value.settings
        val expectedPin = if (role == UserRole.OWNER) currentSettings.ownerPin else currentSettings.kasirPin
        if (pin.trim() != expectedPin) return false

        val profile = if (role == UserRole.OWNER) {
            UserProfile("user-owner", "Owner Warung Senja", UserRole.OWNER, currentSettings.ownerPin)
        } else {
            UserProfile("user-kasir-1", "Kasir 1", UserRole.KASIR, currentSettings.kasirPin)
        }
        repository.saveCurrentUser(profile)
        _uiState.update { it.copy(currentUser = profile) }
        return true
    }

    fun logout() {
        repository.saveCurrentUser(null)
        _uiState.update { it.copy(currentUser = null, activeOwnerTab = OwnerTab.DASHBOARD) }
    }

    fun updateProducts(nextProducts: List<ProductItem>) {
        repository.saveProducts(nextProducts)
        _uiState.update { it.copy(products = nextProducts) }
    }

    fun saveSettings(nextSettings: StoreSettings) {
        repository.saveSettings(nextSettings)
        _uiState.update { it.copy(settings = nextSettings) }
    }

    fun saveSheetConfig(nextConfig: GoogleSheetConfig) {
        repository.saveSheetConfig(nextConfig)
        _uiState.update { it.copy(sheetConfig = nextConfig) }
    }

    fun checkoutCart(
        activity: Activity?,
        cart: List<CartItem>,
        customerName: String,
        note: String,
        onResult: (TransactionRecord, Boolean, String) -> Unit
    ) {
        val state = _uiState.value
        val user = state.currentUser ?: return
        val totalQty = cart.sumOf { it.qty }
        val totalAmount = cart.sumOf { it.subtotal }
        val code = Formatter.generateTransactionCode("OUT", state.transactions.size)
        val nowIso = Formatter.nowIsoString()

        val newTrx = TransactionRecord(
            id = "trx-${System.currentTimeMillis()}",
            code = code,
            type = TransactionType.OUT,
            timestamp = nowIso,
            operatorRole = user.role,
            operatorName = user.name,
            items = cart,
            totalQty = totalQty,
            totalAmount = totalAmount,
            recipientOrSupplier = customerName.trim().ifBlank { "Pelanggan Umum" },
            referenceNo = "-",
            note = note.trim(),
            syncStatus = SyncStatus.LOCAL
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val (updatedProducts, updatedTransactions) = checkoutUseCase(
                newTransaction = newTrx,
                currentProducts = state.products,
                currentTransactions = state.transactions
            )
            val savedTrx = updatedTransactions.firstOrNull() ?: newTrx
            _uiState.update {
                it.copy(
                    products = updatedProducts,
                    transactions = updatedTransactions,
                    isBusy = false
                )
            }

            printerManager.playCheckoutSound(state.settings.enableScanSound)

            if (state.settings.printer.autoPrintAfterCheckout) {
                val (ok, msg) = printerManager.printReceipt(activity, savedTrx, state.settings)
                onResult(savedTrx, ok, msg)
            } else {
                onResult(savedTrx, true, "Transaksi $code berhasil disimpan!")
            }
        }
    }

    fun printTransaction(activity: Activity?, trx: TransactionRecord): Pair<Boolean, String> {
        return printerManager.printReceipt(activity, trx, _uiState.value.settings)
    }

    fun printTestReceipt(activity: Activity?): Pair<Boolean, String> {
        return printerManager.printTestReceipt(activity, _uiState.value.settings)
    }

    fun scanPairedBluetoothDevices(): List<BluetoothDeviceItem> {
        val list = printerManager.getPairedDevices()
        _uiState.update { it.copy(pairedBluetoothDevices = list) }
        return list
    }

    fun connectBluetoothPrinter(device: BluetoothDeviceItem): Pair<Boolean, String> {
        val updatedPrinter = _uiState.value.settings.printer.copy(
            bluetoothDeviceName = device.name,
            bluetoothDeviceAddress = device.address
        )
        saveSettings(_uiState.value.settings.copy(printer = updatedPrinter))
        return printerManager.connectBluetooth(device.address)
    }

    fun disconnectBluetoothPrinter(): Pair<Boolean, String> {
        return printerManager.disconnectBluetooth()
    }

    fun retrySyncTransaction(trx: TransactionRecord, onComplete: (Boolean, String) -> Unit) {
        val webhook = _uiState.value.sheetConfig.webhookUrl
        if (webhook.isBlank()) {
            onComplete(false, "URL Webhook Google Sheets belum diisi.")
            return
        }
        viewModelScope.launch {
            val res = repository.syncTransactionToSheet(webhook, trx, _uiState.value.products)
            val nowIso = Formatter.nowIsoString()
            val updatedList = _uiState.value.transactions.map { item ->
                if (item.id == trx.id) {
                    item.copy(
                        syncStatus = if (res.success) SyncStatus.SYNCED else SyncStatus.FAILED,
                        syncedAt = if (res.success) nowIso else item.syncedAt,
                        syncError = if (res.success) null else res.message
                    )
                } else {
                    item
                }
            }
            repository.saveTransactions(updatedList)
            _uiState.update { it.copy(transactions = updatedList) }
            onComplete(res.success, res.message)
        }
    }

    fun pingGoogleSheet(webhookUrl: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repository.pingGoogleSheet(webhookUrl)
            if (res.success) {
                val updated = _uiState.value.sheetConfig.copy(
                    webhookUrl = webhookUrl.trim(),
                    isConnected = true,
                    lastSyncAt = Formatter.nowIsoString()
                )
                saveSheetConfig(updated)
            }
            _uiState.update { it.copy(isBusy = false) }
            onComplete(res.success, res.message)
        }
    }

    fun pushAllStockToSheet(webhookUrl: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repository.syncAllStockToSheet(webhookUrl, _uiState.value.products)
            if (res.success) {
                val updated = _uiState.value.sheetConfig.copy(
                    webhookUrl = webhookUrl.trim(),
                    isConnected = true,
                    lastSyncAt = Formatter.nowIsoString()
                )
                saveSheetConfig(updated)
            }
            _uiState.update { it.copy(isBusy = false) }
            onComplete(res.success, res.message)
        }
    }

    fun pullStockFromSheet(webhookUrl: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = repository.fetchStockFromSheet(webhookUrl)
            if (res.success && res.fetchedItems.isNotEmpty()) {
                updateProducts(res.fetchedItems)
            }
            _uiState.update { it.copy(isBusy = false) }
            onComplete(res.success, res.message)
        }
    }

    fun exportBackupJson(): String = backupRestoreUseCase.exportJson()

    fun restoreBackupJson(rawJson: String): Result<Pair<Int, Int>> {
        return try {
            val restored = backupRestoreUseCase.restoreJson(rawJson)
            _uiState.update {
                it.copy(
                    products = restored.products,
                    transactions = restored.transactions,
                    settings = restored.settings,
                    sheetConfig = restored.sheetConfig
                )
            }
            Result.success(restored.products.size to restored.transactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetAllData() {
        val resetPayload = resetAllDataUseCase()
        _uiState.update {
            it.copy(
                products = resetPayload.products,
                transactions = resetPayload.transactions,
                settings = resetPayload.settings,
                sheetConfig = resetPayload.sheetConfig
            )
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PosViewModel(container) as T
                }
            }
    }
}
