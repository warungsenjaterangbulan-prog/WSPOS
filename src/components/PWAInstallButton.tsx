import React, { useState } from 'react';
import { Download } from 'lucide-react';
import { usePWAInstall } from '../hooks/usePWAInstall';
import { PWAInstallModal } from './PWAInstallModal';

export const PWAInstallButton: React.FC = () => {
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [modalOpen, setModalOpen] = useState(false);

  if (isInstalled) return null;

  return (
    <>
      <button
        type="button"
        onClick={async () => {
          if (isInstallable) {
            const accepted = await install();
            if (!accepted) setModalOpen(true);
          } else {
            setModalOpen(true);
          }
        }}
        className="flex min-h-[40px] items-center gap-1.5 rounded-lg border border-amber-300 bg-amber-50 px-3 py-1.5 text-xs font-medium text-amber-900 transition-colors hover:bg-amber-100 whitespace-nowrap shrink-0"
        title="Install Aplikasi Kasir Terang Bulan"
      >
        <Download className="h-3.5 w-3.5 shrink-0" />
        <span className="hidden sm:inline">Install App</span>
      </button>

      <PWAInstallModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        isIOS={isIOS}
        isInstallable={isInstallable}
        onInstall={install}
      />
    </>
  );
};
