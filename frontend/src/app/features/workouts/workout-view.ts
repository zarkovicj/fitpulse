import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api/api-error';
import { Workout } from '../../core/workout/workout.model';
import { LiveWorkout } from './live-workout';
import { WorkoutSummaryView } from './workout-summary';

// /workouts/:id: trening u toku se vodi uživo, završen se samo prikazuje
@Component({
  selector: 'app-workout-view',
  imports: [RouterLink, LiveWorkout, WorkoutSummaryView],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error(); as message) {
      <div class="max-w-3xl">
        <p class="alert-error" role="alert">{{ message }}</p>
        <a routerLink="/workouts" class="btn-secondary mt-4">Nazad na treninge</a>
      </div>
    } @else if (workout(); as w) {
      @if (w.status === 'IN_PROGRESS') {
        <app-live-workout [initial]="w" (ended)="onEnded($event)" />
      } @else {
        <app-workout-summary [workout]="w" [justFinished]="justFinished()" />
      }
    } @else {
      <p class="text-slate">Učitavanje…</p>
    }
  `,
})
export class WorkoutView {
  readonly id = input.required<string>();

  // prati id iz rute, pa radi i prelaz sa jednog treninga na drugi ("Ponovi")
  protected readonly resource = httpResource<Workout>(() => `/api/workouts/${this.id()}`);
  protected readonly workout = computed(() => (this.resource.hasValue() ? this.resource.value() : null));
  protected readonly justFinished = linkedSignal({ source: this.id, computation: () => false });

  protected readonly error = computed(() => {
    const error = this.resource.error();
    if (!error) return null;
    return (error as { status?: number }).status === 404 ? 'Trening ne postoji.' : errorMessage(error, 'Trening nije učitan.');
  });

  protected onEnded(workout: Workout): void {
    this.justFinished.set(true);
    this.resource.set(workout);
    window.scrollTo({ top: 0 });
  }
}
