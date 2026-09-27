const KG = new Intl.NumberFormat('sr-Latn', { maximumFractionDigits: 2 });
const DAY = new Intl.DateTimeFormat('sr-Latn', { weekday: 'long', day: 'numeric', month: 'long' });
const DAY_WITH_YEAR = new Intl.DateTimeFormat('sr-Latn', { day: 'numeric', month: 'long', year: 'numeric' });
const TIME = new Intl.DateTimeFormat('sr-Latn', { hour: '2-digit', minute: '2-digit' });

/** 62.5 → "62,5 kg"; bez tega → null. */
export function formatKg(kg: number | null | undefined): string | null {
  return kg ? `${KG.format(kg)} kg` : null;
}

export function formatNumber(value: number): string {
  return KG.format(value);
}

/** Proteklo vreme kao na štoperici: "7:05", "42:10", "1:05:12". */
export function formatDuration(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const pad = (n: number) => n.toString().padStart(2, '0');
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

/** Odmor: "45 s", "1:30", "3:00". */
export function formatRest(seconds: number): string {
  return seconds < 60 ? `${seconds} s` : formatDuration(seconds * 1000);
}

/** Trajanje u minutima za istoriju: "48 min", "1 h 12 min". */
export function formatMinutes(ms: number): string {
  const minutes = Math.max(1, Math.round(ms / 60000));
  return minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
}

/** "2026-09-23" → "sreda, 23. septembar"; stariji od godinu dana dobija i godinu. */
export function formatDay(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  const yearAgo = new Date();
  yearAgo.setFullYear(yearAgo.getFullYear() - 1);
  return date < yearAgo ? DAY_WITH_YEAR.format(date) : DAY.format(date);
}

/** "2026-09-23T…" → "23. septembar 2026." */
export function formatDate(value: string): string {
  return DAY_WITH_YEAR.format(new Date(value));
}

/** Lokalni datum kao "2026-09-23" (za Instant sa servera ili današnji dan). */
export function localDate(value: string | Date = new Date()): string {
  const date = typeof value === 'string' ? new Date(value) : value;
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** "2026-09-23" → vreme u ms za lokalnu ponoć tog dana. */
export function dayToTime(isoDate: string): number {
  const [y, m, d] = isoDate.split('-').map(Number);
  return new Date(y, m - 1, d).getTime();
}

export function formatTime(isoInstant: string): string {
  return TIME.format(new Date(isoInstant));
}
