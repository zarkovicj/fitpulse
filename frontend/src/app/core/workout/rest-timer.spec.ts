import { TestBed } from '@angular/core/testing';

import { DEFAULT_REST, RestTimer } from './rest-timer';

describe('RestTimer', () => {
  let vibrate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.useFakeTimers();
    localStorage.clear();
    vibrate = vi.fn();
    Object.defineProperty(navigator, 'vibrate', { value: vibrate, configurable: true });
  });

  afterEach(() => vi.useRealTimers());

  const create = () => TestBed.inject(RestTimer);

  it('start_shouldCountDownToZeroAndNotifyOnce', () => {
    const timer = create();
    timer.start(1, 60, 'Bench Press');
    expect(timer.remaining()).toBe(60);

    vi.advanceTimersByTime(30_000);
    expect(timer.remaining()).toBe(30);
    expect(vibrate).not.toHaveBeenCalled();

    vi.advanceTimersByTime(30_000);
    TestBed.tick();
    expect(timer.done()).toBe(true);
    expect(vibrate).toHaveBeenCalledTimes(1);

    // i posle isteka ostaje jedno obaveštenje
    vi.advanceTimersByTime(10_000);
    TestBed.tick();
    expect(vibrate).toHaveBeenCalledTimes(1);
  });

  it('retime_shouldCountFromSameStartForSameSetOnly', () => {
    const timer = create();
    timer.start(2, 60, 'Squat');
    vi.advanceTimersByTime(20_000);

    // odmor serije promenjen na 90 s: ostaje 70 s, jer je 20 s već prošlo
    timer.retime(2, 90);
    expect(timer.remaining()).toBe(70);

    // izmena druge serije ne dira odbrojavanje koje teče
    timer.retime(7, 30);
    expect(timer.remaining()).toBe(70);
    expect(timer.setId()).toBe(2);
  });

  it('stop_shouldCancelNotification', () => {
    const timer = create();
    timer.start(3, 10, 'Pull-ups');
    timer.stop();

    vi.advanceTimersByTime(15_000);
    TestBed.tick();

    expect(timer.active()).toBe(false);
    expect(vibrate).not.toHaveBeenCalled();
  });

  it('runningRest_shouldSurviveReloadButNotRingLateForExpiredOne', () => {
    // odmor je istekao pre minut, dok je stranica bila zatvorena
    localStorage.setItem('fitpulse.rest', JSON.stringify({ endsAt: Date.now() - 60_000, duration: 90, label: 'Bench' }));

    const timer = create();
    TestBed.tick();

    expect(timer.done()).toBe(true);
    expect(vibrate).not.toHaveBeenCalled();
  });

  it('restFor_shouldRememberChoicePerExercise', () => {
    const timer = create();
    expect(timer.restFor(5)).toBe(DEFAULT_REST);
    expect(timer.restFor(5, 120)).toBe(120);

    timer.setRestFor(5, 150);

    expect(timer.restFor(5, 120)).toBe(150);
    expect(JSON.parse(localStorage.getItem('fitpulse.rest-prefs')!)).toEqual({ 5: 150 });
  });
});
