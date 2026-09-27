import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ExerciseInfo } from '../../core/exercise/exercise-info';
import { errorMessage } from '../../core/api/api-error';
import { Ownership } from '../../core/auth/ownership';
import { muscleGroupLabel } from '../../core/exercise/exercise.model';
import { TemplateApi } from '../../core/template/template-api';
import { ActiveWorkout } from '../../core/workout/active-workout';
import { Template } from '../../core/template/template.model';
import { ConfirmDialog } from '../../shared/dialog/confirm-dialog';
import { Icon } from '../../shared/icon/icon';
import { formatKg } from '../../shared/text/format';

@Component({
  selector: 'app-template-detail',
  imports: [RouterLink, Icon, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './template-detail.html',
})
export class TemplateDetail {
  private readonly api = inject(TemplateApi);
  protected readonly info = inject(ExerciseInfo);
  private readonly router = inject(Router);
  private readonly active = inject(ActiveWorkout);
  protected readonly ownership = inject(Ownership);

  readonly id = input.required<string>();

  protected readonly template = httpResource<Template>(() => `/api/templates/${this.id()}`);
  protected readonly value = computed(() => (this.template.hasValue() ? this.template.value() : null));
  protected readonly canModify = computed(() => {
    const template = this.value();
    return template !== null && this.ownership.canModify(template.createdBy);
  });

  protected readonly groupLabel = muscleGroupLabel;
  protected readonly formatKg = formatKg;

  protected readonly busy = signal(false);
  protected readonly actionError = signal<string | null>(null);
  protected readonly confirmingDelete = signal(false);

  protected readonly loadError = computed(() => {
    const error = this.template.error();
    if (!error) return null;
    return (error as { status?: number }).status === 404
      ? 'Šablon ne postoji ili nije dostupan.'
      : errorMessage(error, 'Šablon nije učitan.');
  });

  protected startWorkout(): void {
    const template = this.value();
    if (!template) return;

    this.busy.set(true);
    this.actionError.set(null);
    this.active.start({ templateId: template.id, exercises: null }).subscribe({
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(errorMessage(err));
      },
    });
  }

  /** Kopija postaje korisnikov šablon koji sme slobodno da menja. */
  protected duplicate(): void {
    const template = this.value();
    if (!template) return;

    this.busy.set(true);
    this.actionError.set(null);
    this.api
      .create({
        name: `${template.name} (kopija)`,
        description: template.description,
        exercises: template.exercises.map((e) => ({
          exerciseId: e.exerciseId,
          setCount: e.setCount,
          reps: e.reps,
          weight: e.weight,
        })),
      })
      .subscribe({
        next: (copy) => this.router.navigate(['/templates', copy.id, 'edit']),
        error: (err) => {
          this.busy.set(false);
          this.actionError.set(errorMessage(err));
        },
      });
  }

  protected confirmDelete(): void {
    const template = this.value();
    if (!template) return;

    this.busy.set(true);
    this.api.delete(template.id).subscribe({
      next: () =>
        this.ownership.isAdmin()
          ? this.router.navigate(['/admin'], { queryParams: { tab: 'templates' } })
          : this.router.navigate(['/templates']),
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(errorMessage(err));
        this.confirmingDelete.set(false);
      },
    });
  }
}
