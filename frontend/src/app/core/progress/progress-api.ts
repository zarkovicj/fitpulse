import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { BodyGoal, WeightEntry } from './progress.model';

@Injectable({ providedIn: 'root' })
export class ProgressApi {
  private readonly http = inject(HttpClient);

  updateGoal(goal: BodyGoal): Observable<BodyGoal> {
    return this.http.put<BodyGoal>('/api/progress/goal', goal);
  }

  /** Jedno merenje po danu: isti datum menja postojeće. */
  logWeight(date: string, weight: number): Observable<WeightEntry> {
    return this.http.put<WeightEntry>(`/api/progress/weight/${date}`, { weight });
  }

  deleteWeight(date: string): Observable<void> {
    return this.http.delete<void>(`/api/progress/weight/${date}`);
  }
}
