import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { switchMap } from 'rxjs';
import { changeImpactLabel, maintenanceOperationLabel } from '../../core/labels/response-labels';
import { problemMessage, problemOf, problemSlug, unexpectedErrorMessage } from '../../core/problem-messages';
import { ToastService } from '../../core/toast.service';
import { ItemBody, LabelService, NoActiveTaskError, TaskBody } from '../label.service';

/** Review of the held block (spec 007, US3): every answer, edit links, and Submit. */
@Component({
  selector: 'app-label-review',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, RouterLink],
  template: `
    <section class="review">
      <h1 class="page-title">
        <mat-icon class="page-title-icon" fontSet="material-symbols-outlined" aria-hidden="true">rate_review</mat-icon>
        <span i18n="@@label.review.title">Review</span>
      </h1>
      @if (busy()) {
        <mat-spinner diameter="40"></mat-spinner>
      } @else if (task()) {
        <p i18n="@@label.review.intro">Check your answers. Open an item to change it, then submit the block.</p>

        <ol class="items">
          @for (item of items(); track item.position) {
            <li class="item" [class.pending]="item.draft === null">
              <div class="head">
                <strong i18n="@@label.review.item">Item {{ item.position }} of {{ items().length }}</strong>
                <span class="where">{{ item.system }} · {{ item.uc }} · {{ item.field }}</span>
                <a mat-stroked-button class="edit" [routerLink]="['/label/items', item.position]">
                  <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">edit</mat-icon>
                  <span i18n="@@label.review.edit">Edit</span>
                </a>
              </div>
              @if (item.draft; as draft) {
                <dl>
                  <div>
                    <dt i18n="@@label.review.impact">Impact</dt>
                    <dd class="impact">{{ impactLabel(draft.changeImpact ?? '') }}</dd>
                  </div>
                  <div>
                    <dt i18n="@@label.review.operation">Operation</dt>
                    <dd class="operation">{{ operationLabel(draft.maintenanceOperation ?? '') }}</dd>
                  </div>
                  <div>
                    <dt i18n="@@label.review.comment">Comment</dt>
                    <dd class="comment">{{ draft.comment || '—' }}</dd>
                  </div>
                </dl>
              } @else {
                <p class="not-answered" i18n="@@label.review.pending">Not answered yet</p>
              }
            </li>
          }
        </ol>

        @if (expired()) {
          <a mat-stroked-button routerLink="/label">
            <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">refresh</mat-icon>
            <span i18n="@@label.retry">Try again</span>
          </a>
        }
        @if (firstPending(); as pending) {
          <p class="hint" i18n="@@label.review.incomplete">Answer every item before submitting the block.</p>
          <div class="actions">
            <a mat-stroked-button [routerLink]="['/label/items', pending]">
              <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">arrow_forward</mat-icon>
              <span i18n="@@label.review.firstPending">Go to the first pending item</span>
            </a>
            <button mat-flat-button type="button" class="submit" disabled>
              <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">send</mat-icon>
              <span i18n="@@label.review.submit">Submit block</span>
            </button>
          </div>
        } @else {
          <div class="actions">
            <button
              mat-flat-button
              type="button"
              class="submit"
              [disabled]="submitting() || expired()"
              (click)="submit()"
            >
              <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">send</mat-icon>
              <span i18n="@@label.review.submit">Submit block</span>
            </button>
            @if (submitting()) {
              <mat-spinner diameter="24"></mat-spinner>
            }
          </div>
        }
      } @else {
        <button mat-flat-button type="button" (click)="load()">
          <mat-icon fontSet="material-symbols-outlined" aria-hidden="true">refresh</mat-icon>
          <span i18n="@@label.retry">Try again</span>
        </button>
      }
    </section>
  `,
  styles: `
    .review {
      display: grid;
      gap: 1rem;
      justify-items: start;
      padding: 1.5rem 0;
    }
    .items {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 0.75rem;
      width: 100%;
    }
    .item {
      border: 1px solid var(--mat-sys-outline-variant, #ccc);
      border-radius: 8px;
      padding: 0.75rem 1rem;
    }
    .item.pending {
      border-color: var(--mat-sys-error, #b3261e);
    }
    .head {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.75rem;
    }
    .where {
      flex: 1;
      opacity: 0.8;
    }
    dl {
      display: grid;
      gap: 0.25rem;
      margin: 0.5rem 0 0;
    }
    dt {
      font-weight: 600;
    }
    dd {
      margin: 0;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
    }
    .not-answered,
    .error {
      color: var(--mat-sys-error, #b3261e);
      margin: 0.5rem 0 0;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.75rem;
    }
  `,
})
export class LabelReviewComponent implements OnInit {
  private readonly labels = inject(LabelService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly impactLabel = changeImpactLabel;
  protected readonly operationLabel = maintenanceOperationLabel;

  protected readonly busy = signal(true);
  protected readonly submitting = signal(false);
  protected readonly expired = signal(false);
  protected readonly task = signal<TaskBody | null>(null);
  protected readonly items = signal<ItemBody[]>([]);
  /** Position of the first item without a draft, or `null` when every item has one. */
  protected readonly firstPending = computed(() => this.items().find((item) => item.draft === null)?.position ?? null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.busy.set(true);
        this.expired.set(false);
    this.task.set(null);
    this.labels
      .currentTask()
      .pipe(switchMap((current) => this.labels.loadDraftsForReview(current.id)))
      .subscribe({
        next: ({ task, items }) => {
          this.task.set(task);
          this.items.set(items);
          this.busy.set(false);
        },
        error: (error: unknown) => this.failLoading(error),
      });
  }

  protected submit(): void {
    const task = this.task();
    if (!task || this.submitting()) {
      return;
    }
    this.submitting.set(true);
        this.labels.submit(task).subscribe({
      next: (done) => {
        this.submitting.set(false);
        void this.router.navigate(['/label/done'], {
          queryParams: done.outcome ? { outcome: done.outcome } : {},
        });
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const problem = problemOf(error);
        if (problemSlug(problem) === 'block-incomplete') {
          void this.router.navigate(['/label/items', problem?.firstPendingPosition ?? this.firstPending() ?? 1]);
          return;
        }
        this.expired.set(problemSlug(problem) === 'reservation-expired');
        this.toast.error(problemMessage(problem) ?? unexpectedErrorMessage());
      },
    });
  }

  private failLoading(error: unknown): void {
    this.busy.set(false);
    if (error instanceof NoActiveTaskError || (error instanceof HttpErrorResponse && error.status === 404)) {
      void this.router.navigate(['/label'], { replaceUrl: true });
      return;
    }
    this.toast.error(problemMessage(problemOf(error)) ?? unexpectedErrorMessage());
  }
}
