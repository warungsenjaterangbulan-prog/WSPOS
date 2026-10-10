import React, { useState } from 'react';
import {
  Download,
  Edit3,
  Plus,
  RefreshCw,
  Trash2,
  X,
} from 'lucide-react';
import { ProductItem, ProductVariant, StoreSettings } from '../../types';
import {
  formatRupiah,
  generateMenuBarcode,
} from '../../utils/formatter';

interface OwnerInventoryViewProps {
  products: ProductItem[];
  settings: StoreSettings;
  onUpdateProducts: (nextProducts: ProductItem[]) => void;
}

interface VariantDraft {
  id: string;
  name: string;
  purchasePrice: string;
  sellingPrice: string;
}

export const OwnerInventoryView: React.FC<OwnerInventoryViewProps> = ({
  products,
  onUpdateProducts,
}) => {
  const [editingProduct, setEditingProduct] = useState<ProductItem | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  // Form states
  const [formMenuNumber, setFormMenuNumber] = useState('');
  const [formName, setFormName] = useState('');
  const [formVariants, setFormVariants] = useState<VariantDraft[]>([
    { id: 'v-1', name: '', purchasePrice: '', sellingPrice: '' },
  ]);

  const showNotice = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => {
      setToastMsg((prev) => (prev === msg ? null : prev));
    }, 3000);
  };

  const getNextAvailableMenuNumber = () => {
    const usedNumbers = new Set(
      products.map((p) => p.barcode.trim().toLowerCase())
    );
    for (let i = 1; i <= 999; i++) {
      const candidate = generateMenuBarcode(i);
      if (!usedNumbers.has(candidate.toLowerCase())) {
        return candidate;
      }
    }
    return generateMenuBarcode(products.length + 1);
  };

  const openCreateModal = () => {
    setIsCreating(true);
    setEditingProduct(null);
    setFormMenuNumber(getNextAvailableMenuNumber());
    setFormName('');
    setFormVariants([
      { id: `v-${Date.now()}-1`, name: '', purchasePrice: '', sellingPrice: '' },
    ]);
  };

  const openEditModal = (product: ProductItem) => {
    setIsCreating(false);
    setEditingProduct(product);
    setFormMenuNumber(product.barcode);
    setFormName(product.name);
    const existingVariants =
      product.variants && product.variants.length > 0
        ? product.variants.map((v) => ({
            id: v.id,
            name: v.name,
            purchasePrice: String(v.purchasePrice || ''),
            sellingPrice: String(v.sellingPrice || ''),
          }))
        : [
            {
              id: `v-${Date.now()}-1`,
              name: product.category || '',
              purchasePrice: String(product.purchasePrice || ''),
              sellingPrice: String(product.sellingPrice || ''),
            },
          ];
    setFormVariants(existingVariants);
  };

  const closeFormModal = () => {
    setIsCreating(false);
    setEditingProduct(null);
  };

  const handleAddVariantRow = () => {
    setFormVariants((prev) => [
      ...prev,
      {
        id: `v-${Date.now()}-${prev.length + 1}`,
        name: '',
        purchasePrice: '',
        sellingPrice: '',
      },
    ]);
  };

  const handleRemoveVariantRow = (id: string) => {
    if (formVariants.length <= 1) return;
    setFormVariants((prev) => prev.filter((v) => v.id !== id));
  };

  const handleUpdateVariantDraft = (
    id: string,
    field: 'name' | 'purchasePrice' | 'sellingPrice',
    value: string
  ) => {
    setFormVariants((prev) =>
      prev.map((v) => (v.id === id ? { ...v, [field]: value } : v))
    );
  };

  const handleSaveMenu = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanMenuNo = formMenuNumber.trim();
    const cleanName = formName.trim();
    if (!cleanMenuNo || !cleanName) return;

    const duplicate = products.find(
      (p) =>
        p.barcode.toLowerCase() === cleanMenuNo.toLowerCase() &&
        p.id !== editingProduct?.id
    );
    if (duplicate) {
      showNotice(`Nomor Menu ${cleanMenuNo} sudah digunakan oleh "${duplicate.name}".`);
      return;
    }

    const cleanedVariants: ProductVariant[] = formVariants
      .map((v, idx) => ({
        id: v.id || `v-${Date.now()}-${idx}`,
        name: v.name.trim(),
        purchasePrice: Math.max(0, parseInt(v.purchasePrice.replace(/\D/g, ''), 10) || 0),
        sellingPrice: Math.max(0, parseInt(v.sellingPrice.replace(/\D/g, ''), 10) || 0),
      }))
      .filter((v) => v.name.length > 0);

    if (cleanedVariants.length === 0) {
      showNotice('Minimal isi 1 nama varian beserta harganya.');
      return;
    }

    const primaryVariant = cleanedVariants[0];
    const nowIso = new Date().toISOString();

    if (isCreating) {
      const newMenu: ProductItem = {
        id: `tb-${Date.now()}`,
        barcode: cleanMenuNo,
        name: cleanName,
        category: primaryVariant.name,
        variants: cleanedVariants,
        stock: 999,
        unit: 'pcs',
        minStock: 0,
        purchasePrice: primaryVariant.purchasePrice,
        sellingPrice: primaryVariant.sellingPrice,
        location: '-',
        lastUpdated: nowIso,
      };
      onUpdateProducts([...products, newMenu]);
      showNotice(`Menu No. ${newMenu.barcode} "${newMenu.name}" ditambahkan.`);
    } else if (editingProduct) {
      const updated = products.map((p) =>
        p.id === editingProduct.id
          ? {
              ...p,
              barcode: cleanMenuNo,
              name: cleanName,
              category: primaryVariant.name,
              variants: cleanedVariants,
              purchasePrice: primaryVariant.purchasePrice,
              sellingPrice: primaryVariant.sellingPrice,
              lastUpdated: nowIso,
            }
          : p
      );
      onUpdateProducts(updated);
      showNotice(`Menu No. ${cleanMenuNo} "${cleanName}" diperbarui.`);
    }
    closeFormModal();
  };

  const handleDeleteMenu = (product: ProductItem) => {
    const updated = products.filter((p) => p.id !== product.id);
    onUpdateProducts(updated);
    showNotice(`Menu "${product.name}" dihapus.`);
  };

  const handleExportCsv = () => {
    const headers = [
      'No. Menu',
      'Nama Menu',
      'Varian',
      'Harga Modal',
      'Harga Jual',
    ];
    const rows = products.map((p) => {
      const variantSummary =
        p.variants && p.variants.length > 0
          ? p.variants.map((v) => `${v.name} (${v.sellingPrice})`).join(' | ')
          : p.category;
      return [
        `"${p.barcode}"`,
        `"${p.name.replace(/"/g, '""')}"`,
        `"${variantSummary.replace(/"/g, '""')}"`,
        p.purchasePrice,
        p.sellingPrice,
      ];
    });
    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute(
      'download',
      `menu-terang-bulan-${new Date().toISOString().slice(0, 10)}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-4">
      {toastMsg && (
        <div className="flex items-center justify-between rounded-xl border border-yellow-400/40 bg-yellow-400/15 px-4 py-2.5 text-xs font-medium text-yellow-300">
          <span>{toastMsg}</span>
          <button type="button" onClick={() => setToastMsg(null)}>
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      <div className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900 shadow-lg">
        <div className="flex flex-wrap items-center justify-between gap-3 bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-5 py-4 text-zinc-950">
          <div>
            <h1 className="font-display text-base sm:text-lg font-bold text-zinc-950">
              Daftar Menu & Varian Terang Bulan ({products.length})
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleExportCsv}
              className="flex min-h-[38px] items-center gap-1.5 rounded-xl border border-zinc-950/25 bg-zinc-950/10 px-3 py-1.5 text-xs font-bold text-zinc-950 hover:bg-zinc-950/20 whitespace-nowrap"
            >
              <Download className="h-3.5 w-3.5" />
              <span>CSV</span>
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              className="flex min-h-[38px] items-center gap-1.5 rounded-xl bg-zinc-950 px-3.5 py-1.5 text-xs font-bold text-yellow-400 shadow-xs hover:bg-zinc-900 whitespace-nowrap"
            >
              <Plus className="h-4 w-4" />
              <span>Tambah Menu</span>
            </button>
          </div>
        </div>

        <div className="p-4 sm:p-5">
          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-zinc-800 bg-zinc-950/60 text-zinc-400">
                  <th className="py-2.5 pr-4 pl-3 font-semibold whitespace-nowrap">No. Menu</th>
                  <th className="px-4 py-2.5 font-semibold whitespace-nowrap">Nama Menu</th>
                  <th className="px-4 py-2.5 font-semibold">Varian & Harga Jual</th>
                  <th className="px-4 py-2.5 text-right font-semibold whitespace-nowrap">Harga Modal</th>
                  <th className="px-4 py-2.5 text-right font-semibold whitespace-nowrap">Harga Jual</th>
                  <th className="py-2.5 pr-3 pl-4 text-right font-semibold whitespace-nowrap">Aksi</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/70">
                {products.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-zinc-500">
                      Belum ada daftar menu. Klik tombol &ldquo;Tambah Menu&rdquo; untuk membuat menu baru.
                    </td>
                  </tr>
                ) : (
                  products.map((p) => {
                    const variantsList =
                      p.variants && p.variants.length > 0
                        ? p.variants
                        : [
                            {
                              id: 'default',
                              name: p.category,
                              purchasePrice: p.purchasePrice,
                              sellingPrice: p.sellingPrice,
                            },
                          ];
                    const modalPrices = variantsList.map((v) => v.purchasePrice);
                    const minModal = Math.min(...modalPrices);
                    const maxModal = Math.max(...modalPrices);
                    const jualPrices = variantsList.map((v) => v.sellingPrice);
                    const minJual = Math.min(...jualPrices);
                    const maxJual = Math.max(...jualPrices);

                    return (
                      <tr key={p.id} className="hover:bg-zinc-800/40">
                        <td className="py-3 pr-4 pl-3 font-mono font-bold text-yellow-400 tabular-nums whitespace-nowrap">
                          No. {p.barcode}
                        </td>
                        <td className="px-4 py-3 font-semibold text-white">{p.name}</td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-1.5">
                            {variantsList.map((v) => (
                              <span
                                key={v.id}
                                className="inline-flex items-center gap-1 rounded-lg border border-zinc-800 bg-zinc-950 px-2.5 py-1 text-[11px] text-zinc-200"
                              >
                                <span className="font-medium">{v.name}:</span>
                                <span className="font-mono font-bold text-yellow-400 tabular-nums">
                                  {formatRupiah(v.sellingPrice)}
                                </span>
                              </span>
                            ))}
                          </div>
                        </td>
                        <td className="px-4 py-3 text-right font-mono tabular-nums text-zinc-400 whitespace-nowrap">
                          {minModal !== maxModal
                            ? `${formatRupiah(minModal)} – ${formatRupiah(maxModal)}`
                            : formatRupiah(minModal)}
                        </td>
                        <td className="px-4 py-3 text-right font-mono font-bold tabular-nums text-yellow-400 whitespace-nowrap">
                          {minJual !== maxJual
                            ? `${formatRupiah(minJual)} – ${formatRupiah(maxJual)}`
                            : formatRupiah(minJual)}
                        </td>
                        <td className="py-3 pr-3 pl-4 text-right whitespace-nowrap">
                          <div className="inline-flex items-center gap-1">
                            <button
                              type="button"
                              onClick={() => openEditModal(p)}
                              className="flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-700 bg-zinc-950 text-yellow-400 hover:border-yellow-400"
                              title="Edit Menu & Varian"
                            >
                              <Edit3 className="h-3.5 w-3.5" />
                            </button>
                            <button
                              type="button"
                              onClick={() => handleDeleteMenu(p)}
                              className="flex h-8 w-8 items-center justify-center rounded-lg border border-zinc-800 bg-zinc-950 text-zinc-400 hover:border-red-500 hover:text-red-400"
                              title="Hapus"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Add / Edit Modal with Multi-Variant & Price Support */}
      {(isCreating || editingProduct) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-3 sm:p-4 backdrop-blur-xs">
          <div className="max-h-[90dvh] w-full max-w-lg overflow-y-auto rounded-3xl border border-yellow-400/30 bg-zinc-900 shadow-2xl">
            <div className="sticky top-0 z-10 flex items-center justify-between bg-gradient-to-r from-yellow-400 via-yellow-400 to-amber-500 px-5 py-3.5 text-zinc-950">
              <h3 className="font-display text-sm font-bold">
                {isCreating ? 'Tambah Menu & Varian Baru' : 'Edit Menu & Varian'}
              </h3>
              <button
                type="button"
                onClick={closeFormModal}
                className="text-zinc-900 hover:text-black"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSaveMenu} className="p-5 space-y-4 text-xs">
              <div className="grid grid-cols-12 gap-2.5">
                <div className="col-span-4">
                  <label className="block font-medium text-zinc-300 mb-1">
                    No. Menu
                  </label>
                  <div className="flex gap-1.5">
                    <input
                      type="text"
                      required
                      value={formMenuNumber}
                      onChange={(e) => setFormMenuNumber(e.target.value)}
                      placeholder="01"
                      className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 font-mono text-yellow-400 focus:border-yellow-400 focus:outline-none"
                    />
                    <button
                      type="button"
                      onClick={() =>
                        setFormMenuNumber(getNextAvailableMenuNumber())
                      }
                      className="flex items-center justify-center rounded-xl border border-zinc-800 bg-zinc-950 px-2.5 text-yellow-400 hover:border-yellow-400"
                      title="Nomor Otomatis"
                    >
                      <RefreshCw className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>

                <div className="col-span-8">
                  <label className="block font-medium text-zinc-300 mb-1">
                    Nama Menu Terang Bulan
                  </label>
                  <input
                    type="text"
                    required
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    placeholder="Contoh: Terang Bulan Coklat"
                    className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-zinc-100 focus:border-yellow-400 focus:outline-none"
                  />
                </div>
              </div>

              {/* Dynamic Variants & Prices */}
              <div className="space-y-2.5">
                <div className="flex items-center justify-between">
                  <label className="font-semibold text-zinc-200">
                    Daftar Varian & Harga Menu
                  </label>
                  <button
                    type="button"
                    onClick={handleAddVariantRow}
                    className="flex items-center gap-1 rounded-lg bg-yellow-400 px-2.5 py-1 text-[11px] font-bold text-zinc-950 hover:bg-yellow-300"
                  >
                    <Plus className="h-3 w-3" />
                    <span>Tambah Varian</span>
                  </button>
                </div>

                <div className="space-y-2.5">
                  {formVariants.map((v, index) => (
                    <div
                      key={v.id}
                      className="rounded-2xl border border-zinc-800 bg-zinc-950 p-3.5 space-y-2.5"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-mono text-[11px] font-bold text-yellow-400">
                          Varian #{index + 1}
                        </span>
                        {formVariants.length > 1 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveVariantRow(v.id)}
                            className="text-zinc-500 hover:text-red-400"
                            title="Hapus Varian"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        )}
                      </div>

                      <div>
                        <label className="block text-[11px] font-medium text-zinc-400 mb-1">
                          Nama Varian (Ketik Manual)
                        </label>
                        <input
                          type="text"
                          required
                          value={v.name}
                          onChange={(e) =>
                            handleUpdateVariantDraft(v.id, 'name', e.target.value)
                          }
                          placeholder="Contoh: Coklat Biasa / Coklat Spesial / Keju"
                          className="w-full rounded-xl border border-zinc-800 bg-zinc-900 px-3 py-2 text-zinc-100 focus:border-yellow-400 focus:outline-none"
                        />
                      </div>

                      <div className="grid grid-cols-2 gap-2.5">
                        <div>
                          <label className="block text-[11px] font-medium text-zinc-400 mb-1">
                            Harga Beli / Modal
                          </label>
                          <div className="relative">
                            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 font-mono text-xs text-zinc-500">
                              Rp
                            </span>
                            <input
                              type="text"
                              inputMode="numeric"
                              required
                              value={v.purchasePrice}
                              onChange={(e) =>
                                handleUpdateVariantDraft(
                                  v.id,
                                  'purchasePrice',
                                  e.target.value.replace(/\D/g, '')
                                )
                              }
                              placeholder="0"
                              className="w-full rounded-xl border border-zinc-800 bg-zinc-900 py-2 pr-3 pl-8 font-mono text-zinc-100 focus:border-yellow-400 focus:outline-none"
                            />
                          </div>
                        </div>

                        <div>
                          <label className="block text-[11px] font-medium text-zinc-400 mb-1">
                            Harga Jual Kasir
                          </label>
                          <div className="relative">
                            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 font-mono text-xs text-yellow-400">
                              Rp
                            </span>
                            <input
                              type="text"
                              inputMode="numeric"
                              required
                              value={v.sellingPrice}
                              onChange={(e) =>
                                handleUpdateVariantDraft(
                                  v.id,
                                  'sellingPrice',
                                  e.target.value.replace(/\D/g, '')
                                )
                              }
                              placeholder="0"
                              className="w-full rounded-xl border border-zinc-800 bg-zinc-900 py-2 pr-3 pl-8 font-mono font-bold text-yellow-400 focus:border-yellow-400 focus:outline-none"
                            />
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={closeFormModal}
                  className="min-h-[40px] rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2 text-zinc-300"
                >
                  Batal
                </button>
                <button
                  type="submit"
                  className="min-h-[40px] rounded-xl bg-yellow-400 px-5 py-2 font-bold text-zinc-950 hover:bg-yellow-300"
                >
                  Simpan Menu
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
