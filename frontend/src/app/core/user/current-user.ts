import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, computed, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { AuthService } from '../auth/auth';
import { UpdateUserRequest, User } from './user.model';

@Injectable({ providedIn: 'root' })
export class CurrentUser {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  // ponovo se učitava pri svakoj prijavi, a posle odjave je prazan
  private readonly resource = httpResource<User>(() =>
    this.auth.token() ? '/api/user/me' : undefined,
  );

  readonly user = computed(() => (this.resource.hasValue() ? this.resource.value() : null));
  readonly initials = computed(() => {
    const user = this.user();
    return user ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase() : '';
  });

  update(request: UpdateUserRequest): Observable<User> {
    return this.http
      .put<User>('/api/user/me', request)
      .pipe(tap((user) => this.resource.set(user)));
  }

  reload(): void {
    this.resource.reload();
  }
}
