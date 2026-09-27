import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api/api-error';
import { AuthService } from '../../core/auth/auth';
import { CurrentUser } from '../../core/user/current-user';
import { Dialog } from '../../shared/dialog/dialog';
import { HasUnsavedChanges } from '../../shared/guards/unsaved-changes';
import { formatDate, formatNumber, localDate } from '../../shared/text/format';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, RouterLink, Dialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './profile.html',
})
export class Profile implements HasUnsavedChanges {
  protected readonly currentUser = inject(CurrentUser);
  private readonly auth = inject(AuthService);

  protected readonly deleting = signal(false);
  protected readonly deletePasswordValue = signal('');
  protected readonly deleteBusy = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  protected readonly today = localDate();
  protected readonly saving = signal(false);
  protected readonly message = signal<{ ok: boolean; text: string } | null>(null);
  protected readonly formatDate = formatDate;
  protected readonly formatNumber = formatNumber;

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    birthDate: [''],
    height: this.fb.control<number | null>(null, [Validators.min(50), Validators.max(260)]),
  });

  constructor() {
    // popuni formu kad stigne profil, ali ne pregazi ono što korisnik već kuca
    effect(() => {
      const user = this.currentUser.user();
      if (user && this.form.pristine) {
        this.form.reset({
          firstName: user.firstName,
          lastName: user.lastName,
          birthDate: user.birthDate ?? '',
          height: user.height,
        });
      }
    });
  }

  protected invalid(name: 'firstName' | 'lastName' | 'birthDate' | 'height'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  protected submit(): void {
    const { firstName, lastName, birthDate, height } = this.form.getRawValue();
    if (birthDate && birthDate >= this.today) {
      this.form.controls.birthDate.setErrors({ future: true });
      this.form.controls.birthDate.markAsTouched();
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.message.set(null);
    this.currentUser
      .update({ firstName: firstName.trim(), lastName: lastName.trim(), birthDate: birthDate || null, height: height || null })
      .subscribe({
        next: (user) => {
          this.saving.set(false);
          this.form.reset({ firstName: user.firstName, lastName: user.lastName, birthDate: user.birthDate ?? '', height: user.height });
          this.message.set({ ok: true, text: 'Izmene su sačuvane.' });
        },
        error: (err) => {
          this.saving.set(false);
          this.message.set({ ok: false, text: errorMessage(err) });
        },
      });
  }

  protected openDelete(): void {
    this.deletePasswordValue.set('');
    this.deleteError.set(null);
    this.deleting.set(true);
  }

  protected confirmDelete(): void {
    if (!this.deletePasswordValue()) return;
    this.deleteBusy.set(true);
    this.deleteError.set(null);
    this.auth.deleteAccount(this.deletePasswordValue()).subscribe({
      error: (err) => {
        this.deleteBusy.set(false);
        this.deleteError.set(errorMessage(err));
      },
    });
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty;
  }
}
