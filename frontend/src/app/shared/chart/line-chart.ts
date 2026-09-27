import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';

import { linear, nearestIndex, niceTicks } from './scale';

export interface ChartPoint {
  /** vreme u milisekundama */
  x: number;
  y: number;
}

export interface ReferenceLine {
  value: number;
  label: string;
}

// desno ima mesta za vrednost na kraju linije
const MARGIN = { top: 20, right: 72, bottom: 30, left: 48 };
const SHORT_DATE = new Intl.DateTimeFormat('sr-Latn', { day: 'numeric', month: 'short' });

/** Linijski grafik kroz vreme: jedna serija, opciona referentna linija (npr. cilj). */
@Component({
  selector: 'app-line-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'relative block' },
  templateUrl: './line-chart.html',
})
export class LineChart {
  readonly points = input.required<readonly ChartPoint[]>();
  readonly reference = input<ReferenceLine | null>(null);
  readonly formatValue = input<(value: number) => string>((v) => String(v));
  readonly label = input.required<string>();
  readonly valueLabel = input('Vrednost');
  readonly height = input(240);

  protected readonly width = signal(0);
  protected readonly active = signal<number | null>(null);
  protected readonly margin = MARGIN;

  constructor() {
    const host = inject(ElementRef<HTMLElement>).nativeElement as HTMLElement;
    const destroyRef = inject(DestroyRef);
    // širina se meri, a ne skalira viewBox-om, da tekst i linije ostanu oštri
    afterNextRender(() => {
      const observer = new ResizeObserver(([entry]) => this.width.set(Math.floor(entry.contentRect.width)));
      observer.observe(host);
      destroyRef.onDestroy(() => observer.disconnect());
    });
  }

  private readonly sorted = computed(() => [...this.points()].sort((a, b) => a.x - b.x));

  private readonly yAxis = computed(() => {
    const values = this.sorted().map((p) => p.y);
    const ref = this.reference();
    if (ref) values.push(ref.value);
    return niceTicks(Math.min(...values), Math.max(...values));
  });

  private readonly xDomain = computed<[number, number]>(() => {
    const points = this.sorted();
    const first = points[0]?.x ?? 0;
    const last = points.at(-1)?.x ?? 0;
    const day = 86_400_000;
    return first === last ? [first - day, last + day] : [first, last];
  });

  protected readonly x = computed(() =>
    linear(this.xDomain(), [MARGIN.left, Math.max(MARGIN.left, this.width() - MARGIN.right)]),
  );
  protected readonly y = computed(() => linear(this.yAxis().domain, [this.height() - MARGIN.bottom, MARGIN.top]));

  protected readonly yTicks = computed(() => this.yAxis().ticks.map((value) => ({ value, y: this.y()(value) })));

  // do 4 datuma na x osi: prvi, poslednji i ravnomerno između
  protected readonly xTicks = computed(() => {
    const points = this.sorted();
    const count = Math.min(points.length, this.width() < 420 ? 3 : 4);
    if (count === 0) return [];
    const indexes =
      count === 1 ? [0] : Array.from({ length: count }, (_, i) => Math.round((i * (points.length - 1)) / (count - 1)));
    const unique = [...new Set(indexes)].filter(
      (i, n, all) => n === 0 || SHORT_DATE.format(points[i].x) !== SHORT_DATE.format(points[all[n - 1]].x),
    );
    return unique.map((i, n, all) => ({
      x: this.x()(points[i].x),
      label: SHORT_DATE.format(points[i].x),
      anchor: all.length === 1 ? 'middle' : n === 0 ? 'start' : n === all.length - 1 ? 'end' : 'middle',
    }));
  });

  protected readonly positioned = computed(() =>
    this.sorted().map((p) => ({ ...p, px: this.x()(p.x), py: this.y()(p.y) })),
  );

  protected readonly path = computed(() =>
    this.positioned()
      .map((p, i) => `${i === 0 ? 'M' : 'L'}${p.px.toFixed(1)},${p.py.toFixed(1)}`)
      .join(' '),
  );

  protected readonly area = computed(() => {
    const points = this.positioned();
    if (points.length < 2) return '';
    const base = (this.height() - MARGIN.bottom).toFixed(1);
    return `${this.path()} L${points.at(-1)!.px.toFixed(1)},${base} L${points[0].px.toFixed(1)},${base} Z`;
  });

  protected readonly referenceY = computed(() => {
    const ref = this.reference();
    return ref ? this.y()(ref.value) : null;
  });

  protected readonly last = computed(() => this.positioned().at(-1) ?? null);

  protected readonly tooltip = computed(() => {
    const index = this.active();
    if (index === null) return null;
    const point = this.positioned()[index];
    if (!point) return null;
    const flip = point.px > this.width() - 150;
    return {
      point,
      value: this.formatValue()(point.y),
      date: new Intl.DateTimeFormat('sr-Latn', { day: 'numeric', month: 'long', year: 'numeric' }).format(point.x),
      left: flip ? point.px - 12 : point.px + 12,
      flip,
    };
  });

  protected readonly tableRows = computed(() =>
    [...this.sorted()].reverse().map((p) => ({
      date: new Intl.DateTimeFormat('sr-Latn', { day: 'numeric', month: 'long', year: 'numeric' }).format(p.x),
      value: this.formatValue()(p.y),
    })),
  );

  protected onPointer(event: PointerEvent): void {
    const svg = event.currentTarget as SVGElement;
    const offsetX = event.clientX - svg.getBoundingClientRect().left;
    this.active.set(nearestIndex(this.positioned().map((p) => p.px), offsetX));
  }

  protected onKey(event: KeyboardEvent): void {
    const count = this.positioned().length;
    if (count === 0) return;
    const current = this.active() ?? count - 1;
    if (event.key === 'ArrowLeft') this.active.set(Math.max(0, current - 1));
    else if (event.key === 'ArrowRight') this.active.set(Math.min(count - 1, current + 1));
    else if (event.key === 'Home') this.active.set(0);
    else if (event.key === 'End') this.active.set(count - 1);
    else return;
    event.preventDefault();
  }
}
