import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth';
import { rememberedMail } from '../../core/auth/browser-credentials';
import { errorMessage } from '../../core/api/api-error';
import { AuthLayout } from './auth-layout';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, AuthLayout],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-auth-layout>
      <h1 class="font-display text-4xl font-bold">Zaboravljena lozinka</h1>

      @if (sentTo(); as email) {
        <p class="mt-6 rounded-md border border-plate-green/30 bg-plate-green/10 px-3 py-3 text-plate-green" role="status">
          Ako postoji nalog sa adresom <strong>{{ email }}</strong>, na nju smo poslali link za novu lozinku.
          Link važi 30 minuta.
        </p>
        <p class="mt-4 text-sm text-slate">Ne vidiš mail? Proveri i folder za neželjenu poštu.</p>
      } @else {
        <p class="mt-2 text-slate">Unesi mail naloga, pa ćemo ti poslati link za novu lozinku.</p>

        <form class="mt-8 space-y-5" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          @if (error(); as message) {
            <p class="alert-error" role="alert">{{ message }}</p>
          }
          <div>
            <label class="field-label" for="email">Mail</label>
            <input id="email" name="username" class="field-input" type="email" formControlName="email" autocomplete="username"
                   [attr.aria-invalid]="form.controls.email.invalid && form.controls.email.touched" />
            @if (form.controls.email.invalid && form.controls.email.touched) {
              <p class="field-error">Unesi ispravan mail.</p>
            }
          </div>
          <button class="btn-primary w-full" type="submit" [disabled]="submitting()">
            {{ submitting() ? 'Slanje…' : 'Pošalji link' }}
          </button>
        </form>
      }

      <p class="mt-8 text-slate">
        Setio si se?
        <a class="font-semibold text-plate-blue hover:underline" routerLink="/login">Nazad na prijavu</a>
      </p>
    </app-auth-layout>
  `,
})
export class ForgotPassword {
  private readonly auth = inject(AuthService);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sentTo = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: [rememberedMail(), [Validators.required, Validators.email]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const email = this.form.getRawValue().email.trim();
    this.submitting.set(true);
    this.error.set(null);
    this.auth.forgotPassword(email).subscribe({
      next: () => this.sentTo.set(email),
      error: (err) => {
        this.error.set(errorMessage(err));
        this.submitting.set(false);
      },
    });
  }
}
