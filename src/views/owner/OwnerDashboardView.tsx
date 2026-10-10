import React, { useMemo } from 'react';
import {
  Coins,
  Package,
  ShoppingBag,
  Store,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import {
  CartItem,
  OwnerTab,
  ProductItem,
  StoreSettings,
  TransactionRecord,
} from '../../types';
import { formatDateTime, formatRupiah } from '../../utils/formatter';

interface OwnerDashboardViewProps {
  products: ProductItem[];
  transactions: TransactionRecord[];
  settings: StoreSettings;
  onNavigateTab: (tab: OwnerTab) => void;
}

export const OwnerDashboardView: React.FC<OwnerDashboardViewProps> = ({
  products,
  transactions,
  settings,
  onNavigateTab,
}) => {
  const getItemUnitCost = (item: CartItem): number => {
    if (typeof item.unitCost === 'number' && item.unitCost > 0) {
      return item.unitCost;
    }
    const prod = products.find(
      (p) => p.id === item.productId || p.barcode === item.barcode
    );
    if (!prod) return 0;
    const matchedVar = prod.variants?.find(
      (v) => v.name.toLowerCase() === (item.category || '').toLowerCase()
    );
    return matchedVar ? matchedVar.purchasePrice : prod.purchasePrice || 0;
  };

  const getTransactionCost = (trx: TransactionRecord): number => {
    return trx.items.reduce(
      (sum, item) => sum + getItemUnitCost(item) * item.qty,
      0
    );
  };

  const stats = useMemo(() => {
    const totalMenu = products.length;
    const salesTransactions = transactions.filter((t) => t.type === 'OUT');

    const totalSalesQty = salesTransactions.reduce((sum, t) => sum + t.totalQty, 0);
    const totalRevenue = salesTransactions.reduce(
      (sum, t) => sum + t.totalAmount,
      0
    );
    const totalModal = salesTransactions.reduce(
      (sum, t) => sum + getTransactionCost(t),
      0
    );
    const totalLaba = totalRevenue - totalModal;

    return {
      totalMenu,
      totalSalesCount: salesTransactions.length,
      totalSalesQty,
      totalRevenue,
      totalModal,
      totalLaba,
    };
  }, [products, transactions]);

  return (
    <div className="space-y-4">
      {/* Modern Yellow & Black Store Hero Banner */}
      <div className="overflow-hidden rounded-3xl bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 p-5 sm:p-6 text-zinc-950 shadow-lg">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div className="flex items-center gap-3.5">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-zinc-950 text-yellow-400 shadow-md">
              <Store className="h-6 w-6" />
            </div>
            <div>
              <h1 className="font-display text-xl sm:text-2xl font-bold text-zinc-950">
                {settings.storeName || 'Warung Senja Terang Bulan'}
              </h1>
              <p className="text-xs font-medium text-zinc-900/80">
                {settings.storeAddress} · {settings.operationalHours}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={() => onNavigateTab('kasir')}
              className="min-h-[42px] rounded-xl bg-zinc-950 px-4 py-2 text-xs font-bold text-yellow-400 shadow-sm hover:bg-zinc-900 whitespace-nowrap"
            >
              Buka Kasir
            </button>
            <button
              type="button"
              onClick={() => onNavigateTab('inventory')}
              className="min-h-[42px] rounded-xl border border-zinc-950/25 bg-zinc-950/10 px-4 py-2 text-xs font-bold text-zinc-950 hover:bg-zinc-950/20 whitespace-nowrap"
            >
              Daftar Menu
            </button>
          </div>
        </div>
      </div>

      {/* Primary Financial & Operational Metrics (Omzet, Modal, Laba, Terjual, Total Menu) */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900 p-4 shadow-sm">
          <div className="flex items-center justify-between text-xs font-medium text-zinc-400">
            <span>Omzet</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-yellow-400/15 text-yellow-400">
              <TrendingUp className="h-4 w-4 shrink-0" />
            </div>
          </div>
          <div className="mt-2 font-mono text-base sm:text-lg font-bold text-yellow-400 tabular-nums">
            {formatRupiah(stats.totalRevenue)}
          </div>
          <div className="mt-0.5 text-[11px] text-zinc-500 font-mono">
            {stats.totalSalesCount} transaksi
          </div>
        </div>

        <div className="rounded-2xl border border-zinc-800 bg-zinc-900 p-4 shadow-sm">
          <div className="flex items-center justify-between text-xs font-medium text-zinc-400">
            <span>Modal</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-zinc-800 text-zinc-300">
              <Wallet className="h-4 w-4 shrink-0" />
            </div>
          </div>
          <div className="mt-2 font-mono text-base sm:text-lg font-bold text-zinc-200 tabular-nums">
            {formatRupiah(stats.totalModal)}
          </div>
          <div className="mt-0.5 text-[11px] text-zinc-500 font-mono">
            Harga beli / modal
          </div>
        </div>

        <div className="rounded-2xl border border-yellow-400/40 bg-zinc-900 p-4 shadow-sm">
          <div className="flex items-center justify-between text-xs font-semibold text-yellow-400">
            <span>Laba</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-yellow-400 text-zinc-950">
              <Coins className="h-4 w-4 shrink-0" />
            </div>
          </div>
          <div className="mt-2 font-mono text-base sm:text-lg font-bold text-emerald-400 tabular-nums">
            {formatRupiah(stats.totalLaba)}
          </div>
          <div className="mt-0.5 text-[11px] text-zinc-400 font-mono">
            Keuntungan bersih
          </div>
        </div>

        <div className="rounded-2xl border border-zinc-800 bg-zinc-900 p-4 shadow-sm">
          <div className="flex items-center justify-between text-xs font-medium text-zinc-400">
            <span>Terjual</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-yellow-400/15 text-yellow-400">
              <ShoppingBag className="h-4 w-4 shrink-0" />
            </div>
          </div>
          <div className="mt-2 font-mono text-base sm:text-lg font-bold text-white tabular-nums">
            {stats.totalSalesQty} pcs
          </div>
          <div className="mt-0.5 text-[11px] text-zinc-500 font-mono">
            Pesanan selesai
          </div>
        </div>

        <div className="col-span-2 sm:col-span-1 rounded-2xl border border-zinc-800 bg-zinc-900 p-4 shadow-sm">
          <div className="flex items-center justify-between text-xs font-medium text-zinc-400">
            <span>Total Menu</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-yellow-400/15 text-yellow-400">
              <Package className="h-4 w-4 shrink-0" />
            </div>
          </div>
          <div className="mt-2 font-mono text-base sm:text-lg font-bold text-white tabular-nums">
            {stats.totalMenu} Menu
          </div>
          <div className="mt-0.5 text-[11px] text-zinc-500 font-mono">
            Aktif di Kasir
          </div>
        </div>
      </div>

      {/* Modern Recent Transactions with Modal & Laba */}
      <div className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 shadow-sm">
        <div className="flex items-center justify-between border-b border-zinc-800 px-5 py-3.5">
          <h2 className="font-display text-sm font-bold text-white">
            Transaksi Terbaru (Rincian Modal & Laba)
          </h2>
          <button
            type="button"
            onClick={() => onNavigateTab('history')}
            className="text-xs font-bold text-yellow-400 hover:underline"
          >
            Lihat Semua →
          </button>
        </div>

        <div className="p-4 sm:p-5">
          {transactions.length === 0 ? (
            <div className="py-6 text-center text-xs text-zinc-500">
              Belum ada transaksi tercatat.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-zinc-800 text-zinc-400">
                    <th className="py-2 pr-4 font-semibold whitespace-nowrap">Kode TRX</th>
                    <th className="px-4 py-2 font-semibold whitespace-nowrap">Waktu</th>
                    <th className="px-4 py-2 font-semibold whitespace-nowrap">Pelanggan</th>
                    <th className="px-4 py-2 text-right font-semibold whitespace-nowrap">Qty</th>
                    <th className="px-4 py-2 text-right font-semibold whitespace-nowrap">Modal</th>
                    <th className="px-4 py-2 text-right font-semibold whitespace-nowrap">Penjualan</th>
                    <th className="py-2 pl-4 text-right font-semibold whitespace-nowrap">Laba</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800/70">
                  {transactions
                    .filter((t) => t.type === 'OUT')
                    .slice(0, 6)
                    .map((trx) => {
                      const trxModal = getTransactionCost(trx);
                      const trxLaba = trx.totalAmount - trxModal;
                      return (
                        <tr key={trx.id} className="hover:bg-zinc-800/40">
                          <td className="py-3 pr-4 font-mono font-semibold text-zinc-100 whitespace-nowrap">
                            {trx.code}
                          </td>
                          <td className="px-4 py-3 text-zinc-400 font-mono tabular-nums whitespace-nowrap">
                            {formatDateTime(trx.timestamp)}
                          </td>
                          <td className="px-4 py-3 text-zinc-200">
                            {trx.recipientOrSupplier || 'Pelanggan Umum'}
                          </td>
                          <td className="px-4 py-3 text-right font-mono tabular-nums text-zinc-300 whitespace-nowrap">
                            {trx.totalQty} pcs
                          </td>
                          <td className="px-4 py-3 text-right font-mono tabular-nums text-zinc-400 whitespace-nowrap">
                            {formatRupiah(trxModal)}
                          </td>
                          <td className="px-4 py-3 text-right font-mono font-bold tabular-nums text-yellow-400 whitespace-nowrap">
                            {formatRupiah(trx.totalAmount)}
                          </td>
                          <td className="py-3 pl-4 text-right font-mono font-bold tabular-nums text-emerald-400 whitespace-nowrap">
                            +{formatRupiah(trxLaba)}
                          </td>
                        </tr>
                      );
                    })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
