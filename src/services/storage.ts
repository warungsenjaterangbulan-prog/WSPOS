import {
  INITIAL_PRODUCTS,
  INITIAL_SETTINGS,
  INITIAL_SHEET_CONFIG,
  INITIAL_TRANSACTIONS,
  INITIAL_USERS,
} from '../data/initialData';
import {
  GoogleSheetConfig,
  ProductItem,
  StoreSettings,
  TransactionRecord,
  UserProfile,
} from '../types';

export const STORAGE_KEYS = {
  PRODUCTS: 'kasir_terang_bulan_products_v1',
  TRANSACTIONS: 'kasir_terang_bulan_transactions_v1',
  CURRENT_USER: 'kasir_terang_bulan_current_user_v1',
  USERS: 'kasir_terang_bulan_users_v1',
  SETTINGS: 'kasir_terang_bulan_settings_v1',
  SHEET_CONFIG: 'kasir_terang_bulan_sheet_config_v1',
};

function safeParse<T>(raw: string | null, fallback: T): T {
  if (!raw) return fallback;
  try {
    const parsed = JSON.parse(raw);
    return parsed ?? fallback;
  } catch {
    return fallback;
  }
}

export const storageService = {
  getProducts(): ProductItem[] {
    const raw = localStorage.getItem(STORAGE_KEYS.PRODUCTS);
    if (raw === null) {
      localStorage.setItem(STORAGE_KEYS.PRODUCTS, JSON.stringify(INITIAL_PRODUCTS));
      return INITIAL_PRODUCTS;
    }
    const data = safeParse<ProductItem[]>(raw, []);
    if (!Array.isArray(data)) {
      localStorage.setItem(STORAGE_KEYS.PRODUCTS, JSON.stringify([]));
      return [];
    }
    let migrated = false;
    const normalized = data.map((item, idx) => {
      const nextBarcode =
        item.barcode && item.barcode.startsWith('8997026')
          ? String(idx + 1).padStart(2, '0')
          : item.barcode;
      if (nextBarcode !== item.barcode) {
        migrated = true;
      }
      if (!Array.isArray(item.variants) || item.variants.length === 0) {
        migrated = true;
        const initialMatch = INITIAL_PRODUCTS.find((ip) => ip.id === item.id);
        const defaultVariants = initialMatch?.variants || [
          {
            id: `v-${item.id}-1`,
            name: item.category || 'Reguler',
            purchasePrice: item.purchasePrice,
            sellingPrice: item.sellingPrice,
          },
        ];
        return {
          ...item,
          barcode: nextBarcode,
          variants: defaultVariants,
        };
      }
      return {
        ...item,
        barcode: nextBarcode,
      };
    });
    if (migrated) {
      localStorage.setItem(STORAGE_KEYS.PRODUCTS, JSON.stringify(normalized));
    }
    return normalized;
  },

  saveProducts(products: ProductItem[]): void {
    localStorage.setItem(STORAGE_KEYS.PRODUCTS, JSON.stringify(products));
  },

  getTransactions(): TransactionRecord[] {
    const raw = localStorage.getItem(STORAGE_KEYS.TRANSACTIONS);
    if (raw === null) {
      localStorage.setItem(
        STORAGE_KEYS.TRANSACTIONS,
        JSON.stringify(INITIAL_TRANSACTIONS)
      );
      return INITIAL_TRANSACTIONS;
    }
    const data = safeParse<TransactionRecord[]>(raw, []);
    if (!Array.isArray(data)) {
      localStorage.setItem(STORAGE_KEYS.TRANSACTIONS, JSON.stringify([]));
      return [];
    }
    return data;
  },

  saveTransactions(transactions: TransactionRecord[]): void {
    localStorage.setItem(STORAGE_KEYS.TRANSACTIONS, JSON.stringify(transactions));
  },

  getUsers(): UserProfile[] {
    const data = safeParse<UserProfile[]>(
      localStorage.getItem(STORAGE_KEYS.USERS),
      INITIAL_USERS
    );
    if (!Array.isArray(data) || data.length === 0) {
      localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(INITIAL_USERS));
      return INITIAL_USERS;
    }
    return data;
  },

  saveUsers(users: UserProfile[]): void {
    localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
  },

  getCurrentUser(): UserProfile | null {
    return safeParse<UserProfile | null>(
      localStorage.getItem(STORAGE_KEYS.CURRENT_USER),
      null
    );
  },

  saveCurrentUser(user: UserProfile | null): void {
    if (!user) {
      localStorage.removeItem(STORAGE_KEYS.CURRENT_USER);
    } else {
      localStorage.setItem(STORAGE_KEYS.CURRENT_USER, JSON.stringify(user));
    }
  },

  getSettings(): StoreSettings {
    const saved = safeParse<Partial<StoreSettings>>(
      localStorage.getItem(STORAGE_KEYS.SETTINGS),
      INITIAL_SETTINGS
    );
    const savedHeaderNote = saved.printer?.customHeaderNote;
    const savedOpHours = saved.operationalHours;
    return {
      ...INITIAL_SETTINGS,
      ...saved,
      operationalHours:
        !savedOpHours || savedOpHours === 'Setiap Hari 15.00 - 21.00'
          ? 'Buka Jam 15.00 - 21.00'
          : savedOpHours,
      printer: {
        ...INITIAL_SETTINGS.printer,
        ...(saved.printer || {}),
        customHeaderNote:
          !savedHeaderNote || savedHeaderNote === 'Terang Bulan & Martabak Manis'
            ? 'Terang Bulan / Martabak Manis'
            : savedHeaderNote,
      },
    };
  },

  saveSettings(settings: StoreSettings): void {
    localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(settings));
    const users = this.getUsers().map((u) => {
      if (u.role === 'owner') return { ...u, pin: settings.ownerPin };
      if (u.role === 'kasir') return { ...u, pin: settings.kasirPin };
      return u;
    });
    this.saveUsers(users);
  },

  getSheetConfig(): GoogleSheetConfig {
    const saved = safeParse<Partial<GoogleSheetConfig>>(
      localStorage.getItem(STORAGE_KEYS.SHEET_CONFIG),
      INITIAL_SHEET_CONFIG
    );
    return {
      ...INITIAL_SHEET_CONFIG,
      ...saved,
    };
  },

  saveSheetConfig(config: GoogleSheetConfig): void {
    localStorage.setItem(STORAGE_KEYS.SHEET_CONFIG, JSON.stringify(config));
  },

  exportBackupJson(): string {
    const payload = {
      app: 'Kasir Terang Bulan',
      version: '1.0.0',
      exportedAt: new Date().toISOString(),
      products: this.getProducts(),
      transactions: this.getTransactions(),
      settings: this.getSettings(),
      sheetConfig: this.getSheetConfig(),
      users: this.getUsers(),
    };
    return JSON.stringify(payload, null, 2);
  },

  restoreBackupJson(jsonText: string): {
    products: ProductItem[];
    transactions: TransactionRecord[];
    settings: StoreSettings;
    sheetConfig: GoogleSheetConfig;
  } {
    const parsed = JSON.parse(jsonText);
    if (!parsed || typeof parsed !== 'object') {
      throw new Error('Format file backup tidak valid.');
    }
    if (!Array.isArray(parsed.products) || !Array.isArray(parsed.transactions)) {
      throw new Error('Data products atau transactions tidak ditemukan dalam file backup.');
    }
    const products: ProductItem[] = parsed.products;
    const transactions: TransactionRecord[] = parsed.transactions;
    const settings: StoreSettings = {
      ...INITIAL_SETTINGS,
      ...(parsed.settings || {}),
      printer: {
        ...INITIAL_SETTINGS.printer,
        ...(parsed.settings?.printer || {}),
      },
    };
    const sheetConfig: GoogleSheetConfig = {
      ...INITIAL_SHEET_CONFIG,
      ...(parsed.sheetConfig || {}),
    };

    this.saveProducts(products);
    this.saveTransactions(transactions);
    this.saveSettings(settings);
    this.saveSheetConfig(sheetConfig);

    return { products, transactions, settings, sheetConfig };
  },

  resetAllData(): {
    products: ProductItem[];
    transactions: TransactionRecord[];
    settings: StoreSettings;
    sheetConfig: GoogleSheetConfig;
  } {
    const emptyProducts: ProductItem[] = [];
    const emptyTransactions: TransactionRecord[] = [];
    const currentSettings = this.getSettings();
    const currentSheetConfig = this.getSheetConfig();

    this.saveProducts(emptyProducts);
    this.saveTransactions(emptyTransactions);

    return {
      products: emptyProducts,
      transactions: emptyTransactions,
      settings: currentSettings,
      sheetConfig: currentSheetConfig,
    };
  },
};
