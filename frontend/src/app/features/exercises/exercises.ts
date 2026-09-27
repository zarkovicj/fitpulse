import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { ExerciseInfo } from '../../core/exercise/exercise-info';
import { errorMessage } from '../../core/api/api-error';
import { Ownership } from '../../core/auth/ownership';
import { ExerciseApi } from '../../core/exercise/exercise-api';
import { Exercise, MUSCLE_GROUPS, MuscleGroup } from '../../core/exercise/exercise.model';
import { ConfirmDialog } from '../../shared/dialog/confirm-dialog';
import { Dialog } from '../../shared/dialog/dialog';
import { Icon } from '../../shared/icon/icon';
import { normalize } from '../../shared/text/normalize';
import { plural } from '../../shared/text/plural';
import { ExerciseForm } from './exercise-form';

interface Section {
  group: MuscleGroup;
  label: string;
  exercises: Exercise[];
}

@Component({
  selector: 'app-exercises',
  imports: [Icon, Dialog, ConfirmDialog, ExerciseForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './exercises.html',
})
export class Exercises {
  private readonly api = inject(ExerciseApi);
  protected readonly info = inject(ExerciseInfo);
  protected readonly ownership = inject(Ownership);

  // lista je mala, ima nekoliko vezbi, pa se učitava jednom i filtrira u browser-u
  protected readonly library = httpResource<Exercise[]>(() => '/api/exercises', { defaultValue: [] });

  protected readonly groups = MUSCLE_GROUPS;
  protected readonly search = signal('');
  protected readonly group = signal<MuscleGroup | null>(null);
  protected readonly onlyMine = signal(false);

  protected readonly filtered = computed(() => {
    const query = normalize(this.search());
    const group = this.group();
    const onlyMine = this.onlyMine();
    const all = this.library.hasValue() ? this.library.value() : [];
    return all
      .filter((e) => !group || e.muscleGroup === group)
      .filter((e) => !onlyMine || this.ownership.isMine(e.createdBy))
      .filter((e) => !query || normalize(e.name).includes(query) || normalize(e.description ?? '').includes(query));
  });

  protected readonly sections = computed<Section[]>(() =>
    MUSCLE_GROUPS.map(({ value, label }) => ({
      group: value,
      label,
      exercises: this.filtered()
        .filter((e) => e.muscleGroup === value)
        .sort((a, b) => a.name.localeCompare(b.name, 'sr')),
    })).filter((section) => section.exercises.length > 0),
  );

  // null = zatvoren, { exercise: null } = nova vežba
  protected readonly editing = signal<{ exercise: Exercise | null } | null>(null);
  protected readonly deleting = signal<Exercise | null>(null);
  protected readonly deleteBusy = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  protected readonly errorText = computed(() =>
    this.library.error() ? errorMessage(this.library.error(), 'Vežbe nisu učitane.') : null,
  );

  protected toggleGroup(group: MuscleGroup | null): void {
    this.group.update((current) => (current === group ? null : group));
  }

  protected clearFilters(): void {
    this.search.set('');
    this.group.set(null);
    this.onlyMine.set(false);
  }

  protected onSaved(): void {
    this.editing.set(null);
    this.library.reload();
  }

  protected askDelete(exercise: Exercise): void {
    this.deleteError.set(null);
    this.deleting.set(exercise);
  }

  protected confirmDelete(): void {
    const exercise = this.deleting();
    if (!exercise) return;

    this.deleteBusy.set(true);
    this.api.delete(exercise.id).subscribe({
      next: () => {
        this.deleteBusy.set(false);
        this.deleting.set(null);
        this.library.reload();
      },
      error: (err) => {
        this.deleteBusy.set(false);
        this.deleteError.set(
          err?.status === 409
            ? 'Vežba se koristi u nekom šablonu ili treningu, pa ne može da se obriše.'
            : errorMessage(err),
        );
      },
    });
  }

  protected countLabel(count: number): string {
    return `${count} ${plural(count, 'vežba', 'vežbe', 'vežbi')}`;
  }
}
