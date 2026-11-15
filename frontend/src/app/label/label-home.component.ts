import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LabelService, TaskBody } from './label.service';

/** Resolves the current reservation and routes to the item screen or the done screen. */
@Component({
  selector: 'app-label-home',
  imports: [MatButtonModule, MatProgressSpinnerModule],
  template: `
    <section class="label-home">
      @if (busy()) {
        <mat-spinner diameter="40"></mat-spinner>
      } @else if (error()) {
        <p class="error">{{ error() }}</p>
        <button mat-flat-button type="button" (click)="start()" i18n="@@label.retry">Try again</button>
      }
    </section>
  `,
  styles: `
    .label-home {
      display: grid;
      gap: 1rem;
      justify-items: start;
      padding: 1.5rem 0;
    }
    .error {
      color: var(--mat-sys-error, #b3261e);
    }
  `,
})
export class LabelHomeComponent implements OnInit {
  private readonly labels = inject(LabelService);
  private readonly router = inject(Router);

  readonly busy = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.start();
  }

  start(): void {
    this.busy.set(true);
    this.error.set(null);
    this.labels.reserve().subscribe({
      next: (body) => {
        if (this.labels.isTask(body)) {
          this.resume(body);
        } else {
          this.busy.set(false);
          void this.router.navigate(['/label/done'], {
            replaceUrl: true,
            queryParams: { outcome: body.outcome },
          });
        }
      },
      error: () => {
        this.busy.set(false);
        this.error.set($localize`:@@label.reserveFailed:Could not request a block. Please try again.`);
      },
    });
  }

  /** Opens the first item without a draft, or the review when every item has one. */
  private resume(task: TaskBody): void {
    this.labels.nextPending(task, 0).subscribe({
      next: (position) => {
        this.busy.set(false);
        void this.router.navigate(position === null ? ['/label/review'] : ['/label/items', position], {
          replaceUrl: true,
        });
      },
      error: () => {
        this.busy.set(false);
        this.error.set($localize`:@@label.reserveFailed:Could not request a block. Please try again.`);
      },
    });
  }
}
