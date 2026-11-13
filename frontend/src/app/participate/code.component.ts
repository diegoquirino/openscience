import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';
import {
  fieldMessage,
  Problem,
  problemMessage,
  problemOf,
  problemSlug,
  unexpectedErrorMessage,
} from '../core/problem-messages';
import { SessionService } from '../core/session.service';
import { codeValidator } from './access-validators';
import { clearPendingCode, PendingCode, readPendingCode, savePendingCode } from './pending-code';

/** Second step of the sign-in: the code received by e-mail (spec 004, US1). */
@Component({
  selector: 'app-code',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './code.component.html',
  styleUrl: './code.component.scss',
})
export class CodeComponent {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly codeError = fieldMessage({ field: 'code', code: 'required' });
  protected readonly form = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: codeValidator }),
  });
  private readonly code = this.form.controls.code;

  protected readonly pending = signal<PendingCode | null>(readPendingCode());
  private readonly now = signal(Date.now());
  protected readonly secondsToResend = computed(() =>
    Math.max(0, Math.ceil(((this.pending()?.resendAt ?? 0) - this.now()) / 1000)),
  );

  protected readonly sending = signal(false);
  protected readonly message = signal<string | undefined>(undefined);
  protected readonly codeExpired = signal(false);
  protected readonly resent = signal(false);

  constructor() {
    if (!this.pending()) {
      void this.router.navigate(['/participate']);
      return;
    }
    const timer = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected submit(): void {
    const pending = this.pending();
    this.message.set(undefined);
    this.resent.set(false);
    if (!pending || this.code.invalid) {
      this.code.markAsTouched();
      return;
    }
    this.sending.set(true);
    this.session
      .signIn(pending.signInHref, pending.email, this.code.value)
      .pipe(switchMap(() => this.session.refresh()))
      .subscribe({
        next: () => {
          clearPendingCode();
          void this.router.navigate(['/welcome']);
        },
        error: (error: unknown) => {
          this.sending.set(false);
          this.show(problemOf(error));
        },
      });
  }

  protected resend(): void {
    const pending = this.pending();
    if (!pending) {
      return;
    }
    this.message.set(undefined);
    this.resent.set(false);
    this.sending.set(true);
    this.session.requestCode(pending.name, pending.typedEmail).subscribe({
      next: (requested) => {
        this.update({
          ...pending,
          email: requested.email,
          validityMinutes: requested.validityMinutes,
          resendAt: Date.now() + requested.resendAfterSeconds * 1000,
          signInHref: requested.signInHref,
        });
        this.sending.set(false);
        this.codeExpired.set(false);
        this.code.reset();
        this.resent.set(true);
      },
      error: (error: unknown) => {
        this.sending.set(false);
        const problem = problemOf(error);
        if (problemSlug(problem) === 'code-resend-too-soon' && problem?.retryAfterSeconds) {
          this.update({ ...pending, resendAt: Date.now() + problem.retryAfterSeconds * 1000 });
        }
        this.message.set(problemMessage(problem) ?? unexpectedErrorMessage());
      },
    });
  }

  /** Back to the form, filled with the name and e-mail of the pending code. */
  protected correctEmail(): void {
    void this.router.navigate(['/participate']);
  }

  private update(pending: PendingCode): void {
    savePendingCode(pending);
    this.pending.set(pending);
    this.now.set(Date.now());
  }

  private show(problem: Problem | undefined): void {
    switch (problemSlug(problem)) {
      case 'invalid-request':
        this.code.setErrors({ required: true });
        this.code.markAsTouched();
        return;
      case 'code-expired':
        this.codeExpired.set(true);
        break;
    }
    this.message.set(problemMessage(problem) ?? unexpectedErrorMessage());
  }
}
