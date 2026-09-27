import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { SetRequest, StartWorkoutRequest, Workout, WorkoutExercise, WorkoutSet } from './workout.model';

@Injectable({ providedIn: 'root' })
export class WorkoutApi {
  private readonly http = inject(HttpClient);

  get(id: number): Observable<Workout> {
    return this.http.get<Workout>(`/api/workouts/${id}`);
  }

  /** null kad nema treninga u toku (204). */
  active(): Observable<Workout | null> {
    return this.http.get<Workout | null>('/api/workouts/active');
  }

  start(request: StartWorkoutRequest): Observable<Workout> {
    return this.http.post<Workout>('/api/workouts', request);
  }

  finish(id: number, updateTemplate: boolean): Observable<Workout> {
    return this.http.put<Workout>(`/api/workouts/${id}/finish`, { updateTemplate });
  }

  cancel(id: number): Observable<Workout> {
    return this.http.put<Workout>(`/api/workouts/${id}/cancel`, null);
  }

  addExercise(id: number, exerciseId: number): Observable<WorkoutExercise> {
    return this.http.post<WorkoutExercise>(`/api/workouts/${id}/exercises`, { exerciseId });
  }

  removeExercise(id: number, exerciseId: number): Observable<void> {
    return this.http.delete<void>(`/api/workouts/${id}/exercises/${exerciseId}`);
  }

  addSet(id: number, exerciseId: number): Observable<WorkoutSet> {
    return this.http.post<WorkoutSet>(`/api/workouts/${id}/exercises/${exerciseId}/sets`, null);
  }

  updateSet(id: number, exerciseId: number, setId: number, request: SetRequest): Observable<WorkoutSet> {
    return this.http.put<WorkoutSet>(`/api/workouts/${id}/exercises/${exerciseId}/sets/${setId}`, request);
  }

  removeSet(id: number, exerciseId: number, setId: number): Observable<void> {
    return this.http.delete<void>(`/api/workouts/${id}/exercises/${exerciseId}/sets/${setId}`);
  }
}
