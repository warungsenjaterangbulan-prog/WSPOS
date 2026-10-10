import React from 'react';
import { LogOut, Repeat, Store } from 'lucide-react';
import { OwnerTab, StoreSettings, UserProfile } from '../types';

interface RoleHeaderProps {
  currentUser: UserProfile;
  settings: StoreSettings;
  activeTab?: OwnerTab;
  onSelectTab?: (tab: OwnerTab) => void;
  onOpenRoleSwitch: () => void;
  onLogout: () => void;
}

const OWNER_NAV_ITEMS: { id: OwnerTab; label: string }[] = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'kasir', label: 'Kasir' },
  { id: 'inventory', label: 'Menu' },
  { id: 'history', label: 'Riwayat' },
  { id: 'sheets', label: 'Sheets' },
  { id: 'settings', label: 'Pengaturan' },
];

export const RoleHeader: React.FC<RoleHeaderProps> = ({
  currentUser,
  activeTab,
  onSelectTab,
  onOpenRoleSwitch,
  onLogout,
}) => {
  return (
    <header className="sticky top-0 z-30 bg-zinc-950/90 backdrop-blur-md">
      <div className="mx-auto max-w-6xl px-3 py-2.5 sm:px-6">
        <div className="flex items-center justify-between gap-2">
          <a
            href="#top"
            onClick={(e) => {
              e.preventDefault();
              if (onSelectTab && currentUser.role === 'owner') {
                onSelectTab('dashboard');
              }
            }}
            className="flex items-center gap-2.5 shrink-0"
          >
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

          {/* Desktop / Landscape Inline Nav */}
          {currentUser.role === 'owner' && onSelectTab && (
            <nav className="hidden md:flex items-center gap-1 rounded-2xl bg-zinc-900 p-1 border border-zinc-800">
              {OWNER_NAV_ITEMS.map((item) => {
                const isActive = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => onSelectTab(item.id)}
                    className={`rounded-xl px-3.5 py-1.5 text-xs font-bold transition-all whitespace-nowrap ${
                      isActive
                        ? 'bg-yellow-400 text-zinc-950 shadow-xs'
                        : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'
                    }`}
                  >
                    {item.label}
                  </button>
                );
              })}
            </nav>
          )}

          <div className="flex items-center gap-1.5 shrink-0">
            <button
              type="button"
              onClick={onOpenRoleSwitch}
              className="flex min-h-[36px] items-center gap-1.5 rounded-xl border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-xs font-semibold text-zinc-200 hover:border-yellow-400/50 hover:text-yellow-400 whitespace-nowrap"
            >
              <Repeat className="h-3.5 w-3.5 text-yellow-400" />
              <span>{currentUser.role === 'owner' ? 'Kasir' : 'Owner'}</span>
            </button>

            <button
              type="button"
              onClick={onLogout}
              className="flex min-h-[36px] items-center gap-1.5 rounded-xl bg-yellow-400 px-3 py-1.5 text-xs font-bold text-zinc-950 hover:bg-yellow-300 whitespace-nowrap"
              title="Keluar"
            >
              <LogOut className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Keluar</span>
            </button>
          </div>
        </div>

        {/* Portrait Mobile Single Compact Nav Row */}
        {currentUser.role === 'owner' && onSelectTab && (
          <nav className="mt-2 flex md:hidden items-center gap-1 overflow-x-auto rounded-2xl bg-zinc-900 p-1 border border-zinc-800">
            {OWNER_NAV_ITEMS.map((item) => {
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => onSelectTab(item.id)}
                  className={`flex-1 rounded-xl px-2.5 py-1.5 text-xs font-bold transition-all whitespace-nowrap ${
                    isActive
                      ? 'bg-yellow-400 text-zinc-950 shadow-xs'
                      : 'text-zinc-400'
                  }`}
                >
                  {item.label}
                </button>
              );
            })}
          </nav>
        )}
      </div>
    </header>
  );
};
