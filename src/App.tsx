/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { LoginModal } from './components/LoginModal';
import { OfflineIndicator } from './components/OfflineIndicator';
import { RoleHeader } from './components/RoleHeader';
import { googleSheetsService } from './services/googleSheets';
import { printerService } from './services/printer';
import { storageService } from './services/storage';
import {
  GoogleSheetConfig,
  OwnerTab,
  ProductItem,
  StoreSettings,
  TransactionRecord,
  UserProfile,
} from './types';
import { KasirView } from './views/KasirView';
import { LoginView } from './views/LoginView';
import { OwnerContainer } from './views/OwnerContainer';

export default function App() {
  const [products, setProducts] = useState<ProductItem[]>(() =>
    storageService.getProducts()
  );
  const [transactions, setTransactions] = useState<TransactionRecord[]>(() =>
    storageService.getTransactions()
  );
  const [settings, setSettings] = useState<StoreSettings>(() =>
    storageService.getSettings()
  );
  const [sheetConfig, setSheetConfig] = useState<GoogleSheetConfig>(() =>
    storageService.getSheetConfig()
  );
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(() =>
    storageService.getCurrentUser()
  );
  const [activeOwnerTab, setActiveOwnerTab] = useState<OwnerTab>('dashboard');
  const [roleSwitchModalOpen, setRoleSwitchModalOpen] = useState(false);

  const handleLoginSuccess = (user: UserProfile) => {
    setCurrentUser(user);
    storageService.saveCurrentUser(user);
  };

  const handleLogout = () => {
    setCurrentUser(null);
    storageService.saveCurrentUser(null);
  };

  const handleUpdateProducts = (nextProducts: ProductItem[]) => {
    setProducts(nextProducts);
    storageService.saveProducts(nextProducts);
  };

  const handleSaveSettings = (nextSettings: StoreSettings) => {
    setSettings(nextSettings);
    storageService.saveSettings(nextSettings);
  };

  const handleSaveSheetConfig = (nextConfig: GoogleSheetConfig) => {
    setSheetConfig(nextConfig);
    storageService.saveSheetConfig(nextConfig);
  };

  const handlePrintTransaction = (trx: TransactionRecord) => {
    return printerService.printReceipt(trx, settings);
  };

  const handleSaveTransaction = async (
    trx: TransactionRecord,
    updatedProducts: ProductItem[]
  ): Promise<{
    synced: boolean;
    printResult?: { success: boolean; message: string };
  }> => {
    // 1. Update local products & transactions first (Offline-First)
    setProducts(updatedProducts);
    storageService.saveProducts(updatedProducts);

    let finalTrx: TransactionRecord = { ...trx };
    const initialList = [finalTrx, ...transactions];
    setTransactions(initialList);
    storageService.saveTransactions(initialList);

    // 2. Sync to Google Sheets if configured and online
    let synced = false;
    if (
      sheetConfig.autoSync &&
      sheetConfig.webhookUrl &&
      typeof navigator !== 'undefined' &&
      navigator.onLine
    ) {
      const syncRes = await googleSheetsService.syncTransaction(
        sheetConfig.webhookUrl,
        finalTrx,
        updatedProducts
      );
      if (syncRes.success) {
        synced = true;
        finalTrx = {
          ...finalTrx,
          syncStatus: 'synced',
          syncedAt: new Date().toISOString(),
        };
      } else {
        finalTrx = {
          ...finalTrx,
          syncStatus: 'failed',
          syncError: syncRes.message,
        };
      }
      const syncedList = [finalTrx, ...transactions];
      setTransactions(syncedList);
      storageService.saveTransactions(syncedList);
    }

    // 3. Auto Print if enabled
    let printResult: { success: boolean; message: string } | undefined;
    if (settings.printer.autoPrintAfterCheckout) {
      printResult = printerService.printReceipt(finalTrx, settings);
    }

    return { synced, printResult };
  };

  const handleRetrySync = async (trx: TransactionRecord) => {
    if (!sheetConfig.webhookUrl) return;
    const res = await googleSheetsService.syncTransaction(
      sheetConfig.webhookUrl,
      trx,
      products
    );
    const updatedList = transactions.map((t) =>
      t.id === trx.id
        ? {
            ...t,
            syncStatus: (res.success ? 'synced' : 'failed') as TransactionRecord['syncStatus'],
            syncedAt: res.success ? new Date().toISOString() : t.syncedAt,
            syncError: res.success ? undefined : res.message,
          }
        : t
    );
    setTransactions(updatedList);
    storageService.saveTransactions(updatedList);
  };

  if (!currentUser) {
    return (
      <>
        <LoginView settings={settings} onLoginSuccess={handleLoginSuccess} />
        <OfflineIndicator />
      </>
    );
  }

  return (
    <div id="top" className="min-h-dvh bg-zinc-950 text-zinc-100 flex flex-col justify-between">
      <div>
        <RoleHeader
          currentUser={currentUser}
          settings={settings}
          activeTab={currentUser.role === 'owner' ? activeOwnerTab : undefined}
          onSelectTab={
            currentUser.role === 'owner' ? setActiveOwnerTab : undefined
          }
          onOpenRoleSwitch={() => setRoleSwitchModalOpen(true)}
          onLogout={handleLogout}
        />

        {currentUser.role === 'owner' ? (
          <OwnerContainer
            activeTab={activeOwnerTab}
            onSelectTab={setActiveOwnerTab}
            products={products}
            transactions={transactions}
            currentUser={currentUser}
            settings={settings}
            sheetConfig={sheetConfig}
            onUpdateProducts={handleUpdateProducts}
            onSaveTransaction={handleSaveTransaction}
            onRetrySync={handleRetrySync}
            onPrintTransaction={handlePrintTransaction}
            onSaveSettings={handleSaveSettings}
            onSaveSheetConfig={handleSaveSheetConfig}
            onRestoreBackupData={(restored) => {
              setProducts(restored.products);
              setTransactions(restored.transactions);
              setSettings(restored.settings);
              setSheetConfig(restored.sheetConfig);
            }}
            onResetAllData={() => {
              const reset = storageService.resetAllData();
              setProducts(reset.products);
              setTransactions(reset.transactions);
              setSettings(reset.settings);
              setSheetConfig(reset.sheetConfig);
            }}
          />
        ) : (
          <KasirView
            products={products}
            transactions={transactions}
            currentUser={currentUser}
            settings={settings}
            onSaveTransaction={handleSaveTransaction}
            onPrintTransaction={handlePrintTransaction}
          />
        )}
      </div>

      <footer className="bg-zinc-950 px-6 py-3 text-center text-xs text-zinc-500">
        <span className="font-semibold text-yellow-400">{settings.storeName}</span>
        <span className="mx-1.5" aria-hidden="true">·</span>
        <span>{settings.storeAddress}</span>
      </footer>

      <LoginModal
        isOpen={roleSwitchModalOpen}
        onClose={() => setRoleSwitchModalOpen(false)}
        targetRole={currentUser.role === 'owner' ? 'kasir' : 'owner'}
        settings={settings}
        onSuccessSwitch={(nextUser) => {
          setCurrentUser(nextUser);
          storageService.saveCurrentUser(nextUser);
        }}
      />

      <OfflineIndicator />
    </div>
  );
}
