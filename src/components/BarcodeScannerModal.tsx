import React, { useEffect, useRef, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import { Camera, Flashlight, Keyboard, X } from 'lucide-react';
import { soundService } from '../services/sound';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onScanSuccess: (barcode: string) => void;
  allowManualInput?: boolean;
  title?: string;
}

export const BarcodeScannerModal: React.FC<BarcodeScannerModalProps> = ({
  isOpen,
  onClose,
  onScanSuccess,
  allowManualInput = true,
  title = 'Scan Barcode Menu Terang Bulan',
}) => {
  const [errorMsg, setErrorMsg] = useState<string>('');
  const [manualCode, setManualCode] = useState<string>('');
  const [torchOn, setTorchOn] = useState<boolean>(false);
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const lastScanRef = useRef<{ code: string; time: number }>({ code: '', time: 0 });

  useEffect(() => {
    if (!isOpen) return;

    let mounted = true;
    const regionId = 'kasir-terang-bulan-qr-reader';

    const startScanner = async () => {
      setErrorMsg('');
      try {
        const html5QrCode = new Html5Qrcode(regionId);
        scannerRef.current = html5QrCode;

        await html5QrCode.start(
          { facingMode: 'environment' },
          {
            fps: 10,
            qrbox: { width: 250, height: 150 },
          },
          (decodedText) => {
            if (!mounted) return;
            const now = Date.now();
            if (
              lastScanRef.current.code === decodedText &&
              now - lastScanRef.current.time < 1800
            ) {
              return;
            }
            lastScanRef.current = { code: decodedText, time: now };
            soundService.playScanSuccess();
            onScanSuccess(decodedText.trim());
            onClose();
          },
          () => {
            // Frame scan error ignored
          }
        );

        if (mounted) {
          setIsScanning(true);
        }
      } catch (err) {
        if (!mounted) return;
        soundService.playScanError();
        setErrorMsg(
          err instanceof Error
            ? `Kamera tidak dapat diakses: ${err.message}`
            : 'Kamera tidak tersedia atau izin ditolak. Gunakan input manual di bawah.'
        );
      }
    };

    const timer = setTimeout(startScanner, 150);

    return () => {
      mounted = false;
      clearTimeout(timer);
      setIsScanning(false);
      setTorchOn(false);
      if (scannerRef.current) {
        const instance = scannerRef.current;
        scannerRef.current = null;
        instance
          .stop()
          .catch(() => {})
          .finally(() => {
            try {
              instance.clear();
            } catch {
              // ignore
            }
          });
      }
    };
  }, [isOpen, onClose, onScanSuccess]);

  const toggleTorch = async () => {
    if (!scannerRef.current || !isScanning) return;
    try {
      const nextState = !torchOn;
      await scannerRef.current.applyVideoConstraints({
        advanced: [{ torch: nextState } as unknown as MediaTrackConstraintSet],
      });
      setTorchOn(nextState);
    } catch {
      setErrorMsg('Lampu senter (torch) tidak didukung pada perangkat ini.');
    }
  };

  const handleManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = manualCode.trim();
    if (!cleaned) return;
    soundService.playScanSuccess();
    onScanSuccess(cleaned);
    setManualCode('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-950/75 p-4 backdrop-blur-xs">
      <div className="w-full max-w-md overflow-hidden rounded-2xl border border-amber-200 bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-stone-200 bg-amber-50/70 px-5 py-3.5">
          <div className="flex items-center gap-2">
            <Camera className="h-5 w-5 text-amber-700" />
            <h3 className="text-sm font-semibold text-stone-900">{title}</h3>
          </div>
          <div className="flex items-center gap-1">
            {isScanning && (
              <button
                type="button"
                onClick={toggleTorch}
                className={`flex h-10 w-10 items-center justify-center rounded-lg border text-xs transition-colors ${
                  torchOn
                    ? 'border-amber-500 bg-amber-500 text-white'
                    : 'border-stone-200 bg-white text-stone-700 hover:bg-stone-100'
                }`}
                title="Nyalakan / Matikan Senter"
              >
                <Flashlight className="h-4 w-4" />
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              className="flex h-10 w-10 items-center justify-center rounded-lg text-stone-500 hover:bg-stone-100"
              aria-label="Tutup Scanner"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="p-5">
          <div
            id="kasir-terang-bulan-qr-reader"
            className="min-h-[220px] w-full overflow-hidden rounded-xl border border-stone-200 bg-stone-900"
          />

          {errorMsg && (
            <div className="mt-3 rounded-xl border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900">
              {errorMsg}
            </div>
          )}

          {allowManualInput && (
            <form onSubmit={handleManualSubmit} className="mt-4 pt-3 border-t border-stone-200">
              <label className="block text-xs font-medium text-stone-700 mb-1.5">
                Input Barcode Manual / SKU Menu
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Keyboard className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
                  <input
                    type="text"
                    value={manualCode}
                    onChange={(e) => setManualCode(e.target.value)}
                    placeholder="Contoh: 899702600002"
                    className="w-full rounded-xl border border-stone-300 py-2.5 pr-3 pl-9 text-sm font-mono text-stone-900 focus:border-amber-600 focus:outline-none"
                  />
                </div>
                <button
                  type="submit"
                  className="min-h-[44px] rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-700 whitespace-nowrap"
                >
                  Pilih Menu
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
