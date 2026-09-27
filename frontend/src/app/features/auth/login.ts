import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth';
import { offerToSavePassword, rememberMail, rememberedMail } from '../../core/auth/browser-credentials';
import { errorMessage } from '../../core/api/api-error';
import { AuthLayout } from './auth-layout';

// poruke koje druge stranice ostave pri preusmeravanju na prijavu
const NOTICES: Record<string, string> = {
  deleted: 'Nalog je obrisan.',
  reset: 'Lozinka je promenjena. Prijavi se novom lozinkom.',
};

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, AuthLayout],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = NOTICES[this.route.snapshot.queryParamMap.get('notice') ?? ''] ?? null;

  private readonly savedMail = rememberedMail();
  protected readonly form = inject(NonNullableFormBuilder).group({
    email: [this.savedMail, [Validators.required, Validators.email]],
    password: ['', Validators.required],
    remember: [this.savedMail !== ''],
  });

  protected invalid(name: 'email' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { email, password, remember } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.auth.login({ email, password }).subscribe({
      next: async () => {
        rememberMail(remember ? email : null);
        await offerToSavePassword(email, password);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl?.startsWith('/') ? returnUrl : '/');
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.submitting.set(false);
      },
    });
  }
}
