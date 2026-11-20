import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OnboardingService, routeOfStep } from '../core/onboarding.service';
import { unexpectedErrorMessage } from '../core/problem-messages';
import { ToastService } from '../core/toast.service';

/**
 * Participant landing of spec 006: no content of its own. It follows `vd:onboarding` and opens the
 * screen of the first pending step (consent, skill, screening, labeling, or a closing screen).
 */
@Component({
  selector: 'app-welcome',
  imports: [MatButtonModule, MatProgressSpinnerModule],
  template: `
    <section class="welcome">
      @if (failed()) {
        <button mat-flat-button type="button" (click)="resolve()" i18n="@@onboarding.retry">Try again</button>
      } @else {
        <mat-spinner diameter="40"></mat-spinner>
      }
    </section>
  `,
  styles: `
    .welcome {
      display: grid;
      gap: 1rem;
      justify-items: start;
      padding: 1.5rem 0;
    }
  `,
})
export class WelcomeComponent implements OnInit {
  private readonly onboarding = inject(OnboardingService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly failed = signal(false);

  ngOnInit(): void {
    this.resolve();
  }

  resolve(): void {
    this.failed.set(false);
    this.onboarding.state().subscribe({
      next: (state) => void this.router.navigateByUrl(routeOfStep(state.step), { replaceUrl: true }),
      error: () => {
        this.failed.set(true);
        this.toast.error(unexpectedErrorMessage());
      },
    });
  }
}
