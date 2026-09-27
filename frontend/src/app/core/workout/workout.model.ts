import { MuscleGroup } from '../exercise/exercise.model';

export type WorkoutStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface WorkoutSet {
  id: number;
  position: number;
  reps: number;
  weight: number | null;
  completed: boolean;
  restAfter: number | null;
}

export interface WorkoutExercise {
  id: number;
  exerciseId: number;
  exerciseName: string;
  muscleGroup: MuscleGroup;
  position: number;
  setCount: number;
  completed: boolean;
  restAfter: number | null;
  sets: WorkoutSet[];
}

export interface Workout {
  id: number;
  templateId: number | null;
  templateName: string | null;
  date: string;
  startedAt: string;
  finishedAt: string | null;
  status: WorkoutStatus;
  exercises: WorkoutExercise[];
}

export interface WorkoutSummary {
  id: number;
  templateName: string | null;
  date: string;
  startedAt: string;
  finishedAt: string | null;
  status: WorkoutStatus;
  exerciseCount: number;
  totalVolume: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface StartWorkoutRequest {
  templateId: number | null;
  exercises: { exerciseId: number }[] | null;
}

export interface SetRequest {
  reps: number;
  weight: number | null;
  completed: boolean;
  restAfter: number | null;
}

export function workoutTitle(workout: { templateName: string | null }): string {
  return workout.templateName ?? 'Slobodan trening';
}
