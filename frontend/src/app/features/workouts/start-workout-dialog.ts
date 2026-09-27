import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';

import { errorMessage } from '../../core/api/api-error';
import { Ownership } from '../../core/auth/ownership';
import { Template } from '../../core/template/template.model';
import { ActiveWorkout } from '../../core/workout/active-workout';
import { StartWorkoutRequest } from '../../core/workout/workout.model';
import { Dialog } from '../../shared/dialog/dialog';
import { Icon } from '../../shared/icon/icon';
import { plural } from '../../shared/text/plural';

@Component({
  selector: 'app-start-workout-dialog',
  imports: [Dialog, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-dialog [open]="open()" labelledBy="start-title" (closed)="closed.emit()">
      <div class="flex items-start justify-between gap-4">
        <h2 id="start-title" class="font-display text-2xl font-bold">Započni trening</h2>
        <button type="button" class="-mt-1 -mr-2 rounded-md p-2 text-slate hover:bg-steel/5" aria-label="Zatvori"
                (click)="closed.emit()">
          <app-icon name="close" />
        </button>
      </div>

      @if (error(); as message) {
        <p class="alert-error mt-4" role="alert">{{ message }}</p>
      }

      <button type="button" [disabled]="busy()"
              class="mt-4 flex w-full items-center gap-3 rounded-lg border-2 border-dashed border-line px-4 py-3 text-left hover:border-plate-blue"
              (click)="start({ templateId: null, exercises: null })">
        <app-icon name="plus" class="text-plate-blue" />
        <span>
          <span class="block font-semibold">Prazan trening</span>
          <span class="block text-sm text-slate">Vežbe dodaješ usput.</span>
        </span>
      </button>

      <p class="mt-5 text-sm font-medium text-slate">Ili izaberi šablon</p>
      <ul class="mt-2 max-h-[45dvh] divide-y divide-line overflow-y-auto rounded-lg border border-line">
        @for (template of ordered(); track template.id) {
          <li>
            <button type="button" [disabled]="busy()"
                    class="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-concrete/60"
                    (click)="start({ templateId: template.id, exercises: null })">
              <span class="min-w-0 flex-1">
                <span class="block font-semibold">{{ template.name }}</span>
                <span class="block text-sm text-slate">
                  {{ count(template) }}{{ template.system ? ', gotov šablon' : '' }}
                </span>
              </span>
              <app-icon name="play" [size]="18" class="text-plate-blue" />
            </button>
          </li>
        } @empty {
          <li class="px-4 py-5 text-slate">{{ templates.isLoading() ? 'Učitavanje…' : 'Nema šablona.' }}</li>
        }
      </ul>
    </app-dialog>
  `,
})
export class StartWorkoutDialog {
  private readonly active = inject(ActiveWorkout);
  private readonly ownership = inject(Ownership);

  readonly open = input(false);
  readonly closed = output<void>();

  protected readonly templates = httpResource<Template[]>(() => (this.open() ? '/api/templates' : undefined));

  // prvo svoji, pa gotovi; tuđe (vidi ih samo admin) ne nudimo za trening
  protected readonly ordered = computed(() => {
    const all = this.templates.hasValue() ? (this.templates.value() ?? []) : [];
    return [
      ...all.filter((t) => this.ownership.isMine(t.createdBy)),
      ...all.filter((t) => t.system),
    ];
  });

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected count(template: Template): string {
    const n = template.exercises.length;
    return `${n} ${plural(n, 'vežba', 'vežbe', 'vežbi')}`;
  }

  protected start(request: StartWorkoutRequest): void {
    this.busy.set(true);
    this.error.set(null);
    this.active.start(request).subscribe({
      next: () => {
        this.busy.set(false);
        this.closed.emit();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
