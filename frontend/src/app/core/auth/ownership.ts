import { Injectable, computed, inject } from '@angular/core';

import { AuthService } from './auth';

/**
 * Ista pravila kao OwnershipGuard na backend-u; ovde služe samo za prikaz dugmića.
 * Korisnik menja svoje resurse, a admin samo sistemske (createdBy = null).
 */
@Injectable({ providedIn: 'root' })
export class Ownership {
  private readonly auth = inject(AuthService);

  readonly isAdmin = computed(() => this.auth.isAdmin());

  canModify(createdBy: number | null): boolean {
    return this.auth.isAdmin() ? createdBy === null : createdBy !== null && createdBy === this.auth.userId();
  }

  isMine(createdBy: number | null): boolean {
    return createdBy !== null && createdBy === this.auth.userId();
  }
}
