import { BluetoothDeviceItem, StoreSettings, TransactionRecord } from '../types';
import { formatDateTime, formatRupiah } from '../utils/formatter';

export const SPP_UUID = '000018F0-0000-1000-8000-00805F9B34FB';

// Standard BLE Thermal Printer Service UUIDs supported by Web Bluetooth API
const THERMAL_PRINTER_SERVICE_UUIDS = [
  '000018f0-0000-1000-8000-00805f9b34fb',
  'e7810a71-73ae-499d-8c15-faa9aef0c3f2',
  '49535343-fe7d-4ae5-8fa9-9fafd205e455',
  '0000ff00-0000-1000-8000-00805f9b34fb',
  '0000ffe0-0000-1000-8000-00805f9b34fb',
];

interface WebBluetoothCharacteristic {
  properties?: {
    write?: boolean;
    writeWithoutResponse?: boolean;
  };
  writeValue?: (value: BufferSource) => Promise<void>;
  writeValueWithoutResponse?: (value: BufferSource) => Promise<void>;
}

interface WebBluetoothService {
  getCharacteristics: () => Promise<WebBluetoothCharacteristic[]>;
}

interface WebBluetoothGattServer {
  connected: boolean;
  connect: () => Promise<WebBluetoothGattServer>;
  disconnect: () => void;
  getPrimaryServices: () => Promise<WebBluetoothService[]>;
}

interface WebBluetoothDevice {
  id: string;
  name?: string;
  gatt?: WebBluetoothGattServer;
}

interface WebBluetoothNavigator extends Navigator {
  bluetooth?: {
    requestDevice: (options: {
      acceptAllDevices?: boolean;
      optionalServices?: string[];
    }) => Promise<WebBluetoothDevice>;
    getDevices?: () => Promise<WebBluetoothDevice[]>;
  };
}

const PAIRED_DEVICES_STORAGE_KEY = 'tb_pos_web_bluetooth_devices_v1';

let activeGattDevice: WebBluetoothDevice | null = null;
let activeWriteCharacteristic: WebBluetoothCharacteristic | null = null;

