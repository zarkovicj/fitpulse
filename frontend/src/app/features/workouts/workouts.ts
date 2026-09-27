import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api/api-error';
import { Clock } from '../../core/time/clock';
import { ActiveWorkout } from '../../core/workout/active-workout';
import { Page, WorkoutSummary, workoutTitle } from '../../core/workout/workout.model';
import { Icon } from '../../shared/icon/icon';
import { formatDay, formatDuration, formatMinutes, formatNumber } from '../../shared/text/format';
import { plural } from '../../shared/text/plural';
import { StartWorkoutDialog } from './start-workout-dialog';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-workouts',
  imports: [RouterLink, Icon, StartWorkoutDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './workouts.html',
})
export class Workouts implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly clock = inject(Clock);
  protected readonly active = inject(ActiveWorkout);

  protected readonly items = signal<WorkoutSummary[]>([]);
  protected readonly nextPage = signal<number | null>(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly startOpen = signal(false);

  // trening u toku ima svoju karticu na vrhu, pa ga u istoriji ne ponavljamo
  protected readonly history = computed(() => this.items().filter((w) => w.status !== 'IN_PROGRESS'));
  protected readonly activeElapsed = computed(() => {
    const workout = this.active.workout();
    return workout ? formatDuration(this.clock.now() - Date.parse(workout.startedAt)) : '';
  });

  protected readonly title = workoutTitle;
  protected readonly day = formatDay;

  ngOnInit(): void {
    this.loadMore();
  }

  protected loadMore(): void {
    const page = this.nextPage();
    if (page === null || this.loading()) return;

    this.loading.set(true);
    this.error.set(null);
    this.http.get<Page<WorkoutSummary>>('/api/workouts', { params: { page, size: PAGE_SIZE } }).subscribe({
      next: (result) => {
        this.items.update((items) => [...items, ...result.content]);
        this.nextPage.set(result.last ? null : page + 1);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err, 'Istorija treninga nije učitana.'));
        this.loading.set(false);
      },
    });
  }

  protected duration(w: WorkoutSummary): string | null {
    return w.finishedAt ? formatMinutes(Date.parse(w.finishedAt) - Date.parse(w.startedAt)) : null;
  }

  protected volume(w: WorkoutSummary): string | null {
    return w.totalVolume > 0 ? `${formatNumber(w.totalVolume)} kg` : null;
  }

  protected exercises(w: WorkoutSummary): string {
    return `${w.exerciseCount} ${plural(w.exerciseCount, 'vežba', 'vežbe', 'vežbi')}`;
  }
}
