import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { problemMessage, problemOf, unexpectedErrorMessage } from '../../core/problem-messages';
import { ToastService } from '../../core/toast.service';
import { ExportIndex, ExportService, problemOfBlob } from './export.service';
import { FileSaver } from './file-saver';

/** Administrator CSV downloads, all reached through the HAL links of `vd:exports` (spec 009). */
@Component({
  selector: 'app-export',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  templateUrl: './export.component.html',
  styleUrl: './export.component.scss',
})
export class ExportComponent implements OnInit {
  private readonly exports = inject(ExportService);
  private readonly saver = inject(FileSaver);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly index = signal<ExportIndex | null>(null);

  ngOnInit(): void {
    this.exports.index().subscribe({
      next: (index) => {
        this.loading.set(false);
        this.index.set(index);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        const problem = problemOf(error);
        this.toast.error(problem ? (problemMessage(problem) ?? unexpectedErrorMessage()) : unexpectedErrorMessage());
      },
    });
  }

  protected download(href: string | null): void {
    if (!href || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.exports.download(href).subscribe({
      next: (file) => {
        this.busy.set(false);
        this.saver.save(file.filename, file.blob);
      },
      error: (error: unknown) => {
        problemOfBlob(error).subscribe((problem) => {
          this.busy.set(false);
          this.toast.error(problem ? (problemMessage(problem) ?? unexpectedErrorMessage()) : unexpectedErrorMessage());
        });
      },
    });
  }
}