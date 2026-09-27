import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { AuthService } from '../auth/auth';
import { Clock } from '../time/clock';
import { CurrentUser } from '../user/current-user';
import { ActiveWorkout } from '../workout/active-workout';
import { RestTimer } from '../workout/rest-timer';
import { workoutTitle } from '../workout/workout.model';
import { ExerciseDetail } from '../../features/exercises/exercise-detail';
import { Icon, IconName } from '../../shared/icon/icon';
import { formatDuration } from '../../shared/text/format';

interface NavItem {
  path: string;
  label: string;
  icon: IconName;
  exact?: boolean;
}

const MAIN_NAV: NavItem[] = [
  { path: '/', label: 'Početna', icon: 'home', exact: true },
  { path: '/workouts', label: 'Treninzi', icon: 'history' },
  { path: '/templates', label: 'Šabloni', icon: 'clipboard' },
  { path: '/exercises', label: 'Vežbe', icon: 'dumbbell' },
  { path: '/progress', label: 'Napredak', icon: 'chart' },
];

const ADMIN_NAV: NavItem[] = [{ path: '/admin', label: 'Administracija', icon: 'shield' }];

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon, ExerciseDetail],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly clock = inject(Clock);
  private readonly active = inject(ActiveWorkout);
  private readonly rest = inject(RestTimer);
  protected readonly currentUser = inject(CurrentUser);

  // admin vidi samo administraciju; treninzi i napredak su za korisnike
  protected readonly mainNav = computed(() => (this.auth.isAdmin() ? ADMIN_NAV : MAIN_NAV));
  protected readonly accountNav: NavItem[] = [{ path: '/profile', label: 'Profil', icon: 'user' }];

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  // podsetnik na trening u toku, osim kad je korisnik već na njegovom ekranu
  protected readonly showActive = computed(() => {
    const workout = this.active.workout();
    return workout && this.url() !== `/workouts/${workout.id}` ? workout : null;
  });
  protected readonly activeTitle = computed(() => {
    const workout = this.active.workout();
    return workout ? workoutTitle(workout) : '';
  });
  protected readonly activeElapsed = computed(() => {
    const workout = this.active.workout();
    return workout ? formatDuration(this.clock.now() - Date.parse(workout.startedAt)) : '';
  });

  // dok traje odmor, podsetnik pokazuje odbrojavanje umesto trajanja treninga
  protected readonly indicatorTime = computed(() => {
    const remaining = this.rest.remaining();
    if (remaining === null) return this.activeElapsed();
    return remaining === 0 ? 'Odmor gotov' : `Odmor ${formatDuration(remaining * 1000)}`;
  });

  protected logout(): void {
    this.rest.stop();
    this.active.set(null);
    this.auth.logout();
  }
}
