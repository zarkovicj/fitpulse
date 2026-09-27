import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ExerciseProgressPoint, PersonalRecord } from '../../core/progress/progress.model';
import { Strength } from './strength';

const record = (id: number, exerciseId: number, name: string, type: PersonalRecord['type'], achievedAt: string): PersonalRecord => ({
  id,
  exerciseId,
  exerciseName: name,
  type,
  weight: 70,
  reps: 6,
  estimated1rm: 84,
  achievedAt,
  workoutId: 1,
});

const point = (workoutId: number, date: string, best1rm: number, volume: number): ExerciseProgressPoint => ({
  workoutId,
  date,
  maxWeight: 70,
  best1rm,
  maxReps: 6,
  volume,
});

describe('Strength', () => {
  // httpResource obrađuje odgovor asinhrono, pa se sačeka jedan krug pre provere
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
  };

  async function setup() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Strength);
    const component = fixture.componentInstance as unknown as {
      byExercise(): { exerciseId: number; maxWeight: unknown; e1rm: unknown }[];
      selectedId(): number | null;
      metric: { set(m: string): void };
      chartPoints(): { x: number; y: number }[];
    };
    TestBed.tick();
    http.expectOne('/api/progress/records').flush([
      record(1, 10, 'Bench Press', 'MAX_WEIGHT', '2026-09-01T10:00:00Z'),
      record(2, 10, 'Bench Press', 'ESTIMATED_1RM', '2026-09-01T10:00:00Z'),
      record(3, 20, 'Squat', 'MAX_WEIGHT', '2026-09-20T10:00:00Z'),
    ]);
    await settle();
    return { http, component };
  }

  it('records_shouldBeGroupedPerExerciseWithMostRecentFirst', async () => {
    const { component } = await setup();

    expect(component.byExercise().map((e) => e.exerciseId)).toEqual([20, 10]);
    expect(component.byExercise()[1].maxWeight).not.toBeNull();
    expect(component.byExercise()[1].e1rm).not.toBeNull();
    // bez ?exercise= u adresi bira se vežba sa najskorijim rekordom
    expect(component.selectedId()).toBe(20);
  });

  it('chart_withTwoWorkoutsSameDay_shouldShowOnePointPerDay', async () => {
    const { http, component } = await setup();
    http.expectOne('/api/progress/exercises/20').flush([
      point(1, '2026-09-01', 80, 1000),
      point(2, '2026-09-20', 84, 1200),
      point(3, '2026-09-20', 87, 500),
    ]);
    await settle();

    // 1RM: najbolji tog dana
    expect(component.chartPoints().map((p) => p.y)).toEqual([80, 87]);

    // volumen: zbir tog dana
    component.metric.set('volume');
    expect(component.chartPoints().map((p) => p.y)).toEqual([1000, 1700]);
  });
});
