import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ExerciseInfo } from '../../core/exercise/exercise-info';
import { PersonalRecord, ProgressSummary, RECORD_LABELS } from '../../core/progress/progress.model';
import { Clock } from '../../core/time/clock';
import { CurrentUser } from '../../core/user/current-user';
import { ActiveWorkout } from '../../core/workout/active-workout';
import { Page, WorkoutSummary, workoutTitle } from '../../core/workout/workout.model';
import { Icon } from '../../shared/icon/icon';
import { formatDay, formatDuration, formatKg, formatMinutes, formatNumber, localDate } from '../../shared/text/format';
import { plural } from '../../shared/text/plural';
import { StartWorkoutDialog } from '../workouts/start-workout-dialog';

const WEEKS = 8;
// više od ovoga ne staje u kolonu; broj iznad kolone je uvek tačan
const MAX_PLATES = 7;
const SHORT_DATE = new Intl.DateTimeFormat('sr-Latn', { day: 'numeric', month: 'short' });

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, Icon, StartWorkoutDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly clock = inject(Clock);
  protected readonly info = inject(ExerciseInfo);
  protected readonly currentUser = inject(CurrentUser);
  protected readonly active = inject(ActiveWorkout);

  protected readonly summary = httpResource<ProgressSummary>(() => '/api/progress/summary');
  protected readonly history = httpResource<Page<WorkoutSummary>>(() => ({
    url: '/api/workouts',
    // dovoljno za 8 nedelja i poslednje treninge
    params: { page: 0, size: 100 },
  }));
  protected readonly records = httpResource<PersonalRecord[]>(() => '/api/progress/records');

  protected readonly startOpen = signal(false);
  protected readonly today = formatDay(localDate());

  private readonly completed = computed(() =>
    (this.history.hasValue() ? (this.history.value()?.content ?? []) : []).filter((w) => w.status === 'COMPLETED'),
  );
  protected readonly recent = computed(() => this.completed().slice(0, 3));
  protected readonly latestRecords = computed(() =>
    (this.records.hasValue() ? (this.records.value() ?? []) : []).slice(0, 3),
  );
  protected readonly stats = computed(() => (this.summary.hasValue() ? this.summary.value() : null));

  // poslednjih 8 nedelja (od ponedeljka); svaki završen trening je jedan "teg" u koloni
  protected readonly weeks = computed(() => {
    const now = new Date();
    const thisMonday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - ((now.getDay() + 6) % 7));
    const days = this.completed().map((w) => w.date);
    return Array.from({ length: WEEKS }, (_, i) => {
      const monday = new Date(thisMonday.getFullYear(), thisMonday.getMonth(), thisMonday.getDate() - (WEEKS - 1 - i) * 7);
      const start = localDate(monday);
      const end = localDate(new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 7));
      const count = days.filter((d) => d >= start && d < end).length;
      return {
        start,
        label: SHORT_DATE.format(monday),
        count,
        plates: Array.from({ length: Math.min(count, MAX_PLATES) }),
        current: i === WEEKS - 1,
      };
    });
  });

  protected weekLabel(week: { label: string; count: number; current: boolean }): string {
    const when = week.current ? 'Ova nedelja' : `Nedelja od ${week.label}`;
    return `${when}: ${week.count} ${plural(week.count, 'trening', 'treninga', 'treninga')}`;
  }
  protected readonly activeElapsed = computed(() => {
    const workout = this.active.workout();
    return workout ? formatDuration(this.clock.now() - Date.parse(workout.startedAt)) : '';
  });

  protected readonly title = workoutTitle;
  protected readonly formatDay = formatDay;
  protected readonly formatNumber = formatNumber;
  protected readonly recordLabels = RECORD_LABELS;

  protected duration(w: WorkoutSummary): string {
    return w.finishedAt ? formatMinutes(Date.parse(w.finishedAt) - Date.parse(w.startedAt)) : '';
  }

  protected recordValue(r: PersonalRecord): string {
    if (r.type === 'ESTIMATED_1RM') return formatKg(r.estimated1rm) ?? '–';
    if (r.type === 'MAX_WEIGHT') return formatKg(r.weight) ?? '–';
    return `${r.reps} ${plural(r.reps, 'ponavljanje', 'ponavljanja', 'ponavljanja')}`;
  }
}
