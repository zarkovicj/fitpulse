import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Template, TemplateRequest } from './template.model';

@Injectable({ providedIn: 'root' })
export class TemplateApi {
  private readonly http = inject(HttpClient);

  get(id: number): Observable<Template> {
    return this.http.get<Template>(`/api/templates/${id}`);
  }

  create(request: TemplateRequest): Observable<Template> {
    return this.http.post<Template>('/api/templates', request);
  }

  update(id: number, request: TemplateRequest): Observable<Template> {
    return this.http.put<Template>(`/api/templates/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/templates/${id}`);
  }
}
