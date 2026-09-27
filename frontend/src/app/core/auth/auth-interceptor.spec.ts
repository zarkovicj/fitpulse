import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { fakeJwt } from '../../../testing/fake-jwt';
import { AuthService } from './auth';
import { authInterceptor } from './auth-interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let auth: AuthService;
  const token = fakeJwt(3600);

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);

    auth.login({ email: 'pera@example.com', password: 'lozinka123' }).subscribe();
    controller.expectOne('/api/auth/login').flush({ token, userId: 1, email: 'pera@example.com', role: 'USER' });
  });

  afterEach(() => controller.verify());

  it('request_toApi_shouldCarryBearerToken', () => {
    http.get('/api/exercises').subscribe();

    const request = controller.expectOne('/api/exercises');
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    request.flush([]);
  });

  it('request_toAuthEndpoint_shouldNotCarryToken', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => {} });

    const request = controller.expectOne('/api/auth/login');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(null, { status: 401, statusText: 'Unauthorized' });

    // pogrešna lozinka ne sme da odjavi korisnika
    expect(auth.isLoggedIn()).toBe(true);
  });

  it('response_with401_shouldLogOut', () => {
    const logout = vi.spyOn(auth, 'logout').mockImplementation(() => {});

    http.get('/api/user/me').subscribe({ error: () => {} });
    controller.expectOne('/api/user/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(logout).toHaveBeenCalled();
  });
});
