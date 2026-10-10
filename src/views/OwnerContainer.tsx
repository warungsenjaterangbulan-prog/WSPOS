import React from 'react';
import {
  GoogleSheetConfig,
  OwnerTab,
  ProductItem,
  StoreSettings,
  TransactionRecord,
  UserProfile,
} from '../types';
import { KasirView } from './KasirView';
import { OwnerDashboardView } from './owner/OwnerDashboardView';
import { OwnerGoogleSheetView } from './owner/OwnerGoogleSheetView';
import { OwnerHistoryView } from './owner/OwnerHistoryView';
import { OwnerInventoryView } from './owner/OwnerInventoryView';
import { OwnerSettingsView } from './owner/OwnerSettingsView';

interface OwnerContainerProps {
  activeTab: OwnerTab;
  onSelectTab: (tab: OwnerTab) => void;
  products: ProductItem[];
  transactions: TransactionRecord[];
  currentUser: UserProfile;
  settings: StoreSettings;
  sheetConfig: GoogleSheetConfig;
  onUpdateProducts: (nextProducts: ProductItem[]) => void;
  onSaveTransaction: (
    trx: TransactionRecord,
    updatedProducts: ProductItem[]
  ) => Promise<{ synced: boolean; printResult?: { success: boolean; message: string } }>;
  onRetrySync: (trx: TransactionRecord) => Promise<void>;
  onPrintTransaction: (trx: TransactionRecord) => { success: boolean; message: string };
  onSaveSettings: (nextSettings: StoreSettings) => void;
  onSaveSheetConfig: (nextConfig: GoogleSheetConfig) => void;
  onRestoreBackupData: (data: {
    products: ProductItem[];
    transactions: TransactionRecord[];
    settings: StoreSettings;
    sheetConfig: GoogleSheetConfig;
  }) => void;
  onResetAllData: () => void;
}

export const OwnerContainer: React.FC<OwnerContainerProps> = ({
  activeTab,
  onSelectTab,
  products,
  transactions,
  currentUser,
  settings,
  sheetConfig,
  onUpdateProducts,
  onSaveTransaction,
  onRetrySync,
  onPrintTransaction,
  onSaveSettings,
  onSaveSheetConfig,
  onRestoreBackupData,
  onResetAllData,
}) => {
  return (
    <div className="pb-12">
      {activeTab === 'kasir' ? (
        <KasirView
          products={products}
          transactions={transactions}
          currentUser={currentUser}
          settings={settings}
          onSaveTransaction={onSaveTransaction}
          onPrintTransaction={onPrintTransaction}
        />
      ) : (
        <main className="mx-auto max-w-6xl px-3 py-4 sm:px-6 sm:py-6">
          {activeTab === 'dashboard' && (
            <OwnerDashboardView
              products={products}
              transactions={transactions}
              settings={settings}
              onNavigateTab={onSelectTab}
            />
          )}

          {activeTab === 'inventory' && (
            <OwnerInventoryView
              products={products}
              settings={settings}
              onUpdateProducts={onUpdateProducts}
            />
          )}

          {activeTab === 'history' && (
            <OwnerHistoryView
              transactions={transactions}
              settings={settings}
              onRetrySync={onRetrySync}
              onPrintTransaction={onPrintTransaction}
            />
          )}

          {activeTab === 'sheets' && (
            <OwnerGoogleSheetView
              sheetConfig={sheetConfig}
              products={products}
              onSaveSheetConfig={onSaveSheetConfig}
              onUpdateProductsFromSheet={onUpdateProducts}
            />
          )}

          {activeTab === 'settings' && (
            <OwnerSettingsView
              settings={settings}
              onSaveSettings={onSaveSettings}
              onRestoreBackupData={onRestoreBackupData}
              onResetAllData={onResetAllData}
            />
          )}
        </main>
      )}
    </div>
  );
};
