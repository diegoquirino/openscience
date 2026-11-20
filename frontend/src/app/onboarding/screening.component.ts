import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { LanguageService } from '../core/language.service';
import { isScreeningAnswer, SCREENING_ANSWERS, screeningAnswerLabel, screeningQuestion } from '../core/labels/screening-labels';
import { OnboardingService, ScreeningExample } from '../core/onboarding.service';
import { problemMessage, problemOf, problemSlug, unexpectedErrorMessage } from '../core/problem-messages';
import { ToastService } from '../core/toast.service';

/** Route key of the answer kept across a language switch (`veredito-unsaved:/screening`). */
export const SCREENING_UNSAVED_KEY = '/screening';

/**
 * Third step: the English use case and test case, the question and its three answers. The example
 * is study data, English in every language (spec 006, US3).
 */
@Component({
  selector: 'app-screening',
  imports: [MatButtonModule, MatProgressSpinnerModule, MatRadioModule],
  template: `
    <section class="screening">
      <h1 i18n="@@screening.title">Screening</h1>
      <p i18n="@@screening.intro">
        Read the example below (it is shown in English for every participant), then answer the question.
      </p>
      @if (example(); as sample) {
        <div class="cards" lang="en" translate="no">
          <article class="card use-case">
            <h2>{{ sample.useCase.id }} - {{ sample.useCase.title }}</h2>
            <ol>
              @for (step of sample.useCase.basicFlow; track $index) {
                <li>{{ step }}</li>
              }
            </ol>
            <h3>{{ sample.useCase.exceptionFlow.id }}: {{ sample.useCase.exceptionFlow.condition }}</h3>
            <ol>
              @for (step of sample.useCase.exceptionFlow.steps; track $index) {
                <li>{{ step }}</li>
              }
            </ol>
          </article>
          <article class="card test-case">
            <h2>{{ sample.testCase.id }} - {{ sample.testCase.title }}</h2>
            <p>{{ sample.testCase.derivedFrom.join(', ') }}</p>
            <ol>
              @for (step of sample.testCase.steps; track $index) {
                <li>{{ step }}</li>
              }
            </ol>
            <p>{{ sample.testCase.expected }}</p>
          </article>
        </div>
        <mat-radio-group
          class="answers"
          aria-labelledby="screening-question"
          [value]="answer()"
          (change)="answer.set($event.value)"
        >
          <span id="screening-question" class="question">{{ question }}</span>
          @for (code of codes; track code) {
            <mat-radio-button [value]="code">{{ label(code) }}</mat-radio-button>
          }
        </mat-radio-group>
                <div class="actions">
          <button
            mat-flat-button
            color="primary"
            type="button"
            class="continue"
            [disabled]="!valid() || saving()"
            (click)="submit()"
            i18n="@@screening.continue"
          >
            Continue
          </button>
        </div>
      } @else if (failed()) {
        <button mat-flat-button type="button" (click)="load()" i18n="@@screening.retry">Try again</button>
      } @else {
        <mat-spinner diameter="40"></mat-spinner>
      }
    </section>
  `,
  styles: `
    .screening {
      display: grid;
      gap: 1rem;
      padding: 1rem 0;
    }
    .cards {
      display: grid;
      gap: 1rem;
      grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
    }
    .card {
      padding: 1rem;
      border: 1px solid var(--mat-sys-outline-variant, #c4c7c5);
      border-radius: 0.5rem;
    }
    .card h2 {
      font-size: 1.1rem;
      margin: 0 0 0.5rem;
    }
    .card h3 {
      font-size: 1rem;
    }
    .answers {
      display: grid;
    }
    .question {
      font-weight: 600;
    }
    .error {
      color: var(--mat-sys-error, #b3261e);
      margin: 0;
    }
  `,
})
export class ScreeningComponent implements OnInit {
  private readonly onboarding = inject(OnboardingService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly language = inject(LanguageService);

  protected readonly codes = SCREENING_ANSWERS;
  protected readonly question = screeningQuestion();

  readonly example = signal<ScreeningExample | null>(null);
  readonly answer = signal<string | null>(null);
  readonly saving = signal(false);
  readonly failed = signal(false);
  readonly valid = computed(() => isScreeningAnswer(this.answer()));

  constructor() {
    const kept = this.language.takeUnsaved(SCREENING_UNSAVED_KEY);
    if (kept && isScreeningAnswer(kept['answer'])) {
      this.answer.set(kept['answer']);
    }
    this.language.keepOnSwitch(SCREENING_UNSAVED_KEY, () => ({ answer: this.answer() ?? '' }));
  }

  ngOnInit(): void {
    this.load();
  }

  protected label(code: string): string {
    return screeningAnswerLabel(code);
  }

  load(): void {
    this.failed.set(false);
    this.example.set(null);
    this.onboarding.screeningExample().subscribe({
      next: (example) => this.example.set(example),
      error: (error: unknown) => {
        this.failed.set(true);
        this.toast.error(problemMessage(problemOf(error)) ?? unexpectedErrorMessage());
      },
    });
  }

  submit(): void {
    const answer = this.answer();
    if (!isScreeningAnswer(answer) || this.saving()) {
      return;
    }
    this.saving.set(true);
        this.onboarding.recordScreening(answer).subscribe({
      next: () => {
        this.language.clearUnsaved(SCREENING_UNSAVED_KEY);
        void this.router.navigateByUrl('/welcome');
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const problem = problemOf(error);
        if (problemSlug(problem) === 'step-not-pending') {
          void this.router.navigateByUrl('/welcome', { replaceUrl: true });
          return;
        }
        this.toast.error(problemMessage(problem) ?? unexpectedErrorMessage());
      },
    });
  }
}
