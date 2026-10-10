import React, { useState } from 'react';
import {
  CheckCircle2,
  CloudUpload,
  Copy,
  DownloadCloud,
  FileSpreadsheet,
  RefreshCw,
} from 'lucide-react';
import {
  APPS_SCRIPT_TEMPLATE,
  googleSheetsService,
} from '../../services/googleSheets';
import { GoogleSheetConfig, ProductItem } from '../../types';
import { formatDateTime } from '../../utils/formatter';

interface OwnerGoogleSheetViewProps {
  sheetConfig: GoogleSheetConfig;
  products: ProductItem[];
  onSaveSheetConfig: (config: GoogleSheetConfig) => void;
  onUpdateProductsFromSheet: (items: ProductItem[]) => void;
}

export const OwnerGoogleSheetView: React.FC<OwnerGoogleSheetViewProps> = ({
  sheetConfig,
  products,
  onSaveSheetConfig,
  onUpdateProductsFromSheet,
}) => {
  const [webhookUrl, setWebhookUrl] = useState(sheetConfig.webhookUrl);
  const [spreadsheetUrl, setSpreadsheetUrl] = useState(
    sheetConfig.spreadsheetUrl || ''
  );
  const [autoSync, setAutoSync] = useState(sheetConfig.autoSync);
  const [statusMessage, setStatusMessage] = useState<{
    ok: boolean;
    text: string;
  } | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [showScriptCode, setShowScriptCode] = useState(false);

  const handleSaveConfig = (e: React.FormEvent) => {
    e.preventDefault();
    const updated: GoogleSheetConfig = {
      ...sheetConfig,
      webhookUrl: webhookUrl.trim(),
      spreadsheetUrl: spreadsheetUrl.trim(),
      autoSync,
    };
    onSaveSheetConfig(updated);
    setStatusMessage({
      ok: true,
      text: 'Konfigurasi Google Sheets berhasil disimpan secara lokal.',
    });
  };

  const handlePingTest = async () => {
    setBusyAction('ping');
    const res = await googleSheetsService.pingConnection(webhookUrl);
    setBusyAction(null);
    setStatusMessage({ ok: res.success, text: res.message });
    if (res.success) {
      onSaveSheetConfig({
        ...sheetConfig,
        webhookUrl: webhookUrl.trim(),
        spreadsheetUrl: spreadsheetUrl.trim(),
        autoSync,
        isConnected: true,
        lastSyncAt: new Date().toISOString(),
      });
    }
  };

  const handlePushAllStock = async () => {
    setBusyAction('push');
    const res = await googleSheetsService.syncAllStock(webhookUrl, products);
    setBusyAction(null);
    setStatusMessage({ ok: res.success, text: res.message });
    if (res.success) {
      onSaveSheetConfig({
        ...sheetConfig,
        webhookUrl: webhookUrl.trim(),
        lastSyncAt: new Date().toISOString(),
        isConnected: true,
      });
    }
  };

  const handlePullFromSheet = async () => {
    setBusyAction('pull');
    const res = await googleSheetsService.fetchStockFromSheet(webhookUrl);
    setBusyAction(null);
    setStatusMessage({ ok: res.success, text: res.message });
    if (res.success && res.items && res.items.length > 0) {
      const merged: ProductItem[] = res.items.map((raw, idx) => {
        const category = String(raw.category || 'Original');
        const purchasePrice = Number(raw.purchasePrice ?? 10000);
        const sellingPrice = Number(raw.sellingPrice ?? 20000);
        return {
          id: `tb-sheet-${idx}-${Date.now()}`,
          barcode: String(raw.barcode || String(idx + 1).padStart(2, '0')),
          name: String(raw.name || 'Menu Terang Bulan'),
          category,
          variants: [
            {
              id: `v-sheet-${idx}-1`,
              name: category,
              purchasePrice,
              sellingPrice,
            },
          ],
          stock: 999,
          unit: String(raw.unit || 'pcs'),
          minStock: 0,
          purchasePrice,
          sellingPrice,
          location: '-',
          lastUpdated: String(raw.lastUpdated || new Date().toISOString()),
        };
      });
      onUpdateProductsFromSheet(merged);
    }
  };

  return (
    <div className="space-y-4">
      <div className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900 shadow-lg">
        <div className="flex items-center gap-3.5 bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-5 py-4 text-zinc-950">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-zinc-950 text-yellow-400">
            <FileSpreadsheet className="h-5 w-5" />
          </div>
          <div>
            <h1 className="font-display text-base sm:text-lg font-bold text-zinc-950">
              Integrasi Google Sheets — Warung Senja Terang Bulan
            </h1>
            <p className="text-xs font-medium text-zinc-900/80">
              Sinkronisasi otomatis dengan sheet{' '}
              <span className="font-mono font-bold text-zinc-950">Data_Stock</span> dan{' '}
              <span className="font-mono font-bold text-zinc-950">Stock_Keluar</span>.
            </p>
          </div>
        </div>

        <div className="p-5">
          {statusMessage && (
            <div
              className={`mb-4 flex items-center gap-2 rounded-xl border p-3 text-xs font-medium ${
                statusMessage.ok
                  ? 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300'
                  : 'border-yellow-400/40 bg-yellow-400/15 text-yellow-300'
              }`}
            >
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              <span>{statusMessage.text}</span>
            </div>
          )}

          <form onSubmit={handleSaveConfig} className="space-y-3.5 text-xs">
            <div>
              <label className="block font-medium text-zinc-300 mb-1">
                URL Webhook Google Apps Script (Deployment Web App URL)
              </label>
              <input
                type="url"
                value={webhookUrl}
                onChange={(e) => setWebhookUrl(e.target.value)}
                placeholder="https://script.google.com/macros/s/.../exec"
                className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 font-mono text-xs text-zinc-100 placeholder:text-zinc-600 focus:border-yellow-400 focus:outline-none"
              />
            </div>

            <div>
              <label className="block font-medium text-zinc-300 mb-1">
                Link Google Spreadsheet (Opsional)
              </label>
              <input
                type="url"
                value={spreadsheetUrl}
                onChange={(e) => setSpreadsheetUrl(e.target.value)}
                placeholder="https://docs.google.com/spreadsheets/d/..."
                className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 font-mono text-xs text-zinc-100 placeholder:text-zinc-600 focus:border-yellow-400 focus:outline-none"
              />
            </div>

            <div className="flex items-center justify-between rounded-2xl border border-zinc-800 bg-zinc-950 p-3.5">
              <div>
                <div className="font-semibold text-white">
                  Sinkronisasi Otomatis Saat Transaksi
                </div>
                <div className="text-[11px] text-zinc-400">
                  Kirim otomatis setiap transaksi penjualan ke Google Sheets.
                </div>
              </div>
              <input
                type="checkbox"
                checked={autoSync}
                onChange={(e) => setAutoSync(e.target.checked)}
                className="h-4 w-4 accent-yellow-400"
              />
            </div>

            {sheetConfig.lastSyncAt && (
              <div className="text-[11px] font-mono text-zinc-400">
                Terakhir sinkron: {formatDateTime(sheetConfig.lastSyncAt)}
              </div>
            )}

            <div className="flex flex-wrap items-center gap-2 pt-2">
              <button
                type="submit"
                className="min-h-[40px] rounded-xl bg-yellow-400 px-4 py-2 font-bold text-zinc-950 shadow-xs hover:bg-yellow-300"
              >
                Simpan Konfigurasi
              </button>

              <button
                type="button"
                disabled={busyAction !== null}
                onClick={handlePingTest}
                className="flex min-h-[40px] items-center gap-1.5 rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2 font-semibold text-yellow-400 hover:border-yellow-400/50"
              >
                <RefreshCw
                  className={`h-3.5 w-3.5 ${
                    busyAction === 'ping' ? 'animate-spin' : ''
                  }`}
                />
                <span>Test Koneksi</span>
              </button>

              <button
                type="button"
                disabled={busyAction !== null}
                onClick={handlePushAllStock}
                className="flex min-h-[40px] items-center gap-1.5 rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2 font-semibold text-zinc-200 hover:border-yellow-400/50"
              >
                <CloudUpload className="h-3.5 w-3.5 text-yellow-400" />
                <span>Kirim Menu ke Sheet</span>
              </button>

              <button
                type="button"
                disabled={busyAction !== null}
                onClick={handlePullFromSheet}
                className="flex min-h-[40px] items-center gap-1.5 rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2 font-semibold text-zinc-200 hover:border-yellow-400/50"
              >
                <DownloadCloud className="h-3.5 w-3.5 text-yellow-400" />
                <span>Ambil Menu dari Sheet</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Schema & Apps Script Template */}
      <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5 space-y-4 text-xs shadow-lg">
        <div className="flex items-center justify-between gap-2">
          <div>
            <h2 className="font-display text-base font-bold text-white">
              Struktur Kolom & Kode Google Apps Script
            </h2>
            <p className="text-zinc-400">
              Salin script ke Extensions → Apps Script pada Google Spreadsheet Anda.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setShowScriptCode((prev) => !prev)}
            className="min-h-[38px] rounded-xl bg-yellow-400 px-3.5 py-1.5 font-bold text-zinc-950 hover:bg-yellow-300 whitespace-nowrap"
          >
            {showScriptCode ? 'Sembunyikan Script' : 'Lihat Kode Apps Script'}
          </button>
        </div>

        <div className="grid gap-3 md:grid-cols-2">
          <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4">
            <div className="font-mono font-bold text-yellow-400">1. Data_Stock</div>
            <p className="mt-1 text-[11px] text-zinc-400 leading-relaxed">
              No. Menu · Nama Menu · Varian · Satuan · Harga Beli · Harga Jual · Terakhir Update
            </p>
          </div>
          <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4">
            <div className="font-mono font-bold text-yellow-400">2. Stock_Keluar</div>
            <p className="mt-1 text-[11px] text-zinc-400 leading-relaxed">
              ID Transaksi · Kode TRX · Tanggal & Waktu · Jenis · No. Menu · Nama Menu · Varian · Qty Keluar · Satuan · Harga Jual Satuan · Total Subtotal · Customer / Tujuan · Operator · Role · Catatan
            </p>
          </div>
        </div>

        {showScriptCode && (
          <div className="space-y-2">
            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => {
                  navigator.clipboard?.writeText(APPS_SCRIPT_TEMPLATE);
                  setStatusMessage({
                    ok: true,
                    text: 'Kode Google Apps Script berhasil disalin ke clipboard.',
                  });
                }}
                className="flex items-center gap-1.5 rounded-lg bg-yellow-400 px-3 py-1.5 text-xs font-bold text-zinc-950 hover:bg-yellow-300"
              >
                <Copy className="h-3.5 w-3.5" />
                <span>Salin Kode Script</span>
              </button>
            </div>
            <pre className="max-h-[320px] overflow-auto rounded-2xl border border-zinc-800 bg-zinc-950 p-4 font-mono text-[11px] text-yellow-300">
              {APPS_SCRIPT_TEMPLATE}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
};
