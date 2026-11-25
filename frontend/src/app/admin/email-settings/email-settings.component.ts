import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { problemMessage, problemOf, unexpectedErrorMessage } from '../../core/problem-messages';
import { ToastService } from '../../core/toast.service';
import {
  EmailSettings,
  EmailSettingsService,
  EmailTemplate,
  InviteTemplateKind,
} from './email-settings.service';

/** Admin date-and-time send settings + editable invite templates (spec 031). */
@Component({
  selector: 'app-email-settings',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, RouterLink],
  templateUrl: './email-settings.component.html',
  styleUrl: './email-settings.component.scss',
})
export class EmailSettingsComponent implements OnInit {
  private readonly api = inject(EmailSettingsService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly settings = signal<EmailSettings | null>(null);

  protected caae = '';
  protected platformUrl = '';
  protected collectionStartLocal = '';
  protected collectionEndLocal = '';
  protected reminderLocal = '';

  protected readonly kinds: InviteTemplateKind[] = ['inicial', 'lembrete', 'agradecimento'];
  protected templates: Record<InviteTemplateKind, EmailTemplate | null> = {
    inicial: null,
    lembrete: null,
    agradecimento: null,
  };
  protected subjects: Record<InviteTemplateKind, string> = {
    inicial: '',
    lembrete: '',
    agradecimento: '',
  };
  protected bodies: Record<InviteTemplateKind, string> = {
    inicial: '',
    lembrete: '',
    agradecimento: '',
  };

  ngOnInit(): void {
    this.api.loadSettings().subscribe({
      next: (settings) => {
        this.applySettings(settings);
        forkJoin({
          inicial: this.api.loadTemplate(settings, 'inicial'),
          lembrete: this.api.loadTemplate(settings, 'lembrete'),
          agradecimento: this.api.loadTemplate(settings, 'agradecimento'),
        }).subscribe({
          next: (loaded) => {
            for (const kind of this.kinds) {
              this.templates[kind] = loaded[kind];
              this.subjects[kind] = loaded[kind].subject;
              this.bodies[kind] = loaded[kind].body;
            }
            this.loading.set(false);
          },
          error: (error: unknown) => this.fail(error),
        });
      },
      error: (error: unknown) => this.fail(error),
    });
  }

  protected saveSettings(): void {
    const current = this.settings();
    if (!current || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.api
      .saveSettings(current, {
        caae: this.caae.trim() || null,
        platformUrl: this.platformUrl.trim(),
        collectionStartAt: EmailSettingsService.fromLocalInput(this.collectionStartLocal),
        collectionEndAt: EmailSettingsService.fromLocalInput(this.collectionEndLocal),
        reminderAt: EmailSettingsService.fromLocalInput(this.reminderLocal),
      })
      .subscribe({
        next: (saved) => {
          this.applySettings(saved);
          this.busy.set(false);
          this.toast.success($localize`:@@emailSettings.saved:Send settings saved`);
        },
        error: (error: unknown) => {
          this.busy.set(false);
          this.fail(error);
        },
      });
  }

  protected saveTemplate(kind: InviteTemplateKind): void {
    const template = this.templates[kind];
    if (!template || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.api.saveTemplate(template, this.subjects[kind], this.bodies[kind]).subscribe({
      next: (saved) => {
        this.templates[kind] = saved;
        this.subjects[kind] = saved.subject;
        this.bodies[kind] = saved.body;
        this.busy.set(false);
        this.toast.success($localize`:@@emailSettings.templateSaved:Template saved`);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.fail(error);
      },
    });
  }

  protected kindLabel(kind: InviteTemplateKind): string {
    switch (kind) {
      case 'inicial':
        return $localize`:@@emailSettings.kind.inicial:Initial invite`;
      case 'lembrete':
        return $localize`:@@emailSettings.kind.lembrete:Reminder`;
      case 'agradecimento':
        return $localize`:@@emailSettings.kind.agradecimento:Thank-you`;
    }
  }

  private applySettings(settings: EmailSettings): void {
    this.settings.set(settings);
    this.caae = settings.caae ?? '';
    this.platformUrl = settings.platformUrl ?? '';
    this.collectionStartLocal = EmailSettingsService.toLocalInput(settings.collectionStartAt);
    this.collectionEndLocal = EmailSettingsService.toLocalInput(settings.collectionEndAt);
    this.reminderLocal = EmailSettingsService.toLocalInput(settings.reminderAt);
  }

  private fail(error: unknown): void {
    this.loading.set(false);
    const problem = problemOf(error);
    this.toast.error(problem ? (problemMessage(problem) ?? unexpectedErrorMessage()) : unexpectedErrorMessage());
  }
}
