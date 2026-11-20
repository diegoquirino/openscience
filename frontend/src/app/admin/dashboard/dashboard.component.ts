import { DatePipe, PercentPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { skillLabel } from '../../core/labels/skill-labels';
import { problemMessage, problemOf, unexpectedErrorMessage } from '../../core/problem-messages';
import { DashboardService, DashboardView, ProgressRow } from './dashboard.service';

/** The administrator's read-only progress snapshot, reached through the HAL link `vd:dashboard` (spec 010). */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, PercentPipe, MatButtonModule, MatIconModule, RouterLink],
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

  /** Overall progress row (system/batch null), used by the summary metric cards. */
  protected readonly overall = computed<ProgressRow | null>(() => this.view()?.snapshot.progress[0] ?? null);

  protected readonly freeTasks = computed(() => {
    const row = this.overall();
    return row ? row.role1.free + row.role2.free : 0;
  });

  protected readonly inProgressTasks = computed(() => {
    const row = this.overall();
    return row ? row.role1.inProgress + row.role2.inProgress : 0;
  });

  protected readonly completedTasks = computed(() => {
    const row = this.overall();
    return row ? row.role1.completed + row.role2.completed : 0;
  });

  protected readonly completedPercent = computed(() => {
    const total = this.freeTasks() + this.inProgressTasks() + this.completedTasks();
    return total === 0 ? 0 : this.completedTasks() / total;
  });

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
