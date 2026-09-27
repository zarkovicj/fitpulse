import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth';
import { errorMessage } from '../../core/api/api-error';
import { AuthLayout } from './auth-layout';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const { password, confirm } = group.value as { password: string; confirm: string };
  return password === confirm ? null : { mismatch: true };
}

/** Stranica na koju vodi link iz maila: /reset-password?token=… */
@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, AuthLayout],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-auth-layout>
      <h1 class="font-display text-4xl font-bold">Nova lozinka</h1>

      @if (!token()) {
        <p class="alert-error mt-6" role="alert">Link nije potpun. Otvori ga ponovo iz maila ili zatraži novi.</p>
        <a routerLink="/forgot-password" class="btn-secondary mt-4">Zatraži novi link</a>
      } @else {
        <p class="mt-2 text-slate">Izaberi novu lozinku za svoj nalog.</p>

        <form class="mt-8 space-y-5" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          @if (error(); as message) {
            <p class="alert-error" role="alert">
              {{ message }}
              <a routerLink="/forgot-password" class="font-semibold underline">Zatraži novi link</a>
            </p>
          }
          <div>
            <label class="field-label" for="password">Nova lozinka</label>
            <input id="password" name="new-password" class="field-input" type="password" formControlName="password" maxlength="72"
                   autocomplete="new-password"
                   [attr.aria-invalid]="form.controls.password.invalid && form.controls.password.touched" />
            @if (form.controls.password.invalid && form.controls.password.touched) {
              <p class="field-error">Lozinka mora imati bar 6 karaktera.</p>
            } @else {
              <p class="mt-1.5 text-sm text-slate">Najmanje 6 karaktera.</p>
            }
          </div>
          <div>
            <label class="field-label" for="confirm">Ponovi lozinku</label>
            <input id="confirm" class="field-input" type="password" formControlName="confirm" autocomplete="new-password" maxlength="72"
                   [attr.aria-invalid]="form.hasError('mismatch') && form.controls.confirm.touched" />
            @if (form.hasError('mismatch') && form.controls.confirm.touched) {
              <p class="field-error">Lozinke se ne poklapaju.</p>
            }
          </div>
          <button class="btn-primary w-full" type="submit" [disabled]="submitting()">
            {{ submitting() ? 'Čuvanje…' : 'Sačuvaj novu lozinku' }}
          </button>
        </form>
      }
    </app-auth-layout>
  `,
})
export class ResetPassword {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** ?token= iz linka u mailu */
  readonly token = input<string>();

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirm: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.auth.resetPassword(this.token()!, this.form.getRawValue().password).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { notice: 'reset' } }),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.submitting.set(false);
      },
    });
  }
}
