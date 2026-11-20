import { Component, inject, OnInit, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConsentForm, OnboardingService } from '../core/onboarding.service';
import { problemMessage, problemOf, unexpectedErrorMessage } from '../core/problem-messages';
import { ToastService } from '../core/toast.service';
import { switchMap } from 'rxjs';

/** The pt-BR reference text of the consent form; the rest of the page stays in the interface language (spec 006, US4). */
@Component({
  selector: 'app-consent-reference',
  imports: [MatProgressSpinnerModule],
  template: `
    <section class="reference">
      <h1 id="reference-heading" i18n="@@consent.reference.title">Reference text (Portuguese, Brazil)</h1>
      @if (reference(); as current) {
        <div class="text" role="region" aria-labelledby="reference-heading" tabindex="0" lang="pt-BR">{{ current.text }}</div>
      } @else {
        <mat-spinner diameter="40"></mat-spinner>
      }
    </section>
  `,
  styles: `
    .reference {
      display: grid;
      gap: 1rem;
      padding: 1rem 0;
    }
    .text {
      white-space: pre-line;
      padding: 1rem;
      border: 1px solid var(--mat-sys-outline-variant, #c4c7c5);
      border-radius: 0.5rem;
    }
    .error {
      color: var(--mat-sys-error, #b3261e);
    }
  `,
})
export class ConsentReferenceComponent implements OnInit {
  private readonly onboarding = inject(OnboardingService);
  private readonly toast = inject(ToastService);

  readonly reference = signal<ConsentForm | null>(null);

  ngOnInit(): void {
    this.onboarding
      .consentForm()
      .pipe(switchMap((form) => this.onboarding.referenceText(form)))
      .subscribe({
        next: (reference) => this.reference.set(reference),
        error: (error: unknown) => this.toast.error(problemMessage(problemOf(error)) ?? unexpectedErrorMessage()),
      });
  }
}
