import React, { useMemo, useState } from 'react';
import {
  CheckCircle2,
  Clock,
  Download,
  Eye,
  Printer,
  Receipt,
  RefreshCw,
  Search,
  ShoppingBag,
  TrendingUp,
  X,
} from 'lucide-react';
import { StoreSettings, TransactionRecord } from '../../types';
import { formatDateTime, formatRupiah } from '../../utils/formatter';

interface OwnerHistoryViewProps {
  transactions: TransactionRecord[];
  settings: StoreSettings;
  onRetrySync: (trx: TransactionRecord) => Promise<void>;
  onPrintTransaction: (trx: TransactionRecord) => { success: boolean; message: string };
}

export const OwnerHistoryView: React.FC<OwnerHistoryViewProps> = ({
  transactions,
  settings,
  onRetrySync,
  onPrintTransaction,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTrx, setSelectedTrx] = useState<TransactionRecord | null>(null);
  const [retryingId, setRetryingId] = useState<string | null>(null);

  const filteredTransactions = useMemo(() => {
    return transactions.filter((t) => {
      if (t.type !== 'OUT') return false;
      const q = searchQuery.trim().toLowerCase();
      return (
        !q ||
        t.code.toLowerCase().includes(q) ||
        t.operatorName.toLowerCase().includes(q) ||
        t.recipientOrSupplier.toLowerCase().includes(q) ||
        t.items.some(
          (i) =>
            i.name.toLowerCase().includes(q) ||
            i.barcode.toLowerCase().includes(q) ||
            i.category.toLowerCase().includes(q)
        )
      );
    });
  }, [transactions, searchQuery]);

  const summary = useMemo(() => {
    const totalCount = filteredTransactions.length;
    const totalQty = filteredTransactions.reduce((sum, t) => sum + t.totalQty, 0);
    const totalRevenue = filteredTransactions.reduce(
      (sum, t) => sum + t.totalAmount,
      0
    );
    return { totalCount, totalQty, totalRevenue };
  }, [filteredTransactions]);

  const handleExportCsv = () => {
    const headers = [
      'Kode Transaksi',
      'Waktu',
      'Operator',
      'Pelanggan',
      'Total Qty',
      'Total Nilai (Rp)',
      'Status Sync',
    ];
    const rows = filteredTransactions.map((t) => [
      `"${t.code}"`,
      `"${t.timestamp}"`,
      `"${t.operatorName}"`,
      `"${(t.recipientOrSupplier || '').replace(/"/g, '""')}"`,
      t.totalQty,
      t.totalAmount,
      `"${t.syncStatus}"`,
    ]);
    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute(
      'download',
      `riwayat-terang-bulan-${new Date().toISOString().slice(0, 10)}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-4">
      {/* Clear & Easy-to-See Transaction Count Indicators */}
      <div className="grid grid-cols-3 gap-3">
        <div className="rounded-2xl border border-yellow-400/30 bg-zinc-900 p-4 shadow-sm">
          <div className="flex items-center justify-between text-xs font-medium text-zinc-400">
            <span>Jumlah Transaksi</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-yellow-400/15 text-yellow-400">
              <Receipt className="h-4 w-4 shrink-0" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-1.5">
            <span className="font-mono text-xl sm:text-2xl font-bold text-yellow-400 tabular-nums">
              {summary.totalCount}
            </span>
            <span className="text-xs font-semibold text-zinc-400">Transaksi</span>
          </div>
        </div>

        <div className="rounded-2xl border border-zinc-800 bg-zinc-900 p-4 shadow-sm">
          <div className="flex items-center justify-between text-xs font-medium text-zinc-400">
            <span>Total Menu Terjual</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-yellow-400/15 text-yellow-400">
              <ShoppingBag className="h-4 w-4 shrink-0" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-1.5">
            <span className="font-mono text-xl sm:text-2xl font-bold text-white tabular-nums">
              {summary.totalQty}
            </span>
            <span className="text-xs font-semibold text-zinc-400">pcs</span>
          </div>
        </div>

        <div className="rounded-2xl border border-zinc-800 bg-zinc-900 p-4 shadow-sm">
          <div className="flex items-center justify-between text-xs font-medium text-zinc-400">
            <span>Total Penjualan</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-yellow-400/15 text-yellow-400">
              <TrendingUp className="h-4 w-4 shrink-0" />
            </div>
          </div>
          <div className="mt-2 font-mono text-base sm:text-xl font-bold text-yellow-400 tabular-nums">
            {formatRupiah(summary.totalRevenue)}
          </div>
        </div>
      </div>

      {/* Main History Table Card */}
      <div className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900 shadow-lg">
        <div className="flex flex-wrap items-center justify-between gap-3 bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-5 py-4 text-zinc-950">
          <div className="flex items-center gap-3">
            <h1 className="font-display text-base sm:text-lg font-bold text-zinc-950">
              Riwayat Penjualan
            </h1>
            <span className="inline-flex items-center rounded-xl bg-zinc-950 px-3 py-1 font-mono text-xs font-bold text-yellow-400 tabular-nums shadow-xs">
              {summary.totalCount} Transaksi
            </span>
          </div>
          <button
            type="button"
            onClick={handleExportCsv}
            className="flex min-h-[38px] items-center gap-1.5 rounded-xl bg-zinc-950 px-3.5 py-1.5 text-xs font-bold text-yellow-400 shadow-xs hover:bg-zinc-900 whitespace-nowrap"
          >
            <Download className="h-3.5 w-3.5" />
            <span>Export CSV</span>
          </button>
        </div>

        <div className="p-4 sm:p-5">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-yellow-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Cari kode TRX, menu, varian, atau pelanggan..."
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 py-2.5 pr-4 pl-10 text-xs text-zinc-100 placeholder:text-zinc-500 focus:border-yellow-400 focus:outline-none"
            />
          </div>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-zinc-800 bg-zinc-950/60 text-zinc-400">
                  <th className="py-3 pr-3 pl-3 font-semibold whitespace-nowrap">No.</th>
                  <th className="px-3 py-3 font-semibold whitespace-nowrap">Kode TRX</th>
                  <th className="px-4 py-3 font-semibold whitespace-nowrap">Waktu</th>
                  <th className="px-4 py-3 font-semibold whitespace-nowrap">Pelanggan</th>
                  <th className="px-4 py-3 text-center font-semibold whitespace-nowrap">Jumlah</th>
                  <th className="py-3 pr-8 pl-4 text-right font-semibold whitespace-nowrap">Total</th>
                  <th className="py-3 pr-4 pl-6 font-semibold whitespace-nowrap">Status Sync</th>
                  <th className="py-3 pr-3 pl-4 text-right font-semibold whitespace-nowrap">Aksi</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/70">
                {filteredTransactions.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-8 text-center text-zinc-500">
                      Belum ada transaksi yang sesuai.
                    </td>
                  </tr>
                ) : (
                  filteredTransactions.map((trx, idx) => (
                    <tr key={trx.id} className="hover:bg-zinc-800/40">
                      <td className="py-3.5 pr-3 pl-3 font-mono font-bold text-zinc-400 tabular-nums whitespace-nowrap">
                        #{idx + 1}
                      </td>
                      <td className="px-3 py-3.5 font-mono font-bold text-zinc-100 whitespace-nowrap">
                        {trx.code}
                      </td>
                      <td className="px-4 py-3.5 text-zinc-400 font-mono tabular-nums whitespace-nowrap">
                        {formatDateTime(trx.timestamp)}
                      </td>
                      <td className="px-4 py-3.5 text-zinc-200">
                        <div className="font-semibold text-white">
                          {trx.recipientOrSupplier || 'Pelanggan Umum'}
                        </div>
                        <div className="text-[11px] text-zinc-400 truncate max-w-[200px]">
                          {trx.items.map((i) => `${i.qty}x ${i.name}`).join(', ')}
                        </div>
                      </td>
                      <td className="px-4 py-3.5 text-center whitespace-nowrap">
                        <span className="inline-flex min-w-[64px] items-center justify-center rounded-lg border border-yellow-400/30 bg-yellow-400/10 px-2.5 py-1 font-mono text-xs font-bold text-yellow-400 tabular-nums">
                          {trx.totalQty} pcs
                        </span>
                      </td>
                      <td className="py-3.5 pr-8 pl-4 text-right font-mono font-bold tabular-nums text-yellow-400 whitespace-nowrap">
                        {formatRupiah(trx.totalAmount)}
                      </td>
                      <td className="py-3.5 pr-4 pl-6 whitespace-nowrap">
                        <div className="flex items-center gap-1.5 text-[11px]">
                          {trx.syncStatus === 'synced' ? (
                            <span className="inline-flex items-center gap-1 font-medium text-emerald-400">
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              <span>Synced</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 font-medium text-zinc-400">
                              <Clock className="h-3.5 w-3.5" />
                              <span>Lokal</span>
                            </span>
                          )}
                          {trx.syncStatus !== 'synced' && (
                            <button
                              type="button"
                              disabled={retryingId === trx.id}
                              onClick={async () => {
                                setRetryingId(trx.id);
                                await onRetrySync(trx);
                                setRetryingId(null);
                              }}
                              className="rounded-md border border-zinc-700 bg-zinc-950 px-1.5 py-0.5 text-[10px] font-medium text-yellow-400 hover:border-yellow-400"
                              title="Sync ke Sheets"
                            >
                              <RefreshCw
                                className={`h-3 w-3 ${
                                  retryingId === trx.id ? 'animate-spin' : ''
                                }`}
                              />
                            </button>
                          )}
                        </div>
                      </td>
                      <td className="py-3.5 pr-3 pl-4 text-right whitespace-nowrap">
                        <div className="inline-flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => setSelectedTrx(trx)}
                            className="flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-800 bg-zinc-950 text-zinc-300 hover:border-zinc-600"
                            title="Detail"
                          >
                            <Eye className="h-3.5 w-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => onPrintTransaction(trx)}
                            className="flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-700 bg-zinc-950 text-yellow-400 hover:border-yellow-400"
                            title="Cetak Struk"
                          >
                            <Printer className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Detail Modal */}
      {selectedTrx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-3 sm:p-4 backdrop-blur-xs">
          <div className="max-h-[90dvh] w-full max-w-sm overflow-y-auto rounded-3xl border border-yellow-400/30 bg-zinc-900 shadow-2xl">
            <div className="sticky top-0 z-10 flex items-center justify-between bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-5 py-3.5 text-zinc-950">
              <div>
                <h3 className="font-display text-sm font-bold">
                  Detail Penjualan
                </h3>
                <p className="font-mono text-[11px] font-bold text-zinc-900/80">{selectedTrx.code}</p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedTrx(null)}
                className="text-zinc-900 hover:text-black"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="p-5 space-y-3 text-xs">
              {settings.printer.showReceiptLogo &&
                settings.printer.receiptLogoDataUrl && (
                  <div className="flex justify-center">
                    <img
                      src={settings.printer.receiptLogoDataUrl}
                      alt={`Logo ${settings.storeName}`}
                      className="max-h-14 max-w-[110px] object-contain"
                    />
                  </div>
                )}

              <div className="grid grid-cols-2 gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3">
                <div>
                  <span className="text-zinc-400">Waktu:</span>
                  <div className="font-mono font-medium text-zinc-100">
                    {formatDateTime(selectedTrx.timestamp)}
                  </div>
                </div>
                <div>
                  <span className="text-zinc-400">Pelanggan:</span>
                  <div className="font-medium text-zinc-100">
                    {selectedTrx.recipientOrSupplier || 'Pelanggan Umum'}
                  </div>
                </div>
              </div>

              <div className="divide-y divide-zinc-800 max-h-[200px] overflow-y-auto border-y border-zinc-800 py-1.5">
                {selectedTrx.items.map((item, idx) => (
                  <div
                    key={item.cartItemKey || `${item.productId}-${idx}`}
                    className="flex items-center justify-between py-1.5"
                  >
                    <div>
                      <div className="font-semibold text-white">
                        {item.name} {item.category ? `(${item.category})` : ''}
                      </div>
                      <div className="font-mono text-[11px] text-zinc-400 tabular-nums">
                        {item.qty} x {formatRupiah(item.unitPrice)}
                      </div>
                    </div>
                    <div className="font-mono font-semibold text-yellow-400 tabular-nums">
                      {formatRupiah(item.subtotal)}
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex items-center justify-between font-mono text-sm font-bold text-white tabular-nums">
                <span>TOTAL ({selectedTrx.totalQty} pcs)</span>
                <span className="text-yellow-400">
                  {formatRupiah(selectedTrx.totalAmount)}
                </span>
              </div>

              {selectedTrx.note && (
                <div className="rounded-lg bg-zinc-950 border border-zinc-800 p-2 text-zinc-300">
                  Catatan: {selectedTrx.note}
                </div>
              )}

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setSelectedTrx(null)}
                  className="min-h-[38px] rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-1.5 text-xs font-medium text-zinc-300"
                >
                  Tutup
                </button>
                <button
                  type="button"
                  onClick={() => onPrintTransaction(selectedTrx)}
                  className="flex min-h-[38px] items-center gap-1.5 rounded-xl bg-yellow-400 px-4 py-1.5 text-xs font-bold text-zinc-950 hover:bg-yellow-300"
                >
                  <Printer className="h-3.5 w-3.5" />
                  <span>Cetak Struk</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
