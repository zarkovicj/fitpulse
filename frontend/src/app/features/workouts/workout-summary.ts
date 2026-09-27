import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ExerciseInfo } from '../../core/exercise/exercise-info';
import { errorMessage } from '../../core/api/api-error';
import { muscleGroupLabel } from '../../core/exercise/exercise.model';
import { PersonalRecord, RECORD_LABELS } from '../../core/progress/progress.model';
import { ActiveWorkout } from '../../core/workout/active-workout';
import { Workout, workoutTitle } from '../../core/workout/workout.model';
import { Icon } from '../../shared/icon/icon';
import { formatDay, formatKg, formatMinutes, formatNumber, formatRest, formatTime } from '../../shared/text/format';

@Component({
  selector: 'app-workout-summary',
  imports: [RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './workout-summary.html',
})
export class WorkoutSummaryView {
  private readonly active = inject(ActiveWorkout);
  protected readonly info = inject(ExerciseInfo);

  readonly workout = input.required<Workout>();
  readonly justFinished = input(false);

  protected readonly records = httpResource<PersonalRecord[]>(() =>
    this.workout().status === 'COMPLETED' ? `/api/progress/records?workoutId=${this.workout().id}` : undefined,
  );
  protected readonly recordList = computed(() => (this.records.hasValue() ? this.records.value() ?? [] : []));

  protected readonly title = computed(() => workoutTitle(this.workout()));
  protected readonly day = computed(() => formatDay(this.workout().date));
  protected readonly timeRange = computed(() => {
    const w = this.workout();
    return w.finishedAt ? `${formatTime(w.startedAt)}–${formatTime(w.finishedAt)}` : formatTime(w.startedAt);
  });
  protected readonly duration = computed(() => {
    const w = this.workout();
    return w.finishedAt ? formatMinutes(Date.parse(w.finishedAt) - Date.parse(w.startedAt)) : '–';
  });

  private readonly doneSets = computed(() => this.workout().exercises.flatMap((e) => e.sets).filter((s) => s.completed));
  protected readonly doneCount = computed(() => this.doneSets().length);
  protected readonly volume = computed(() => {
    const total = this.doneSets().reduce((sum, s) => sum + (s.weight ?? 0) * s.reps, 0);
    return total > 0 ? `${formatNumber(total)} kg` : '–';
  });

  protected readonly groupLabel = muscleGroupLabel;
  protected readonly recordLabels = RECORD_LABELS;
  protected readonly formatKg = formatKg;
  protected readonly formatRest = formatRest;

  protected readonly repeating = signal(false);
  protected readonly repeatError = signal<string | null>(null);

  /** Krupno ono što je oboreno, sitno serija iz koje je rekord. */
  protected recordValue(record: PersonalRecord): { main: string; detail: string | null } {
    switch (record.type) {
      case 'MAX_WEIGHT':
        return { main: formatKg(record.weight) ?? '–', detail: `${record.reps} ponavljanja` };
      case 'ESTIMATED_1RM':
        return { main: formatKg(record.estimated1rm) ?? '–', detail: `iz ${formatKg(record.weight)} × ${record.reps}` };
      case 'MAX_REPS':
        return { main: `${record.reps} ponavljanja`, detail: formatKg(record.weight) ? `sa ${formatKg(record.weight)}` : null };
    }
  }

  /** Isti trening ponovo: iz istog šablona, ili sa istim vežbama ako je bio slobodan. */
  protected repeat(): void {
    const w = this.workout();
    this.repeating.set(true);
    this.repeatError.set(null);
    this.active
      .start(
        w.templateId
          ? { templateId: w.templateId, exercises: null }
          : { templateId: null, exercises: w.exercises.map((e) => ({ exerciseId: e.exerciseId })) },
      )
      .subscribe({
        error: (err) => {
          this.repeating.set(false);
          this.repeatError.set(errorMessage(err));
        },
      });
  }
}
