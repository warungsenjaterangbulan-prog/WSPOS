package com.warungsenja.kasirterangbulan.data.local

import com.warungsenja.kasirterangbulan.domain.model.CartItem
import com.warungsenja.kasirterangbulan.domain.model.GoogleSheetConfig
import com.warungsenja.kasirterangbulan.domain.model.PrinterSettings
import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.ProductVariant
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.SyncStatus
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.domain.model.TransactionType
import com.warungsenja.kasirterangbulan.domain.model.UserRole

object InitialData {
    private const val DEFAULT_TIMESTAMP = "2026-10-04T16:30:00.000Z"

    val INITIAL_SETTINGS = StoreSettings(
        storeName = "Warung Senja Terang Bulan",
        storeAddress = "Jl. Raya Bedadung Wisata Senja",
        storePhone = "085881028733",
        operationalHours = "Buka Jam 15.00 - 21.00",
        receiptFooter = "Terima kasih telah membeli di Warung Senja Terang Bulan!",
        ownerPin = "1234",
        kasirPin = "0000",
        allowManualBarcode = false,
        enableScanSound = true,
        printer = PrinterSettings(
            connectionMode = "browser",
            paperSize = "58mm",
            bluetoothDeviceName = "",
            bluetoothDeviceAddress = "",
            autoPrintAfterCheckout = false,
            printCopies = 1,
            feedLinesAfterPrint = 3,
            showStoreAddress = true,
            showStorePhone = true,
            showCashierName = true,
            showReceiptLogo = true,
            receiptLogoDataUrl = "",
            showReceiptFooter = true,
            customHeaderNote = "Terang Bulan / Martabak Manis"
        )
    )

    val INITIAL_SHEET_CONFIG = GoogleSheetConfig(
        webhookUrl = "",
        spreadsheetUrl = "",
        autoSync = true,
        lastSyncAt = null,
        isConnected = false
    )

