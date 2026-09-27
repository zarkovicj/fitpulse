import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth';
import { offerToSavePassword } from '../../core/auth/browser-credentials';
import { errorMessage } from '../../core/api/api-error';
import { AuthLayout } from './auth-layout';

type Field = 'firstName' | 'lastName' | 'email' | 'password' | 'birthDate';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, AuthLayout],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register.html',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly today = new Date().toISOString().slice(0, 10);

  protected readonly form = inject(NonNullableFormBuilder).group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    birthDate: [''],
  });

  protected invalid(name: Field): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { birthDate, ...rest } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.auth.register({ ...rest, birthDate: birthDate || null }).subscribe({
      next: async () => {
        await offerToSavePassword(rest.email, rest.password, `${rest.firstName} ${rest.lastName}`);
        this.router.navigateByUrl('/');
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.submitting.set(false);
      },
    });
  }
}
