package com.warungsenja.kasirterangbulan.data.remote

import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.ProductVariant
import com.warungsenja.kasirterangbulan.domain.model.SyncResult
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GoogleSheetsDataSource {

    companion object {
        const val APPS_SCRIPT_TEMPLATE = """function doPost(e) {
  var lock = LockService.getScriptLock();
  lock.tryLock(10000);
  try {
    var payload = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var action = payload.action;

    if (action === 'PING') {
      return responseJson({ success: true, message: 'Terhubung ke Google Sheets Warung Senja Terang Bulan' });
    }

    if (action === 'SYNC_STOCK' && payload.products) {
      var stockSheet = getOrCreateSheet(ss, 'Data_Stock', [
        'No. Menu', 'Nama Menu', 'Varian', 'Satuan', 'Harga Beli', 'Harga Jual', 'Terakhir Update'
      ]);
      if (stockSheet.getLastRow() > 1) {
        stockSheet.getRange(2, 1, stockSheet.getLastRow() - 1, 7).clearContent();
      }
      var rows = payload.products.map(function(p) {
        return [p.barcode, p.name, p.category, p.unit, p.purchasePrice, p.sellingPrice, p.lastUpdated];
      });
      if (rows.length > 0) {
        stockSheet.getRange(2, 1, rows.length, 7).setValues(rows);
      }
      return responseJson({ success: true, message: 'Data_Stock berhasil diperbarui' });
    }

    if (action === 'SYNC_TRANSACTION' && payload.transaction) {
      var trx = payload.transaction;
      var sheet = getOrCreateSheet(ss, 'Stock_Keluar', [
        'ID Transaksi', 'Kode TRX', 'Tanggal & Waktu', 'Jenis', 'No. Menu', 'Nama Menu',
        'Varian', 'Qty Keluar', 'Satuan', 'Harga Jual Satuan', 'Total Subtotal',
        'Customer / Tujuan', 'Operator', 'Role', 'Catatan'
      ]);
      trx.items.forEach(function(item) {
        sheet.appendRow([
          trx.id, trx.code, trx.timestamp, trx.type, item.barcode, item.name,
          item.category, item.qty, item.unit, item.unitPrice, item.subtotal,
          trx.recipientOrSupplier, trx.operatorName, trx.operatorRole, trx.note
        ]);
      });
      return responseJson({ success: true, message: 'Transaksi tercatat di Stock_Keluar' });
    }

    return responseJson({ success: false, message: 'Action tidak dikenali' });
  } catch (err) {
    return responseJson({ success: false, message: err.toString() });
  } finally {
    lock.releaseLock();
  }
}

function getOrCreateSheet(ss, name, headers) {
  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    sheet.appendRow(headers);
  }
  return sheet;
}

function responseJson(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}"""
    }

    suspend fun pingConnection(webhookUrl: String): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext SyncResult(false, "URL Webhook Google Apps Script masih kosong.")
        }
        val body = JSONObject().apply {
            put("action", "PING")
            put("timestamp", System.currentTimeMillis())
        }
        postJson(cleanUrl, body, "Koneksi ke Google Sheets berhasil!")
    }

    suspend fun syncTransaction(
        webhookUrl: String,
        transaction: TransactionRecord,
        products: List<ProductItem>
    ): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext SyncResult(false, "URL Webhook belum dikonfigurasi.")
        }
        val trxObj = JSONObject().apply {
            put("id", transaction.id)
            put("code", transaction.code)
            put("type", transaction.type.rawValue)
            put("timestamp", transaction.timestamp)
            put("operatorRole", transaction.operatorRole.rawValue)
            put("operatorName", transaction.operatorName)
            put("totalQty", transaction.totalQty)
            put("totalAmount", transaction.totalAmount)
            put("recipientOrSupplier", transaction.recipientOrSupplier)
            put("note", transaction.note)
            val itemsArr = JSONArray()
            for (item in transaction.items) {
                itemsArr.put(
                    JSONObject().apply {
                        put("barcode", item.barcode)
                        put("name", item.name)
                        put("category", item.category)
                        put("qty", item.qty)
                        put("unit", item.unit)
                        put("unitPrice", item.unitPrice)
                        put("subtotal", item.subtotal)
                    }
                )
            }
            put("items", itemsArr)
        }
        val body = JSONObject().apply {
            put("action", "SYNC_TRANSACTION")
            put("transaction", trxObj)
            put("products", buildProductsJsonArray(products))
        }
        postJson(cleanUrl, body, "Transaksi ${transaction.code} berhasil disinkronkan ke Google Sheets.")
    }

    suspend fun syncAllStock(
        webhookUrl: String,
        products: List<ProductItem>
    ): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext SyncResult(false, "URL Webhook belum dikonfigurasi.")
        }
        val body = JSONObject().apply {
            put("action", "SYNC_STOCK")
            put("products", buildProductsJsonArray(products))
        }
        postJson(cleanUrl, body, "${products.size} menu berhasil dikirim ke sheet Data_Stock.")
    }

    suspend fun fetchStockFromSheet(webhookUrl: String): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext SyncResult(false, "URL Webhook belum dikonfigurasi.")
        }
        try {
            val urlWithParam = if (cleanUrl.contains("?")) "$cleanUrl&action=GET_STOCK" else "$cleanUrl?action=GET_STOCK"
            val conn = (URL(urlWithParam).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            val responseText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = JSONObject(responseText)
            val arr = json.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<ProductItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val cat = obj.optString("category", "Original")
                val buy = obj.optLong("purchasePrice", 10000L)
                val sell = obj.optLong("sellingPrice", 20000L)
                items.add(
                    ProductItem(
                        id = "tb-sheet-$i-${System.currentTimeMillis()}",
                        barcode = obj.optString("barcode", "%02d".format(i + 1)),
                        name = obj.optString("name", "Menu Terang Bulan"),
                        category = cat,
                        variants = listOf(ProductVariant("v-sheet-$i", cat, buy, sell)),
                        stock = 999,
                        unit = obj.optString("unit", "pcs"),
                        purchasePrice = buy,
                        sellingPrice = sell
                    )
                )
            }
            SyncResult(
                success = true,
                message = "Berhasil mengambil ${items.size} menu dari Google Sheets.",
                fetchedItems = items
            )
        } catch (e: Exception) {
            SyncResult(false, "Gagal mengambil data dari Sheet: ${e.localizedMessage ?: "Error jaringan"}")
        }
    }

    private fun buildProductsJsonArray(products: List<ProductItem>): JSONArray {
        val arr = JSONArray()
        for (p in products) {
            arr.put(
                JSONObject().apply {
                    put("barcode", p.barcode)
                    put("name", p.name)
                    put("category", p.category)
                    put("unit", p.unit)
                    put("purchasePrice", p.purchasePrice)
                    put("sellingPrice", p.sellingPrice)
                    put("lastUpdated", p.lastUpdated)
                }
            )
        }
        return arr
    }

    private fun postJson(urlStr: String, body: JSONObject, successMessage: String): SyncResult {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "text/plain;charset=utf-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }
            val code = conn.responseCode
            if (code in 200..399) {
                SyncResult(true, successMessage)
            } else {
                SyncResult(false, "HTTP $code saat menghubungi Google Sheets.")
            }
        } catch (e: Exception) {
            SyncResult(false, "Gagal menghubungi Google Sheets: ${e.localizedMessage ?: "Periksa koneksi internet"}")
        }
    }
}
