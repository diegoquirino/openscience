import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import {
  codePoints,
  isSkill,
  SKILL_OTHER_MAX,
  skillAnswerValid,
  skillLabel,
  SKILLS,
} from '../core/labels/skill-labels';
import { OnboardingService } from '../core/onboarding.service';
import {
  fieldMessage,
  FieldError,
  problemMessage,
  problemOf,
  problemSlug,
  unexpectedErrorMessage,
} from '../core/problem-messages';
import { ToastService } from '../core/toast.service';
import { SessionService } from '../core/session.service';

/** View and edit name (and skill for participants) from the header menu (spec 014). */
@Component({
  selector: 'app-profile',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule, MatRadioModule, RouterLink],
  template: `
    <section class="profile">
      <h1 i18n="@@profile.title">Profile</h1>
      <p class="page-intro" i18n="@@profile.intro">Update how your name appears. E-mail stays the same.</p>

      <div class="page-panel">
      <mat-form-field appearance="outline" class="name">
        <mat-label i18n="@@profile.name">Name</mat-label>
        <input
          matInput
          class="name-input"
          [value]="name()"
          (input)="name.set($any($event.target).value)"
          autocomplete="name"
          maxlength="120"
        />
      </mat-form-field>

      @if (isParticipant()) {
        <mat-radio-group
          class="options"
          aria-labelledby="profile-skill-legend"
          [value]="skill()"
          (change)="choose($event.value)"
        >
          <span id="profile-skill-legend" class="legend" i18n="@@profile.skill.legend">Professional skill</span>
          @for (code of codes; track code) {
            <mat-radio-button [value]="code">{{ label(code) }}</mat-radio-button>
          }
        </mat-radio-group>
        @if (skill() === 'other') {
          <mat-form-field appearance="outline" class="other">
            <mat-label i18n="@@profile.skill.other">Describe your skill</mat-label>
            <input
              matInput
              class="other-input"
              [value]="other()"
              (input)="other.set($any($event.target).value)"
              autocomplete="off"
            />
            <mat-hint align="end">{{ length() }} / {{ max }}</mat-hint>
          </mat-form-field>
          @if (otherError(); as text) {
            <p class="error" role="alert">{{ text }}</p>
          }
          @if (otherTooLong()) {
            <p class="error" role="alert">{{ tooLongMessage }}</p>
          }
        }
      }


      <div class="actions">
        <button mat-button type="button" routerLink="/" i18n="@@profile.back">Back</button>
        <button
          mat-flat-button
          color="primary"
          type="button"
          [disabled]="!valid() || saving()"
          (click)="submit()"
          i18n="@@profile.save"
        >
          Save
        </button>
      </div>
      </div>
    </section>
  `,
  styles: `
    .profile {
      display: grid;
      gap: 1rem;
      padding: 1rem 0;
      max-width: 36rem;
    }
    .options {
      display: grid;
    }
    .legend {
      font-weight: 600;
    }
    .error {
      color: var(--mat-sys-error, #b3261e);
      margin: 0;
    }
    .ok {
      color: var(--mat-sys-primary, #1b5e20);
      margin: 0;
    }
    .actions {
      display: flex;
      gap: 0.75rem;
    }
  `,
})
export class ProfileComponent implements OnInit {
  private readonly onboarding = inject(OnboardingService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);
  private readonly http = inject(HttpClient);

  protected readonly codes = SKILLS;
  protected readonly max = SKILL_OTHER_MAX;
  protected readonly tooLongMessage = fieldMessage({ field: 'skillOther', code: 'size' });

  readonly name = signal('');
  readonly skill = signal<string | null>(null);
  readonly other = signal('');
  readonly saving = signal(false);
  readonly saved = signal(false);

  readonly isParticipant = computed(() => this.session.subject() === 'participant');
  readonly length = computed(() => codePoints(this.other()));
  readonly otherTooLong = computed(() => this.length() > SKILL_OTHER_MAX);
  readonly valid = computed(() => {
    if (this.name().trim().length < 2) {
      return false;
    }
    if (!this.isParticipant()) {
      return true;
    }
    return skillAnswerValid(this.skill(), this.other());
  });
  readonly otherError = computed(() =>
    this.other().length > 0 && this.other().trim().length === 0
      ? fieldMessage({ field: 'skillOther', code: 'required' })
      : null,
  );

  ngOnInit(): void {
    const current = this.session.session();
    if (current?.name) {
      this.name.set(current.name);
    }
    if (this.isParticipant()) {
      this.onboarding.loadSkill().subscribe({
        next: (profile) => {
          if (profile.name) {
            this.name.set(profile.name);
          }
          this.skill.set(isSkill(profile.skill) ? profile.skill : null);
          this.other.set(profile.skillOther ?? '');
        },
        error: () => this.toast.error(unexpectedErrorMessage()),
      });
    }
  }

  protected label(code: string): string {
    return skillLabel(code);
  }

  choose(code: string): void {
    this.skill.set(code);
    if (code !== 'other') {
      this.other.set('');
    }
    this.saved.set(false);
  }

  submit(): void {
    if (!this.valid() || this.saving()) {
      return;
    }
    this.saving.set(true);
        this.saved.set(false);
    if (this.isParticipant()) {
      const skill = this.skill()!;
      this.onboarding.recordSkill(this.name().trim(), skill, skill === 'other' ? this.other() : null).subscribe({
        next: () => this.afterSave(),
        error: (error: unknown) => this.fail(error),
      });
      return;
    }
    const href = this.session.session()?.links.href('me');
    if (!href) {
      this.saving.set(false);
      this.toast.error(unexpectedErrorMessage());
      return;
    }
    this.http.put(href, { name: this.name().trim() }).subscribe({
      next: () => this.afterSave(),
      error: (error: unknown) => this.fail(error),
    });
  }

  private afterSave(): void {
    this.session.refresh().subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.set(true);
        this.toast.success($localize`:@@profile.saved:Saved.`);
      },
      error: () => {
        this.saving.set(false);
        this.saved.set(true);
        this.toast.success($localize`:@@profile.saved:Saved.`);
      },
    });
  }

  private fail(error: unknown): void {
    this.saving.set(false);
    const problem = problemOf(error);
    if (problemSlug(problem) === 'invalid-request') {
      const errors: FieldError[] = problem?.errors ?? [];
      this.toast.error(errors.map((e) => fieldMessage(e)).find((m) => m !== undefined) ?? unexpectedErrorMessage());
      return;
    }
    this.toast.error(problemMessage(problem) ?? unexpectedErrorMessage());
  }
}
