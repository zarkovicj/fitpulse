import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.hasValidSession()
    ? true
    : inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasValidSession() ? inject(Router).createUrlTree(['/']) : true;
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAdmin() ? true : inject(Router).createUrlTree(['/']);
};

/** Treninzi, šabloni, vežbe i napredak su za korisnike; admin ide na administraciju. */
export const userGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAdmin() ? inject(Router).createUrlTree(['/admin']) : true;
};
