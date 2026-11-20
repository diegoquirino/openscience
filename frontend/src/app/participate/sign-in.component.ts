import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import {
  fieldMessage,
  Problem,
  problemMessage,
  problemOf,
  problemSlug,
  unexpectedErrorMessage,
} from '../core/problem-messages';
import { LanguageService } from '../core/language.service';
import { SessionService } from '../core/session.service';
import { emailValidator } from './access-validators';
import { readPendingCode, savePendingCode } from './pending-code';

const ROUTE = '/participate';

/** First step of the sign-in: e-mail only, then "Send code" (spec 013). */
@Component({
  selector: 'app-sign-in',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './sign-in.component.html',
  styleUrl: './sign-in.component.scss',
})
export class SignInComponent {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly language = inject(LanguageService);

  protected readonly emailError = fieldMessage({ field: 'email', code: 'email' });

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: emailValidator }),
  });

  protected readonly sending = signal(false);
  protected readonly message = signal<string | undefined>(undefined);

  constructor() {
    const pending = readPendingCode();
    if (pending) {
      this.form.setValue({ email: pending.typedEmail });
    }
    const unsaved = this.language.takeUnsaved(ROUTE);
    if (unsaved) {
      this.form.patchValue({ email: unsaved['email'] ?? '' });
    }
    this.language.keepOnSwitch(ROUTE, () => this.form.getRawValue());
  }

  protected submit(): void {
    this.message.set(undefined);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email } = this.form.getRawValue();
    this.sending.set(true);
    this.session.requestCode(email).subscribe({
      next: (requested) => {
        savePendingCode({
          typedEmail: email,
          email: requested.email,
          validityMinutes: requested.validityMinutes,
          resendAt: Date.now() + requested.resendAfterSeconds * 1000,
          signInHref: requested.signInHref,
        });
        void this.router.navigate(['/participate/code']);
      },
      error: (error: unknown) => {
        this.sending.set(false);
        this.show(problemOf(error));
      },
    });
  }

  private show(problem: Problem | undefined): void {
    if (problemSlug(problem) === 'invalid-request' && problem?.errors?.length) {
      for (const error of problem.errors) {
        const control = this.form.get(error.field);
        control?.setErrors({ [error.code]: true });
        control?.markAsTouched();
      }
      return;
    }
    this.message.set(problemMessage(problem) ?? unexpectedErrorMessage());
  }
}
