import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api/api-error';
import { ExerciseApi } from '../../core/exercise/exercise-api';
import { Exercise, MUSCLE_GROUPS, muscleGroupLabel } from '../../core/exercise/exercise.model';
import { TemplateApi } from '../../core/template/template-api';
import { Template } from '../../core/template/template.model';
import { ConfirmDialog } from '../../shared/dialog/confirm-dialog';
import { Dialog } from '../../shared/dialog/dialog';
import { Icon } from '../../shared/icon/icon';
import { normalize } from '../../shared/text/normalize';
import { plural } from '../../shared/text/plural';
import { ExerciseForm } from '../exercises/exercise-form';
import { AdminUsers } from './admin-users';

type Tab = 'exercises' | 'templates' | 'users';
const TABS: Tab[] = ['exercises', 'templates', 'users'];

type Deleting = { kind: 'exercise'; item: Exercise } | { kind: 'template'; item: Template };

/** Upravljanje gotovim (sistemskim) vežbama i šablonima koje vide svi korisnici. */
@Component({
  selector: 'app-admin',
  imports: [RouterLink, Icon, Dialog, ConfirmDialog, ExerciseForm, AdminUsers],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin.html',
  styles: `
    @reference '../../../styles.css';
    .admin-tab {
      @apply -mb-px border-b-2 border-transparent px-4 py-2.5 font-semibold text-slate hover:text-steel;
    }
    .admin-tab[aria-selected='true'] {
      @apply border-plate-blue text-steel;
    }
  `,
})
export class Admin {
  private readonly router = inject(Router);
  private readonly exerciseApi = inject(ExerciseApi);
  private readonly templateApi = inject(TemplateApi);

  /** ?tab=templates, da povratak iz editora šablona vrati na pravu karticu. */
  readonly tab = input<string>();
  protected readonly activeTab = linkedSignal<Tab>(() => (TABS.find((t) => t === this.tab()) ?? 'exercises'));

  protected readonly exercises = httpResource<Exercise[]>(() => '/api/exercises');
  protected readonly templates = httpResource<Template[]>(() => '/api/templates');

  protected readonly search = signal('');
  protected readonly systemExercises = computed(() => {
    const query = normalize(this.search());
    const order = MUSCLE_GROUPS.map((g) => g.value);
    return (this.exercises.hasValue() ? (this.exercises.value() ?? []) : [])
      .filter((e) => e.system)
      .filter((e) => !query || normalize(e.name).includes(query))
      .sort(
        (a, b) =>
          order.indexOf(a.muscleGroup) - order.indexOf(b.muscleGroup) || a.name.localeCompare(b.name, 'sr'),
      );
  });
  protected readonly systemTemplates = computed(() =>
    (this.templates.hasValue() ? (this.templates.value() ?? []) : []).filter((t) => t.system),
  );

  protected readonly editing = signal<{ exercise: Exercise | null } | null>(null);
  protected readonly deleting = signal<Deleting | null>(null);
  protected readonly deleteBusy = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  protected readonly groupLabel = muscleGroupLabel;

  protected selectTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.router.navigate([], { queryParams: { tab: tab === 'exercises' ? null : tab }, replaceUrl: true });
  }

  protected exerciseCount(template: Template): string {
    const n = template.exercises.length;
    return `${n} ${plural(n, 'vežba', 'vežbe', 'vežbi')}`;
  }

  protected onSaved(): void {
    this.editing.set(null);
    this.exercises.reload();
  }

  protected askDelete(target: Deleting): void {
    this.deleteError.set(null);
    this.deleting.set(target);
  }

  protected deleteMessage(): string {
    const target = this.deleting();
    if (!target) return '';
    return target.kind === 'exercise'
      ? `Vežba „${target.item.name}“ nestaće iz biblioteke svih korisnika.`
      : `Šablon „${target.item.name}“ nestaće za sve korisnike. Već odrađeni treninzi ostaju u istoriji.`;
  }

  protected confirmDelete(): void {
    const target = this.deleting();
    if (!target) return;

    const request =
      target.kind === 'exercise' ? this.exerciseApi.delete(target.item.id) : this.templateApi.delete(target.item.id);
    this.deleteBusy.set(true);
    request.subscribe({
      next: () => {
        this.deleteBusy.set(false);
        this.deleting.set(null);
        if (target.kind === 'exercise') this.exercises.reload();
        else this.templates.reload();
      },
      error: (err) => {
        this.deleteBusy.set(false);
        this.deleteError.set(
          err?.status === 409
            ? 'Vežba se koristi u nečijem šablonu ili treningu, pa ne može da se obriše.'
            : errorMessage(err),
        );
      },
    });
  }
}
