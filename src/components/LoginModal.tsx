import React, { useState } from 'react';
import { KeyRound, ShieldCheck, UserCheck, X } from 'lucide-react';
import { StoreSettings, UserProfile, UserRole } from '../types';

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetRole: UserRole;
  settings: StoreSettings;
  onSuccessSwitch: (user: UserProfile) => void;
}

export const LoginModal: React.FC<LoginModalProps> = ({
  isOpen,
  onClose,
  targetRole,
  settings,
  onSuccessSwitch,
}) => {
  const [pin, setPin] = useState('');
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const expectedPin = targetRole === 'owner' ? settings.ownerPin : settings.kasirPin;

    if (pin.trim() === expectedPin) {
      const nextUser: UserProfile =
        targetRole === 'owner'
          ? { id: 'user-owner', name: 'Owner Warung Senja', role: 'owner', pin: settings.ownerPin }
          : { id: 'user-kasir-1', name: 'Kasir 1', role: 'kasir', pin: settings.kasirPin };
      setPin('');
      setError('');
      onSuccessSwitch(nextUser);
      onClose();
    } else {
      setError('PIN tidak sesuai. Silakan coba kembali.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-3 sm:p-4 backdrop-blur-xs">
      <div className="max-h-[90dvh] w-full max-w-sm overflow-y-auto rounded-3xl border border-yellow-400/30 bg-zinc-900 p-5 sm:p-6 text-zinc-100 shadow-2xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {targetRole === 'owner' ? (
              <ShieldCheck className="h-5 w-5 text-yellow-400" />
            ) : (
              <UserCheck className="h-5 w-5 text-yellow-400" />
            )}
            <h3 className="text-base font-bold text-white">
              Beralih ke Mode {targetRole === 'owner' ? 'Owner' : 'Kasir'}
            </h3>
          </div>
          <button
            type="button"
            onClick={() => {
              setPin('');
              setError('');
              onClose();
            }}
            className="flex h-9 w-9 items-center justify-center rounded-xl text-zinc-400 hover:bg-zinc-800 hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <p className="mt-1 text-xs text-zinc-400">
          Masukkan kode keamanan PIN {targetRole === 'owner' ? 'Owner' : 'Kasir'} untuk melanjutkan.
        </p>

        <form onSubmit={handleSubmit} className="mt-4 space-y-3.5">
          <div>
            <label className="block text-xs font-semibold text-zinc-300 mb-1">PIN Akses</label>
            <div className="relative">
              <KeyRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-yellow-400" />
              <input
                type="password"
                inputMode="numeric"
                autoFocus
                value={pin}
                onChange={(e) => {
                  setPin(e.target.value);
                  setError('');
                }}
                placeholder="Masukkan PIN"
                className="w-full rounded-xl border border-zinc-800 bg-zinc-950 py-2.5 pr-3 pl-9 text-sm font-mono tracking-widest text-yellow-400 placeholder:text-zinc-600 focus:border-yellow-400 focus:outline-none"
              />
            </div>
            {error && <p className="mt-1.5 text-xs font-medium text-red-400">{error}</p>}
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="min-h-[40px] rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2 text-xs font-medium text-zinc-300 hover:bg-zinc-800"
            >
              Batal
            </button>
            <button
              type="submit"
              className="min-h-[40px] rounded-xl bg-yellow-400 px-5 py-2 text-xs font-bold text-zinc-950 hover:bg-yellow-300"
            >
              Masuk
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
