import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { problemMessage, problemOf, problemSlug, unexpectedErrorMessage } from '../core/problem-messages';
import { ToastService } from '../core/toast.service';
import { ConsentChoice, ConsentForm, NoConsentFormError, OnboardingService } from '../core/onboarding.service';
import { HttpErrorResponse } from '@angular/common/http';

/** Acknowledgment preference on consent: list name vs anonymous bucket (spec 027). */
type AcknowledgmentChoice = 'named' | 'anonymous';

/** First step: the current consent form in the interface language, accepted or refused (spec 006, US1 and US4). */
@Component({
  selector: 'app-consent',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatRadioModule, RouterLink],
  template: `
    <section class="consent">
      <h1 id="consent-heading" class="page-title">
        <mat-icon class="page-title-icon" fontSet="material-symbols-outlined" aria-hidden="true">contract</mat-icon>
        <span i18n="@@consent.title">Consent form</span>
      </h1>
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
        <mat-radio-group
          class="ack-choice"
          aria-labelledby="ack-legend"
          [value]="acknowledgmentChoice()"
          (change)="acknowledgmentChoice.set($event.value)"
        >
          <span id="ack-legend" class="legend" i18n="@@consent.acknowledgmentLegend"
            >Acknowledgments in the article and/or doctoral thesis</span
          >
          <mat-radio-button value="named" i18n="@@consent.acknowledgmentNamed">
            Yes, I want my first and last names to appear in the acknowledgments of the author’s article and/or doctoral thesis.
          </mat-radio-button>
          <mat-radio-button value="anonymous" i18n="@@consent.acknowledgmentAnonymous">
            No, I want to remain anonymous in the acknowledgments of the author’s article and/or doctoral thesis. In that case the system includes me in the collective mention “Anonymized labelers by request [N]” (N = number of such requests), without requiring manual anonymization by the administrator.
          </mat-radio-button>
        </mat-radio-group>
        <div class="actions">
          <button
            mat-flat-button
            color="primary"
            type="button"
            class="accept"
            [disabled]="saving() || acknowledgmentChoice() === null"
            (click)="decide('accepted')"
          >
            <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">check</mat-icon>
            <span i18n="@@consent.accept">I have read it and agree to participate</span>
          </button>
          <button
            mat-stroked-button
            type="button"
            class="refuse"
            [disabled]="saving()"
            (click)="decide('refused')"
          >
            <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">close</mat-icon>
            <span i18n="@@consent.refuse">I do not agree</span>
          </button>
        </div>
      } @else {
        <button mat-flat-button type="button" (click)="load()">
          <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">refresh</mat-icon>
          <span i18n="@@consent.retry">Try again</span>
        </button>
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
    .ack-choice {
      display: grid;
      gap: 0.75rem;
      max-width: 42rem;
    }
    .legend {
      font-weight: 600;
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
  readonly acknowledgmentChoice = signal<AcknowledgmentChoice | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.form.set(null);
    this.acknowledgmentChoice.set(null);
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
    if (decision === 'accepted' && this.acknowledgmentChoice() === null) {
      return;
    }
    this.saving.set(true);
    const anonymous = decision === 'accepted' && this.acknowledgmentChoice() === 'anonymous';
    this.onboarding.recordConsent(decision, form.version, anonymous).subscribe({
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
