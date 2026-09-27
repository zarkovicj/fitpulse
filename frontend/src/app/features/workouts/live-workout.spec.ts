import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth';
import { RestTimer } from '../../core/workout/rest-timer';
import { Workout, WorkoutSet } from '../../core/workout/workout.model';
import { LiveWorkout } from './live-workout';

const set = (id: number, position: number, weight: number | null, completed = false): WorkoutSet => ({
  id,
  position,
  reps: 8,
  weight,
  completed,
  restAfter: null,
});

const WORKOUT: Workout = {
  id: 10,
  templateId: null,
  templateName: null,
  date: '2026-09-23',
  startedAt: new Date().toISOString(),
  finishedAt: null,
  status: 'IN_PROGRESS',
  exercises: [
    {
      id: 100,
      exerciseId: 1,
      exerciseName: 'Bench Press',
      muscleGroup: 'CHEST',
      position: 1,
      setCount: 4,
      completed: false,
      restAfter: null,
      // treća je već urađena, četvrta ima drugačiju kilažu
      sets: [set(1, 1, 60), set(2, 2, 60), set(3, 3, 60, true), set(4, 4, 50)],
    },
  ],
};

describe('LiveWorkout', () => {
  let fixture: ComponentFixture<LiveWorkout>;
  let http: HttpTestingController;
  let root: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { token: signal(null), userId: signal(7), isAdmin: signal(false) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LiveWorkout);
    fixture.componentRef.setInput('initial', WORKOUT);
    await fixture.whenStable();
    root = fixture.nativeElement;
  });

  const kgInputs = () => root.querySelectorAll<HTMLInputElement>('input[inputmode=decimal]');
  const setUrl = (setId: number) => `/api/workouts/10/exercises/100/sets/${setId}`;

  it('toggle_shouldMarkSetCompletedAndSendCurrentValues', async () => {
    root.querySelector<HTMLButtonElement>('button[aria-pressed]')!.click();

    const request = http.expectOne(setUrl(1));
    expect(request.request.method).toBe('PUT');
    // urađena serija nosi i planirani odmor (podrazumevano 90 s)
    expect(request.request.body).toEqual({ reps: 8, weight: 60, completed: true, restAfter: 90 });
    request.flush({ ...set(1, 1, 60), completed: true });
    await fixture.whenStable();

    expect(root.querySelector('button[aria-pressed]')!.getAttribute('aria-pressed')).toBe('true');
  });

  it('restEditor_shouldChangeRestForSetAndFollowingUnfinishedSets', async () => {
    root.querySelector<HTMLButtonElement>('button[aria-label^="Odmor posle serije 1"]')!.click();
    await fixture.whenStable();
    [...root.querySelectorAll<HTMLButtonElement>('button.chip')].find((b) => b.textContent!.trim() === '2:00')!.click();

    // 1, 2 i 4 imaju isti (podrazumevani) odmor; 3. je urađena, pa ostaje kakva je
    for (const id of [1, 2, 4]) {
      const request = http.expectOne(setUrl(id));
      expect(request.request.body.restAfter).toBe(120);
      request.flush({});
    }
    http.expectNone(setUrl(3));
  });

  it('completingSet_shouldCloseOtherRestEditorAndShowControlsWithCountdown', async () => {
    root.querySelector<HTMLButtonElement>('button[aria-label^="Odmor posle serije 2"]')!.click();
    await fixture.whenStable();
    expect(root.querySelector('button[aria-label^="Odmor posle serije 2"]')!.getAttribute('aria-expanded')).toBe('true');

    root.querySelector<HTMLButtonElement>('button[aria-pressed]')!.click();
    http.expectOne(setUrl(1)).flush({});
    await fixture.whenStable();

    // izbor za 2. seriju se zatvorio, a uz odbrojavanje 1. serije kontrole su odmah vidljive
    expect(root.querySelector('button[aria-label^="Odmor posle serije 2"]')!.getAttribute('aria-expanded')).toBe('false');
    expect(root.querySelector('[role=timer]')).not.toBeNull();
    const chips = [...root.querySelectorAll('button.chip')].map((b) => b.textContent!.trim());
    expect(chips).toContain('+15 s');
  });

  it('toggle_shouldStartCountdownWithSetsOwnRest', () => {
    const withRest: Workout = {
      ...WORKOUT,
      exercises: [{ ...WORKOUT.exercises[0], sets: [{ ...set(1, 1, 60), restAfter: 150 }] }],
    };
    fixture.componentRef.setInput('initial', withRest);
    TestBed.tick();

    root.querySelector<HTMLButtonElement>('button[aria-pressed]')!.click();

    expect(http.expectOne(setUrl(1)).request.body.restAfter).toBe(150);
    expect(TestBed.inject(RestTimer).remaining()).toBe(150);
  });

  it('changeKg_shouldCarryOverToFollowingUnfinishedSetsWithSameValue', async () => {
    const first = kgInputs()[0];
    first.value = '62,5';
    first.dispatchEvent(new Event('change'));

    // 2. serija ima istu staru vrednost; 3. je urađena, a 4. ima drugu kilažu, pa se ne diraju
    http.expectOne(setUrl(1)).flush(set(1, 1, 62.5));
    const second = http.expectOne(setUrl(2));
    expect(second.request.body.weight).toBe(62.5);
    second.flush(set(2, 2, 62.5));
    http.expectNone(setUrl(3));
    http.expectNone(setUrl(4));
    await fixture.whenStable();

    expect([...kgInputs()].map((i) => i.value)).toEqual(['62.5', '62.5', '60', '50']);
  });

  it('invalidInput_shouldRestorePreviousValueWithoutRequest', () => {
    const first = kgInputs()[0];
    first.value = 'abc';
    first.dispatchEvent(new Event('change'));

    http.expectNone(setUrl(1));
    expect(first.value).toBe('60');
  });

  it('failedSave_shouldRevertAndShowError', async () => {
    root.querySelector<HTMLButtonElement>('button[aria-pressed]')!.click();
    http.expectOne(setUrl(1)).flush({ message: 'Trening nije u toku' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(root.querySelector('button[aria-pressed]')!.getAttribute('aria-pressed')).toBe('false');
    expect(root.querySelector('[role=alert]')!.textContent).toContain('Trening nije u toku');
  });

  it('quickSecondChange_shouldWaitForFirstRequest_soOrderIsKept', async () => {
    const toggle = () => root.querySelector<HTMLButtonElement>('button[aria-pressed]')!.click();
    toggle();
    TestBed.tick();
    toggle();

    // druga izmena ne ide dok prva ne završi
    const first = http.expectOne(setUrl(1));
    expect(first.request.body.completed).toBe(true);
    first.flush({});

    const second = http.expectOne(setUrl(1));
    expect(second.request.body.completed).toBe(false);
    second.flush({});
    await fixture.whenStable();

    expect(root.querySelector('button[aria-pressed]')!.getAttribute('aria-pressed')).toBe('false');
  });

  it('failedFirstSave_shouldNotRevertNewerQueuedChange', async () => {
    const toggle = () => root.querySelector<HTMLButtonElement>('button[aria-pressed]')!.click();
    toggle();
    TestBed.tick();
    toggle();
    TestBed.tick();
    toggle(); // opet urađena

    http.expectOne(setUrl(1)).flush({ message: 'Greška' }, { status: 500, statusText: 'Error' });
    const latest = http.expectOne(setUrl(1));
    expect(latest.request.body.completed).toBe(true);
    latest.flush({});
    await fixture.whenStable();

    expect(root.querySelector('button[aria-pressed]')!.getAttribute('aria-pressed')).toBe('true');
  });

  it('failedExerciseRemoval_shouldRestoreOnlyThatExercise', async () => {
    const twoExercises: Workout = {
      ...WORKOUT,
      exercises: [
        WORKOUT.exercises[0],
        { ...WORKOUT.exercises[0], id: 200, exerciseId: 2, exerciseName: 'Squat', position: 2, sets: [set(9, 1, 80)] },
      ],
    };
    fixture.componentRef.setInput('initial', twoExercises);
    TestBed.tick();
    const component = fixture.componentInstance as unknown as {
      removingExercise: { set(e: unknown): void };
      confirmRemoveExercise(): void;
    };

    component.removingExercise.set(twoExercises.exercises[1]);
    component.confirmRemoveExercise();
    const removal = http.expectOne('/api/workouts/10/exercises/200');

    // dok brisanje traje, urađena je serija prve vežbe
    root.querySelector<HTMLButtonElement>('button[aria-pressed]')!.click();
    http.expectOne(setUrl(1)).flush({});

    removal.flush({ message: 'Greška' }, { status: 500, statusText: 'Error' });
    await fixture.whenStable();

    expect(root.textContent).toContain('Squat');
    expect(root.querySelector('button[aria-pressed]')!.getAttribute('aria-pressed')).toBe('true');
  });

  it('finish_shouldEmitEndedWorkout', () => {
    const ended = vi.fn();
    fixture.componentInstance.ended.subscribe(ended);

    (fixture.componentInstance as unknown as { finish(update: boolean): void }).finish(false);
    const request = http.expectOne('/api/workouts/10/finish');
    expect(request.request.body).toEqual({ updateTemplate: false });
    request.flush({ ...WORKOUT, status: 'COMPLETED' });

    expect(ended).toHaveBeenCalledWith(expect.objectContaining({ status: 'COMPLETED' }));
  });
});
