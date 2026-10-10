import React, { useRef, useState } from 'react';
import {
  Bluetooth,
  ImagePlus,
  Printer,
  RefreshCw,
  Trash2,
  Unplug,
} from 'lucide-react';
import {
  bluetoothPrinterService,
  SPP_UUID,
} from '../../services/bluetoothPrinter';
import { printerService } from '../../services/printer';
import { BluetoothDeviceItem, PrinterSettings, StoreSettings } from '../../types';

interface PrinterSettingsSectionProps {
  settings: StoreSettings;
  onChangePrinter: (nextPrinter: PrinterSettings) => void;
  onShowFeedback: (msg: string) => void;
}

export const PrinterSettingsSection: React.FC<PrinterSettingsSectionProps> = ({
  settings,
  onChangePrinter,
  onShowFeedback,
}) => {
  const [pairedDevices, setPairedDevices] = useState<BluetoothDeviceItem[]>(() =>
    bluetoothPrinterService.getPairedDevices()
  );
  const logoInputRef = useRef<HTMLInputElement | null>(null);

  const updateField = <K extends keyof PrinterSettings>(
    key: K,
    value: PrinterSettings[K]
  ) => {
    onChangePrinter({
      ...settings.printer,
      [key]: value,
    });
  };

  const handleLogoFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      onShowFeedback('Pilih file gambar yang valid (PNG, JPG, WEBP, atau SVG).');
      e.target.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = String(reader.result || '');
      const img = new Image();
      img.onload = () => {
        try {
          const maxWidth = 260;
          const maxHeight = 140;
          let width = img.width;
          let height = img.height;

          if (width > maxWidth || height > maxHeight) {
            const ratio = Math.min(maxWidth / width, maxHeight / height);
            width = Math.round(width * ratio);
            height = Math.round(height * ratio);
          }

          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d');
          if (ctx) {
            ctx.clearRect(0, 0, width, height);
            ctx.drawImage(img, 0, 0, width, height);
            const compressed = canvas.toDataURL('image/png');
            onChangePrinter({
              ...settings.printer,
              receiptLogoDataUrl: compressed,
              showReceiptLogo: true,
            });
            onShowFeedback('Logo struk berhasil diunggah. Jangan lupa klik Simpan Seluruh Pengaturan.');
          } else {
            onChangePrinter({
              ...settings.printer,
              receiptLogoDataUrl: dataUrl,
              showReceiptLogo: true,
            });
            onShowFeedback('Logo struk berhasil diunggah.');
          }
        } catch {
          onChangePrinter({
            ...settings.printer,
            receiptLogoDataUrl: dataUrl,
            showReceiptLogo: true,
          });
          onShowFeedback('Logo struk berhasil diunggah.');
        }
      };
      img.src = dataUrl;
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const handleRemoveLogo = () => {
    onChangePrinter({
      ...settings.printer,
      receiptLogoDataUrl: '',
    });
    onShowFeedback('Logo struk telah dihapus.');
  };

  const handleScanPairedPrinters = async () => {
    if (bluetoothPrinterService.isWebBluetoothSupported()) {
      const result = await bluetoothPrinterService.scanAndSelectDevice();
      setPairedDevices(result.devices);
      if (result.device) {
        onChangePrinter({
          ...settings.printer,
          bluetoothDeviceName: result.device.name,
          bluetoothDeviceAddress: result.device.address,
        });
      }
      onShowFeedback(result.message);
      return;
    }

    const list = bluetoothPrinterService.getPairedDevices();
    setPairedDevices(list);
    if (list.length === 0) {
      onShowFeedback(
        'Browser ini menggunakan dialog cetak standar. Untuk scan Bluetooth langsung dari browser, gunakan Chrome / Edge di Desktop atau Mobile.'
      );
    } else {
      onShowFeedback(`Ditemukan ${list.length} perangkat Bluetooth tersimpan.`);
    }
  };

  const handleConnectPrinter = (device: BluetoothDeviceItem) => {
    onChangePrinter({
      ...settings.printer,
      bluetoothDeviceName: device.name,
      bluetoothDeviceAddress: device.address,
    });
    const res = bluetoothPrinterService.connect(device.address, device.name);
    onShowFeedback(res.message);
  };

  const handleDisconnectPrinter = () => {
    const res = bluetoothPrinterService.disconnect();
    onShowFeedback(res.message);
  };

  const handleTestPrint = () => {
    if (
      settings.printer.connectionMode === 'bluetooth' &&
      bluetoothPrinterService.isConnected()
    ) {
      const res = bluetoothPrinterService.printTestReceipt(settings);
      onShowFeedback(res.message);
    } else {
      const res = printerService.printTest(settings);
      onShowFeedback(res.message);
    }
  };

  return (
    <div className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900 shadow-lg text-xs">
      <div className="flex flex-col gap-3 bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-5 py-4 text-zinc-950 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-zinc-950 text-yellow-400">
            <Printer className="h-5 w-5" />
          </div>
          <div>
            <h2 className="font-display text-base font-bold text-zinc-950">
              Pengaturan Printer Thermal & Struk
            </h2>
            <p className="text-zinc-900/80">
              Cetak Browser HTML/CSS & Web Bluetooth ESC/POS ({SPP_UUID}).
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={handleTestPrint}
          className="flex min-h-[38px] items-center gap-1.5 rounded-xl bg-zinc-950 px-4 py-2 font-bold text-yellow-400 shadow-xs hover:bg-zinc-900 whitespace-nowrap"
        >
          <Printer className="h-4 w-4" />
          <span>Test Print Struk</span>
        </button>
      </div>

      <div className="p-5 space-y-4">
        {/* Mode & Paper Size */}
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block font-medium text-zinc-300 mb-1.5">
              Jalur Koneksi Printer
            </label>
            <div className="grid grid-cols-2 gap-2 rounded-xl bg-zinc-950 p-1 border border-zinc-800">
              <button
                type="button"
                onClick={() => updateField('connectionMode', 'browser')}
                className={`rounded-lg py-2 font-bold transition-all ${
                  settings.printer.connectionMode === 'browser'
                    ? 'bg-yellow-400 text-zinc-950 shadow-xs'
                    : 'text-zinc-400'
                }`}
              >
                Browser Print
              </button>
              <button
                type="button"
                onClick={() => updateField('connectionMode', 'bluetooth')}
                className={`rounded-lg py-2 font-bold transition-all ${
                  settings.printer.connectionMode === 'bluetooth'
                    ? 'bg-yellow-400 text-zinc-950 shadow-xs'
                    : 'text-zinc-400'
                }`}
              >
                Bluetooth ESC/POS
              </button>
            </div>
          </div>

          <div>
            <label className="block font-medium text-zinc-300 mb-1.5">
              Ukuran Kertas Thermal
            </label>
            <div className="grid grid-cols-2 gap-2 rounded-xl bg-zinc-950 p-1 border border-zinc-800">
              <button
                type="button"
                onClick={() => updateField('paperSize', '58mm')}
                className={`rounded-lg py-2 font-mono font-bold transition-all ${
                  settings.printer.paperSize === '58mm'
                    ? 'bg-yellow-400 text-zinc-950 shadow-xs'
                    : 'text-zinc-400'
                }`}
              >
                58mm (32 char)
              </button>
              <button
                type="button"
                onClick={() => updateField('paperSize', '80mm')}
                className={`rounded-lg py-2 font-mono font-bold transition-all ${
                  settings.printer.paperSize === '80mm'
                    ? 'bg-yellow-400 text-zinc-950 shadow-xs'
                    : 'text-zinc-400'
                }`}
              >
                80mm (48 char)
              </button>
            </div>
          </div>
        </div>

        {/* Upload Logo Struk Panel */}
        <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4 space-y-3">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="font-semibold text-white">
                Upload Logo untuk Ditampilkan di Struk
              </div>
              <p className="mt-0.5 text-[11px] text-zinc-400">
                Unggah gambar logo toko (PNG/JPG/WEBP) agar tercetak pada bagian atas struk transaksi.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2 shrink-0">
              <input
                ref={logoInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp,image/svg+xml"
                onChange={handleLogoFileChange}
                className="hidden"
              />
              <button
                type="button"
                onClick={() => logoInputRef.current?.click()}
                className="flex min-h-[38px] items-center gap-1.5 rounded-xl bg-yellow-400 px-3.5 py-2 font-bold text-zinc-950 shadow-xs hover:bg-yellow-300"
              >
                <ImagePlus className="h-4 w-4" />
                <span>
                  {settings.printer.receiptLogoDataUrl ? 'Ganti Logo Struk' : 'Upload Logo Struk'}
                </span>
              </button>

              {settings.printer.receiptLogoDataUrl && (
                <button
                  type="button"
                  onClick={handleRemoveLogo}
                  className="flex min-h-[38px] items-center gap-1 rounded-xl border border-red-500/40 bg-red-500/10 px-3 py-2 font-semibold text-red-400 hover:bg-red-500/20"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  <span>Hapus Logo</span>
                </button>
              )}
            </div>
          </div>

          {settings.printer.receiptLogoDataUrl && (
            <div className="flex items-center gap-4 rounded-xl border border-zinc-800 bg-zinc-900 p-3">
              <div className="flex h-16 w-24 items-center justify-center rounded-lg border border-zinc-700 bg-white p-1.5">
                <img
                  src={settings.printer.receiptLogoDataUrl}
                  alt="Preview Logo Struk"
                  className="max-h-full max-w-full object-contain"
                />
              </div>
              <div className="text-[11px] text-zinc-400">
                <div className="font-semibold text-yellow-400">Logo Struk Siap Digunakan</div>
                <div>Logo otomatis tampil di bagian atas struk saat fitur Tampilkan Logo aktif.</div>
              </div>
            </div>
          )}
        </div>

        {/* Bluetooth Printer Discovery Panel */}
        <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Bluetooth className="h-4 w-4 text-yellow-400" />
              <span className="font-semibold text-white">
                Printer Bluetooth:{' '}
                {settings.printer.bluetoothDeviceName || 'Belum dipilih'}
              </span>
              {settings.printer.bluetoothDeviceAddress && (
                <span className="font-mono text-zinc-400">
                  ({settings.printer.bluetoothDeviceAddress})
                </span>
              )}
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleScanPairedPrinters}
                className="flex min-h-[36px] items-center gap-1.5 rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-1.5 font-semibold text-yellow-400 hover:border-yellow-400"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Baca Paired Bluetooth</span>
              </button>
              {settings.printer.bluetoothDeviceAddress && (
                <button
                  type="button"
                  onClick={handleDisconnectPrinter}
                  className="flex min-h-[36px] items-center gap-1 rounded-lg border border-zinc-700 bg-zinc-900 px-2.5 py-1.5 text-zinc-400 hover:text-red-400"
                >
                  <Unplug className="h-3.5 w-3.5" />
                  <span>Putus</span>
                </button>
              )}
            </div>
          </div>

          <div className="grid gap-2 sm:grid-cols-2">
            <div>
              <label className="block text-[11px] text-zinc-400 mb-1">
                Nama Perangkat Bluetooth
              </label>
              <input
                type="text"
                value={settings.printer.bluetoothDeviceName}
                onChange={(e) => updateField('bluetoothDeviceName', e.target.value)}
                placeholder="Contoh: RPP02N / MTP-II"
                className="w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-zinc-100 focus:border-yellow-400 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-[11px] text-zinc-400 mb-1">
                Alamat MAC Bluetooth
              </label>
              <input
                type="text"
                value={settings.printer.bluetoothDeviceAddress}
                onChange={(e) => updateField('bluetoothDeviceAddress', e.target.value)}
                placeholder="Contoh: 00:11:22:33:44:55"
                className="w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 font-mono text-zinc-100 focus:border-yellow-400 focus:outline-none"
              />
            </div>
          </div>

          {pairedDevices.length > 0 && (
            <div className="divide-y divide-zinc-800 rounded-lg border border-zinc-800 bg-zinc-900">
              {pairedDevices.map((dev) => (
                <div
                  key={dev.address}
                  className="flex items-center justify-between px-3 py-2"
                >
                  <div>
                    <div className="font-semibold text-white">{dev.name}</div>
                    <div className="font-mono text-[11px] text-zinc-400">
                      {dev.address}
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleConnectPrinter(dev)}
                    className="rounded-lg bg-yellow-400 px-3 py-1.5 font-bold text-zinc-950 hover:bg-yellow-300"
                  >
                    Pilih & Hubungkan
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Print Options */}
        <div className="grid gap-3 sm:grid-cols-3">
          <div>
            <label className="block font-medium text-zinc-300 mb-1">
              Jumlah Copy Struk
            </label>
            <input
              type="number"
              min={1}
              max={3}
              value={settings.printer.printCopies}
              onChange={(e) =>
                updateField('printCopies', Math.max(1, Number(e.target.value) || 1))
              }
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 font-mono text-zinc-100 focus:border-yellow-400 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-zinc-300 mb-1">
              Spasi Baris Akhir (Feed Lines)
            </label>
            <input
              type="number"
              min={1}
              max={8}
              value={settings.printer.feedLinesAfterPrint}
              onChange={(e) =>
                updateField(
                  'feedLinesAfterPrint',
                  Math.max(1, Number(e.target.value) || 3)
                )
              }
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 font-mono text-zinc-100 focus:border-yellow-400 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-zinc-300 mb-1">
              Catatan Header Struk
            </label>
            <input
              type="text"
              value={settings.printer.customHeaderNote}
              onChange={(e) => updateField('customHeaderNote', e.target.value)}
              placeholder="Terang Bulan / Martabak Manis"
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-zinc-100 focus:border-yellow-400 focus:outline-none"
            />
          </div>
        </div>

        {/* Checkboxes */}
        <div className="grid gap-2.5 sm:grid-cols-2 lg:grid-cols-3 pt-1">
          <label className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3 cursor-pointer text-zinc-200">
            <input
              type="checkbox"
              checked={settings.printer.autoPrintAfterCheckout}
              onChange={(e) =>
                updateField('autoPrintAfterCheckout', e.target.checked)
              }
              className="h-4 w-4 accent-yellow-400"
            />
            <span>Auto Print Setelah Checkout</span>
          </label>

          <label className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3 cursor-pointer text-zinc-200">
            <input
              type="checkbox"
              checked={settings.printer.showStoreAddress}
              onChange={(e) => updateField('showStoreAddress', e.target.checked)}
              className="h-4 w-4 accent-yellow-400"
            />
            <span>Tampilkan Alamat Toko</span>
          </label>

          <label className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3 cursor-pointer text-zinc-200">
            <input
              type="checkbox"
              checked={settings.printer.showStorePhone}
              onChange={(e) => updateField('showStorePhone', e.target.checked)}
              className="h-4 w-4 accent-yellow-400"
            />
            <span>Tampilkan Nomor Telepon</span>
          </label>

          <label className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3 cursor-pointer text-zinc-200">
            <input
              type="checkbox"
              checked={settings.printer.showCashierName}
              onChange={(e) => updateField('showCashierName', e.target.checked)}
              className="h-4 w-4 accent-yellow-400"
            />
            <span>Tampilkan Nama Kasir</span>
          </label>

          <label className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3 cursor-pointer text-zinc-200">
            <input
              type="checkbox"
              checked={settings.printer.showReceiptLogo}
              onChange={(e) =>
                updateField('showReceiptLogo', e.target.checked)
              }
              className="h-4 w-4 accent-yellow-400"
            />
            <span>Tampilkan Logo pada Struk</span>
          </label>

          <label className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3 cursor-pointer text-zinc-200">
            <input
              type="checkbox"
              checked={settings.printer.showReceiptFooter}
              onChange={(e) => updateField('showReceiptFooter', e.target.checked)}
              className="h-4 w-4 accent-yellow-400"
            />
            <span>Tampilkan Footer Struk</span>
          </label>
        </div>
      </div>
    </div>
  );
};
