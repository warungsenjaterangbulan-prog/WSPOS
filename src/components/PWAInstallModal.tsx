import React from 'react';
import { Download, Share, X } from 'lucide-react';

interface PWAInstallModalProps {
  isOpen: boolean;
  onClose: () => void;
  isIOS: boolean;
  isInstallable: boolean;
  onInstall: () => Promise<boolean>;
}

export const PWAInstallModal: React.FC<PWAInstallModalProps> = ({
  isOpen,
  onClose,
  isIOS,
  isInstallable,
  onInstall,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-950/60 p-4 backdrop-blur-xs">
      <div className="w-full max-w-md rounded-2xl border border-amber-200 bg-white p-6 shadow-xl">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-lg font-semibold text-stone-900">
              Install Aplikasi Kasir Terang Bulan
            </h3>
            <p className="mt-1 text-xs text-stone-600">
              Pasang di layar utama untuk akses kasir cepat dan penggunaan offline penuh.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-10 w-10 items-center justify-center rounded-lg text-stone-500 hover:bg-stone-100"
            aria-label="Tutup"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-4 space-y-3 text-sm text-stone-700">
          {isInstallable ? (
            <div className="rounded-xl bg-amber-50 p-4 text-xs text-amber-950">
              Perangkat Anda mendukung instalasi langsung. Klik tombol di bawah untuk memasang{' '}
              <strong>Kasir Terang Bulan</strong>.
            </div>
          ) : isIOS ? (
            <div className="space-y-2 rounded-xl bg-amber-50 p-4 text-xs text-amber-950">
              <p className="font-semibold">Panduan Install di iPhone / iPad (Safari):</p>
              <ol className="list-inside list-decimal space-y-1.5">
                <li>
                  Ketuk ikon <Share className="inline h-3.5 w-3.5" /> <strong>Bagikan (Share)</strong> di bilah bawah Safari.
                </li>
                <li>
                  Gulir ke bawah dan pilih <strong>Tambahkan ke Layar Utama (Add to Home Screen)</strong>.
                </li>
                <li>
                  Ketuk <strong>Tambah</strong> di pojok kanan atas.
                </li>
              </ol>
            </div>
          ) : (
            <div className="space-y-2 rounded-xl bg-stone-100 p-4 text-xs text-stone-700">
              <p className="font-semibold">Panduan Install di Browser Chrome / Android:</p>
              <ol className="list-inside list-decimal space-y-1.5">
                <li>Ketuk ikon menu tiga titik di pojok kanan atas browser.</li>
                <li>
                  Pilih <strong>Install aplikasi</strong> atau <strong>Tambahkan ke Layar Utama</strong>.
                </li>
              </ol>
            </div>
          )}
        </div>

        <div className="mt-5 flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="min-h-[44px] rounded-xl border border-stone-200 px-4 py-2 text-xs font-medium text-stone-700 hover:bg-stone-50"
          >
            Tutup
          </button>
          {isInstallable && (
            <button
              type="button"
              onClick={async () => {
                await onInstall();
                onClose();
              }}
              className="flex min-h-[44px] items-center gap-2 rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-700"
            >
              <Download className="h-4 w-4" />
              <span>Install Sekarang</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
