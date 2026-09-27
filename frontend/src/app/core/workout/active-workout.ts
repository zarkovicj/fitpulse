import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { Injectable, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, switchMap, tap, throwError } from 'rxjs';

import { AuthService } from '../auth/auth';
import { WorkoutApi } from './workout-api';
import { StartWorkoutRequest, Workout } from './workout.model';

/** Trening u toku (najviše jedan po korisniku), za meni, početnu i dugmad za start. */
@Injectable({ providedIn: 'root' })
export class ActiveWorkout {
  private readonly api = inject(WorkoutApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // 204 bez tela daje null
  private readonly resource = httpResource<Workout | null>(() =>
    // admin nema treninge (backend mu vraća 403)
    this.auth.token() && !this.auth.isAdmin() ? '/api/workouts/active' : undefined,
  );

  readonly workout = computed(() => (this.resource.hasValue() ? this.resource.value() : null));

  set(workout: Workout | null): void {
    this.resource.set(workout);
  }

  /** Pokreće trening i otvara ga. Ako je neki već u toku (409), otvara taj. */
  start(request: StartWorkoutRequest): Observable<Workout> {
    return this.api.start(request).pipe(
      catchError((error: unknown) =>
        error instanceof HttpErrorResponse && error.status === 409
          ? this.api.active().pipe(
              switchMap((active) => (active ? [active] : throwError(() => error))),
            )
          : throwError(() => error),
      ),
      tap((workout) => {
        this.resource.set(workout);
        this.router.navigate(['/workouts', workout.id]);
      }),
    );
  }
}
