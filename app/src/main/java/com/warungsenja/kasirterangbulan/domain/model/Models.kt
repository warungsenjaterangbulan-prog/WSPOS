package com.warungsenja.kasirterangbulan.domain.model

enum class UserRole(val rawValue: String, val displayName: String) {
    OWNER("owner", "Owner"),
    KASIR("kasir", "Kasir");

    companion object {
        fun fromRaw(raw: String): UserRole =
            entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) } ?: KASIR
    }
}

enum class TransactionType(val rawValue: String) {
    IN("IN"),
    OUT("OUT");

    companion object {
        fun fromRaw(raw: String): TransactionType =
            entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) } ?: OUT
    }
}

enum class SyncStatus(val rawValue: String) {
    SYNCED("synced"),
    PENDING("pending"),
    FAILED("failed"),
    LOCAL("local");

    companion object {
        fun fromRaw(raw: String): SyncStatus =
            entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) } ?: LOCAL
    }
}

enum class OwnerTab(val label: String) {
    DASHBOARD("Dashboard"),
    KASIR("Kasir"),
    INVENTORY("Menu"),
    HISTORY("Riwayat"),
    SHEETS("Sheets"),
    SETTINGS("Pengaturan")
}

data class UserProfile(
    val id: String,
    val name: String,
    val role: UserRole,
    val pin: String
)

data class ProductVariant(
    val id: String,
    val name: String,
    val purchasePrice: Long,
    val sellingPrice: Long
)

data class ProductItem(
    val id: String,
    val barcode: String,
    val name: String,
    val category: String,
    val variants: List<ProductVariant> = emptyList(),
    val stock: Int = 999,
    val unit: String = "pcs",
    val minStock: Int = 0,
    val purchasePrice: Long = 0L,
    val sellingPrice: Long = 0L,
    val location: String = "Dapur",
    val lastUpdated: String = ""
) {
    fun effectiveVariants(): List<ProductVariant> {
        return if (variants.isNotEmpty()) {
            variants
        } else {
            listOf(
                ProductVariant(
                    id = "v-$id-default",
                    name = category.ifBlank { "Original" },
                    purchasePrice = purchasePrice,
                    sellingPrice = sellingPrice
                )
            )
        }
    }
}

data class CartItem(
    val cartItemKey: String,
    val productId: String,
    val barcode: String,
    val name: String,
    val category: String,
    val qty: Int,
    val unit: String = "pcs",
    val unitCost: Long = 0L,
    val unitPrice: Long,
    val subtotal: Long,
    val availableStock: Int = 999
)

data class TransactionRecord(
    val id: String,
    val code: String,
    val type: TransactionType,
    val timestamp: String,
    val operatorRole: UserRole,
    val operatorName: String,
    val items: List<CartItem>,
    val totalQty: Int,
    val totalAmount: Long,
    val recipientOrSupplier: String,
    val referenceNo: String = "-",
    val note: String = "",
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val syncError: String? = null,
    val syncedAt: String? = null
)

data class GoogleSheetConfig(
    val webhookUrl: String = "",
    val spreadsheetUrl: String = "",
    val autoSync: Boolean = true,
    val lastSyncAt: String? = null,
    val isConnected: Boolean = false
)

data class PrinterSettings(
    val connectionMode: String = "browser", // "browser" or "bluetooth"
    val paperSize: String = "58mm",         // "58mm" or "80mm"
    val bluetoothDeviceName: String = "",
    val bluetoothDeviceAddress: String = "",
    val autoPrintAfterCheckout: Boolean = false,
    val printCopies: Int = 1,
    val feedLinesAfterPrint: Int = 3,
    val showStoreAddress: Boolean = true,
    val showStorePhone: Boolean = true,
    val showCashierName: Boolean = true,
    val showReceiptLogo: Boolean = true,
    val receiptLogoDataUrl: String = "",
    val showReceiptFooter: Boolean = true,
    val customHeaderNote: String = "Terang Bulan / Martabak Manis"
)

data class StoreSettings(
    val storeName: String = "Warung Senja Terang Bulan",
    val storeAddress: String = "Jl. Raya Bedadung Wisata Senja",
    val storePhone: String = "085881028733",
    val operationalHours: String = "Buka Jam 15.00 - 21.00",
    val receiptFooter: String = "Terima kasih telah membeli di Warung Senja Terang Bulan!",
    val ownerPin: String = "1234",
    val kasirPin: String = "0000",
    val allowManualBarcode: Boolean = false,
    val enableScanSound: Boolean = true,
    val printer: PrinterSettings = PrinterSettings()
)

data class BluetoothDeviceItem(
    val name: String,
    val address: String
)

data class DashboardStats(
    val totalMenu: Int,
    val totalSalesCount: Int,
    val totalSalesQty: Int,
    val totalRevenue: Long,
    val totalModal: Long,
    val totalLaba: Long
)

data class BackupPayload(
    val products: List<ProductItem>,
    val transactions: List<TransactionRecord>,
    val settings: StoreSettings,
    val sheetConfig: GoogleSheetConfig
)

data class SyncResult(
    val success: Boolean,
    val message: String,
    val fetchedItems: List<ProductItem> = emptyList()
)
