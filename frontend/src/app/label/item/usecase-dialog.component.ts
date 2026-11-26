import { ChangeDetectionStrategy, Component, ElementRef, afterNextRender, inject, viewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { missingUsecaseLabel } from '../../core/labels/item-labels';
import { ItemClock } from './item-clock';
import { ItemProgressComponent } from './item-progress.component';
import { parseUseCase } from './usecase-text';

/** What the dialog needs to show one version of the use case (spec 008, User Story 2). */
export interface UseCaseDialogData {
  /** `origin` is the previous version, `target` the modified one. */
  side: 'origin' | 'target';
  /** Use case name as captured. */
  uc: string;
  /** Version label as stored on the item. */
  version: string;
  /** Full use case text of that version as stored (never changed). */
  text: string | null;
  position: number;
  size: number;
  clock: ItemClock;
}

/** The first highlighted line stays this fraction of the visible height below the top when it opens. */
const SCROLL_OFFSET_RATIO = 1 / 3;

/**
 * The whole use case of one version in a modal: monospaced text with the indentation kept, the lines
 * the pipeline marked with `<- edited` highlighted (the marker is not shown; before/after colors
 * differ per side, spec 035) and the view scrolled to the first of them. The counter and the
 * countdown stay visible in its header (FR-007).
 */
@Component({
  selector: 'app-usecase-dialog',
  imports: [MatDialogModule, MatButtonModule, ItemProgressComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './usecase-dialog.component.html',
  styleUrl: './usecase-dialog.component.scss',
  host: { '[attr.data-side]': 'data.side' },
})
export class UseCaseDialogComponent {
  protected readonly data = inject<UseCaseDialogData>(MAT_DIALOG_DATA);
  protected readonly view = parseUseCase(this.data.text);
  protected readonly missingText = missingUsecaseLabel();
  private readonly body = viewChild<ElementRef<HTMLElement>>('body');

  constructor() {
    afterNextRender(() => this.scrollToFirstEdited());
  }

  private scrollToFirstEdited(): void {
    if (this.view.firstEditedIndex < 0) {
      return;
    }
    const container = this.body()?.nativeElement;
    const line = container?.querySelector<HTMLElement>('.usecase-line.edited');
    if (container && line) {
      // offsetTop is relative to the (positioned) container and is not affected by the opening animation.
      container.scrollTop = Math.max(0, line.offsetTop - container.clientHeight * SCROLL_OFFSET_RATIO);
    }
  }
}
