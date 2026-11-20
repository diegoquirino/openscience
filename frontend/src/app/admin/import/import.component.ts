import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { problemMessage, problemOf, unexpectedErrorMessage } from '../../core/problem-messages';
import { ToastService } from '../../core/toast.service';
import { ImportBatch, ImportPreview, ImportService, RowIssue, StudyPhase } from './import.service';

/** Administrator CSV import: preview, confirm and study-phase banner (spec 005). */
@Component({
  selector: 'app-import',
  imports: [MatButtonModule, RouterLink],
  templateUrl: './import.component.html',
  styleUrl: './import.component.scss',
})
export class ImportComponent implements OnInit {
  private readonly imports = inject(ImportService);
  private readonly toast = inject(ToastService);

  protected readonly busy = signal(false);
  protected readonly preview = signal<ImportPreview | null>(null);
  protected readonly batch = signal<ImportBatch | null>(null);
  protected readonly studyPhase = signal<StudyPhase | null>(null);
  protected readonly batches = signal<ImportBatch[]>([]);

  ngOnInit(): void {
    this.imports.studyPhase().subscribe({
      next: (phase) => this.studyPhase.set(phase),
      error: () => this.studyPhase.set(null),
    });
    this.reloadBatches();
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    this.batch.set(null);
    this.busy.set(true);
    this.imports.requestPreview(file).subscribe({
      next: (preview) => {
        this.busy.set(false);
        this.preview.set(preview);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.preview.set(null);
        const problem = problemOf(error);
        this.toast.error(problem ? (problemMessage(problem) ?? unexpectedErrorMessage()) : unexpectedErrorMessage());
      },
    });
  }

  protected confirm(): void {
    const current = this.preview();
    if (!current?.confirmHref || current.includedCount <= 0) {
      return;
    }
    this.busy.set(true);
    this.imports.confirm(current).subscribe({
      next: (batch) => {
        this.busy.set(false);
        this.batch.set(batch);
        this.preview.set(null);
        this.reloadPhase();
        this.reloadBatches();
      },
      error: (error: unknown) => {
        this.busy.set(false);
        const problem = problemOf(error);
        this.toast.error(problem ? (problemMessage(problem) ?? unexpectedErrorMessage()) : unexpectedErrorMessage());
      },
    });
  }

  protected switchToCollection(): void {
    const phase = this.studyPhase();
    if (!phase?.switchAllowed) {
      return;
    }
    if (
      !globalThis.confirm(
        $localize`:confirm dialog before opening collection@@import.switchConfirm:Open the collection phase? Only possible with an empty study database.`,
      )
    ) {
      return;
    }
    this.busy.set(true);
    this.imports.switchToCollection(phase.selfHref).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.studyPhase.set(updated);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        const problem = problemOf(error);
        this.toast.error(problem ? (problemMessage(problem) ?? unexpectedErrorMessage()) : unexpectedErrorMessage());
      },
    });
  }

  protected cancel(): void {
    this.preview.set(null);
    this.batch.set(null);
  }

  protected trackIssue(_index: number, issue: RowIssue): string {
    return `${issue.line}:${issue.code}:${issue.editKey}`;
  }

  private reloadPhase(): void {
    this.imports.studyPhase().subscribe({
      next: (phase) => this.studyPhase.set(phase),
      error: () => this.studyPhase.set(null),
    });
  }

  private reloadBatches(): void {
    this.imports.listBatches().subscribe({
      next: (rows) => this.batches.set(rows),
      error: () => this.batches.set([]),
    });
  }
}
