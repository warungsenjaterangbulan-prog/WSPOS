export type UserRole = 'owner' | 'kasir';

export type TransactionType = 'IN' | 'OUT';

export type SyncStatus = 'synced' | 'pending' | 'failed' | 'local';

export type OwnerTab =
  | 'dashboard'
  | 'kasir'
  | 'inventory'
  | 'history'
  | 'sheets'
  | 'settings';

export interface UserProfile {
  id: string;
  name: string;
  role: UserRole;
  pin: string;
}

export interface ProductVariant {
  id: string;
  name: string;
  purchasePrice: number;
  sellingPrice: number;
}

export interface ProductItem {
  id: string;
  barcode: string;
  name: string;
  category: string;
  variants?: ProductVariant[];
  stock: number;
  unit: string;
  minStock: number;
  purchasePrice: number;
  sellingPrice: number;
  location: string;
  lastUpdated: string;
}

export interface CartItem {
  cartItemKey?: string;
  productId: string;
  barcode: string;
  name: string;
  category: string;
  qty: number;
  unit: string;
  unitCost?: number;
  unitPrice: number;
  subtotal: number;
  availableStock: number;
}

export interface TransactionRecord {
  id: string;
  code: string;
  type: TransactionType;
  timestamp: string;
  operatorRole: UserRole;
  operatorName: string;
  items: CartItem[];
  totalQty: number;
  totalAmount: number;
  recipientOrSupplier: string;
  referenceNo: string;
  note: string;
  syncStatus: SyncStatus;
  syncError?: string;
  syncedAt?: string;
}

export interface GoogleSheetConfig {
  webhookUrl: string;
  spreadsheetUrl?: string;
  autoSync: boolean;
  lastSyncAt?: string;
  isConnected?: boolean;
}

export interface PrinterSettings {
  connectionMode: 'browser' | 'bluetooth';
  paperSize: '58mm' | '80mm';
  bluetoothDeviceName: string;
  bluetoothDeviceAddress: string;
  autoPrintAfterCheckout: boolean;
  printCopies: number;
  feedLinesAfterPrint: number;
  showStoreAddress: boolean;
  showStorePhone: boolean;
  showCashierName: boolean;
  showReceiptLogo: boolean;
  receiptLogoDataUrl: string;
  showReceiptFooter: boolean;
  customHeaderNote: string;
}

export interface StoreSettings {
  storeName: string;
  storeAddress: string;
  storePhone: string;
  operationalHours: string;
  receiptFooter: string;
  ownerPin: string;
  kasirPin: string;
  allowManualBarcode: boolean;
  enableScanSound: boolean;
  printer: PrinterSettings;
}

export interface BluetoothDeviceItem {
  name: string;
  address: string;
}
