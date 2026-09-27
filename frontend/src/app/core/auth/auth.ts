import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { AuthResponse, LoginRequest, RegisterRequest, Session } from './auth.model';

const STORAGE_KEY = 'fitpulse.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(restoreSession());

  readonly token = computed(() => this.session()?.token ?? null);
  readonly userId = computed(() => this.session()?.userId ?? null);
  readonly role = computed(() => this.session()?.role ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.role === 'ADMIN');

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', request)
      .pipe(tap((response) => this.start(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', request)
      .pipe(tap((response) => this.start(response)));
  }

  // token je stateless, pa je odjava samo brisanje lokalne sesije
  logout(returnUrl?: string): void {
    this.clear();
    this.router.navigate(['/login'], returnUrl ? { queryParams: { returnUrl } } : undefined);
  }

  // Link za novu lozinku stize na mail; odgovor je isti i kad nalog ne postoji.
  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/forgot-password', { email });
  }

  // posle promene lozinke, stari token ne važi, pa se i lokalna sesija briše
  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http
      .post<void>('/api/auth/reset-password', { token, newPassword })
      .pipe(tap(() => this.clear()));
  }

  // Trajno brisanje sopstvenog naloga; posle toga prijava prikazuje potvrdu. 
  deleteAccount(password: string): Observable<void> {
    return this.http.delete<void>('/api/user/me', { body: { password } }).pipe(
      tap(() => {
        this.clear();
        this.router.navigate(['/login'], { queryParams: { notice: 'deleted' } });
      }),
    );
  }

  // Sesija važi samo dok token nije istekao. 
  hasValidSession(): boolean {
    const current = this.session();
    if (current && current.expiresAt <= Date.now()) {
      this.clear();
      return false;
    }
    return current !== null;
  }

  private start(response: AuthResponse): void {
    const session: Session = { ...response, expiresAt: tokenExpiry(response.token) };
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // privatni prozor bez storage-a: sesija traje dok je tab otvoren
    }
    this.session.set(session);
  }

  private clear(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {}
    this.session.set(null);
  }
}

function restoreSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as Session;
    return session.expiresAt > Date.now() ? session : null;
  } catch {
    return null;
  }
}

// Čita `exp` iz JWT payload-a (u sekundama) i vraća milisekunde. 
export function tokenExpiry(token: string): number {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const { exp } = JSON.parse(atob(payload)) as { exp?: number };
    return exp ? exp * 1000 : 0;
  } catch {
    return 0;
  }
}
