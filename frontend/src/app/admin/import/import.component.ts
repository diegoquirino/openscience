import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { problemMessage, problemOf, unexpectedErrorMessage } from '../../core/problem-messages';
import { ImportBatch, ImportPreview, ImportService, RowIssue } from './import.service';

/** Administrator CSV import: preview and confirm (spec 005 US1–US2). */
@Component({
  selector: 'app-import',
  imports: [MatButtonModule, RouterLink],
  templateUrl: './import.component.html',
  styleUrl: './import.component.scss',
})
export class ImportComponent {
  private readonly imports = inject(ImportService);

  protected readonly busy = signal(false);
  protected readonly message = signal<string | undefined>(undefined);
  protected readonly preview = signal<ImportPreview | null>(null);
  protected readonly batch = signal<ImportBatch | null>(null);

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    this.message.set(undefined);
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
        this.message.set(problem ? problemMessage(problem) : unexpectedErrorMessage());
      },
    });
  }

  protected confirm(): void {
    const current = this.preview();
    if (!current?.confirmHref || current.includedCount <= 0) {
      return;
    }
    this.message.set(undefined);
    this.busy.set(true);
    this.imports.confirm(current).subscribe({
      next: (batch) => {
        this.busy.set(false);
        this.batch.set(batch);
        this.preview.set(null);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        const problem = problemOf(error);
        this.message.set(problem ? problemMessage(problem) : unexpectedErrorMessage());
      },
    });
  }

  protected cancel(): void {
    this.preview.set(null);
    this.batch.set(null);
    this.message.set(undefined);
  }

  protected trackIssue(_index: number, issue: RowIssue): string {
    return `${issue.line}:${issue.code}:${issue.editKey}`;
  }
}
