package com.warungsenja.kasirterangbulan.data.local

import android.content.Context
import android.content.SharedPreferences
import com.warungsenja.kasirterangbulan.domain.model.BackupPayload
import com.warungsenja.kasirterangbulan.domain.model.CartItem
import com.warungsenja.kasirterangbulan.domain.model.GoogleSheetConfig
import com.warungsenja.kasirterangbulan.domain.model.PrinterSettings
import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.ProductVariant
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.SyncStatus
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.domain.model.TransactionType
import com.warungsenja.kasirterangbulan.domain.model.UserProfile
import com.warungsenja.kasirterangbulan.domain.model.UserRole
import org.json.JSONArray
import org.json.JSONObject

class LocalStorageDataSource(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("warung_senja_terang_bulan_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRODUCTS = "tb_pos_products_v2"
        private const val KEY_TRANSACTIONS = "tb_pos_transactions_v2"
        private const val KEY_SETTINGS = "tb_pos_settings_v2"
        private const val KEY_SHEET_CONFIG = "tb_pos_sheet_config_v2"
        private const val KEY_CURRENT_USER = "tb_pos_current_user_v2"
    }

    fun getProducts(): List<ProductItem> {
        val raw = prefs.getString(KEY_PRODUCTS, null)
        if (raw == null) {
            saveProducts(InitialData.INITIAL_PRODUCTS)
            return InitialData.INITIAL_PRODUCTS
        }
        return try {
            parseProductsArray(JSONArray(raw))
        } catch (_: Exception) {
            InitialData.INITIAL_PRODUCTS
        }
    }

    fun saveProducts(products: List<ProductItem>) {
        prefs.edit().putString(KEY_PRODUCTS, serializeProductsArray(products).toString()).apply()
    }

    fun getTransactions(): List<TransactionRecord> {
        val raw = prefs.getString(KEY_TRANSACTIONS, null)
        if (raw == null) {
            saveTransactions(InitialData.INITIAL_TRANSACTIONS)
            return InitialData.INITIAL_TRANSACTIONS
        }
        return try {
            parseTransactionsArray(JSONArray(raw))
        } catch (_: Exception) {
            InitialData.INITIAL_TRANSACTIONS
        }
    }

    fun saveTransactions(transactions: List<TransactionRecord>) {
        prefs.edit().putString(KEY_TRANSACTIONS, serializeTransactionsArray(transactions).toString()).apply()
    }

    fun getSettings(): StoreSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return InitialData.INITIAL_SETTINGS
        return try {
            parseSettingsObject(JSONObject(raw))
        } catch (_: Exception) {
            InitialData.INITIAL_SETTINGS
        }
    }

    fun saveSettings(settings: StoreSettings) {
        prefs.edit().putString(KEY_SETTINGS, serializeSettingsObject(settings).toString()).apply()
    }

    fun getSheetConfig(): GoogleSheetConfig {
        val raw = prefs.getString(KEY_SHEET_CONFIG, null) ?: return InitialData.INITIAL_SHEET_CONFIG
        return try {
            parseSheetConfigObject(JSONObject(raw))
        } catch (_: Exception) {
            InitialData.INITIAL_SHEET_CONFIG
        }
    }

    fun saveSheetConfig(config: GoogleSheetConfig) {
        prefs.edit().putString(KEY_SHEET_CONFIG, serializeSheetConfigObject(config).toString()).apply()
    }

    fun getCurrentUser(): UserProfile? {
        val raw = prefs.getString(KEY_CURRENT_USER, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            UserProfile(
                id = obj.optString("id", "user-kasir"),
                name = obj.optString("name", "Kasir 1"),
                role = UserRole.fromRaw(obj.optString("role", "kasir")),
                pin = obj.optString("pin", "0000")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveCurrentUser(user: UserProfile?) {
        if (user == null) {
            prefs.edit().remove(KEY_CURRENT_USER).apply()
        } else {
            val obj = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("role", user.role.rawValue)
                put("pin", user.pin)
            }
            prefs.edit().putString(KEY_CURRENT_USER, obj.toString()).apply()
        }
    }

    fun exportBackupJson(): String {
        val root = JSONObject().apply {
            put("app", "Kasir Terang Bulan — Warung Senja")
            put("version", "2.0")
            put("products", serializeProductsArray(getProducts()))
            put("transactions", serializeTransactionsArray(getTransactions()))
            put("settings", serializeSettingsObject(getSettings()))
            put("sheetConfig", serializeSheetConfigObject(getSheetConfig()))
        }
        return root.toString(2)
    }

    fun restoreBackupJson(rawJson: String): BackupPayload {
        val root = JSONObject(rawJson)
        val products = root.optJSONArray("products")?.let { parseProductsArray(it) } ?: emptyList()
        val transactions =
            root.optJSONArray("transactions")?.let { parseTransactionsArray(it) } ?: emptyList()
        val settings = root.optJSONObject("settings")?.let { parseSettingsObject(it) }
            ?: InitialData.INITIAL_SETTINGS
        val sheetConfig = root.optJSONObject("sheetConfig")?.let { parseSheetConfigObject(it) }
            ?: InitialData.INITIAL_SHEET_CONFIG

        saveProducts(products)
        saveTransactions(transactions)
        saveSettings(settings)
        saveSheetConfig(sheetConfig)

        return BackupPayload(
            products = products,
            transactions = transactions,
            settings = settings,
            sheetConfig = sheetConfig
        )
    }

    fun resetAllData(): BackupPayload {
        val emptyProducts = emptyList<ProductItem>()
        val emptyTransactions = emptyList<TransactionRecord>()
        val currentSettings = getSettings()
        val currentSheetConfig = getSheetConfig()

        saveProducts(emptyProducts)
        saveTransactions(emptyTransactions)

        return BackupPayload(
            products = emptyProducts,
            transactions = emptyTransactions,
            settings = currentSettings,
            sheetConfig = currentSheetConfig
        )
    }

    private fun serializeProductsArray(products: List<ProductItem>): JSONArray {
        val arr = JSONArray()
        for (p in products) {
            val variantsArr = JSONArray()
            for (v in p.variants) {
                variantsArr.put(
                    JSONObject().apply {
                        put("id", v.id)
                        put("name", v.name)
                        put("purchasePrice", v.purchasePrice)
                        put("sellingPrice", v.sellingPrice)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("barcode", p.barcode)
                    put("name", p.name)
                    put("category", p.category)
                    put("variants", variantsArr)
                    put("stock", p.stock)
                    put("unit", p.unit)
                    put("minStock", p.minStock)
                    put("purchasePrice", p.purchasePrice)
                    put("sellingPrice", p.sellingPrice)
                    put("location", p.location)
                    put("lastUpdated", p.lastUpdated)
                }
            )
        }
        return arr
    }

    private fun parseProductsArray(arr: JSONArray): List<ProductItem> {
        val list = mutableListOf<ProductItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val variantsList = mutableListOf<ProductVariant>()
            val variantsArr = obj.optJSONArray("variants")
            if (variantsArr != null) {
                for (j in 0 until variantsArr.length()) {
                    val vObj = variantsArr.optJSONObject(j) ?: continue
                    variantsList.add(
                        ProductVariant(
                            id = vObj.optString("id", "v-$i-$j"),
                            name = vObj.optString("name", "Original"),
                            purchasePrice = vObj.optLong("purchasePrice", 0L),
                            sellingPrice = vObj.optLong("sellingPrice", 0L)
                        )
                    )
                }
            }
            list.add(
                ProductItem(
                    id = obj.optString("id", "tb-$i"),
                    barcode = obj.optString("barcode", "%02d".format(i + 1)),
                    name = obj.optString("name", "Menu Terang Bulan"),
                    category = obj.optString("category", "Original"),
                    variants = variantsList,
                    stock = obj.optInt("stock", 999),
                    unit = obj.optString("unit", "pcs"),
                    minStock = obj.optInt("minStock", 0),
                    purchasePrice = obj.optLong("purchasePrice", 0L),
                    sellingPrice = obj.optLong("sellingPrice", 0L),
                    location = obj.optString("location", "Dapur"),
                    lastUpdated = obj.optString("lastUpdated", "")
                )
            )
        }
        return list
    }

    private fun serializeTransactionsArray(transactions: List<TransactionRecord>): JSONArray {
        val arr = JSONArray()
        for (t in transactions) {
            val itemsArr = JSONArray()
            for (item in t.items) {
                itemsArr.put(
                    JSONObject().apply {
                        put("cartItemKey", item.cartItemKey)
                        put("productId", item.productId)
                        put("barcode", item.barcode)
                        put("name", item.name)
                        put("category", item.category)
                        put("qty", item.qty)
                        put("unit", item.unit)
                        put("unitCost", item.unitCost)
                        put("unitPrice", item.unitPrice)
                        put("subtotal", item.subtotal)
                        put("availableStock", item.availableStock)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("code", t.code)
                    put("type", t.type.rawValue)
                    put("timestamp", t.timestamp)
                    put("operatorRole", t.operatorRole.rawValue)
                    put("operatorName", t.operatorName)
                    put("items", itemsArr)
                    put("totalQty", t.totalQty)
                    put("totalAmount", t.totalAmount)
                    put("recipientOrSupplier", t.recipientOrSupplier)
                    put("referenceNo", t.referenceNo)
                    put("note", t.note)
                    put("syncStatus", t.syncStatus.rawValue)
                    put("syncError", t.syncError ?: "")
                    put("syncedAt", t.syncedAt ?: "")
                }
            )
        }
        return arr
    }

    private fun parseTransactionsArray(arr: JSONArray): List<TransactionRecord> {
        val list = mutableListOf<TransactionRecord>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val itemsList = mutableListOf<CartItem>()
            val itemsArr = obj.optJSONArray("items")
            if (itemsArr != null) {
                for (j in 0 until itemsArr.length()) {
                    val iObj = itemsArr.optJSONObject(j) ?: continue
                    val pId = iObj.optString("productId", "tb-$j")
                    itemsList.add(
                        CartItem(
                            cartItemKey = iObj.optString("cartItemKey", pId),
                            productId = pId,
                            barcode = iObj.optString("barcode", ""),
                            name = iObj.optString("name", ""),
                            category = iObj.optString("category", ""),
                            qty = iObj.optInt("qty", 1),
                            unit = iObj.optString("unit", "pcs"),
                            unitCost = iObj.optLong("unitCost", 0L),
                            unitPrice = iObj.optLong("unitPrice", 0L),
                            subtotal = iObj.optLong("subtotal", 0L),
                            availableStock = iObj.optInt("availableStock", 999)
                        )
                    )
                }
            }
            list.add(
                TransactionRecord(
                    id = obj.optString("id", "trx-$i"),
                    code = obj.optString("code", "TB-000"),
                    type = TransactionType.fromRaw(obj.optString("type", "OUT")),
                    timestamp = obj.optString("timestamp", ""),
                    operatorRole = UserRole.fromRaw(obj.optString("operatorRole", "kasir")),
                    operatorName = obj.optString("operatorName", "Kasir 1"),
                    items = itemsList,
                    totalQty = obj.optInt("totalQty", 0),
                    totalAmount = obj.optLong("totalAmount", 0L),
                    recipientOrSupplier = obj.optString("recipientOrSupplier", "Pelanggan Umum"),
                    referenceNo = obj.optString("referenceNo", "-"),
                    note = obj.optString("note", ""),
                    syncStatus = SyncStatus.fromRaw(obj.optString("syncStatus", "local")),
                    syncError = obj.optString("syncError").takeIf { it.isNotBlank() },
                    syncedAt = obj.optString("syncedAt").takeIf { it.isNotBlank() }
                )
            )
        }
        return list
    }

    private fun serializeSettingsObject(s: StoreSettings): JSONObject {
        val p = s.printer
        val printerObj = JSONObject().apply {
            put("connectionMode", p.connectionMode)
            put("paperSize", p.paperSize)
            put("bluetoothDeviceName", p.bluetoothDeviceName)
            put("bluetoothDeviceAddress", p.bluetoothDeviceAddress)
            put("autoPrintAfterCheckout", p.autoPrintAfterCheckout)
            put("printCopies", p.printCopies)
            put("feedLinesAfterPrint", p.feedLinesAfterPrint)
            put("showStoreAddress", p.showStoreAddress)
            put("showStorePhone", p.showStorePhone)
            put("showCashierName", p.showCashierName)
            put("showReceiptLogo", p.showReceiptLogo)
            put("receiptLogoDataUrl", p.receiptLogoDataUrl)
            put("showReceiptFooter", p.showReceiptFooter)
            put("customHeaderNote", p.customHeaderNote)
        }
        return JSONObject().apply {
            put("storeName", s.storeName)
            put("storeAddress", s.storeAddress)
            put("storePhone", s.storePhone)
            put("operationalHours", s.operationalHours)
            put("receiptFooter", s.receiptFooter)
            put("ownerPin", s.ownerPin)
            put("kasirPin", s.kasirPin)
            put("allowManualBarcode", s.allowManualBarcode)
            put("enableScanSound", s.enableScanSound)
            put("printer", printerObj)
        }
    }

    private fun parseSettingsObject(obj: JSONObject): StoreSettings {
        val def = InitialData.INITIAL_SETTINGS
        val pObj = obj.optJSONObject("printer")
        val pDef = def.printer
        val printer = if (pObj != null) {
            PrinterSettings(
                connectionMode = pObj.optString("connectionMode", pDef.connectionMode),
                paperSize = pObj.optString("paperSize", pDef.paperSize),
                bluetoothDeviceName = pObj.optString("bluetoothDeviceName", pDef.bluetoothDeviceName),
                bluetoothDeviceAddress = pObj.optString("bluetoothDeviceAddress", pDef.bluetoothDeviceAddress),
                autoPrintAfterCheckout = pObj.optBoolean("autoPrintAfterCheckout", pDef.autoPrintAfterCheckout),
                printCopies = pObj.optInt("printCopies", pDef.printCopies),
                feedLinesAfterPrint = pObj.optInt("feedLinesAfterPrint", pDef.feedLinesAfterPrint),
                showStoreAddress = pObj.optBoolean("showStoreAddress", pDef.showStoreAddress),
                showStorePhone = pObj.optBoolean("showStorePhone", pDef.showStorePhone),
                showCashierName = pObj.optBoolean("showCashierName", pDef.showCashierName),
                showReceiptLogo = pObj.optBoolean("showReceiptLogo", pDef.showReceiptLogo),
                receiptLogoDataUrl = pObj.optString("receiptLogoDataUrl", pDef.receiptLogoDataUrl),
                showReceiptFooter = pObj.optBoolean("showReceiptFooter", pDef.showReceiptFooter),
                customHeaderNote = pObj.optString("customHeaderNote", pDef.customHeaderNote)
            )
        } else {
            pDef
        }
        val rawHours = obj.optString("operationalHours", def.operationalHours)
        val normalizedHours = if (rawHours == "Setiap Hari, 16:00 - 23:00 WIB") {
            "Buka Jam 15.00 - 21.00"
        } else {
            rawHours
        }
        return StoreSettings(
            storeName = obj.optString("storeName", def.storeName),
            storeAddress = obj.optString("storeAddress", def.storeAddress),
            storePhone = obj.optString("storePhone", def.storePhone),
            operationalHours = normalizedHours,
            receiptFooter = obj.optString("receiptFooter", def.receiptFooter),
            ownerPin = obj.optString("ownerPin", def.ownerPin),
            kasirPin = obj.optString("kasirPin", def.kasirPin),
            allowManualBarcode = obj.optBoolean("allowManualBarcode", def.allowManualBarcode),
            enableScanSound = obj.optBoolean("enableScanSound", def.enableScanSound),
            printer = printer
        )
    }

    private fun serializeSheetConfigObject(c: GoogleSheetConfig): JSONObject {
        return JSONObject().apply {
            put("webhookUrl", c.webhookUrl)
            put("spreadsheetUrl", c.spreadsheetUrl)
            put("autoSync", c.autoSync)
            put("lastSyncAt", c.lastSyncAt ?: "")
            put("isConnected", c.isConnected)
        }
    }

    private fun parseSheetConfigObject(obj: JSONObject): GoogleSheetConfig {
        return GoogleSheetConfig(
            webhookUrl = obj.optString("webhookUrl", ""),
            spreadsheetUrl = obj.optString("spreadsheetUrl", ""),
            autoSync = obj.optBoolean("autoSync", true),
            lastSyncAt = obj.optString("lastSyncAt").takeIf { it.isNotBlank() },
            isConnected = obj.optBoolean("isConnected", false)
        )
    }
}
