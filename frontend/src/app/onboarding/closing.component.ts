import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { SessionService } from '../core/session.service';

/** The three ways the onboarding can end: a refusal, a "No" in the screening, a study not open yet. */
export type ClosingKind = 'declined' | 'ineligible' | 'not-open';

/**
 * Closing screens of spec 006 (contracts/web.md "Closing screens"): thanks and "Sign out", never a link
 * to the labeling. After a refusal the way back to the consent form is offered. The kind comes from
 * the route data.
 */
@Component({
  selector: 'app-closing',
  imports: [MatButtonModule, RouterLink],
  template: `
    <section class="closing">
      @switch (kind()) {
        @case ('declined') {
          <h1 i18n="@@consent.declined.title">Thank you for considering the study</h1>
          <p i18n="@@consent.declined.text">
            No labeling data is recorded without your consent. You can read the consent form again if you change your mind.
          </p>
        }
        @case ('ineligible') {
          <h1 i18n="@@screening.closed.title">Thank you for your interest</h1>
          <p i18n="@@screening.closed.text">
            This study requires experience with use cases and test cases, so your participation ends here.
          </p>
        }
        @default {
          <h1 i18n="@@notOpen.title">The study is not open yet</h1>
          <p i18n="@@notOpen.text">Please come back later.</p>
        }
      }
      <div class="actions">
        @if (kind() === 'declined') {
          <a mat-flat-button class="again" routerLink="/consent" i18n="@@consent.declined.again">Read the consent form again</a>
        }
        <button mat-stroked-button type="button" class="sign-out" (click)="signOut()" i18n="@@shell.signOut">
          Sign out
        </button>
      </div>
    </section>
  `,
  styles: `
    .closing {
      display: grid;
      gap: 1rem;
      justify-items: start;
      padding: 1.5rem 0;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
  `,
})
export class ClosingComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly kind = computed<ClosingKind>(() => {
    const value: unknown = this.route.snapshot.data['kind'];
    return value === 'declined' || value === 'ineligible' ? value : 'not-open';
  });

  /** Same as the shell: the session is gone locally even if the call fails. */
  protected signOut(): void {
    this.session.signOut().subscribe(() => void this.router.navigateByUrl('/'));
  }
}
