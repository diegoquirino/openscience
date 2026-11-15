import { Component, DestroyRef, Injector, computed, inject, runInInjectionContext, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { Subscription, map, switchMap } from 'rxjs';
import { LanguageService } from '../../core/language.service';
import {
  CHANGE_IMPACTS,
  ChangeImpact,
  MAINTENANCE_OPERATIONS,
  MaintenanceOperation,
  changeImpactLabel,
  isChangeImpact,
  isMaintenanceOperation,
  maintenanceOperationLabel,
} from '../../core/labels/response-labels';
import { Links } from '../../core/hal';
import { problemMessage, problemOf, problemSlug, unexpectedErrorMessage } from '../../core/problem-messages';
import { formatCountdown, remainingSeconds } from '../countdown';
import { ItemBody, LabelService, NoActiveTaskError } from '../label.service';

export const COMMENT_MAX_LENGTH = 1000;

/** Key of the unsaved choices of one item (FR-016, FR-078): task and position. */
export function unsavedKey(taskId: number, position: number): string {
  return `label-item:${taskId}:${position}`;
}

/** Simple item screen of spec 007 (replaced by the full screen of spec 008 later). */
@Component({
  selector: 'app-label-item',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    RouterLink,
  ],
  templateUrl: './item.component.html',
  styleUrl: './item.component.scss',
})
export class LabelItemComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labels = inject(LabelService);
  private readonly language = inject(LanguageService);
  private readonly injector = inject(Injector);
  private loading: Subscription | undefined;

  protected readonly commentMax = COMMENT_MAX_LENGTH;
  protected readonly impacts = CHANGE_IMPACTS;
  protected readonly operations = MAINTENANCE_OPERATIONS;
  protected readonly impactLabel = changeImpactLabel;
  protected readonly operationLabel = maintenanceOperationLabel;

  protected readonly busy = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | undefined>(undefined);
  protected readonly blockLost = signal(false);
  protected readonly item = signal<ItemBody | null>(null);
  protected readonly impact = signal<ChangeImpact | null>(null);
  protected readonly operation = signal<MaintenanceOperation | null>(null);
  protected readonly comment = signal('');
  private readonly now = signal(Date.now());

  protected readonly task = this.labels.task;
  protected readonly position = computed(() => this.item()?.position ?? 0);
  protected readonly size = computed(() => this.task()?.size ?? 0);
  protected readonly remaining = computed(() =>
    remainingSeconds(this.task()?.reservationExpiresAt, this.labels.clockOffsetMs(), this.now()),
  );
  protected readonly countdown = computed(() => {
    const seconds = this.remaining();
    return seconds === null ? '' : formatCountdown(seconds);
  });
  protected readonly expired = computed(() => this.remaining() === 0);
  protected readonly canContinue = computed(
    () =>
      this.item() !== null &&
      this.impact() !== null &&
      this.operation() !== null &&
      this.comment().length <= COMMENT_MAX_LENGTH &&
      !this.saving(),
  );

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(timer);
      this.loading?.unsubscribe();
    });
    this.route.paramMap
      .pipe(
        map((params) => Number(params.get('position'))),
        takeUntilDestroyed(),
      )
      .subscribe((position) => this.load(position));
  }

  protected load(position: number): void {
    this.loading?.unsubscribe();
    this.busy.set(true);
    this.message.set(undefined);
    this.item.set(null);
    if (!Number.isInteger(position) || position < 1) {
      void this.router.navigate(['/label'], { replaceUrl: true });
      return;
    }
    this.loading = this.labels
      .currentTask()
      .pipe(switchMap((task) => this.labels.getItem(task.id, position)))
      .subscribe({
        next: (item) => {
          this.show(item);
          this.busy.set(false);
        },
        error: (error: unknown) => {
          this.busy.set(false);
          if (error instanceof NoActiveTaskError || (error instanceof HttpErrorResponse && error.status === 404)) {
            void this.router.navigate(['/label'], { replaceUrl: true });
            return;
          }
          this.message.set(this.messageOf(error));
        },
      });
  }

  protected setImpact(value: unknown): void {
    this.impact.set(isChangeImpact(value) ? value : null);
  }

  protected setOperation(value: unknown): void {
    this.operation.set(isMaintenanceOperation(value) ? value : null);
  }

  protected onComment(event: Event): void {
    this.comment.set((event.target as HTMLTextAreaElement).value);
  }

  protected previousPosition(): number | null {
    return this.position() > 1 ? this.position() - 1 : null;
  }

  protected retry(): void {
    this.load(this.position() || Number(this.route.snapshot.paramMap.get('position')));
  }

  protected submit(): void {
    const item = this.item();
    const impact = this.impact();
    const operation = this.operation();
    if (!item || !impact || !operation || !this.canContinue()) {
      return;
    }
    const href = Links.of(item).href('save-response');
    if (!href) {
      this.message.set(unexpectedErrorMessage());
      return;
    }
    this.saving.set(true);
    this.message.set(undefined);
    this.blockLost.set(false);
    const position = item.position;
    this.labels
      .saveResponse(href, {
        changeImpact: impact,
        maintenanceOperation: operation,
        comment: this.comment().length > 0 ? this.comment() : null,
      })
      .pipe(
        switchMap((task) => {
          this.language.clearUnsaved(unsavedKey(task.id, position));
          return this.labels.nextPending(task, position);
        }),
      )
      .subscribe({
        next: (next) => {
          this.saving.set(false);
          void this.router.navigate(next === null ? ['/label/review'] : ['/label/items', next]);
        },
        error: (error: unknown) => {
          this.saving.set(false);
          // The block is gone (given to someone else, or this person holds another one): offer a new block.
          this.blockLost.set(
            problemSlug(problemOf(error)) === 'reservation-expired' ||
              (error instanceof HttpErrorResponse && error.status === 404),
          );
          this.message.set(this.messageOf(error));
        },
      });
  }

  private show(item: ItemBody): void {
    this.item.set(item);
    const taskId = this.labels.task()?.id ?? 0;
    const key = unsavedKey(taskId, item.position);
    const unsaved = this.language.takeUnsaved(key);
    const impact = unsaved ? unsaved['impact'] : item.draft?.changeImpact;
    const operation = unsaved ? unsaved['operation'] : item.draft?.maintenanceOperation;
    this.impact.set(isChangeImpact(impact) ? impact : null);
    this.operation.set(isMaintenanceOperation(operation) ? operation : null);
    this.comment.set((unsaved ? unsaved['comment'] : item.draft?.comment) ?? '');
    runInInjectionContext(this.injector, () =>
      this.language.keepOnSwitch(key, () => ({
        impact: this.impact() ?? '',
        operation: this.operation() ?? '',
        comment: this.comment(),
      })),
    );
  }

  private messageOf(error: unknown): string {
    return problemMessage(problemOf(error)) ?? unexpectedErrorMessage();
  }
}
