import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Exercise, ExerciseRequest } from './exercise.model';

@Injectable({ providedIn: 'root' })
export class ExerciseApi {
  private readonly http = inject(HttpClient);

  create(request: ExerciseRequest): Observable<Exercise> {
    return this.http.post<Exercise>('/api/exercises', request);
  }

  update(id: number, request: ExerciseRequest): Observable<Exercise> {
    return this.http.put<Exercise>(`/api/exercises/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/exercises/${id}`);
  }

  uploadImage(id: number, image: Blob): Observable<Exercise> {
    const form = new FormData();
    form.append('file', image, 'image.jpg');
    return this.http.put<Exercise>(`/api/exercises/${id}/image`, form);
  }

  deleteImage(id: number): Observable<void> {
    return this.http.delete<void>(`/api/exercises/${id}/image`);
  }
}
