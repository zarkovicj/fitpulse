import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AdminUser } from './admin-users.model';

@Injectable({ providedIn: 'root' })
export class AdminUsersApi {
  private readonly http = inject(HttpClient);

  setActive(id: number, active: boolean): Observable<AdminUser> {
    return this.http.put<AdminUser>(`/api/admin/users/${id}/status`, { active });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/users/${id}`);
  }
}
