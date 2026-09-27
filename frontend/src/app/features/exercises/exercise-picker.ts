import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import { Exercise, MUSCLE_GROUPS, MuscleGroup, muscleGroupLabel } from '../../core/exercise/exercise.model';
import { Dialog } from '../../shared/dialog/dialog';
import { Icon } from '../../shared/icon/icon';
import { normalize } from '../../shared/text/normalize';

/** Dijalog za biranje vežbi; ostaje otvoren da bi se dodalo više vežbi zaredom. */
@Component({
  selector: 'app-exercise-picker',
  imports: [Dialog, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './exercise-picker.html',
})
export class ExercisePicker {
  readonly open = input(false);
  readonly allowed = input<(exercise: Exercise) => boolean>(() => true);
  readonly addedIds = input<readonly number[]>([]);

  readonly picked = output<Exercise>();
  readonly closed = output<void>();

  // učitava se pri svakom otvaranju, da se vide i vežbe napravljene u međuvremenu
  protected readonly library = httpResource<Exercise[]>(() => (this.open() ? '/api/exercises' : undefined));

  protected readonly groups = MUSCLE_GROUPS;
  protected readonly groupLabel = muscleGroupLabel;
  protected readonly search = signal('');
  protected readonly group = signal<MuscleGroup | null>(null);

  protected readonly results = computed(() => {
    const all = this.library.hasValue() ? this.library.value() : [];
    const query = normalize(this.search());
    const group = this.group();
    const allowed = this.allowed();
    return all
      .filter(allowed)
      .filter((e) => !group || e.muscleGroup === group)
      .filter((e) => !query || normalize(e.name).includes(query))
      .sort((a, b) => a.name.localeCompare(b.name, 'sr'));
  });

  protected isAdded(exercise: Exercise): boolean {
    return this.addedIds().includes(exercise.id);
  }
}
