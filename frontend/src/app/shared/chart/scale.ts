/** Linearna skala: domen (podaci) → opseg (pikseli). */
export function linear(domain: [number, number], range: [number, number]): (value: number) => number {
  const [d0, d1] = domain;
  const [r0, r1] = range;
  if (d0 === d1) return () => (r0 + r1) / 2;
  return (value) => r0 + ((value - d0) / (d1 - d0)) * (r1 - r0);
}

/**
 * "Lepe" podele ose (1, 2, 2.5, 5 × 10^n) koje obuhvataju [min, max].
 * Vraća i proširen domen, da prva i poslednja podela budu na ivicama grafika.
 */
export function niceTicks(min: number, max: number, count = 4): { ticks: number[]; domain: [number, number] } {
  if (!Number.isFinite(min) || !Number.isFinite(max)) {
    return { ticks: [0, 1], domain: [0, 1] };
  }
  if (min === max) {
    // jedna vrednost: malo prostora iznad i ispod
    const pad = Math.abs(min) * 0.1 || 1;
    min -= pad;
    max += pad;
  }

  const rawStep = (max - min) / count;
  const magnitude = 10 ** Math.floor(Math.log10(rawStep));
  const step = [1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((s) => s >= rawStep)!;

  const start = Math.floor(min / step) * step;
  const end = Math.ceil(max / step) * step;
  const ticks: number[] = [];
  for (let value = start; value <= end + step / 2; value += step) {
    ticks.push(Number(value.toFixed(10)));
  }
  return { ticks, domain: [start, end] };
}

/** Indeks tačke najbliže po x osi (za crosshair koji "hvata" najbliži datum). */
export function nearestIndex(xs: readonly number[], target: number): number {
  let best = 0;
  for (let i = 1; i < xs.length; i++) {
    if (Math.abs(xs[i] - target) < Math.abs(xs[best] - target)) best = i;
  }
  return best;
}
