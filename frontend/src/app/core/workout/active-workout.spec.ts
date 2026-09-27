import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../auth/auth';
import { ActiveWorkout } from './active-workout';

describe('ActiveWorkout', () => {
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;
  let active: ActiveWorkout;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        // bez tokena resource ne šalje zahtev, pa test kontroliše samo start
        { provide: AuthService, useValue: { token: signal(null) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    active = TestBed.inject(ActiveWorkout);
  });

  afterEach(() => http.verify());

  it('start_shouldOpenNewWorkout', () => {
    active.start({ templateId: 3, exercises: null }).subscribe();

    const request = http.expectOne('/api/workouts');
    expect(request.request.body).toEqual({ templateId: 3, exercises: null });
    request.flush({ id: 11, status: 'IN_PROGRESS' });

    expect(navigate).toHaveBeenCalledWith(['/workouts', 11]);
    expect(active.workout()?.id).toBe(11);
  });

  it('start_whenWorkoutAlreadyInProgress_shouldOpenExistingOne', () => {
    const error = vi.fn();
    active.start({ templateId: null, exercises: null }).subscribe({ error });

    http.expectOne('/api/workouts').flush({ message: 'Već imate trening u toku' }, { status: 409, statusText: 'Conflict' });
    http.expectOne('/api/workouts/active').flush({ id: 7, status: 'IN_PROGRESS' });

    expect(error).not.toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/workouts', 7]);
  });
});
