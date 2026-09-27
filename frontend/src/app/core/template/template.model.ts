import { MuscleGroup } from '../exercise/exercise.model';

export interface TemplateExercise {
  id: number;
  exerciseId: number;
  exerciseName: string;
  muscleGroup: MuscleGroup;
  setCount: number;
  reps: number;
  weight: number | null;
  position: number;
}

export interface Template {
  id: number;
  name: string;
  description: string | null;
  createdBy: number | null;
  system: boolean;
  exercises: TemplateExercise[];
}

export interface TemplateExerciseRequest {
  exerciseId: number;
  setCount: number;
  reps: number;
  weight: number | null;
}

export interface TemplateRequest {
  name: string;
  description: string | null;
  // izostavljeno na PUT-u = menja se samo naziv i opis
  exercises?: TemplateExerciseRequest[];
}
