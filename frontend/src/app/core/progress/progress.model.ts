export type RecordType = 'MAX_WEIGHT' | 'ESTIMATED_1RM' | 'MAX_REPS';

export interface PersonalRecord {
  id: number;
  exerciseId: number;
  exerciseName: string;
  type: RecordType;
  weight: number | null;
  reps: number;
  estimated1rm: number | null;
  achievedAt: string;
  workoutId: number | null;
}

export const RECORD_LABELS: Record<RecordType, string> = {
  MAX_WEIGHT: 'Najveća kilaža',
  ESTIMATED_1RM: 'Procenjeni 1RM',
  MAX_REPS: 'Najviše ponavljanja',
};

export interface ProgressSummary {
  totalWorkouts: number;
  workoutsThisWeek: number;
  volumeLast30Days: number;
  recordCount: number;
  currentWeight: number | null;
  goalWeight: number | null;
  goalBodyFatPercent: number | null;
}

/** Jedna tačka napretka za vežbu = jedan završen trening. */
export interface ExerciseProgressPoint {
  workoutId: number;
  date: string;
  maxWeight: number | null;
  best1rm: number | null;
  maxReps: number;
  volume: number;
}

export interface BodyGoal {
  weight: number | null;
  bodyFatPercent: number | null;
}

export interface WeightEntry {
  date: string;
  weight: number;
}
