export type MuscleGroup = 'CHEST' | 'BACK' | 'LEGS' | 'SHOULDERS' | 'ARMS' | 'CORE' | 'CARDIO';

export const MUSCLE_GROUPS: readonly { value: MuscleGroup; label: string }[] = [
  { value: 'CHEST', label: 'Grudi' },
  { value: 'BACK', label: 'Leđa' },
  { value: 'LEGS', label: 'Noge' },
  { value: 'SHOULDERS', label: 'Ramena' },
  { value: 'ARMS', label: 'Ruke' },
  { value: 'CORE', label: 'Stomak' },
  { value: 'CARDIO', label: 'Kardio' },
];

export function muscleGroupLabel(group: MuscleGroup): string {
  return MUSCLE_GROUPS.find((g) => g.value === group)?.label ?? group;
}

export interface Exercise {
  id: number;
  name: string;
  muscleGroup: MuscleGroup;
  imageUrl: string | null;
  description: string | null;
  videoUrl: string | null;
  createdBy: number | null;
  system: boolean;
}

export interface ExerciseRequest {
  name: string;
  muscleGroup: MuscleGroup;
  description: string | null;
  videoUrl: string | null;
}
