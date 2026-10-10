import { StoreSettings, TransactionRecord } from '../types';
import { formatDateTime, formatRupiah } from '../utils/formatter';
import { bluetoothPrinterService } from './bluetoothPrinter';

function buildReceiptHtml(trx: TransactionRecord, settings: StoreSettings): string {
  const is80mm = settings.printer.paperSize === '80mm';
  const widthMm = is80mm ? '80mm' : '58mm';
  const p = settings.printer;

  const itemsRows = trx.items
    .map(
      (item) => `
      <div class="item-row">
        <div class="item-name">${item.name}${item.category ? ` (${item.category})` : ''}</div>
        <div class="item-calc">
          <span>${item.qty} ${item.unit} x ${formatRupiah(item.unitPrice)}</span>
          <span class="right">${formatRupiah(item.subtotal)}</span>
        </div>
      </div>`
    )
    .join('');

  const feedBr = '<br/>'.repeat(Math.max(1, p.feedLinesAfterPrint || 2));

  return `<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8" />
  <title>Struk ${trx.code}</title>
  <style>
    @page {
      size: ${widthMm} auto;
      margin: 0;
    }
    * {
      box-sizing: border-box;
    }
    body {
      width: ${widthMm};
      margin: 0 auto;
      padding: 4mm 3mm;
      font-family: 'Courier New', Courier, monospace;
      font-size: ${is80mm ? '12px' : '11px'};
      line-height: 1.35;
      color: #000;
      background: #fff;
    }
    .center { text-align: center; }
    .right { text-align: right; }
    .bold { font-weight: bold; }
    .store-logo {
      max-width: ${is80mm ? '130px' : '100px'};
      max-height: 72px;
      object-fit: contain;
      display: block;
      margin: 0 auto 6px auto;
    }
    .store-title {
      font-size: ${is80mm ? '15px' : '13px'};
      font-weight: bold;
      text-transform: uppercase;
      margin-bottom: 2px;
    }
    .divider {
      border-top: 1px dashed #000;
      margin: 6px 0;
    }
    .double-divider {
      border-top: 2px solid #000;
      margin: 6px 0;
    }
    .meta-row {
      display: flex;
      justify-content: space-between;
      font-size: ${is80mm ? '11px' : '10px'};
    }
    .item-row {
      margin-bottom: 5px;
    }
    .item-name {
      font-weight: bold;
    }
    .item-calc {
      display: flex;
      justify-content: space-between;
      font-size: ${is80mm ? '11px' : '10px'};
    }
    .total-row {
      display: flex;
      justify-content: space-between;
      font-weight: bold;
      font-size: ${is80mm ? '13px' : '12px'};
      margin-top: 4px;
    }
    .footer {
      margin-top: 8px;
      font-size: ${is80mm ? '11px' : '10px'};
      text-align: center;
    }
  </style>
</head>
<body>
  <div class="center">
    ${
      p.showReceiptLogo && p.receiptLogoDataUrl
        ? `<img src="${p.receiptLogoDataUrl}" alt="Logo ${settings.storeName}" class="store-logo" />`
        : ''
    }
    <div class="store-title">${settings.storeName}</div>
    ${p.customHeaderNote ? `<div>${p.customHeaderNote}</div>` : ''}
    ${p.showStoreAddress && settings.storeAddress ? `<div>${settings.storeAddress}</div>` : ''}
    ${p.showStorePhone && settings.storePhone ? `<div>Telp/WA: ${settings.storePhone}</div>` : ''}
    ${settings.operationalHours ? `<div>${settings.operationalHours}</div>` : ''}
  </div>

  <div class="double-divider"></div>

  <div class="meta-row">
    <span>No. TRX</span>
    <span class="bold">${trx.code}</span>
  </div>
  <div class="meta-row">
    <span>Waktu</span>
    <span>${formatDateTime(trx.timestamp)}</span>
  </div>
  <div class="meta-row">
    <span>Jenis</span>
    <span>${trx.type === 'OUT' ? 'Penjualan' : 'Stock Masuk'}</span>
  </div>
  ${
    p.showCashierName
      ? `<div class="meta-row"><span>Operator</span><span>${trx.operatorName} (${trx.operatorRole})</span></div>`
      : ''
  }
  ${
    trx.recipientOrSupplier
      ? `<div class="meta-row"><span>${
          trx.type === 'OUT' ? 'Pelanggan' : 'Supplier'
        }</span><span>${trx.recipientOrSupplier}</span></div>`
      : ''
  }

  <div class="divider"></div>

  ${itemsRows}

  <div class="divider"></div>

  <div class="meta-row">
    <span>Total Qty</span>
    <span>${trx.totalQty} pcs</span>
  </div>
  <div class="total-row">
    <span>TOTAL</span>
    <span>${formatRupiah(trx.totalAmount)}</span>
  </div>

  ${
    trx.note
      ? `<div class="divider"></div><div style="font-size:10px;">Catatan: ${trx.note}</div>`
      : ''
  }

  <div class="divider"></div>

  ${
    p.showReceiptFooter && settings.receiptFooter
      ? `<div class="footer">${settings.receiptFooter}</div>`
      : ''
  }

  ${feedBr}

  <script>
    window.onload = function() {
      setTimeout(function() {
        window.focus();
        window.print();
      }, 250);
    };
  </script>
</body>
</html>`;
}

