import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterRenderEffect,
  input,
  output,
  viewChild,
} from '@angular/core';

// omotač oko native <dialog>: fokus, Esc i backdrop dobijamo od browser-a
@Component({
  selector: 'app-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog
      #dialog
      class="m-auto w-[calc(100%-2rem)] rounded-lg bg-paper p-0 text-steel shadow-2xl backdrop:bg-steel/60"
      [class]="wide() ? 'max-w-2xl' : 'max-w-lg'"
      [attr.aria-labelledby]="labelledBy()"
      (close)="closed.emit()"
      (click)="onBackdropClick($event)"
    >
      <div class="p-6">
        <ng-content />
      </div>
    </dialog>
  `,
})
export class Dialog {
  readonly open = input(false);
  readonly wide = input(false);
  readonly labelledBy = input<string>();
  readonly closed = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    afterRenderEffect(() => {
      const element = this.dialog().nativeElement;
      if (this.open() && !element.open) {
        element.showModal();
      } else if (!this.open() && element.open) {
        element.close();
      }
    });
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.dialog().nativeElement.close();
    }
  }
}
