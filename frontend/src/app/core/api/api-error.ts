import { HttpErrorResponse } from '@angular/common/http';

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

/** Poruka za korisnika iz greške backend-a (ApiError) ili mreže. */
export function errorMessage(error: unknown, fallback = 'Nešto nije u redu. Pokušaj ponovo.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Server nije dostupan. Proveri da li je backend pokrenut.';
    }
    const body = error.error as Partial<ApiError> | null;
    if (body?.message) {
      return body.message;
    }
  }
  return fallback;
}
