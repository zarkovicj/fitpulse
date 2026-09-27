import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';

import { fakeJwt } from '../../../testing/fake-jwt';
import { AuthService } from '../../core/auth/auth';
import { Login } from './login';
import { ResetPassword } from './reset-password';

describe('account flows', () => {
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  function setup(queryParams: Record<string, string> = {}) {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('login_withRememberChecked_shouldStoreOnlyTheMail', async () => {
    setup();
    const fixture = TestBed.createComponent(Login);
    const login = fixture.componentInstance as unknown as { form: Login['form']; submit(): void };
    login.form.setValue({ email: 'ana@example.com', password: 'tajna123', remember: true });

    login.submit();
    http.expectOne('/api/auth/login').flush({ token: fakeJwt(3600), userId: 1, email: 'ana@example.com', role: 'USER' });
    await fixture.whenStable();

    expect(localStorage.getItem('fitpulse.remembered-mail')).toBe('ana@example.com');
    // lozinka nikad ne ide u storage
    expect(JSON.stringify({ ...localStorage })).not.toContain('tajna123');
  });

  it('login_shouldPrefillRememberedMailAndShowNotice', async () => {
    localStorage.setItem('fitpulse.remembered-mail', 'ana@example.com');
    setup({ notice: 'reset' });
    const fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
    const root: HTMLElement = fixture.nativeElement;

    expect(root.querySelector<HTMLInputElement>('#email')!.value).toBe('ana@example.com');
    expect(root.querySelector('[role=status]')!.textContent).toContain('Lozinka je promenjena');
    expect(root.querySelector('#email')!.getAttribute('autocomplete')).toBe('username');
    expect(root.querySelector('#password')!.getAttribute('autocomplete')).toBe('current-password');
  });

  it('resetPassword_shouldRequireMatchingPasswordsThenGoToLogin', () => {
    setup();
    const fixture = TestBed.createComponent(ResetPassword);
    fixture.componentRef.setInput('token', 'abc123');
    const page = fixture.componentInstance as unknown as { form: ResetPassword['form']; submit(): void };

    page.form.setValue({ password: 'nova1234', confirm: 'nova12345' });
    page.submit();
    http.expectNone('/api/auth/reset-password');

    page.form.setValue({ password: 'nova1234', confirm: 'nova1234' });
    page.submit();
    const request = http.expectOne('/api/auth/reset-password');
    expect(request.request.body).toEqual({ token: 'abc123', newPassword: 'nova1234' });
    request.flush(null);

    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { notice: 'reset' } });
  });

  it('deleteAccount_shouldSendPasswordThenLogOut', () => {
    setup();
    const auth = TestBed.inject(AuthService);
    auth.login({ email: 'ana@example.com', password: 'x' }).subscribe();
    http.expectOne('/api/auth/login').flush({ token: fakeJwt(3600), userId: 1, email: 'ana@example.com', role: 'USER' });

    auth.deleteAccount('tajna123').subscribe();
    const request = http.expectOne({ method: 'DELETE', url: '/api/user/me' });
    expect(request.request.body).toEqual({ password: 'tajna123' });
    request.flush(null);

    expect(auth.isLoggedIn()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { notice: 'deleted' } });
  });
});
