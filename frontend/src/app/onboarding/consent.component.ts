import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { problemMessage, problemOf, problemSlug, unexpectedErrorMessage } from '../core/problem-messages';
import { ToastService } from '../core/toast.service';
import { ConsentChoice, ConsentForm, NoConsentFormError, OnboardingService } from '../core/onboarding.service';
import { HttpErrorResponse } from '@angular/common/http';

/** First step: the current consent form in the interface language, accepted or refused (spec 006, US1 and US4). */
@Component({
  selector: 'app-consent',
  imports: [MatButtonModule, MatCheckboxModule, MatProgressSpinnerModule, RouterLink],
  template: `
    <section class="consent">
      <h1 id="consent-heading" i18n="@@consent.title">Consent form</h1>
      @if (loading()) {
        <mat-spinner diameter="40"></mat-spinner>
      } @else if (form(); as current) {
        @if (notice()) {
          <p class="notice" role="status" i18n="@@consent.changed">
            The consent form changed. Read the new version before you decide.
          </p>
        }
        @if (current.translation) {
          <p class="translation" role="note">
            <span i18n="@@consent.translationNote"
              >This text is a translation. The reference version, approved by the research ethics committee, is the Portuguese (Brazil) one.</span
            >
            <a
              class="reference"
              [routerLink]="['/consent/reference', current.version]"
              target="_blank"
              rel="noopener"
              i18n="@@consent.referenceLink"
              >Read the reference text (pt-BR)</a
            >
          </p>
        }
        <div class="text" role="region" aria-labelledby="consent-heading" tabindex="0" [attr.lang]="current.language">{{ current.text }}</div>
        <mat-checkbox
          class="ack-anon"
          [checked]="acknowledgmentAnonymous()"
          (change)="acknowledgmentAnonymous.set($event.checked)"
          i18n="@@consent.acknowledgmentAnonymous"
        >
          I want to remain anonymous in paper acknowledgments (listed as “Anonymized labelers by request [N]” instead of my first and last name)
        </mat-checkbox>
        <div class="actions">
          <button
            mat-flat-button
            color="primary"
            type="button"
            class="accept"
            [disabled]="saving()"
            (click)="decide('accepted')"
            i18n="@@consent.accept"
          >
            I have read it and agree to participate
          </button>
          <button
            mat-stroked-button
            type="button"
            class="refuse"
            [disabled]="saving()"
            (click)="decide('refused')"
            i18n="@@consent.refuse"
          >
            I do not agree
          </button>
        </div>
      } @else {
        <button mat-flat-button type="button" (click)="load()" i18n="@@consent.retry">Try again</button>
      }
    </section>
  `,
  styles: `
    .consent {
      display: grid;
      gap: 1rem;
      padding: 1rem 0 0;
    }
    .text {
      white-space: pre-line;
      overflow-y: auto;
      max-height: calc(100vh - 22rem);
      min-height: 8rem;
      padding: 1rem;
      border: 1px solid var(--mat-sys-outline-variant, #c4c7c5);
      border-radius: 0.5rem;
    }
    .ack-anon {
      max-width: 40rem;
    }
    .translation,
    .notice {
      margin: 0;
    }
    .reference {
      margin-left: 0.5rem;
    }
    .error {
      color: var(--mat-sys-error, #b3261e);
      margin: 0;
    }
    .actions {
      position: sticky;
      bottom: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      padding: 0.75rem 0;
      background: var(--mat-sys-surface, #fff);
    }
  `,
})
export class ConsentComponent implements OnInit {
  private readonly onboarding = inject(OnboardingService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly form = signal<ConsentForm | null>(null);
  readonly notice = signal(false);
  readonly acknowledgmentAnonymous = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.form.set(null);
    this.onboarding.consentForm().subscribe({
      next: (form) => {
        this.form.set(form);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        if (error instanceof NoConsentFormError || (error instanceof HttpErrorResponse && error.status === 404)) {
          void this.router.navigateByUrl('/not-open', { replaceUrl: true });
          return;
        }
        this.toast.error(problemMessage(problemOf(error)) ?? unexpectedErrorMessage());
      },
    });
  }

  /** Records the decision on the text shown; the version sent is the one on screen. */
  decide(decision: ConsentChoice): void {
    const form = this.form();
    if (!form || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.onboarding
      .recordConsent(decision, form.version, decision === 'accepted' ? this.acknowledgmentAnonymous() : false)
      .subscribe({
        next: () => {
          this.saving.set(false);
          void this.router.navigateByUrl(decision === 'accepted' ? '/welcome' : '/consent/declined');
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.fail(error);
        },
      });
  }

  private fail(error: unknown): void {
    const problem = problemOf(error);
    switch (problemSlug(problem)) {
      case 'consent-form-outdated':
        this.notice.set(true);
        this.load();
        return;
      case 'study-not-open':
        void this.router.navigateByUrl('/not-open', { replaceUrl: true });
        return;
      case 'step-not-pending':
        void this.router.navigateByUrl('/welcome', { replaceUrl: true });
        return;
      default:
        this.toast.error(problemMessage(problem) ?? unexpectedErrorMessage());
    }
  }
}
