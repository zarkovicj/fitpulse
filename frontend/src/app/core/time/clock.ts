import { Injectable, signal } from '@angular/core';

/** Jedan zajednički otkucaj u sekundi za sve tajmere u aplikaciji. */
@Injectable({ providedIn: 'root' })
export class Clock {
  private readonly current = signal(Date.now());
  readonly now = this.current.asReadonly();

  constructor() {
    setInterval(() => this.current.set(Date.now()), 1000);
  }
}
