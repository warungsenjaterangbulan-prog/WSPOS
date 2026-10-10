import { ProductItem, TransactionRecord } from '../types';

export interface SheetSyncResult {
  success: boolean;
  message: string;
  data?: unknown;
}

export const APPS_SCRIPT_TEMPLATE = `/**
 * GOOGLE APPS SCRIPT — KASIR TERANG BULAN (WARUNG SENJA TERANG BULAN)
 * Sheets: Data_Stock, Stock_Keluar
 */

function ensureSheets() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  var stockSheet = ss.getSheetByName('Data_Stock');
  if (!stockSheet) {
    stockSheet = ss.insertSheet('Data_Stock');
    stockSheet.appendRow([
      'No. Menu',
      'Nama Menu',
      'Varian',
      'Satuan',
      'Harga Beli',
      'Harga Jual',
      'Terakhir Update'
    ]);
  }

  var outSheet = ss.getSheetByName('Stock_Keluar');
  if (!outSheet) {
    outSheet = ss.insertSheet('Stock_Keluar');
    outSheet.appendRow([
      'ID Transaksi',
      'Kode TRX',
      'Tanggal & Waktu',
      'Jenis',
      'No. Menu',
      'Nama Menu',
      'Varian',
      'Qty Keluar',
      'Satuan',
      'Harga Jual Satuan',
      'Total Subtotal',
      'Customer / Tujuan',
      'Operator',
      'Role',
      'Catatan'
    ]);
  }

  return { stockSheet: stockSheet, outSheet: outSheet };
}

function doGet(e) {
  var sheets = ensureSheets();
  var rows = sheets.stockSheet.getDataRange().getValues();
  var items = [];
  for (var i = 1; i < rows.length; i++) {
    items.push({
      barcode: String(rows[i][0]),
      name: String(rows[i][1]),
      category: String(rows[i][2]),
      unit: String(rows[i][3] || 'pcs'),
      purchasePrice: Number(rows[i][4] || 0),
      sellingPrice: Number(rows[i][5] || 0),
      lastUpdated: String(rows[i][6] || new Date().toISOString())
    });
  }
  return ContentService.createTextOutput(JSON.stringify({
    status: 'ok',
    app: 'Kasir Terang Bulan',
    items: items
  })).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  var sheets = ensureSheets();
  var payload = JSON.parse(e.postData.contents);
  var action = payload.action;

  if (action === 'ping' || action === 'init') {
    return ContentService.createTextOutput(JSON.stringify({
      status: 'ok',
      message: 'Koneksi Google Sheets Warung Senja Terang Bulan aktif!'
    })).setMimeType(ContentService.MimeType.JSON);
  }

  if (action === 'sync_all_stock') {
    var stockSheet = sheets.stockSheet;
    stockSheet.clearContents();
    stockSheet.appendRow([
      'No. Menu',
      'Nama Menu',
      'Varian',
      'Satuan',
      'Harga Beli',
      'Harga Jual',
      'Terakhir Update'
    ]);
    var products = payload.products || [];
    for (var i = 0; i < products.length; i++) {
      var p = products[i];
      stockSheet.appendRow([
        p.barcode,
        p.name,
        p.category,
        p.unit,
        p.purchasePrice,
        p.sellingPrice,
        p.lastUpdated
      ]);
    }
    return ContentService.createTextOutput(JSON.stringify({
      status: 'ok',
      message: 'Seluruh master menu Terang Bulan berhasil disinkronkan ke Data_Stock.'
    })).setMimeType(ContentService.MimeType.JSON);
  }

  if (action === 'stock_out') {
    var trx = payload.transaction;
    var outSheet = sheets.outSheet;
    var items = trx.items || [];
    for (var j = 0; j < items.length; j++) {
      var item = items[j];
      outSheet.appendRow([
        trx.id,
        trx.code,
        trx.timestamp,
        'Penjualan',
        item.barcode,
        item.name,
        item.category,
        item.qty,
        item.unit,
        item.unitPrice,
        item.subtotal,
        trx.recipientOrSupplier || '-',
        trx.operatorName,
        trx.operatorRole,
        trx.note || ''
      ]);
    }
    return ContentService.createTextOutput(JSON.stringify({
      status: 'ok',
      message: 'Transaksi ' + trx.code + ' berhasil dicatat ke Stock_Keluar.'
    })).setMimeType(ContentService.MimeType.JSON);
  }

  return ContentService.createTextOutput(JSON.stringify({
    status: 'error',
    message: 'Action tidak dikenali: ' + action
  })).setMimeType(ContentService.MimeType.JSON);
}`;

