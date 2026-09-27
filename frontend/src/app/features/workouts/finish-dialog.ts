import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, output } from '@angular/core';

import { Ownership } from '../../core/auth/ownership';
import { Template } from '../../core/template/template.model';
import { Workout } from '../../core/workout/workout.model';
import { Dialog } from '../../shared/dialog/dialog';

@Component({
  selector: 'app-finish-dialog',
  imports: [Dialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-dialog [open]="open()" labelledBy="finish-title" (closed)="cancelled.emit()">
      <h2 id="finish-title" class="font-display text-2xl font-bold">Završetak treninga</h2>

      <p class="mt-2 text-slate">
        Urađeno je <strong class="text-steel">{{ done() }} od {{ total() }}</strong> serija.
        @if (done() === 0) {
          Pošto nijedna serija nije označena, trening se neće računati u rekorde.
        } @else if (done() < total()) {
          Neoznačene serije ostaju zapisane, ali se ne računaju u rekorde i statistiku.
        }
      </p>

      @if (canUpdateTemplate()) {
        <label class="mt-5 flex items-start gap-3 rounded-md border border-line p-3">
          <input type="checkbox" class="mt-1 size-4 accent-plate-blue"
                 [checked]="updateTemplate()" (change)="updateTemplate.set(!updateTemplate())" />
          <span>
            <span class="block font-medium">Ažuriraj šablon „{{ template()?.name }}“</span>
            <span class="block text-sm text-slate">
              Šablon dobija današnji broj serija, a ponavljanja i kilažu iz poslednje urađene serije svake vežbe.
            </span>
          </span>
        </label>
      }

      <div class="mt-6 flex justify-end gap-2">
        <button type="button" class="btn-ghost" (click)="cancelled.emit()">Nastavi trening</button>
        <button type="button" class="btn-primary" [disabled]="busy()" (click)="confirmed.emit(canUpdateTemplate() && updateTemplate())">
          {{ busy() ? 'Čuvanje…' : 'Završi trening' }}
        </button>
      </div>
    </app-dialog>
  `,
})
export class FinishDialog {
  private readonly ownership = inject(Ownership);

  readonly open = input(false);
  readonly workout = input.required<Workout>();
  readonly busy = input(false);

  readonly confirmed = output<boolean>();
  readonly cancelled = output<void>();

  // šablon treba samo da bi se znalo da li korisnik sme da ga menja
  private readonly templateResource = httpResource<Template>(() => {
    const templateId = this.workout().templateId;
    return this.open() && templateId ? `/api/templates/${templateId}` : undefined;
  });
  protected readonly template = computed(() =>
    this.templateResource.hasValue() ? this.templateResource.value() : null,
  );
  protected readonly canUpdateTemplate = computed(() => {
    const template = this.template();
    return template !== null && !template.system && this.ownership.canModify(template.createdBy);
  });

  // pri svakom otvaranju kreće neoznačeno
  protected readonly updateTemplate = linkedSignal({ source: this.open, computation: () => false });

  private readonly sets = computed(() => this.workout().exercises.flatMap((e) => e.sets));
  protected readonly total = computed(() => this.sets().length);
  protected readonly done = computed(() => this.sets().filter((s) => s.completed).length);
}
