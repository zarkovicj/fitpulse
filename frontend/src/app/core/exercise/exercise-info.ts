import { Injectable, inject, signal } from '@angular/core';
import { NavigationStart, Router } from '@angular/router';
import { filter } from 'rxjs';

/**
 * Otvara detalje vežbe sa bilo kog ekrana (vežbe, šablon, trening).
 * Sam modal živi jednom u shell-u, pa ekrani samo pozovu open(id).
 */
@Injectable({ providedIn: 'root' })
export class ExerciseInfo {
  private readonly current = signal<number | null>(null);
  readonly exerciseId = this.current.asReadonly();

  constructor() {
    // link iz modala (npr. na trening iz istorije) zatvara modal
    inject(Router)
      .events.pipe(filter((e) => e instanceof NavigationStart))
      .subscribe(() => this.close());
  }

  open(exerciseId: number): void {
    this.current.set(exerciseId);
  }

  close(): void {
    this.current.set(null);
  }
}
