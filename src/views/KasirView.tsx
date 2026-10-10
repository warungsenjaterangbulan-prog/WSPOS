import React, { useMemo, useState } from 'react';
import confetti from 'canvas-confetti';
import {
  CheckCircle2,
  Minus,
  Plus,
  Printer,
  RotateCcw,
  ShoppingBag,
  Trash2,
  X,
} from 'lucide-react';
import { soundService } from '../services/sound';
import {
  CartItem,
  ProductItem,
  ProductVariant,
  StoreSettings,
  TransactionRecord,
  UserProfile,
} from '../types';
import {
  formatDateTime,
  formatRupiah,
  generateTransactionCode,
} from '../utils/formatter';

interface KasirViewProps {
  products: ProductItem[];
  transactions: TransactionRecord[];
  currentUser: UserProfile;
  settings: StoreSettings;
  onSaveTransaction: (
    trx: TransactionRecord,
    updatedProducts: ProductItem[]
  ) => Promise<{ synced: boolean; printResult?: { success: boolean; message: string } }>;
  onPrintTransaction: (trx: TransactionRecord) => { success: boolean; message: string };
}

export const KasirView: React.FC<KasirViewProps> = ({
  products,
  transactions,
  currentUser,
  settings,
  onSaveTransaction,
  onPrintTransaction,
}) => {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [customerName, setCustomerName] = useState('');
  const [note, setNote] = useState('');
  const [feedback, setFeedback] = useState<{
    type: 'success' | 'error' | 'warning';
    text: string;
  } | null>(null);
  const [completedTrx, setCompletedTrx] = useState<TransactionRecord | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  // Modal state for picking Varian & Harga when clicking a menu
  const [pickingProduct, setPickingProduct] = useState<ProductItem | null>(null);
  const [pickedVariantName, setPickedVariantName] = useState('');
  const [pickedPriceStr, setPickedPriceStr] = useState('');
  const [pickedQty, setPickedQty] = useState(1);

  const showToast = (type: 'success' | 'error' | 'warning', text: string) => {
    setFeedback({ type, text });
    setTimeout(() => {
      setFeedback((prev) => (prev?.text === text ? null : prev));
    }, 3500);
  };

  const getProductVariants = (product: ProductItem): ProductVariant[] => {
    if (product.variants && product.variants.length > 0) {
      return product.variants;
    }
    return [
      {
        id: `v-${product.id}-default`,
        name: product.category || 'Original',
        purchasePrice: product.purchasePrice,
        sellingPrice: product.sellingPrice,
      },
    ];
  };

  const openVariantPicker = (product: ProductItem) => {
    const variants = getProductVariants(product);
    const first = variants[0];
    setPickingProduct(product);
    setPickedVariantName(first.name);
    setPickedPriceStr(String(first.sellingPrice || ''));
    setPickedQty(1);
  };

  const handleConfirmPickVariant = (e: React.FormEvent) => {
    e.preventDefault();
    if (!pickingProduct) return;

    const cleanVariant = pickedVariantName.trim() || pickingProduct.category || 'Reguler';
    const unitPrice = Math.max(0, parseInt(pickedPriceStr.replace(/\D/g, ''), 10) || 0);
    const matchedVariant = getProductVariants(pickingProduct).find(
      (v) => v.name.toLowerCase() === cleanVariant.toLowerCase()
    );
    const unitCost = matchedVariant
      ? matchedVariant.purchasePrice
      : pickingProduct.purchasePrice;
    const addQty = Math.max(1, pickedQty);
    const itemKey = `${pickingProduct.id}::${cleanVariant.toLowerCase()}::${unitPrice}`;

    setCart((prev) => {
      const existingIndex = prev.findIndex(
        (item) => (item.cartItemKey || item.productId) === itemKey
      );
      if (existingIndex > -1) {
        const currentItem = prev[existingIndex];
        const nextQty = currentItem.qty + addQty;
        const updated = [...prev];
        updated[existingIndex] = {
          ...currentItem,
          qty: nextQty,
          unitCost,
          subtotal: nextQty * currentItem.unitPrice,
        };
        return updated;
      } else {
        return [
          ...prev,
          {
            cartItemKey: itemKey,
            productId: pickingProduct.id,
            barcode: pickingProduct.barcode,
            name: pickingProduct.name,
            category: cleanVariant,
            qty: addQty,
            unit: pickingProduct.unit || 'pcs',
            unitCost,
            unitPrice,
            subtotal: addQty * unitPrice,
            availableStock: pickingProduct.stock,
          },
        ];
      }
    });

    setPickingProduct(null);
  };

  const updateCartItemQty = (itemKey: string, nextQty: number) => {
    if (nextQty <= 0) {
      setCart((prev) =>
        prev.filter((item) => (item.cartItemKey || item.productId) !== itemKey)
      );
      return;
    }

    setCart((prev) =>
      prev.map((item) =>
        (item.cartItemKey || item.productId) === itemKey
          ? {
              ...item,
              qty: nextQty,
              subtotal: nextQty * item.unitPrice,
            }
          : item
      )
    );
  };

  const removeCartItem = (itemKey: string) => {
    setCart((prev) =>
      prev.filter((item) => (item.cartItemKey || item.productId) !== itemKey)
    );
  };

  const resetCart = () => {
    setCart([]);
    setCustomerName('');
    setNote('');
  };

  const totalQty = useMemo(
    () => cart.reduce((acc, item) => acc + item.qty, 0),
    [cart]
  );

  const totalAmount = useMemo(
    () => cart.reduce((acc, item) => acc + item.subtotal, 0),
    [cart]
  );

  const handleCheckout = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cart.length === 0) {
      showToast('error', 'Keranjang kasir masih kosong.');
      return;
    }

    setIsProcessing(true);
    try {
      const nowIso = new Date().toISOString();
      const trxCode = generateTransactionCode('OUT', transactions.length);

      const newTransaction: TransactionRecord = {
        id: `trx-${Date.now()}`,
        code: trxCode,
        type: 'OUT',
        timestamp: nowIso,
        operatorRole: currentUser.role,
        operatorName: currentUser.name,
        items: cart,
        totalQty,
        totalAmount,
        recipientOrSupplier: customerName.trim() || 'Pelanggan Umum',
        referenceNo: '-',
        note: note.trim(),
        syncStatus: 'local',
      };

      const updatedProducts = products.map((p) => {
        const matchingItems = cart.filter((c) => c.productId === p.id);
        if (matchingItems.length === 0) return p;
        const sumQty = matchingItems.reduce((acc, c) => acc + c.qty, 0);
        return {
          ...p,
          stock: Math.max(0, p.stock - sumQty),
          lastUpdated: nowIso,
        };
      });

      const result = await onSaveTransaction(newTransaction, updatedProducts);

      soundService.playCheckoutSuccess();
      try {
        confetti({
          particleCount: 35,
          spread: 55,
          origin: { y: 0.7 },
        });
      } catch {
        // ignore
      }

      setCompletedTrx(newTransaction);
      resetCart();

      if (result.printResult && !result.printResult.success) {
        showToast(
          'warning',
          `Transaksi ${trxCode} tersimpan, namun cetak otomatis gagal: ${result.printResult.message}`
        );
      } else {
        showToast('success', `Transaksi ${trxCode} berhasil disimpan!`);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-3 py-4 sm:px-6 sm:py-5">
      {feedback && (
        <div
          className={`mb-3 flex items-center justify-between rounded-xl border px-4 py-2.5 text-xs font-medium ${
            feedback.type === 'success'
              ? 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300'
              : feedback.type === 'warning'
              ? 'border-yellow-400/40 bg-yellow-400/15 text-yellow-300'
              : 'border-red-500/40 bg-red-500/15 text-red-300'
          }`}
        >
          <span>{feedback.text}</span>
          <button
            type="button"
            onClick={() => setFeedback(null)}
            className="ml-3 text-zinc-400 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-12 landscape:grid-cols-12">
        {/* Left: Clean Menu List (7 cols) */}
        <div className="md:col-span-7 landscape:col-span-7">
          {/* Menu Grid — Click to Choose Varian & Price */}
          <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
            {products.length === 0 ? (
              <div className="col-span-full rounded-2xl border border-zinc-800 bg-zinc-900 p-6 text-center text-xs text-zinc-400">
                Belum ada menu tersedia.
              </div>
            ) : (
              products.map((product) => {
                const productCartItems = cart.filter(
                  (c) => c.productId === product.id
                );
                const qtyInCart = productCartItems.reduce(
                  (acc, c) => acc + c.qty,
                  0
                );
                const variants = getProductVariants(product);

                return (
                  <button
                    key={product.id}
                    type="button"
                    onClick={() => openVariantPicker(product)}
                    className={`flex items-center justify-between gap-3 rounded-2xl border p-3.5 text-left transition-all ${
                      qtyInCart > 0
                        ? 'border-yellow-400 bg-yellow-400/10'
                        : 'border-zinc-800 bg-zinc-900 hover:border-yellow-400/50'
                    }`}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-1.5 text-[11px] text-zinc-400">
                        <span className="font-mono font-bold text-yellow-400">
                          No. {product.barcode}
                        </span>
                        <span aria-hidden="true">·</span>
                        <span className="truncate">
                          {variants.map((v) => v.name).join(', ')}
                        </span>
                      </div>
                      <div className="mt-0.5 text-xs sm:text-sm font-bold text-white truncate">
                        {product.name}
                      </div>
                      <div className="mt-1 font-mono text-xs font-bold text-yellow-400 tabular-nums">
                        {variants.length > 1
                          ? `${formatRupiah(variants[0].sellingPrice)} – ${formatRupiah(
                              variants[variants.length - 1].sellingPrice
                            )}`
                          : formatRupiah(variants[0].sellingPrice)}
                      </div>
                    </div>

                    <div
                      className={`flex h-9 shrink-0 items-center gap-1 rounded-xl px-3 text-xs font-bold ${
                        qtyInCart > 0
                          ? 'bg-yellow-400 text-zinc-950'
                          : 'border border-yellow-400/40 bg-zinc-950 text-yellow-400'
                      }`}
                    >
                      <Plus className="h-3.5 w-3.5" />
                      <span>{qtyInCart > 0 ? `${qtyInCart} Dipilih` : 'Pilih'}</span>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>

        {/* Right: Simple Cart & Checkout (5 cols) */}
        <div id="keranjang-pesanan" className="md:col-span-5 landscape:col-span-5">
          <div className="sticky top-16 overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 shadow-lg">
            <div className="flex items-center justify-between bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-4 py-3.5 text-zinc-950">
              <div className="flex items-center gap-2">
                <ShoppingBag className="h-4 w-4 text-zinc-950" />
                <h2 className="font-display text-sm font-bold text-zinc-950">
                  Keranjang Pesanan ({totalQty})
                </h2>
              </div>
              {cart.length > 0 && (
                <button
                  type="button"
                  onClick={resetCart}
                  className="flex items-center gap-1 rounded-lg bg-zinc-950/15 px-2.5 py-1 text-[11px] font-bold text-zinc-950 hover:bg-zinc-950/25"
                >
                  <RotateCcw className="h-3 w-3" />
                  <span>Reset</span>
                </button>
              )}
            </div>

            <div className="p-4">
              <div className="max-h-[240px] divide-y divide-zinc-800 overflow-y-auto">
                {cart.length === 0 ? (
                  <div className="py-8 text-center text-xs text-zinc-500">
                    Klik menu di samping untuk memilih varian dan harga.
                  </div>
                ) : (
                  cart.map((item) => {
                    const key = item.cartItemKey || item.productId;
                    return (
                      <div key={key} className="flex items-center justify-between gap-2 py-2.5">
                        <div className="min-w-0 flex-1">
                          <div className="text-xs font-bold text-white truncate">
                            {item.name}
                          </div>
                          <div className="text-[11px] text-zinc-400">
                            Varian: <span className="font-semibold text-zinc-200">{item.category}</span> ·{' '}
                            <span className="font-mono">{formatRupiah(item.unitPrice)}</span>
                          </div>
                          <div className="font-mono text-[11px] text-yellow-400 font-bold tabular-nums">
                            Subtotal: {formatRupiah(item.subtotal)}
                          </div>
                        </div>

                        <div className="flex items-center gap-1 shrink-0">
                          <button
                            type="button"
                            onClick={() => updateCartItemQty(key, item.qty - 1)}
                            className="flex h-7 w-7 items-center justify-center rounded-lg border border-zinc-700 bg-zinc-950 text-zinc-200 hover:border-yellow-400"
                          >
                            <Minus className="h-3 w-3" />
                          </button>
                          <span className="w-7 text-center font-mono text-xs font-bold text-yellow-400 tabular-nums">
                            {item.qty}
                          </span>
                          <button
                            type="button"
                            onClick={() => updateCartItemQty(key, item.qty + 1)}
                            className="flex h-7 w-7 items-center justify-center rounded-lg border border-zinc-700 bg-zinc-950 text-zinc-200 hover:border-yellow-400"
                          >
                            <Plus className="h-3 w-3" />
                          </button>
                          <button
                            type="button"
                            onClick={() => removeCartItem(key)}
                            className="ml-1 flex h-7 w-7 items-center justify-center rounded-lg text-zinc-500 hover:text-red-400"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>

              <form onSubmit={handleCheckout} className="mt-3 space-y-2.5 border-t border-zinc-800 pt-3">
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="text"
                    value={customerName}
                    onChange={(e) => setCustomerName(e.target.value)}
                    placeholder="Nama Pelanggan (Opsional)"
                    className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-100 placeholder:text-zinc-500 focus:border-yellow-400 focus:outline-none"
                  />
                  <input
                    type="text"
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    placeholder="Catatan (Opsional)"
                    className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-100 placeholder:text-zinc-500 focus:border-yellow-400 focus:outline-none"
                  />
                </div>

                <div className="flex items-center justify-between rounded-xl bg-yellow-400/10 px-3.5 py-2.5 border border-yellow-400/30">
                  <span className="text-xs font-bold text-zinc-200">
                    Total Bayar ({totalQty} pcs)
                  </span>
                  <span className="font-mono text-base font-bold text-yellow-400 tabular-nums">
                    {formatRupiah(totalAmount)}
                  </span>
                </div>

                <button
                  type="submit"
                  disabled={cart.length === 0 || isProcessing}
                  className="w-full min-h-[44px] rounded-xl bg-yellow-400 px-4 py-2.5 text-xs sm:text-sm font-bold text-zinc-950 shadow-sm hover:bg-yellow-300 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
                >
                  {isProcessing ? 'Memproses...' : 'Simpan & Cetak Struk'}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>

      {/* Modal Pilih Varian & Harga saat klik Menu */}
      {pickingProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-3 sm:p-4 backdrop-blur-xs">
          <div className="max-h-[90dvh] w-full max-w-md overflow-y-auto rounded-3xl border border-yellow-400/30 bg-zinc-900 shadow-2xl">
            <div className="sticky top-0 z-10 flex items-center justify-between bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-5 py-3.5 text-zinc-950">
              <div>
                <div className="font-mono text-[11px] font-bold text-zinc-900/80">
                  Menu No. {pickingProduct.barcode}
                </div>
                <h3 className="font-display text-sm sm:text-base font-bold text-zinc-950">
                  {pickingProduct.name}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setPickingProduct(null)}
                className="text-zinc-900 hover:text-black"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleConfirmPickVariant} className="p-5 space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-zinc-300 mb-2">
                  Pilih Varian & Harga
                </label>
                <div className="grid grid-cols-1 gap-2">
                  {getProductVariants(pickingProduct).map((v) => {
                    const isSelected =
                      pickedVariantName === v.name &&
                      pickedPriceStr === String(v.sellingPrice);
                    return (
                      <button
                        key={v.id}
                        type="button"
                        onClick={() => {
                          setPickedVariantName(v.name);
                          setPickedPriceStr(String(v.sellingPrice));
                        }}
                        className={`flex items-center justify-between rounded-xl border px-3.5 py-2.5 text-left transition-all ${
                          isSelected
                            ? 'border-yellow-400 bg-yellow-400 text-zinc-950 shadow-xs'
                            : 'border-zinc-800 bg-zinc-950 text-zinc-200 hover:border-yellow-400/50'
                        }`}
                      >
                        <span className="font-bold">{v.name}</span>
                        <span
                          className={`font-mono font-bold tabular-nums ${
                            isSelected ? 'text-zinc-950' : 'text-yellow-400'
                          }`}
                        >
                          {formatRupiah(v.sellingPrice)}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2.5 border-t border-zinc-800 pt-3">
                <div>
                  <label className="block font-medium text-zinc-400 mb-1">
                    Varian Dipilih
                  </label>
                  <input
                    type="text"
                    required
                    value={pickedVariantName}
                    onChange={(e) => setPickedVariantName(e.target.value)}
                    placeholder="Nama Varian"
                    className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-zinc-100 focus:border-yellow-400 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block font-medium text-zinc-400 mb-1">
                    Harga Jual (Rp)
                  </label>
                  <div className="relative">
                    <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 font-mono text-xs text-yellow-400">
                      Rp
                    </span>
                    <input
                      type="text"
                      inputMode="numeric"
                      required
                      value={pickedPriceStr}
                      onChange={(e) =>
                        setPickedPriceStr(e.target.value.replace(/\D/g, ''))
                      }
                      placeholder="0"
                      className="w-full rounded-xl border border-zinc-800 bg-zinc-950 py-2 pr-3 pl-8 font-mono font-bold text-yellow-400 focus:border-yellow-400 focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5">
                <span className="font-semibold text-zinc-300">Jumlah Pesanan</span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setPickedQty((q) => Math.max(1, q - 1))}
                    className="flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-700 bg-zinc-900 text-zinc-100 hover:border-yellow-400"
                  >
                    <Minus className="h-3.5 w-3.5" />
                  </button>
                  <span className="w-8 text-center font-mono text-sm font-bold text-yellow-400 tabular-nums">
                    {pickedQty}
                  </span>
                  <button
                    type="button"
                    onClick={() => setPickedQty((q) => q + 1)}
                    className="flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-700 bg-zinc-900 text-zinc-100 hover:border-yellow-400"
                  >
                    <Plus className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => setPickingProduct(null)}
                  className="min-h-[40px] rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2 text-zinc-300 hover:bg-zinc-800"
                >
                  Batal
                </button>
                <button
                  type="submit"
                  className="min-h-[40px] rounded-xl bg-yellow-400 px-5 py-2 font-bold text-zinc-950 hover:bg-yellow-300"
                >
                  Tambah ke Keranjang ({formatRupiah(
                    (parseInt(pickedPriceStr.replace(/\D/g, ''), 10) || 0) * pickedQty
                  )})
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Portrait Mobile Floating Quick Cart Bar */}
      {cart.length > 0 && (
        <div className="fixed inset-x-0 bottom-3 z-40 px-4 md:hidden landscape:hidden">
          <a
            href="#keranjang-pesanan"
            className="flex items-center justify-between rounded-2xl bg-yellow-400 px-4 py-3 text-zinc-950 shadow-xl"
          >
            <span className="text-xs font-bold">{totalQty} pcs dipilih · Lihat Keranjang</span>
            <span className="font-mono text-sm font-bold tabular-nums">
              {formatRupiah(totalAmount)}
            </span>
          </a>
        </div>
      )}

      {/* Completed Transaction Modal / Struk */}
      {completedTrx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-3 sm:p-4 backdrop-blur-xs">
          <div className="max-h-[90dvh] w-full max-w-sm overflow-y-auto rounded-3xl border border-yellow-400/30 bg-zinc-900 p-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
              <div className="flex items-center gap-2 text-yellow-400">
                <CheckCircle2 className="h-5 w-5" />
                <h3 className="text-sm font-bold text-white">
                  Transaksi Selesai
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setCompletedTrx(null)}
                className="text-zinc-400 hover:text-white"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-3 rounded-2xl border border-zinc-800 bg-zinc-950 p-4 font-mono text-xs text-zinc-200 space-y-2">
              <div className="text-center">
                {settings.printer.showReceiptLogo &&
                  settings.printer.receiptLogoDataUrl && (
                    <div className="mb-2 flex justify-center">
                      <img
                        src={settings.printer.receiptLogoDataUrl}
                        alt={`Logo ${settings.storeName}`}
                        className="max-h-14 max-w-[110px] object-contain"
                      />
                    </div>
                  )}
                <div className="font-bold text-xs uppercase text-yellow-400">
                  {settings.storeName}
                </div>
                {settings.printer.customHeaderNote && (
                  <div className="text-[11px] text-zinc-300">
                    {settings.printer.customHeaderNote}
                  </div>
                )}
                {settings.printer.showStoreAddress && settings.storeAddress && (
                  <div className="text-[11px] text-zinc-400">
                    {settings.storeAddress}
                  </div>
                )}
                {settings.printer.showStorePhone && settings.storePhone && (
                  <div className="text-[11px] text-zinc-400">
                    Telp/WA: {settings.storePhone}
                  </div>
                )}
              </div>

              <div className="border-t border-dashed border-zinc-800 pt-2 space-y-1 text-[11px]">
                <div className="flex justify-between">
                  <span className="text-zinc-400">No. TRX</span>
                  <span className="font-semibold text-white">{completedTrx.code}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-400">Waktu</span>
                  <span>{formatDateTime(completedTrx.timestamp)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-400">Pelanggan</span>
                  <span>{completedTrx.recipientOrSupplier}</span>
                </div>
              </div>

              <div className="border-t border-dashed border-zinc-800 pt-2 space-y-1">
                {completedTrx.items.map((item) => (
                  <div
                    key={item.cartItemKey || item.productId}
                    className="flex justify-between text-[11px] tabular-nums"
                  >
                    <span className="truncate pr-2">
                      {item.qty}x {item.name} ({item.category})
                    </span>
                    <span className="font-semibold text-yellow-400 shrink-0">
                      {formatRupiah(item.subtotal)}
                    </span>
                  </div>
                ))}
              </div>

              <div className="border-t border-dashed border-zinc-800 pt-2 flex justify-between text-xs font-bold tabular-nums">
                <span>TOTAL ({completedTrx.totalQty} pcs)</span>
                <span className="text-yellow-400">{formatRupiah(completedTrx.totalAmount)}</span>
              </div>
            </div>

            <div className="mt-4 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setCompletedTrx(null)}
                className="min-h-[40px] rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2 text-xs font-medium text-zinc-300 hover:bg-zinc-800"
              >
                Tutup
              </button>
              <button
                type="button"
                onClick={() => {
                  const res = onPrintTransaction(completedTrx);
                  if (!res.success) {
                    showToast('error', res.message);
                  }
                }}
                className="flex min-h-[40px] items-center gap-1.5 rounded-xl bg-yellow-400 px-4 py-2 text-xs font-bold text-zinc-950 hover:bg-yellow-300"
              >
                <Printer className="h-4 w-4" />
                <span>Cetak Struk</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
