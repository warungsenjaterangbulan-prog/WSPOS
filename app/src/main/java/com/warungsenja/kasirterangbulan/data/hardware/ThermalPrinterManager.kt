package com.warungsenja.kasirterangbulan.data.hardware

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.warungsenja.kasirterangbulan.domain.model.BluetoothDeviceItem
import com.warungsenja.kasirterangbulan.domain.model.CartItem
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.domain.model.TransactionType
import com.warungsenja.kasirterangbulan.domain.model.UserRole
import com.warungsenja.kasirterangbulan.presentation.util.Formatter
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.UUID

class ThermalPrinterManager(private val context: Context) {

    companion object {
        const val SPP_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"
        private val SPP_UUID: UUID = UUID.fromString(SPP_UUID_STRING)
    }

    private var activeSocket: BluetoothSocket? = null
    private var activeOutputStream: OutputStream? = null
    private var connectedAddress: String? = null
    private var printWebView: WebView? = null

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceItem> {
        return try {
            val adapter = getBluetoothAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices?.map { dev ->
                BluetoothDeviceItem(
                    name = dev.name ?: "Printer Bluetooth",
                    address = dev.address ?: ""
                )
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun connectBluetooth(address: String): Pair<Boolean, String> {
        if (address.isBlank()) {
            return false to "Alamat MAC printer Bluetooth kosong."
        }
        return try {
            val adapter = getBluetoothAdapter()
                ?: return false to "Bluetooth tidak tersedia pada perangkat ini."
            if (!adapter.isEnabled) {
                return false to "Bluetooth belum aktif. Silakan nyalakan Bluetooth."
            }
            if (activeSocket?.isConnected == true && connectedAddress.equals(address, ignoreCase = true)) {
                return true to "Sudah terhubung ke printer ($address)."
            }
            disconnectBluetooth()

            val device = adapter.getRemoteDevice(address.uppercase())
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            activeSocket = socket
            activeOutputStream = socket.outputStream
            connectedAddress = address
            true to "Terhubung ke printer ${device.name ?: address}."
        } catch (se: SecurityException) {
            false to "Izin Bluetooth belum diberikan: ${se.localizedMessage ?: ""}"
        } catch (e: Exception) {
            disconnectBluetooth()
            false to "Gagal menghubungkan printer ($address): ${e.localizedMessage ?: "Pastikan printer menyala"}"
        }
    }

    @Synchronized
    fun disconnectBluetooth(): Pair<Boolean, String> {
        return try {
            activeOutputStream?.close()
        } catch (_: Exception) {
        } finally {
            try {
                activeSocket?.close()
            } catch (_: Exception) {
            }
            activeOutputStream = null
            activeSocket = null
            connectedAddress = null
        }.let { true to "Koneksi printer Bluetooth diputus." }
    }

    fun playCheckoutSound(enabled: Boolean) {
        if (!enabled) return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        } catch (_: Exception) {
        }
    }

    fun printReceipt(
        activity: Activity?,
        transaction: TransactionRecord,
        settings: StoreSettings
    ): Pair<Boolean, String> {
        return if (settings.printer.connectionMode == "bluetooth") {
            printViaBluetoothEscPos(transaction, settings)
        } else {
            printViaAndroidPrintManager(activity, transaction, settings)
        }
    }

    fun printTestReceipt(activity: Activity?, settings: StoreSettings): Pair<Boolean, String> {
        val sampleTrx = TransactionRecord(
            id = "test-print",
            code = "TB-TEST-001",
            type = TransactionType.OUT,
            timestamp = Formatter.nowIsoString(),
            operatorRole = UserRole.KASIR,
            operatorName = "Kasir 1",
            items = listOf(
                CartItem(
                    cartItemKey = "tb-002",
                    productId = "tb-002",
                    barcode = "02",
                    name = "Terang Bulan Coklat Lumer",
                    category = "Coklat",
                    qty = 1,
                    unit = "pcs",
                    unitPrice = 20000L,
                    subtotal = 20000L
                ),
                CartItem(
                    cartItemKey = "tb-004",
                    productId = "tb-004",
                    barcode = "04",
                    name = "Terang Bulan Coklat Keju",
                    category = "Coklat Keju",
                    qty = 1,
                    unit = "pcs",
                    unitPrice = 25000L,
                    subtotal = 25000L
                )
            ),
            totalQty = 2,
            totalAmount = 45000L,
            recipientOrSupplier = "Pelanggan Test Print",
            referenceNo = "TEST-01",
            note = "Test cetak struk thermal 58mm / 80mm"
        )
        return printReceipt(activity, sampleTrx, settings)
    }

    @Synchronized
    private fun printViaBluetoothEscPos(
        transaction: TransactionRecord,
        settings: StoreSettings
    ): Pair<Boolean, String> {
        val targetAddr = settings.printer.bluetoothDeviceAddress.trim()
        if (activeSocket?.isConnected != true || activeOutputStream == null) {
            if (targetAddr.isNotBlank()) {
                val (connected, msg) = connectBluetooth(targetAddr)
                if (!connected) return false to msg
            } else {
                return false to "Pilih atau hubungkan perangkat printer Bluetooth terlebih dahulu."
            }
        }
        return try {
            val out = activeOutputStream ?: return false to "Stream printer tidak tersedia."
            val bytes = buildEscPosBytes(transaction, settings)
            val copies = settings.printer.printCopies.coerceIn(1, 3)
            repeat(copies) {
                out.write(bytes)
                out.flush()
            }
            true to "Struk berhasil dicetak ke printer thermal Bluetooth."
        } catch (e: Exception) {
            disconnectBluetooth()
            false to "Gagal mengirim data cetak ke printer Bluetooth: ${e.localizedMessage ?: ""}"
        }
    }

    private fun buildEscPosBytes(trx: TransactionRecord, settings: StoreSettings): ByteArray {
        val width = if (settings.printer.paperSize == "80mm") 48 else 32
        val divider = "-".repeat(width)
        val doubleDivider = "=".repeat(width)
        val out = ByteArrayOutputStream()

        fun writeBytes(vararg b: Int) {
            for (v in b) out.write(v)
        }

        fun writeLine(text: String = "") {
            val clean = text.replace(Regex("[^\\x20-\\x7E]"), " ")
            out.write(clean.toByteArray(Charsets.US_ASCII))
            out.write(0x0A)
        }

        fun leftRight(left: String, right: String): String {
            val safeLeft = left.take((width - right.length - 1).coerceAtLeast(1))
            val spaces = (width - safeLeft.length - right.length).coerceAtLeast(1)
            return safeLeft + " ".repeat(spaces) + right
        }

        // ESC @ Initialize
        writeBytes(0x1B, 0x40)
        // Center + Bold
        writeBytes(0x1B, 0x61, 0x01, 0x1B, 0x45, 0x01)
        writeLine(settings.storeName.ifBlank { "WARUNG SENJA TERANG BULAN" }.uppercase())
        writeBytes(0x1B, 0x45, 0x00)

        if (settings.printer.customHeaderNote.isNotBlank()) {
            writeLine(settings.printer.customHeaderNote)
        }
        if (settings.printer.showStoreAddress && settings.storeAddress.isNotBlank()) {
            writeLine(settings.storeAddress)
        }
        if (settings.printer.showStorePhone && settings.storePhone.isNotBlank()) {
            writeLine("Telp: ${settings.storePhone}")
        }

        writeLine(doubleDivider)
        writeBytes(0x1B, 0x45, 0x01)
        writeLine(if (trx.type == TransactionType.OUT) "STRUK PENJUALAN" else "BUKTI STOCK MASUK")
        writeBytes(0x1B, 0x45, 0x00)
        writeLine(divider)

        // Left align
        writeBytes(0x1B, 0x61, 0x00)
        writeLine(leftRight("No. TRX", trx.code))
        writeLine(leftRight("Tanggal", Formatter.formatDateTime(trx.timestamp)))
        if (settings.printer.showCashierName) {
            writeLine(leftRight("Operator", trx.operatorName))
        }
        if (trx.recipientOrSupplier.isNotBlank()) {
            writeLine(leftRight("Pelanggan", trx.recipientOrSupplier))
        }
        writeLine(divider)

        for (item in trx.items) {
            val label = if (item.category.isNotBlank()) "${item.name} (${item.category})" else item.name
            writeLine(label.take(width))
            writeLine(
                leftRight(
                    "${item.qty} x ${Formatter.formatRupiah(item.unitPrice)}",
                    Formatter.formatRupiah(item.subtotal)
                )
            )
        }

        writeLine(divider)
        writeBytes(0x1B, 0x45, 0x01)
        writeLine(leftRight("TOTAL QTY", "${trx.totalQty} pcs"))
        writeLine(leftRight("TOTAL", Formatter.formatRupiah(trx.totalAmount)))
        writeBytes(0x1B, 0x45, 0x00)
        writeLine(divider)

        if (trx.note.isNotBlank()) {
            writeLine("Catatan: ${trx.note}")
            writeLine(divider)
        }

        writeBytes(0x1B, 0x61, 0x01)
        if (settings.printer.showReceiptFooter && settings.receiptFooter.isNotBlank()) {
            writeLine(settings.receiptFooter)
        }

        val feeds = settings.printer.feedLinesAfterPrint.coerceIn(1, 8)
        repeat(feeds) { out.write(0x0A) }
        return out.toByteArray()
    }

    private fun printViaAndroidPrintManager(
        activity: Activity?,
        trx: TransactionRecord,
        settings: StoreSettings
    ): Pair<Boolean, String> {
        if (activity == null) {
            return false to "Activity cetak tidak tersedia."
        }
        return try {
            val html = buildReceiptHtml(trx, settings)
            activity.runOnUiThread {
                val webView = WebView(activity)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printManager =
                            activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                        val jobName = "Struk_${trx.code}"
                        val adapter = webView.createPrintDocumentAdapter(jobName)
                        printManager?.print(
                            jobName,
                            adapter,
                            PrintAttributes.Builder().build()
                        )
                        printWebView = null
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
                printWebView = webView
            }
            true to "Dialog cetak struk dibuka."
        } catch (e: Exception) {
            false to "Gagal membuka dialog cetak: ${e.localizedMessage ?: ""}"
        }
    }

    private fun buildReceiptHtml(trx: TransactionRecord, settings: StoreSettings): String {
        val is80mm = settings.printer.paperSize == "80mm"
        val widthMm = if (is80mm) "80mm" else "58mm"
        val p = settings.printer
        val itemsHtml = trx.items.joinToString("") { item ->
            """
            <div style="margin-bottom:5px;">
              <div style="font-weight:bold;">${item.name}${if (item.category.isNotBlank()) " (${item.category})" else ""}</div>
              <div style="display:flex;justify-content:space-between;">
                <span>${item.qty} ${item.unit} x ${Formatter.formatRupiah(item.unitPrice)}</span>
                <span>${Formatter.formatRupiah(item.subtotal)}</span>
              </div>
            </div>
            """.trimIndent()
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8" />
              <style>
                body { width: $widthMm; margin: 0 auto; padding: 4mm; font-family: monospace; font-size: 11px; color: #000; }
                .center { text-align: center; }
                .divider { border-top: 1px dashed #000; margin: 6px 0; }
                .row { display: flex; justify-content: space-between; }
              </style>
            </head>
            <body>
              <div class="center">
                <div style="font-weight:bold;font-size:14px;">${settings.storeName}</div>
                ${if (p.customHeaderNote.isNotBlank()) "<div>${p.customHeaderNote}</div>" else ""}
                ${if (p.showStoreAddress) "<div>${settings.storeAddress}</div>" else ""}
                ${if (p.showStorePhone) "<div>Telp/WA: ${settings.storePhone}</div>" else ""}
                <div>${settings.operationalHours}</div>
              </div>
              <div class="divider"></div>
              <div class="row"><span>No. TRX</span><b>${trx.code}</b></div>
              <div class="row"><span>Waktu</span><span>${Formatter.formatDateTime(trx.timestamp)}</span></div>
              <div class="row"><span>Pelanggan</span><span>${trx.recipientOrSupplier}</span></div>
              <div class="divider"></div>
              $itemsHtml
              <div class="divider"></div>
              <div class="row" style="font-weight:bold;">
                <span>TOTAL (${trx.totalQty} pcs)</span>
                <span>${Formatter.formatRupiah(trx.totalAmount)}</span>
              </div>
              ${if (trx.note.isNotBlank()) "<div class='divider'></div><div>Catatan: ${trx.note}</div>" else ""}
              <div class="divider"></div>
              ${if (p.showReceiptFooter) "<div class='center'>${settings.receiptFooter}</div>" else ""}
            </body>
            </html>
        """.trimIndent()
    }
}
