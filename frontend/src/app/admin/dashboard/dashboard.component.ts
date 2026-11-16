import { DatePipe, PercentPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { skillLabel } from '../../core/labels/skill-labels';
import { problemMessage, problemOf, unexpectedErrorMessage } from '../../core/problem-messages';
import { DashboardService, DashboardView } from './dashboard.service';

/** The administrator's read-only progress snapshot, reached through the HAL link `vd:dashboard` (spec 010). */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, PercentPipe, MatButtonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly dashboard = inject(DashboardService);

  protected readonly loading = signal(true);
  protected readonly message = signal<string | undefined>(undefined);
  protected readonly view = signal<DashboardView | null>(null);

  /** No item was imported: every number is zero and the screen offers the Import. */
  protected readonly empty = computed(() => (this.view()?.snapshot.progress[0]?.items ?? 0) === 0);

  protected readonly skills = computed(() =>
    Object.entries(this.view()?.snapshot.participants.bySkill ?? {}).map(([code, total]) => ({
      code,
      label: skillLabel(code),
      total,
    })),
  );

  ngOnInit(): void {
    this.load();
  }

  /** Reads the snapshot again from the same link; the page itself is not reloaded. */
  protected refresh(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.message.set(undefined);
    this.dashboard.load().subscribe({
      next: (view) => {
        this.loading.set(false);
        this.view.set(view);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        const problem = problemOf(error);
        this.message.set(problem ? problemMessage(problem) : unexpectedErrorMessage());
      },
    });
  }
}
