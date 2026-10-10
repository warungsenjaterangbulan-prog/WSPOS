import { TransactionType } from '../types';

export function formatRupiah(amount: number): string {
  const safeAmount = Number.isFinite(amount) ? Math.round(amount) : 0;
  return 'Rp' + safeAmount.toLocaleString('id-ID');
}

export function formatDateTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return isoString;
    return date.toLocaleString('id-ID', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return isoString;
  }
}

export function formatShortDate(isoString: string): string {
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return isoString;
    return date.toLocaleDateString('id-ID', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  } catch {
    return isoString;
  }
}

export function isToday(isoString: string): boolean {
  try {
    const d = new Date(isoString);
    const now = new Date();
    return (
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate()
    );
  } catch {
    return false;
  }
}

export function generateTransactionCode(type: TransactionType = 'OUT', existingCount = 0): string {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  const seq = String(existingCount + 1).padStart(3, '0');
  const suffix = Math.floor(100 + Math.random() * 900);
  const prefix = type === 'OUT' ? 'TB' : 'TBM';
  return `${prefix}-${yyyy}${mm}${dd}-${seq}${suffix}`;
}

export function generateMenuBarcode(index?: number): string {
  const num = typeof index === 'number' && index > 0 ? index : Math.floor(1 + Math.random() * 99);
  return String(num).padStart(2, '0');
}