async function postToWebhook(webhookUrl: string, payload: Record<string, unknown>): Promise<SheetSyncResult> {
  if (!webhookUrl || !webhookUrl.trim().startsWith('https://')) {
    return {
      success: false,
      message: 'URL Webhook Google Apps Script belum diisi atau tidak valid.',
    };
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);

  try {
    const response = await fetch(webhookUrl.trim(), {
      method: 'POST',
      headers: {
        'Content-Type': 'text/plain;charset=utf-8',
      },
      body: JSON.stringify(payload),
      signal: controller.signal,
    });
    clearTimeout(timeout);

    if (!response.ok) {
      return {
        success: false,
        message: `HTTP Error ${response.status}`,
      };
    }

    const text = await response.text();
    try {
      const json = JSON.parse(text);
      return {
        success: json.status === 'ok' || json.success === true,
        message: json.message || 'Sinkronisasi Google Sheets berhasil.',
        data: json,
      };
    } catch {
      return {
        success: true,
        message: 'Permintaan terkirim ke Google Apps Script.',
      };
    }
  } catch (err) {
    clearTimeout(timeout);
    const msg = err instanceof Error ? err.message : 'Gagal menghubungi server Google Sheets';
    return {
      success: false,
      message: msg,
    };
  }
}

export const googleSheetsService = {
  async pingConnection(webhookUrl: string): Promise<SheetSyncResult> {
    return postToWebhook(webhookUrl, {
      action: 'ping',
      appName: 'Kasir Terang Bulan',
      storeName: 'Warung Senja Terang Bulan',
      timestamp: new Date().toISOString(),
    });
  },

  async syncTransaction(
    webhookUrl: string,
    transaction: TransactionRecord,
    products?: ProductItem[]
  ): Promise<SheetSyncResult> {
    const res = await postToWebhook(webhookUrl, {
      action: 'stock_out',
      transaction,
    });
    if (res.success && products) {
      await postToWebhook(webhookUrl, {
        action: 'sync_all_stock',
        products,
      });
    }
    return res;
  },

  async syncAllStock(webhookUrl: string, products: ProductItem[]): Promise<SheetSyncResult> {
    return postToWebhook(webhookUrl, {
      action: 'sync_all_stock',
      products,
    });
  },

  async fetchStockFromSheet(webhookUrl: string): Promise<{
    success: boolean;
    message: string;
    items?: Partial<ProductItem>[];
  }> {
    if (!webhookUrl || !webhookUrl.trim().startsWith('https://')) {
      return {
        success: false,
        message: 'URL Webhook belum dikonfigurasi.',
      };
    }
    try {
      const response = await fetch(webhookUrl.trim(), {
        method: 'GET',
      });
      if (!response.ok) {
        return {
          success: false,
          message: `HTTP Error ${response.status}`,
        };
      }
      const data = await response.json();
      if (Array.isArray(data.items)) {
        return {
          success: true,
          message: `Berhasil memuat ${data.items.length} menu dari Data_Stock.`,
          items: data.items,
        };
      }
      return {
        success: false,
        message: 'Format respons Data_Stock tidak ditemukan.',
      };
    } catch (err) {
      return {
        success: false,
        message: err instanceof Error ? err.message : 'Gagal mengambil data dari Google Sheets.',
      };
    }
  },
};
