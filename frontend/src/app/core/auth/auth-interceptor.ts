import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.token();
  const isApi = req.url.startsWith('/api/');
  const isAuthCall = req.url.startsWith('/api/auth/');

  const request =
    token && isApi && !isAuthCall
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      // 401 na login-u je pogrešna lozinka, a ne istekla sesija
      if (error instanceof HttpErrorResponse && error.status === 401 && isApi && !isAuthCall) {
        auth.logout(router.url);
      }
      return throwError(() => error);
    }),
  );
};
