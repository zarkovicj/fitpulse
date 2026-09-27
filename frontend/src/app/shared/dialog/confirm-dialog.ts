import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { Dialog } from './dialog';

@Component({
  selector: 'app-confirm-dialog',
  imports: [Dialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-dialog [open]="open()" labelledBy="confirm-title" (closed)="cancelled.emit()">
      <h2 id="confirm-title" class="font-display text-2xl font-bold">{{ title() }}</h2>
      <p class="mt-2 text-slate">{{ message() }}</p>

      @if (error(); as error) {
        <p class="mt-4 rounded-md border border-plate-red/30 bg-plate-red/5 px-3 py-2.5 text-sm text-plate-red" role="alert">
          {{ error }}
        </p>
      }

      <div class="mt-6 flex justify-end gap-2">
        <button type="button" class="btn-ghost" (click)="cancelled.emit()">Odustani</button>
        <button type="button" class="btn-danger" [disabled]="busy()" (click)="confirmed.emit()">
          {{ confirmLabel() }}
        </button>
      </div>
    </app-dialog>
  `,
})
export class ConfirmDialog {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input('Obriši');
  readonly busy = input(false);
  readonly error = input<string | null>(null);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