    val INITIAL_PRODUCTS: List<ProductItem> = listOf(
        ProductItem(
            id = "tb-001",
            barcode = "01",
            name = "Terang Bulan Original Klasik",
            category = "Original",
            variants = listOf(
                ProductVariant("v-001-1", "Original Biasa", 8000L, 15000L),
                ProductVariant("v-001-2", "Original Spesial", 10000L, 18000L)
            ),
            stock = 25,
            unit = "pcs",
            minStock = 5,
            purchasePrice = 8000L,
            sellingPrice = 15000L,
            location = "Etalase",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-002",
            barcode = "02",
            name = "Terang Bulan Coklat Lumer",
            category = "Coklat",
            variants = listOf(
                ProductVariant("v-002-1", "Coklat Biasa", 10000L, 18000L),
                ProductVariant("v-002-2", "Coklat Spesial", 12000L, 20000L),
                ProductVariant("v-002-3", "Double Coklat", 14000L, 23000L)
            ),
            stock = 30,
            unit = "pcs",
            minStock = 8,
            purchasePrice = 10000L,
            sellingPrice = 18000L,
            location = "Dapur",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-003",
            barcode = "03",
            name = "Terang Bulan Keju Kraft Parut",
            category = "Keju",
            variants = listOf(
                ProductVariant("v-003-1", "Keju Biasa", 12000L, 20000L),
                ProductVariant("v-003-2", "Keju Spesial", 14000L, 22000L),
                ProductVariant("v-003-3", "Extra Keju Melimpah", 16000L, 26000L)
            ),
            stock = 28,
            unit = "pcs",
            minStock = 6,
            purchasePrice = 12000L,
            sellingPrice = 20000L,
            location = "Dapur",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-004",
            barcode = "04",
            name = "Terang Bulan Coklat Keju",
            category = "Coklat Keju",
            variants = listOf(
                ProductVariant("v-004-1", "Coklat Keju Biasa", 13000L, 22000L),
                ProductVariant("v-004-2", "Coklat Keju Spesial", 15000L, 25000L)
            ),
            stock = 35,
            unit = "pcs",
            minStock = 8,
            purchasePrice = 13000L,
            sellingPrice = 22000L,
            location = "Dapur",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-005",
            barcode = "05",
            name = "Terang Bulan Kacang Coklat",
            category = "Kacang",
            variants = listOf(
                ProductVariant("v-005-1", "Kacang Coklat Biasa", 11000L, 18000L),
                ProductVariant("v-005-2", "Kacang Coklat Spesial", 13000L, 20000L)
            ),
            stock = 24,
            unit = "pcs",
            minStock = 5,
            purchasePrice = 11000L,
            sellingPrice = 18000L,
            location = "Dapur",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-006",
            barcode = "06",
            name = "Terang Bulan Kacang Keju",
            category = "Kacang",
            variants = listOf(
                ProductVariant("v-006-1", "Kacang Keju Biasa", 13000L, 22000L),
                ProductVariant("v-006-2", "Kacang Keju Spesial", 15000L, 25000L)
            ),
            stock = 20,
            unit = "pcs",
            minStock = 5,
            purchasePrice = 13000L,
            sellingPrice = 22000L,
            location = "Dapur",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-007",
            barcode = "07",
            name = "Terang Bulan Pisang Coklat",
            category = "Pisang",
            variants = listOf(
                ProductVariant("v-007-1", "Pisang Coklat", 10000L, 18000L),
                ProductVariant("v-007-2", "Pisang Coklat Keju", 13000L, 22000L)
            ),
            stock = 22,
            unit = "pcs",
            minStock = 5,
            purchasePrice = 10000L,
            sellingPrice = 18000L,
            location = "Etalase",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-008",
            barcode = "08",
            name = "Terang Bulan Pisang Keju",
            category = "Pisang",
            variants = listOf(
                ProductVariant("v-008-1", "Pisang Keju Biasa", 12000L, 20000L),
                ProductVariant("v-008-2", "Pisang Keju Spesial", 14000L, 22000L)
            ),
            stock = 20,
            unit = "pcs",
            minStock = 5,
            purchasePrice = 12000L,
            sellingPrice = 20000L,
            location = "Etalase",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-009",
            barcode = "09",
            name = "Terang Bulan Oreo Cream Cheese",
            category = "Oreo",
            variants = listOf(
                ProductVariant("v-009-1", "Oreo Coklat", 14000L, 25000L),
                ProductVariant("v-009-2", "Oreo Cream Cheese", 16000L, 28000L)
            ),
            stock = 18,
            unit = "pcs",
            minStock = 5,
            purchasePrice = 14000L,
            sellingPrice = 25000L,
            location = "Freezer",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-010",
            barcode = "10",
            name = "Terang Bulan Milo Crunchy",
            category = "Milo",
            variants = listOf(
                ProductVariant("v-010-1", "Milo Susu", 15000L, 27000L),
                ProductVariant("v-010-2", "Milo Keju Crunchy", 17000L, 30000L)
            ),
            stock = 16,
            unit = "pcs",
            minStock = 5,
            purchasePrice = 15000L,
            sellingPrice = 27000L,
            location = "Rak Bahan",
            lastUpdated = DEFAULT_TIMESTAMP
        ),
        ProductItem(
            id = "tb-011",
            barcode = "11",
            name = "Terang Bulan Spesial Campur 8 Rasa",
            category = "Spesial",
            variants = listOf(
                ProductVariant("v-011-1", "Spesial 4 Rasa", 16000L, 30000L),
                ProductVariant("v-011-2", "Spesial 8 Rasa", 19000L, 35000L)
            ),
            stock = 15,
            unit = "pcs",
            minStock = 4,
            purchasePrice = 16000L,
            sellingPrice = 30000L,
            location = "Dapur",
            lastUpdated = DEFAULT_TIMESTAMP
        )
    )

    val INITIAL_TRANSACTIONS: List<TransactionRecord> = listOf(
        TransactionRecord(
            id = "trx-initial-1",
            code = "TB-20261004-001",
            type = TransactionType.OUT,
            timestamp = DEFAULT_TIMESTAMP,
            operatorRole = UserRole.KASIR,
            operatorName = "Kasir 1",
            items = listOf(
                CartItem(
                    cartItemKey = "tb-002::coklat spesial::20000",
                    productId = "tb-002",
                    barcode = "02",
                    name = "Terang Bulan Coklat Lumer",
                    category = "Coklat Spesial",
                    qty = 1,
                    unit = "pcs",
                    unitCost = 12000L,
                    unitPrice = 20000L,
                    subtotal = 20000L,
                    availableStock = 30
                ),
                CartItem(
                    cartItemKey = "tb-003::keju spesial::22000",
                    productId = "tb-003",
                    barcode = "03",
                    name = "Terang Bulan Keju Kraft Parut",
                    category = "Keju Spesial",
                    qty = 1,
                    unit = "pcs",
                    unitCost = 14000L,
                    unitPrice = 22000L,
                    subtotal = 22000L,
                    availableStock = 28
                )
            ),
            totalQty = 2,
            totalAmount = 42000L,
            recipientOrSupplier = "Pelanggan Umum",
            referenceNo = "MEJA-01",
            note = "Manis sedang, potong 8",
            syncStatus = SyncStatus.LOCAL
        )
    )
}
