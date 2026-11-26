import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, DestroyRef, Injector, computed, inject, runInInjectionContext, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
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
import { ToastService } from '../../core/toast.service';
import { ItemBody, LabelService, NoActiveTaskError } from '../label.service';
import { missingTestsLabel, missingUsecaseLabel } from '../../core/labels/item-labels';
import {
  OPERATION_PRECEDENCE,
  impactExplanation,
  impactStatement,
  operationExplanation,
  operationStatement,
  precedenceIntro,
} from '../../core/labels/question-texts';
import { ItemClock } from './item-clock';
import { ItemProgressComponent } from './item-progress.component';
import { buildItemLayout, buildSuiteLayout } from './item-layout';
import { UseCaseDialogComponent, UseCaseDialogData } from './usecase-dialog.component';

export const COMMENT_MAX_LENGTH = 1000;

/** Below this width the use case dialog takes the whole screen (contracts/web.md). */
export const NARROW_QUERY = '(max-width: 599.98px)';

const WIDE_DIALOG: MatDialogConfig = {
  width: '960px',
  maxWidth: 'calc(100vw - 32px)',
  height: '85vh',
};

const FULL_SCREEN_DIALOG: MatDialogConfig = {
  width: '100vw',
  maxWidth: '100vw',
  height: '100dvh',
  maxHeight: '100dvh',
  panelClass: 'usecase-dialog-full',
};

/** Key of the unsaved choices of one item (FR-016, FR-078): task and position. */
export function unsavedKey(taskId: number, position: number): string {
  return `label-item:${taskId}:${position}`;
}

/** Rich item screen of spec 008 (the simple screen of spec 007 stored the same codes and fields). */
@Component({
  selector: 'app-label-item',
  imports: [
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    RouterLink,
    ItemProgressComponent,
  ],
  providers: [ItemClock],
  templateUrl: './item.component.html',
  styleUrl: './item.component.scss',
})
export class LabelItemComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labels = inject(LabelService);
  private readonly toast = inject(ToastService);
  private readonly language = inject(LanguageService);
  private readonly injector = inject(Injector);
  private readonly dialog = inject(MatDialog);
  private readonly breakpoints = inject(BreakpointObserver);
  private loading: Subscription | undefined;

  protected readonly commentMax = COMMENT_MAX_LENGTH;
  protected readonly impacts = CHANGE_IMPACTS;
  protected readonly operations = MAINTENANCE_OPERATIONS;
  protected readonly impactLabel = changeImpactLabel;
  protected readonly operationLabel = maintenanceOperationLabel;
  protected readonly impactExplanation = impactExplanation;
  protected readonly operationExplanation = operationExplanation;
  protected readonly impactText = impactStatement();
  protected readonly operationText = operationStatement();
  protected readonly precedenceText = precedenceIntro();
  protected readonly precedenceOrder = OPERATION_PRECEDENCE.map((code) => maintenanceOperationLabel(code)).join(' > ');

  protected readonly busy = signal(true);
  protected readonly saving = signal(false);
  protected readonly cancelling = signal(false);
  protected readonly blockLost = signal(false);
  protected readonly item = signal<ItemBody | null>(null);
  protected readonly impact = signal<ChangeImpact | null>(null);
  protected readonly operation = signal<MaintenanceOperation | null>(null);
  protected readonly comment = signal('');
  protected readonly task = this.labels.task;
  protected readonly position = computed(() => this.item()?.position ?? 0);
  protected readonly size = computed(() => this.task()?.size ?? 0);
  protected readonly clock = inject(ItemClock);
  protected readonly layout = computed(() => {
    const current = this.item();
    return current ? buildItemLayout(current) : null;
  });
  protected readonly suiteLayout = computed(() => {
    const current = this.item();
    return current ? buildSuiteLayout(current) : null;
  });
  protected readonly missingText = missingUsecaseLabel();
  protected readonly missingTestsText = missingTestsLabel();
  protected readonly canContinue = computed(
    () =>
      this.item() !== null &&
      this.impact() !== null &&
      this.operation() !== null &&
      this.comment().length <= COMMENT_MAX_LENGTH &&
      !this.saving(),
  );

  constructor() {
    inject(DestroyRef).onDestroy(() => this.loading?.unsubscribe());
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
          this.toast.error(this.messageOf(error));
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

  /** Opens the full use case of one version in a modal; the choices of the item are not touched. */
  protected openUsecase(side: 'origin' | 'target', event: Event): void {
    this.openMarkedText(side, event, 'usecase');
  }

  /** Opens the full test suite of one version in a modal (spec 041); choices stay untouched. */
  protected openSuite(side: 'origin' | 'target', event: Event): void {
    this.openMarkedText(side, event, 'suite');
  }

  private openMarkedText(side: 'origin' | 'target', event: Event, kind: 'usecase' | 'suite'): void {
    const current = this.item();
    if (!current) {
      return;
    }
    const origin = side === 'origin';
    const data: UseCaseDialogData = {
      side,
      uc: current.uc,
      version: (origin ? current.originVersion : current.targetVersion) ?? '-',
      text:
        kind === 'suite'
          ? origin
            ? current.originTests
            : current.targetTests
          : origin
            ? current.originUsecase
            : current.targetUsecase,
      position: current.position,
      size: this.size(),
      clock: this.clock,
      kind,
    };
    const size = this.breakpoints.isMatched(NARROW_QUERY) ? FULL_SCREEN_DIALOG : WIDE_DIALOG;
    this.dialog.open(UseCaseDialogComponent, {
      ...size,
      data,
      autoFocus: 'first-tabbable',
      restoreFocus: event.currentTarget instanceof HTMLElement ? event.currentTarget : true,
    });
  }

  protected previousPosition(): number | null {
    return this.position() > 1 ? this.position() - 1 : null;
  }

  protected retry(): void {
    this.load(this.position() || Number(this.route.snapshot.paramMap.get('position')));
  }

  /** Abandon the held block after confirmation; drafts of this task are discarded (spec 023). */
  protected cancelBlock(): void {
    const task = this.task();
    if (!task || this.cancelling() || this.saving()) {
      return;
    }
    const confirmed = globalThis.confirm(
      $localize`:@@label.item.cancelConfirm:Cancel this block? All answers in this block will be discarded and the block will return to the queue.`,
    );
    if (!confirmed) {
      return;
    }
    this.cancelling.set(true);
    this.labels.cancel(task).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.toast.success(
          $localize`:@@label.item.cancelSuccess:Block cancelled. Your answers for this block were discarded.`,
        );
        void this.router.navigate(['/label/done'], { queryParams: { outcome: 'cancelled' } });
      },
      error: (error: unknown) => {
        this.cancelling.set(false);
        this.toast.error(this.messageOf(error));
      },
    });
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
      this.toast.error(unexpectedErrorMessage());
      return;
    }
    this.saving.set(true);
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
          this.toast.error(this.messageOf(error));
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
