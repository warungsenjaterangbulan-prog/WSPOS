import React, { useEffect, useRef, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Download,
  RotateCcw,
  Save,
  Store,
  Upload,
} from 'lucide-react';
import { storageService } from '../../services/storage';
import {
  GoogleSheetConfig,
  ProductItem,
  StoreSettings,
  TransactionRecord,
} from '../../types';
import { PrinterSettingsSection } from './PrinterSettingsSection';

interface OwnerSettingsViewProps {
  settings: StoreSettings;
  onSaveSettings: (nextSettings: StoreSettings) => void;
  onRestoreBackupData: (data: {
    products: ProductItem[];
    transactions: TransactionRecord[];
    settings: StoreSettings;
    sheetConfig: GoogleSheetConfig;
  }) => void;
  onResetAllData: () => void;
}

export const OwnerSettingsView: React.FC<OwnerSettingsViewProps> = ({
  settings,
  onSaveSettings,
  onRestoreBackupData,
  onResetAllData,
}) => {
  const [draft, setDraft] = useState<StoreSettings>(settings);
  const [feedback, setFeedback] = useState<{
    ok: boolean;
    text: string;
  } | null>(null);
  const [confirmResetOpen, setConfirmResetOpen] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setDraft(settings);
  }, [settings]);

  const showToast = (ok: boolean, text: string) => {
    setFeedback({ ok, text });
    setTimeout(() => {
      setFeedback((prev) => (prev?.text === text ? null : prev));
    }, 4000);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    onSaveSettings(draft);
    showToast(
      true,
      'Pengaturan Warung Senja Terang Bulan berhasil disimpan!'
    );
  };

  const handleExportBackup = () => {
    const json = storageService.exportBackupJson();
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `backup-kasir-terang-bulan-${new Date()
      .toISOString()
      .slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast(true, 'File backup JSON Kasir Terang Bulan berhasil diunduh.');
  };

  const handleImportBackupFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const text = String(reader.result || '');
        const restored = storageService.restoreBackupJson(text);
        setDraft(restored.settings);
        onRestoreBackupData(restored);
        showToast(
          true,
          `Data berhasil dipulihkan (${restored.products.length} menu, ${restored.transactions.length} transaksi).`
        );
      } catch (err) {
        showToast(
          false,
          err instanceof Error
            ? err.message
            : 'File backup JSON tidak valid atau rusak.'
        );
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  return (
    <div className="space-y-5">
      {feedback && (
        <div
          className={`flex items-center gap-2 rounded-xl border px-4 py-3 text-xs font-medium ${
            feedback.ok
              ? 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300'
              : 'border-red-500/40 bg-red-500/15 text-red-300'
          }`}
        >
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{feedback.text}</span>
        </div>
      )}

      <form onSubmit={handleSave} className="space-y-5">
        {/* Store Profile & Security */}
        <div className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900 shadow-lg text-xs">
          <div className="flex items-center gap-3.5 bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-5 py-4 text-zinc-950">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-zinc-950 text-yellow-400">
              <Store className="h-5 w-5" />
            </div>
            <div>
              <h1 className="font-display text-base sm:text-lg font-bold text-zinc-950">
                Identitas Toko & Keamanan PIN
              </h1>
              <p className="text-zinc-900/80">
                Informasi toko untuk header aplikasi dan struk penjualan Kasir Terang Bulan.
              </p>
            </div>
          </div>

          <div className="p-5 space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block font-medium text-zinc-300 mb-1">
                  Nama Toko
                </label>
                <input
                  type="text"
                  required
                  value={draft.storeName}
                  onChange={(e) =>
                    setDraft({ ...draft, storeName: e.target.value })
                  }
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 text-zinc-100 focus:border-yellow-400 focus:outline-none"
                />
              </div>

              <div>
                <label className="block font-medium text-zinc-300 mb-1">
                  Nomor Telepon / WhatsApp
                </label>
                <input
                  type="text"
                  value={draft.storePhone}
                  onChange={(e) =>
                    setDraft({ ...draft, storePhone: e.target.value })
                  }
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 font-mono text-zinc-100 focus:border-yellow-400 focus:outline-none"
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block font-medium text-zinc-300 mb-1">
                  Alamat Toko
                </label>
                <input
                  type="text"
                  value={draft.storeAddress}
                  onChange={(e) =>
                    setDraft({ ...draft, storeAddress: e.target.value })
                  }
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 text-zinc-100 focus:border-yellow-400 focus:outline-none"
                />
              </div>

              <div>
                <label className="block font-medium text-zinc-300 mb-1">
                  Jam Operasional
                </label>
                <input
                  type="text"
                  value={draft.operationalHours}
                  onChange={(e) =>
                    setDraft({ ...draft, operationalHours: e.target.value })
                  }
                  placeholder="Buka Jam 15.00 - 21.00"
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 text-zinc-100 focus:border-yellow-400 focus:outline-none"
                />
              </div>

              <div>
                <label className="block font-medium text-zinc-300 mb-1">
                  Footer Struk Penjualan
                </label>
                <input
                  type="text"
                  value={draft.receiptFooter}
                  onChange={(e) =>
                    setDraft({ ...draft, receiptFooter: e.target.value })
                  }
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 text-zinc-100 focus:border-yellow-400 focus:outline-none"
                />
              </div>

              <div>
                <label className="block font-medium text-zinc-300 mb-1">
                  PIN Owner
                </label>
                <input
                  type="password"
                  inputMode="numeric"
                  required
                  value={draft.ownerPin}
                  onChange={(e) =>
                    setDraft({ ...draft, ownerPin: e.target.value })
                  }
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 font-mono tracking-widest text-yellow-400 focus:border-yellow-400 focus:outline-none"
                />
              </div>

              <div>
                <label className="block font-medium text-zinc-300 mb-1">
                  PIN Kasir
                </label>
                <input
                  type="password"
                  inputMode="numeric"
                  required
                  value={draft.kasirPin}
                  onChange={(e) =>
                    setDraft({ ...draft, kasirPin: e.target.value })
                  }
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 font-mono tracking-widest text-yellow-400 focus:border-yellow-400 focus:outline-none"
                />
              </div>
            </div>

            <div className="pt-1">
              <label className="flex items-center gap-2.5 rounded-xl border border-zinc-800 bg-zinc-950 p-3.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={draft.enableScanSound}
                  onChange={(e) =>
                    setDraft({ ...draft, enableScanSound: e.target.checked })
                  }
                  className="h-4 w-4 accent-yellow-400"
                />
                <span className="font-medium text-zinc-200">
                  Aktifkan Suara Notifikasi Saat Checkout Transaksi
                </span>
              </label>
            </div>
          </div>
        </div>

        {/* Printer Settings Section */}
        <PrinterSettingsSection
          settings={draft}
          onChangePrinter={(nextPrinter) => {
            const nextSettings = { ...draft, printer: nextPrinter };
            setDraft(nextSettings);
            onSaveSettings(nextSettings);
          }}
          onShowFeedback={(msg) => showToast(true, msg)}
        />

        <div className="flex justify-end">
          <button
            type="submit"
            className="flex min-h-[46px] items-center gap-2 rounded-xl bg-yellow-400 px-6 py-3 text-sm font-bold text-zinc-950 shadow-md hover:bg-yellow-300"
          >
            <Save className="h-4 w-4" />
            <span>Simpan Seluruh Pengaturan</span>
          </button>
        </div>
      </form>

      {/* Backup, Restore & Reset */}
      <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5 space-y-4 text-xs shadow-lg">
        <h2 className="font-display text-base font-bold text-white">
          Backup, Restore & Reset Data Kasir Terang Bulan
        </h2>
        <p className="text-zinc-400">
          Cadangkan seluruh data menu, riwayat transaksi, dan pengaturan ke file JSON lokal.
        </p>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={handleExportBackup}
            className="flex min-h-[40px] items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2 font-semibold text-zinc-200 hover:border-yellow-400/50"
          >
            <Download className="h-4 w-4 text-yellow-400" />
            <span>Download Backup JSON</span>
          </button>

          <input
            ref={fileInputRef}
            type="file"
            accept=".json,application/json"
            onChange={handleImportBackupFile}
            className="hidden"
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="flex min-h-[40px] items-center gap-2 rounded-xl bg-yellow-400 px-4 py-2 font-bold text-zinc-950 hover:bg-yellow-300"
          >
            <Upload className="h-4 w-4" />
            <span>Restore dari Backup JSON</span>
          </button>

          <button
            type="button"
            onClick={() => setConfirmResetOpen(true)}
            className="flex min-h-[40px] items-center gap-2 rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-2 font-semibold text-red-400 hover:bg-red-500/20"
          >
            <RotateCcw className="h-4 w-4" />
            <span>Reset Data</span>
          </button>
        </div>
      </div>

      {/* Reset Confirmation Modal */}
      {confirmResetOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-xs">
          <div className="max-h-[90dvh] w-full max-w-md overflow-y-auto rounded-3xl border border-red-500/40 bg-zinc-900 p-6 shadow-2xl">
            <div className="flex items-center gap-2 text-red-400">
              <AlertTriangle className="h-5 w-5" />
              <h3 className="text-base font-bold text-white">
                Reset & Kosongkan Data?
              </h3>
            </div>
            <p className="mt-2 text-xs text-zinc-400 leading-relaxed">
              Tindakan ini akan mengosongkan seluruh data <strong className="text-white">Riwayat Penjualan</strong> dan <strong className="text-white">Daftar Menu</strong>. Pastikan Anda sudah mengunduh backup JSON jika diperlukan.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setConfirmResetOpen(false)}
                className="min-h-[40px] rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2 text-xs font-medium text-zinc-300"
              >
                Batal
              </button>
              <button
                type="button"
                onClick={() => {
                  onResetAllData();
                  setConfirmResetOpen(false);
                  showToast(
                    true,
                    'Seluruh data riwayat transaksi dan daftar menu berhasil dikosongkan.'
                  );
                }}
                className="min-h-[40px] rounded-xl bg-red-600 px-4 py-2 text-xs font-bold text-white hover:bg-red-500"
              >
                Ya, Reset Data
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
