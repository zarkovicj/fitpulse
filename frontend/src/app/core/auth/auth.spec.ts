import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { fakeJwt } from '../../../testing/fake-jwt';
import { AuthService, tokenExpiry } from './auth';
import { AuthResponse } from './auth.model';

const STORAGE_KEY = 'fitpulse.session';

describe('AuthService', () => {
  let http: HttpTestingController;

  function setup(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('login_withValidResponse_shouldStoreSession', () => {
    const auth = setup();
    const response: AuthResponse = { token: fakeJwt(3600), userId: 1, email: 'pera@example.com', role: 'ADMIN' };

    auth.login({ email: 'pera@example.com', password: 'lozinka123' }).subscribe();
    http.expectOne('/api/auth/login').flush(response);

    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.isAdmin()).toBe(true);
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY)!).token).toBe(response.token);
  });

  it('constructor_withExpiredStoredSession_shouldStartLoggedOut', () => {
    const token = fakeJwt(-60);
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ token, userId: 1, email: 'x@example.com', role: 'USER', expiresAt: tokenExpiry(token) }),
    );

    const auth = setup();

    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.hasValidSession()).toBe(false);
  });

  it('logout_shouldClearSessionAndGoToLogin', () => {
    const auth = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    auth.login({ email: 'pera@example.com', password: 'lozinka123' }).subscribe();
    http.expectOne('/api/auth/login').flush({ token: fakeJwt(3600), userId: 1, email: 'pera@example.com', role: 'USER' });

    auth.logout('/workouts');

    expect(auth.isLoggedIn()).toBe(false);
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/workouts' } });
  });

  it('tokenExpiry_withMalformedToken_shouldReturnZero', () => {
    expect(tokenExpiry('not-a-jwt')).toBe(0);
  });
});