function printHtmlViaIframe(html: string): { success: boolean; message: string } {
  try {
    const existing = document.getElementById('kasir-terang-bulan-print-frame');
    if (existing) {
      existing.remove();
    }

    const iframe = document.createElement('iframe');
    iframe.id = 'kasir-terang-bulan-print-frame';
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';
    document.body.appendChild(iframe);

    const doc = iframe.contentWindow?.document;
    if (!doc) {
      return {
        success: false,
        message: 'Tidak dapat menyiapkan dokumen cetak browser.',
      };
    }

    doc.open();
    doc.write(html);
    doc.close();

    return {
      success: true,
      message: 'Dialog cetak struk browser dibuka.',
    };
  } catch (err) {
    return {
      success: false,
      message: err instanceof Error ? err.message : 'Gagal mencetak struk via browser.',
    };
  }
}

export const printerService = {
  printReceipt(trx: TransactionRecord, settings: StoreSettings): {
    success: boolean;
    message: string;
  } {
    if (
      settings.printer.connectionMode === 'bluetooth' &&
      bluetoothPrinterService.isConnected()
    ) {
      return bluetoothPrinterService.printTransaction(trx, settings);
    }
    const html = buildReceiptHtml(trx, settings);
    return printHtmlViaIframe(html);
  },

  printTransaction(trx: TransactionRecord, settings: StoreSettings): {
    success: boolean;
    message: string;
  } {
    return this.printReceipt(trx, settings);
  },

  printTest(settings: StoreSettings): { success: boolean; message: string } {
    const sampleTrx: TransactionRecord = {
      id: 'test-print',
      code: 'TB-TEST-001',
      type: 'OUT',
      timestamp: new Date().toISOString(),
      operatorRole: 'kasir',
      operatorName: 'Kasir 1',
      items: [
        {
          productId: 'tb-002',
          barcode: '02',
          name: 'Terang Bulan Coklat Lumer',
          category: 'Coklat',
          qty: 1,
          unit: 'pcs',
          unitPrice: 20000,
          subtotal: 20000,
          availableStock: 30,
        },
        {
          productId: 'tb-004',
          barcode: '04',
          name: 'Terang Bulan Coklat Keju',
          category: 'Coklat Keju',
          qty: 1,
          unit: 'pcs',
          unitPrice: 25000,
          subtotal: 25000,
          availableStock: 35,
        },
      ],
      totalQty: 2,
      totalAmount: 45000,
      recipientOrSupplier: 'Pelanggan Test Print',
      referenceNo: 'TEST-01',
      note: 'Test cetak struk thermal 58mm / 80mm',
      syncStatus: 'local',
    };
    const html = buildReceiptHtml(sampleTrx, settings);
    return printHtmlViaIframe(html);
  },
};
