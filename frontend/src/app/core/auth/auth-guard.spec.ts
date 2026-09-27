import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { AuthService } from './auth';
import { adminGuard, authGuard, guestGuard, userGuard } from './auth-guard';

describe('auth guards', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/progress' } as RouterStateSnapshot;
  let auth: { hasValidSession: ReturnType<typeof vi.fn>; isAdmin: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    auth = { hasValidSession: vi.fn(), isAdmin: vi.fn() };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), { provide: AuthService, useValue: auth }],
    });
  });

  const run = (guard: typeof authGuard) => TestBed.runInInjectionContext(() => guard(route, state));

  it('authGuard_withoutSession_shouldRedirectToLoginWithReturnUrl', () => {
    auth.hasValidSession.mockReturnValue(false);

    const result = run(authGuard) as UrlTree;

    expect(result.toString()).toBe('/login?returnUrl=%2Fprogress');
  });

  it('authGuard_withSession_shouldAllow', () => {
    auth.hasValidSession.mockReturnValue(true);
    expect(run(authGuard)).toBe(true);
  });

  it('guestGuard_withSession_shouldRedirectHome', () => {
    auth.hasValidSession.mockReturnValue(true);
    expect((run(guestGuard) as UrlTree).toString()).toBe('/');
  });

  it('adminGuard_forRegularUser_shouldRedirectHome', () => {
    auth.isAdmin.mockReturnValue(false);
    expect((run(adminGuard) as UrlTree).toString()).toBe('/');
  });

  it('userGuard_forAdmin_shouldRedirectToAdministration', () => {
    auth.isAdmin.mockReturnValue(true);
    expect((run(userGuard) as UrlTree).toString()).toBe('/admin');
  });

  it('userGuard_forRegularUser_shouldAllow', () => {
    auth.isAdmin.mockReturnValue(false);
    expect(run(userGuard)).toBe(true);
  });
});