function loadSavedDevices(): BluetoothDeviceItem[] {
  try {
    const raw = localStorage.getItem(PAIRED_DEVICES_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((d) => d && (d.name || d.address))
      .map((d) => ({
        name: String(d.name || 'Printer Thermal Bluetooth'),
        address: String(d.address || ''),
      }));
  } catch {
    return [];
  }
}

function saveDeviceToHistory(device: BluetoothDeviceItem): BluetoothDeviceItem[] {
  try {
    const current = loadSavedDevices();
    const filtered = current.filter(
      (d) => d.address.toLowerCase() !== device.address.toLowerCase()
    );
    const updated = [device, ...filtered].slice(0, 8);
    localStorage.setItem(PAIRED_DEVICES_STORAGE_KEY, JSON.stringify(updated));
    return updated;
  } catch {
    return [device];
  }
}

function encodeAscii(text: string): number[] {
  const clean = text
    .replace(/[^\x20-\x7E]/g, ' ')
    .replace(/\s+/g, ' ');
  const arr: number[] = [];
  for (let i = 0; i < clean.length; i++) {
    arr.push(clean.charCodeAt(i));
  }
  return arr;
}

function formatLineLeftRight(left: string, right: string, width: number): string {
  const safeLeft = left.slice(0, Math.max(1, width - right.length - 1));
  const spaces = Math.max(1, width - safeLeft.length - right.length);
  return safeLeft + ' '.repeat(spaces) + right;
}

export function buildEscPosReceiptBytes(
  transaction: TransactionRecord,
  settings: StoreSettings
): number[] {
  const width = settings.printer.paperSize === '80mm' ? 48 : 32;
  const divider = '-'.repeat(width);
  const doubleDivider = '='.repeat(width);

  const bytes: number[] = [];
  const pushBytes = (...vals: number[]) => bytes.push(...vals);
  const pushLine = (line = '') => {
    bytes.push(...encodeAscii(line), 0x0a);
  };

  // Initialize printer ESC @
  pushBytes(0x1b, 0x40);

  // Align Center
  pushBytes(0x1b, 0x61, 0x01);
  // Bold On
  pushBytes(0x1b, 0x45, 0x01);
  pushLine((settings.storeName || 'WARUNG SENJA TERANG BULAN').toUpperCase());
  // Bold Off
  pushBytes(0x1b, 0x45, 0x00);

  if (settings.printer.customHeaderNote) {
    pushLine(settings.printer.customHeaderNote);
  }
  if (settings.printer.showStoreAddress && settings.storeAddress) {
    pushLine(settings.storeAddress);
  }
  if (settings.printer.showStorePhone && settings.storePhone) {
    pushLine(`Telp: ${settings.storePhone}`);
  }

  pushLine(doubleDivider);
  pushBytes(0x1b, 0x45, 0x01);
  pushLine(transaction.type === 'OUT' ? 'STRUK PENJUALAN' : 'BUKTI STOCK MASUK');
  pushBytes(0x1b, 0x45, 0x00);
  pushLine(divider);

  // Align Left
  pushBytes(0x1b, 0x61, 0x00);
  pushLine(formatLineLeftRight('No. TRX', transaction.code, width));
  pushLine(formatLineLeftRight('Tanggal', formatDateTime(transaction.timestamp), width));
  if (settings.printer.showCashierName) {
    pushLine(
      formatLineLeftRight(
        transaction.type === 'OUT' ? 'Kasir' : 'Operator',
        transaction.operatorName,
        width
      )
    );
  }
  if (transaction.recipientOrSupplier) {
    pushLine(
      formatLineLeftRight(
        transaction.type === 'OUT' ? 'Pelanggan' : 'Supplier',
        transaction.recipientOrSupplier,
        width
      )
    );
  }
  pushLine(divider);

  for (const item of transaction.items) {
    const labelWithVariant = item.category
      ? `${item.name} (${item.category})`
      : item.name;
    pushLine(labelWithVariant.slice(0, width));
    const qtyPrice = `${item.qty} x ${formatRupiah(item.unitPrice)}`;
    const subStr = formatRupiah(item.subtotal);
    pushLine(formatLineLeftRight(qtyPrice, subStr, width));
  }

  pushLine(divider);
  pushBytes(0x1b, 0x45, 0x01);
  pushLine(formatLineLeftRight('TOTAL QTY', `${transaction.totalQty} pcs`, width));
  pushLine(formatLineLeftRight('TOTAL', formatRupiah(transaction.totalAmount), width));
  pushBytes(0x1b, 0x45, 0x00);
  pushLine(divider);

  if (transaction.note) {
    pushLine(`Catatan: ${transaction.note}`);
    pushLine(divider);
  }

  // Center footer
  pushBytes(0x1b, 0x61, 0x01);
  if (settings.printer.showReceiptFooter && settings.receiptFooter) {
    pushLine(settings.receiptFooter);
  }

  const feeds = Math.max(1, Math.min(8, settings.printer.feedLinesAfterPrint || 3));
  for (let i = 0; i < feeds; i++) {
    pushBytes(0x0a);
  }

  return bytes;
}

async function resolveWritableCharacteristic(
  device: WebBluetoothDevice
): Promise<WebBluetoothCharacteristic | null> {
  if (!device.gatt) return null;
  const server = device.gatt.connected ? device.gatt : await device.gatt.connect();
  const services = await server.getPrimaryServices();
  for (const service of services) {
    try {
      const characteristics = await service.getCharacteristics();
      for (const ch of characteristics) {
        if (ch.properties?.write || ch.properties?.writeWithoutResponse) {
          return ch;
        }
      }
    } catch {
      // continue scanning other services
    }
  }
  return null;
}

async function sendBytesOverWebBluetooth(bytes: number[]): Promise<boolean> {
  if (!activeGattDevice) return false;
  try {
    if (!activeWriteCharacteristic || !activeGattDevice.gatt?.connected) {
      activeWriteCharacteristic = await resolveWritableCharacteristic(activeGattDevice);
    }
    if (!activeWriteCharacteristic) return false;

    const uint8 = new Uint8Array(bytes);
    const chunkSize = 100;
    for (let offset = 0; offset < uint8.length; offset += chunkSize) {
      const slice = uint8.slice(offset, offset + chunkSize);
      if (activeWriteCharacteristic.writeValueWithoutResponse) {
        await activeWriteCharacteristic.writeValueWithoutResponse(slice);
      } else if (activeWriteCharacteristic.writeValue) {
        await activeWriteCharacteristic.writeValue(slice);
      }
    }
    return true;
  } catch {
    return false;
  }
}

export const bluetoothPrinterService = {
  isWebBluetoothSupported(): boolean {
    return (
      typeof navigator !== 'undefined' &&
      Boolean((navigator as WebBluetoothNavigator).bluetooth)
    );
  },

  isConnected(): boolean {
    return Boolean(activeGattDevice?.gatt?.connected && activeWriteCharacteristic);
  },

  getPairedDevices(): BluetoothDeviceItem[] {
    return loadSavedDevices();
  },

  async scanAndSelectDevice(): Promise<{
    success: boolean;
    message: string;
    device?: BluetoothDeviceItem;
    devices: BluetoothDeviceItem[];
  }> {
    const nav = navigator as WebBluetoothNavigator;
    if (!nav.bluetooth) {
      const saved = loadSavedDevices();
      return {
        success: false,
        message:
          'Web Bluetooth API hanya tersedia di Chrome / Edge / Opera (Desktop & Mobile). Untuk browser ini, gunakan mode Browser Print.',
        devices: saved,
      };
    }

    try {
      const device = await nav.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: THERMAL_PRINTER_SERVICE_UUIDS,
      });

      const item: BluetoothDeviceItem = {
        name: device.name || 'Printer Thermal BLE',
        address: device.id || 'WEB-BT-PRINTER',
      };

      activeGattDevice = device;
      activeWriteCharacteristic = await resolveWritableCharacteristic(device);
      const updatedList = saveDeviceToHistory(item);

      return {
        success: true,
        message: activeWriteCharacteristic
          ? `Terhubung ke printer Bluetooth: ${item.name}`
          : `Perangkat ${item.name} dipilih dan disimpan.`,
        device: item,
        devices: updatedList,
      };
    } catch (err) {
      const saved = loadSavedDevices();
      const msg =
        err instanceof Error && err.name === 'NotFoundError'
          ? 'Pemilihan perangkat Bluetooth dibatalkan.'
          : 'Tidak dapat memindai perangkat Bluetooth dari browser.';
      return {
        success: false,
        message: msg,
        devices: saved,
      };
    }
  },

  connect(address: string, name = 'Printer Thermal Bluetooth'): {
    success: boolean;
    message: string;
  } {
    if (!address) {
      return { success: false, message: 'ID / Alamat printer Bluetooth kosong.' };
    }
    saveDeviceToHistory({ name, address });
    if (activeGattDevice && activeGattDevice.id === address && activeGattDevice.gatt) {
      void resolveWritableCharacteristic(activeGattDevice).then((ch) => {
        activeWriteCharacteristic = ch;
      });
      return {
        success: true,
        message: `Printer ${name} (${address}) siap digunakan di browser.`,
      };
    }
    return {
      success: true,
      message: `Printer ${name} (${address}) disimpan sebagai printer utama.`,
    };
  },

  disconnect(): { success: boolean; message: string } {
    try {
      if (activeGattDevice?.gatt?.connected) {
        activeGattDevice.gatt.disconnect();
      }
      activeGattDevice = null;
      activeWriteCharacteristic = null;
      return { success: true, message: 'Koneksi Web Bluetooth printer diputus.' };
    } catch {
      return { success: false, message: 'Gagal memutus koneksi printer.' };
    }
  },

  printTransaction(
    transaction: TransactionRecord,
    settings: StoreSettings
  ): { success: boolean; message: string } {
    if (!this.isConnected()) {
      return {
        success: false,
        message:
          'Printer Bluetooth belum terhubung aktif di sesi browser ini. Mengalihkan ke dialog cetak browser.',
      };
    }

    const bytes = buildEscPosReceiptBytes(transaction, settings);
    const copies = Math.max(1, Math.min(3, settings.printer.printCopies || 1));

    void (async () => {
      for (let c = 0; c < copies; c++) {
        await sendBytesOverWebBluetooth(bytes);
      }
    })();

    return {
      success: true,
      message: 'Struk dikirim ke printer thermal Web Bluetooth.',
    };
  },

  printTestReceipt(settings: StoreSettings): { success: boolean; message: string } {
    const sampleTrx: TransactionRecord = {
      id: 'test-print',
      code: 'TB-TEST-001',
      type: 'OUT',
      timestamp: new Date().toISOString(),
      operatorRole: 'owner',
      operatorName: 'Owner Warung Senja',
      items: [
        {
          productId: 'tb-002',
          barcode: '899702600002',
          name: 'Terang Bulan Coklat Lumer',
          category: 'Coklat',
          qty: 1,
          unit: 'pcs',
          unitPrice: 20000,
          subtotal: 20000,
          availableStock: 25,
        },
        {
          productId: 'tb-003',
          barcode: '899702600003',
          name: 'Terang Bulan Keju Kraft Parut',
          category: 'Keju',
          qty: 1,
          unit: 'pcs',
          unitPrice: 22000,
          subtotal: 22000,
          availableStock: 25,
        },
      ],
      totalQty: 2,
      totalAmount: 42000,
      recipientOrSupplier: 'Pelanggan Test',
      referenceNo: 'TEST-01',
      note: 'Test Printer Kasir Terang Bulan',
      syncStatus: 'local',
    };
    return this.printTransaction(sampleTrx, settings);
  },
};
