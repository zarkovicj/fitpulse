import { Injectable, computed, effect, inject, signal } from '@angular/core';

import { Clock } from '../time/clock';

interface RunningRest {
  setId: number;
  startedAt: number;
  endsAt: number;
  duration: number;
  label: string;
}

const STATE_KEY = 'fitpulse.rest';
const PREFS_KEY = 'fitpulse.rest-prefs';
export const DEFAULT_REST = 90;

/**
 * Odbrojavanje odmora između serija. Čuva se kraj (vreme), a ne preostale sekunde,
 * pa je tačno i posle osvežavanja stranice ili kad je tab bio u pozadini.
 */
@Injectable({ providedIn: 'root' })
export class RestTimer {
  private readonly clock = inject(Clock);

  private readonly running = signal<RunningRest | null>(read<RunningRest>(STATE_KEY));
  private readonly prefs = signal<Record<number, number>>(read<Record<number, number>>(PREFS_KEY) ?? {});
  private notifiedFor: number | null = null;

  readonly label = computed(() => this.running()?.label ?? null);
  /** Serija posle koje teče odmor; odbrojavanje se prikazuje ispod nje. */
  readonly setId = computed(() => this.running()?.setId ?? null);
  readonly active = computed(() => this.running() !== null);

  /** Preostale sekunde; 0 kad je odmor gotov, null kad tajmer ne radi. */
  readonly remaining = computed(() => {
    const rest = this.running();
    if (!rest) return null;
    // sat otkucava jednom u sekundi, pa bez gornje granice odmah posle starta piše 1:31 umesto 1:30
    return Math.min(rest.duration, Math.max(0, Math.ceil((rest.endsAt - this.clock.now()) / 1000)));
  });
  readonly done = computed(() => this.remaining() === 0);
  /** Od 1 do 0, za traku koja se prazni. */
  readonly fraction = computed(() => {
    const rest = this.running();
    const remaining = this.remaining();
    return rest && remaining !== null ? remaining / rest.duration : 0;
  });

  private timeout: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // rezerva za slučaj da setTimeout zakasni (browser usporava tajmere u pozadini)
    effect(() => {
      if (this.done()) this.finish();
    });
    this.schedule();
  }

  start(setId: number, seconds: number, label: string): void {
    const now = Date.now();
    this.set({ setId, startedAt: now, endsAt: now + seconds * 1000, duration: seconds, label });
  }

  /** Kad se promeni odmor serije čiji odmor upravo teče, odbrojavanje se računa od istog početka. */
  retime(setId: number, seconds: number): void {
    const rest = this.running();
    if (!rest || rest.setId !== setId) return;
    this.set({ ...rest, duration: seconds, endsAt: rest.startedAt + seconds * 1000 });
  }

  stop(): void {
    this.set(null);
  }

  /** Odmor koji korisnik želi za vežbu; pamti se po vežbi, i za sledeće treninge. */
  restFor(exerciseId: number, fallback: number | null = null): number {
    return this.prefs()[exerciseId] ?? fallback ?? DEFAULT_REST;
  }

  setRestFor(exerciseId: number, seconds: number): void {
    this.prefs.update((prefs) => ({ ...prefs, [exerciseId]: seconds }));
    write(PREFS_KEY, this.prefs());
  }

  /** Traži dozvolu za sistemska obaveštenja (mora iz klika korisnika). */
  async enableNotifications(): Promise<NotificationPermission | 'unsupported'> {
    if (!('Notification' in window)) return 'unsupported';
    return Notification.permission === 'default' ? Notification.requestPermission() : Notification.permission;
  }

  notificationPermission(): NotificationPermission | 'unsupported' {
    return 'Notification' in window ? Notification.permission : 'unsupported';
  }

  private set(rest: RunningRest | null): void {
    this.running.set(rest);
    write(STATE_KEY, rest);
    this.schedule();
  }

  private schedule(): void {
    clearTimeout(this.timeout);
    const rest = this.running();
    if (rest && rest.endsAt > Date.now()) {
      this.timeout = setTimeout(() => this.finish(), rest.endsAt - Date.now());
    }
  }

  // jedno obaveštenje po odmoru, ma odakle da stigne okidač
  private finish(): void {
    const rest = this.running();
    if (!rest || rest.endsAt > Date.now() || this.notifiedFor === rest.endsAt) return;
    this.notifiedFor = rest.endsAt;
    // ako je odmor istekao dok je stranica bila zatvorena, ne zvoni naknadno
    if (Date.now() - rest.endsAt < 5000) notify(rest.label);
  }
}

function notify(label: string): void {
  beep();
  navigator.vibrate?.([250, 120, 250, 120, 400]);
  // sistemsko obaveštenje samo kad korisnik ne gleda aplikaciju
  if (document.hidden && 'Notification' in window && Notification.permission === 'granted') {
    new Notification('Odmor je gotov', { body: `Sledeća serija: ${label}`, tag: 'fitpulse-rest' });
  }
}

// tri kratka tona preko Web Audio, bez audio fajla
function beep(): void {
  try {
    const context = new AudioContext();
    [0, 0.25, 0.5].forEach((offset) => {
      const oscillator = context.createOscillator();
      const gain = context.createGain();
      oscillator.frequency.value = 880;
      gain.gain.setValueAtTime(0.25, context.currentTime + offset);
      gain.gain.exponentialRampToValueAtTime(0.001, context.currentTime + offset + 0.18);
      oscillator.connect(gain).connect(context.destination);
      oscillator.start(context.currentTime + offset);
      oscillator.stop(context.currentTime + offset + 0.2);
    });
    setTimeout(() => context.close(), 1000);
  } catch {
    // browser bez zvuka: ostaje vibracija i prikaz na ekranu
  }
}

function read<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}

function write(key: string, value: unknown): void {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, JSON.stringify(value));
  } catch {}
}
