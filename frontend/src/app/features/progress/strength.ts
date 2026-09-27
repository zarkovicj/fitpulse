import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api/api-error';
import { ExerciseProgressPoint, PersonalRecord } from '../../core/progress/progress.model';
import { ChartPoint, LineChart } from '../../shared/chart/line-chart';
import { dayToTime, formatDay, formatKg, formatNumber, localDate } from '../../shared/text/format';

interface ExerciseRecords {
  exerciseId: number;
  name: string;
  maxWeight: PersonalRecord | null;
  e1rm: PersonalRecord | null;
  maxReps: PersonalRecord | null;
  latest: string;
}

type Metric = 'best1rm' | 'maxWeight' | 'maxReps' | 'volume';

const METRICS: { value: Metric; label: string }[] = [
  { value: 'best1rm', label: 'Procenjeni 1RM' },
  { value: 'maxWeight', label: 'Najveća kilaža' },
  { value: 'maxReps', label: 'Ponavljanja' },
  { value: 'volume', label: 'Volumen' },
];

@Component({
  selector: 'app-strength',
  imports: [RouterLink, LineChart],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './strength.html',
})
export class Strength {
  private readonly router = inject(Router);

  /** ?exercise=ID iz adrese, da se izbor sačuva pri osvežavanju ili deljenju linka. */
  readonly exercise = input<string>();

  protected readonly records = httpResource<PersonalRecord[]>(() => '/api/progress/records');

  // rekordi grupisani po vežbi, najskorije oborene prve
  protected readonly byExercise = computed<ExerciseRecords[]>(() => {
    const all = this.records.hasValue() ? this.records.value() : [];
    const groups = new Map<number, ExerciseRecords>();
    for (const record of all) {
      const group = groups.get(record.exerciseId) ?? {
        exerciseId: record.exerciseId,
        name: record.exerciseName,
        maxWeight: null,
        e1rm: null,
        maxReps: null,
        latest: record.achievedAt,
      };
      if (record.type === 'MAX_WEIGHT') group.maxWeight = record;
      if (record.type === 'ESTIMATED_1RM') group.e1rm = record;
      if (record.type === 'MAX_REPS') group.maxReps = record;
      if (record.achievedAt > group.latest) group.latest = record.achievedAt;
      groups.set(record.exerciseId, group);
    }
    return [...groups.values()].sort((a, b) => b.latest.localeCompare(a.latest));
  });

  protected readonly selectedId = linkedSignal(() => {
    const fromUrl = Number(this.exercise());
    const list = this.byExercise();
    return list.some((e) => e.exerciseId === fromUrl) ? fromUrl : (list[0]?.exerciseId ?? null);
  });
  protected readonly selected = computed(() => this.byExercise().find((e) => e.exerciseId === this.selectedId()) ?? null);

  protected readonly metrics = METRICS;
  protected readonly metric = signal<Metric>('best1rm');
  protected readonly metricLabel = computed(() => METRICS.find((m) => m.value === this.metric())!.label);

  protected readonly history = httpResource<ExerciseProgressPoint[]>(() => {
    const id = this.selectedId();
    return id ? `/api/progress/exercises/${id}` : undefined;
  });

  protected readonly chartPoints = computed<ChartPoint[]>(() => {
    const points = this.history.hasValue() ? (this.history.value() ?? []) : [];
    const metric = this.metric();
    // više treninga istog dana = jedna tačka: najbolja vrednost dana, a volumen se sabira
    const byDay = new Map<string, number>();
    for (const p of points) {
      const value = p[metric];
      if (value === null || value === 0) continue;
      const previous = byDay.get(p.date);
      byDay.set(
        p.date,
        previous === undefined ? value : metric === 'volume' ? previous + value : Math.max(previous, value),
      );
    }
    return [...byDay].map(([date, y]) => ({ x: dayToTime(date), y }));
  });

  protected readonly formatValue = computed(() =>
    this.metric() === 'maxReps' ? (v: number) => formatNumber(v) : (v: number) => `${formatNumber(v)} kg`,
  );

  protected readonly errorText = computed(() =>
    this.records.error() ? errorMessage(this.records.error(), 'Rekordi nisu učitani.') : null,
  );

  protected readonly formatKg = formatKg;
  protected readonly formatDay = formatDay;

  protected select(exerciseId: number): void {
    this.selectedId.set(exerciseId);
    this.router.navigate([], { queryParams: { exercise: exerciseId }, replaceUrl: true });
  }

  protected dateOf(record: PersonalRecord | null): string {
    return record ? formatDay(localDate(record.achievedAt)) : '';
  }
}
