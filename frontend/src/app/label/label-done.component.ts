import { Component, inject, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { SessionService } from '../core/session.service';

/** End screen of the labeling loop; the text and the actions come from the `outcome` (spec 007, US4; 023). */
@Component({
  selector: 'app-label-done',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
    <section class="done">
      <h1 i18n="@@label.done.title">Thank you</h1>
      <div class="page-panel">
        <p class="page-intro">{{ message() }}</p>
        @if (outcome() === 'cancelled') {
          <nav class="action-grid" aria-label="Next steps after cancelling" i18n-aria-label="@@label.done.cancelMenu">
            <a class="action-card" routerLink="/label" data-testid="done-another-block">
              <mat-icon class="action-card-icon" fontSet="material-symbols-outlined" aria-hidden="true"
                >edit_note</mat-icon
              >
              <span class="action-card-label" i18n="@@label.another">Label another block</span>
            </a>
            <a class="action-card" routerLink="/" data-testid="done-leave">
              <mat-icon class="action-card-icon" fontSet="material-symbols-outlined" aria-hidden="true">home</mat-icon>
              <span class="action-card-label" i18n="@@label.done.leave">Leave</span>
            </a>
          </nav>
        } @else {
          <div class="actions">
            @if (outcome() === 'more-available') {
              <a mat-flat-button class="another" routerLink="/label" i18n="@@label.another">Label another block</a>
            }
            <button mat-stroked-button type="button" class="sign-out" (click)="signOut()" i18n="@@shell.signOut">
              Sign out
            </button>
          </div>
        }
      </div>
    </section>
  `,
  styles: `
    .done {
      display: grid;
      gap: 1rem;
      padding: 1.5rem 0;
      justify-items: start;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
  `,
})
export class LabelDoneComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  readonly outcome = toSignal(this.route.queryParamMap.pipe(map((p) => p.get('outcome') ?? '')), {
    initialValue: '',
  });

  readonly message = computed(() => {
    switch (this.outcome()) {
      case 'no-items-yet':
        return $localize`:@@label.end.noItemsYet:No items to label yet.`;
      case 'database-complete':
        return $localize`:@@label.end.databaseComplete:The database has been completely answered.`;
      case 'none-available-for-you':
        return $localize`:@@label.end.noneForYou:There are no more blocks for you at the moment.`;
      case 'more-available':
        return $localize`:@@label.end.moreAvailable:You can label another block.`;
      case 'cancelled':
        return $localize`:@@label.end.cancelled:You cancelled this block. You can request another one or leave.`;
      default:
        return $localize`:@@label.end.thanks:Thank you for labeling.`;
    }
  });

  /** Same as the shell: the session is gone locally even if the call fails. */
  protected signOut(): void {
    this.session.signOut().subscribe(() => void this.router.navigateByUrl('/'));
  }
}
