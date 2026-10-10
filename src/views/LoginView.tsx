import React, { useState } from 'react';
import { KeyRound, ShieldCheck, Store, UserCheck } from 'lucide-react';
import { StoreSettings, UserProfile, UserRole } from '../types';

interface LoginViewProps {
  settings: StoreSettings;
  onLoginSuccess: (user: UserProfile) => void;
}

export const LoginView: React.FC<LoginViewProps> = ({ settings, onLoginSuccess }) => {
  const [selectedRole, setSelectedRole] = useState<UserRole>('kasir');
  const [pin, setPin] = useState('');
  const [error, setError] = useState('');

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    const expectedPin = selectedRole === 'owner' ? settings.ownerPin : settings.kasirPin;

    if (pin.trim() === expectedPin) {
      const profile: UserProfile =
        selectedRole === 'owner'
          ? {
              id: 'user-owner',
              name: 'Owner Warung Senja',
              role: 'owner',
              pin: settings.ownerPin,
            }
          : {
              id: 'user-kasir-1',
              name: 'Kasir 1',
              role: 'kasir',
              pin: settings.kasirPin,
            };
      setPin('');
      setError('');
      onLoginSuccess(profile);
    } else {
      setError('PIN yang dimasukkan salah. Silakan periksa kembali.');
    }
  };

  const handleDigitClick = (digit: string) => {
    if (pin.length < 8) {
      setPin((prev) => prev + digit);
      setError('');
    }
  };

  const handleBackspace = () => {
    setPin((prev) => prev.slice(0, -1));
    setError('');
  };

  return (
    <div className="min-h-dvh bg-zinc-950 text-zinc-100 flex flex-col justify-between">
      <header className="bg-zinc-950/90 px-4 py-3 sm:px-6 sm:py-4 backdrop-blur-md landscape-compact-py">
        <div className="mx-auto flex max-w-5xl items-center justify-between">
          <a href="#top" className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-yellow-400 text-zinc-950 shadow-xs">
              <Store className="h-5 w-5" />
            </div>
            <div className="flex flex-col">
              <span className="font-display text-sm sm:text-base font-bold leading-tight tracking-tight text-yellow-400">
                Warung Senja
              </span>
              <span className="text-[11px] font-medium leading-tight text-zinc-400">
                Terang Bulan
              </span>
            </div>
          </a>
          <span className="text-xs font-medium text-zinc-400 hidden sm:inline">
            {settings.storeAddress}
          </span>
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-4xl flex-1 items-center justify-center px-3 py-4 sm:px-4 sm:py-8 landscape-compact-py">
        <div className="grid w-full overflow-hidden rounded-3xl border border-yellow-400/25 bg-zinc-900 shadow-2xl sm:grid-cols-12 landscape:grid-cols-12">
          {/* Left Column: Vibrant Modern Yellow Store Identity */}
          <div className="flex flex-col justify-between bg-gradient-to-br from-yellow-400 via-yellow-400 to-amber-500 p-6 sm:p-8 text-zinc-950 sm:col-span-5 landscape:col-span-5 landscape-compact-card">
            <div>
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-zinc-950 text-yellow-400 shadow-md">
                <Store className="h-6 w-6" />
              </div>
              <h1 className="mt-5 sm:mt-6 font-display text-2xl font-bold tracking-tight text-zinc-950 text-balance">
                {settings.storeName || 'Warung Senja Terang Bulan'}
              </h1>
              <p className="mt-2 text-xs sm:text-sm font-medium text-zinc-900/80 leading-relaxed">
                Sistem Kasir POS Modern & Manajemen Menu Terang Bulan / Martabak Manis.
              </p>
            </div>

            <div className="mt-6 sm:mt-8 space-y-1 border-t border-zinc-950/15 pt-4 text-xs text-zinc-900">
              <div className="font-bold text-zinc-950">{settings.storeAddress}</div>
              <div className="font-medium text-zinc-900/80">
                <span>Telp/WA: {settings.storePhone}</span>
                <span className="mx-1.5" aria-hidden="true">·</span>
                <span>{settings.operationalHours || 'Buka Jam 15.00 - 21.00'}</span>
              </div>
            </div>
          </div>

          {/* Right Column: Sleek Dark Obsidian Login Form */}
          <div className="p-5 sm:p-8 sm:col-span-7 landscape:col-span-7 landscape-compact-card bg-zinc-900">
            <h2 className="font-display text-lg sm:text-xl font-bold text-white">
              Masuk Operator Kasir
            </h2>
            <p className="mt-1 text-xs text-zinc-400">
              Pilih peran operator dan masukkan PIN keamanan untuk membuka sesi.
            </p>

            {/* Role Selector Tabs */}
            <div className="mt-4 grid grid-cols-2 gap-2 rounded-2xl bg-zinc-950 p-1.5 border border-zinc-800">
              <button
                type="button"
                onClick={() => {
                  setSelectedRole('kasir');
                  setPin('');
                  setError('');
                }}
                className={`flex min-h-[42px] items-center justify-center gap-2 rounded-xl px-3 py-2 text-xs font-bold transition-all ${
                  selectedRole === 'kasir'
                    ? 'bg-yellow-400 text-zinc-950 shadow-sm'
                    : 'text-zinc-400 hover:text-zinc-100'
                }`}
              >
                <UserCheck className="h-4 w-4 shrink-0" />
                <span>Kasir Penjualan</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setSelectedRole('owner');
                  setPin('');
                  setError('');
                }}
                className={`flex min-h-[42px] items-center justify-center gap-2 rounded-xl px-3 py-2 text-xs font-bold transition-all ${
                  selectedRole === 'owner'
                    ? 'bg-yellow-400 text-zinc-950 shadow-sm'
                    : 'text-zinc-400 hover:text-zinc-100'
                }`}
              >
                <ShieldCheck className="h-4 w-4 shrink-0" />
                <span>Owner / Admin</span>
              </button>
            </div>

            <form onSubmit={handleLogin} className="mt-4 space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-zinc-300 mb-1.5">
                  PIN {selectedRole === 'owner' ? 'Owner' : 'Kasir'}
                </label>
                <div className="relative">
                  <KeyRound className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-yellow-400" />
                  <input
                    type="password"
                    inputMode="numeric"
                    value={pin}
                    onChange={(e) => {
                      setPin(e.target.value);
                      setError('');
                    }}
                    placeholder="Masukkan PIN"
                    className="w-full rounded-xl border border-zinc-800 bg-zinc-950 py-2.5 sm:py-3 pr-4 pl-10 text-base font-mono tracking-widest text-yellow-400 placeholder:text-zinc-600 focus:border-yellow-400 focus:outline-none"
                  />
                </div>
                {error && <p className="mt-1.5 text-xs font-medium text-red-400">{error}</p>}
              </div>

              {/* Quick Keypad */}
              <div className="grid grid-cols-3 gap-2 pt-0.5">
                {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((digit) => (
                  <button
                    key={digit}
                    type="button"
                    onClick={() => handleDigitClick(digit)}
                    className="min-h-[42px] rounded-xl border border-zinc-800 bg-zinc-950/80 py-2 text-sm font-mono font-bold text-zinc-100 transition-colors hover:border-yellow-400/50 hover:text-yellow-400 active:bg-yellow-400 active:text-zinc-950"
                  >
                    {digit}
                  </button>
                ))}
                <button
                  type="button"
                  onClick={() => {
                    setPin('');
                    setError('');
                  }}
                  className="min-h-[42px] rounded-xl border border-zinc-800 bg-zinc-950/40 py-2 text-xs font-semibold text-zinc-400 hover:border-zinc-700 hover:text-zinc-200"
                >
                  Reset
                </button>
                <button
                  type="button"
                  onClick={() => handleDigitClick('0')}
                  className="min-h-[42px] rounded-xl border border-zinc-800 bg-zinc-950/80 py-2 text-sm font-mono font-bold text-zinc-100 transition-colors hover:border-yellow-400/50 hover:text-yellow-400 active:bg-yellow-400 active:text-zinc-950"
                >
                  0
                </button>
                <button
                  type="button"
                  onClick={handleBackspace}
                  className="min-h-[42px] rounded-xl border border-zinc-800 bg-zinc-950/40 py-2 text-xs font-semibold text-zinc-400 hover:border-zinc-700 hover:text-zinc-200"
                >
                  Hapus
                </button>
              </div>

              <button
                type="submit"
                className="w-full min-h-[46px] rounded-xl bg-yellow-400 px-5 py-3 text-sm font-bold text-zinc-950 shadow-md transition-colors hover:bg-yellow-300"
              >
                Buka Kasir Terang Bulan
              </button>
            </form>
          </div>
        </div>
      </main>

      <footer className="bg-zinc-950 px-4 py-2.5 sm:px-6 sm:py-3 text-center text-xs text-zinc-500">
        <span className="font-semibold text-yellow-400">{settings.storeName}</span>
        <span className="mx-1.5" aria-hidden="true">·</span>
        <span>{settings.storeAddress}</span>
      </footer>
    </div>
  );
};
