import { Component, computed, inject, signal } from '@angular/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { Router } from '@angular/router';
import { LanguageService } from '../core/language.service';
import {
  isOrganization,
  ORGANIZATION_OTHER_MAX,
  organizationAnswerValid,
  organizationLabel,
  ORGANIZATIONS,
} from '../core/labels/organization-labels';
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
import { SessionService } from '../core/session.service';
import { ToastService } from '../core/toast.service';

/** Route key of the values kept across a language switch (`veredito-unsaved:/skill`). */
export const SKILL_UNSAVED_KEY = '/skill';

/** Profile step: name, skill and organization (specs 013–025). */
@Component({
  selector: 'app-skill',
  imports: [MatAutocompleteModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatRadioModule],
  template: `
    <section class="skill">
      <h1 i18n="@@skill.title">Your name and professional skill</h1>
      <p i18n="@@skill.intro">Tell us your name and choose the option that best describes you.</p>
      <mat-form-field appearance="outline" class="name">
        <mat-label i18n="@@skill.name">Name</mat-label>
        <input matInput class="name-input" [value]="name()" (input)="name.set($any($event.target).value)" autocomplete="name" maxlength="120" />
      </mat-form-field>
      <mat-radio-group
        class="options"
        aria-labelledby="skill-legend"
        [value]="skill()"
        (change)="choose($event.value)"
      >
        <span id="skill-legend" class="legend" i18n="@@skill.legend">Professional skill</span>
        @for (code of codes; track code) {
          <mat-radio-button [value]="code">{{ label(code) }}</mat-radio-button>
        }
      </mat-radio-group>
      @if (skill() === 'other') {
        <mat-form-field appearance="outline" class="other">
          <mat-label i18n="@@skill.other.label">Describe your skill</mat-label>
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

      <mat-radio-group
        class="options"
        aria-labelledby="org-legend"
        [value]="organization()"
        (change)="chooseOrganization($event.value)"
      >
        <span id="org-legend" class="legend" i18n="@@organization.legend"
          >Organization / employer where you study or work</span
        >
        @for (code of orgCodes; track code) {
          <mat-radio-button [value]="code">{{ orgLabel(code) }}</mat-radio-button>
        }
      </mat-radio-group>
      @if (organization() === 'other') {
        <mat-form-field appearance="outline" class="other">
          <mat-label i18n="@@organization.other.label">Organization name</mat-label>
          <input
            matInput
            class="org-other-input"
            [value]="organizationOther()"
            (input)="onOrganizationOtherInput($any($event.target).value)"
            [matAutocomplete]="orgAuto"
            autocomplete="off"
          />
          <mat-autocomplete
            #orgAuto="matAutocomplete"
            (optionSelected)="onOrganizationOtherInput($event.option.value)"
          >
            @for (suggestion of filteredSuggestions(); track suggestion) {
              <mat-option [value]="suggestion">{{ suggestion }}</mat-option>
            }
          </mat-autocomplete>
          <mat-hint align="end">{{ orgLength() }} / {{ orgMax }}</mat-hint>
        </mat-form-field>
        @if (orgOtherError(); as text) {
          <p class="error" role="alert">{{ text }}</p>
        }
        @if (orgOtherTooLong()) {
          <p class="error" role="alert">{{ orgTooLongMessage }}</p>
        }
      }
      @if (serverFieldError(); as text) {
        <p class="error" role="alert">{{ text }}</p>
      }

      <div class="actions">
        <button
          mat-flat-button
          color="primary"
          type="button"
          class="continue"
          [disabled]="!valid() || saving()"
          (click)="submit()"
          i18n="@@skill.continue"
        >
          Continue
        </button>
      </div>
    </section>
  `,
  styles: `
    .skill {
      display: grid;
      gap: 1rem;
      padding: 1rem 0;
    }
    .options {
      display: grid;
    }
    .legend {
      font-weight: 600;
    }
    .other {
      max-width: 32rem;
    }
    .error {
      color: var(--mat-sys-error, #b3261e);
      margin: 0;
    }
  `,
})
export class SkillComponent {
  private readonly onboarding = inject(OnboardingService);
  private readonly toast = inject(ToastService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly language = inject(LanguageService);

  protected readonly codes = SKILLS;
  protected readonly orgCodes = ORGANIZATIONS;
  protected readonly max = SKILL_OTHER_MAX;
  protected readonly orgMax = ORGANIZATION_OTHER_MAX;
  protected readonly tooLongMessage = fieldMessage({ field: 'skillOther', code: 'size' });
  protected readonly orgTooLongMessage = fieldMessage({ field: 'organizationOther', code: 'size' });

  readonly name = signal('');
  readonly skill = signal<string | null>(null);
  readonly other = signal('');
  readonly organization = signal<string | null>(null);
  readonly organizationOther = signal('');
  readonly suggestions = signal<string[]>([]);
  readonly saving = signal(false);
  readonly serverFieldError = signal<string | null>(null);

  readonly length = computed(() => codePoints(this.other()));
  readonly orgLength = computed(() => codePoints(this.organizationOther()));
  readonly otherTooLong = computed(() => this.length() > SKILL_OTHER_MAX);
  readonly orgOtherTooLong = computed(() => this.orgLength() > ORGANIZATION_OTHER_MAX);
  readonly valid = computed(
    () =>
      this.name().trim().length >= 2 &&
      skillAnswerValid(this.skill(), this.other()) &&
      organizationAnswerValid(this.organization(), this.organizationOther()),
  );
  readonly otherError = computed(() =>
    this.other().length > 0 && this.other().trim().length === 0
      ? fieldMessage({ field: 'skillOther', code: 'required' })
      : null,
  );
  readonly orgOtherError = computed(() =>
    this.organizationOther().length > 0 && this.organizationOther().trim().length === 0
      ? fieldMessage({ field: 'organizationOther', code: 'required' })
      : null,
  );
  readonly filteredSuggestions = computed(() => {
    const q = this.organizationOther().trim().toLowerCase();
    const all = this.suggestions();
    if (!q) {
      return all.slice(0, 20);
    }
    return all.filter((s) => s.toLowerCase().includes(q)).slice(0, 20);
  });

  constructor() {
    const kept = this.language.takeUnsaved(SKILL_UNSAVED_KEY);
    if (kept) {
      this.name.set(kept['name'] ?? '');
      this.skill.set(isSkill(kept['skill']) ? kept['skill'] : null);
      this.other.set(kept['other'] ?? '');
      this.organization.set(isOrganization(kept['organization']) ? kept['organization'] : null);
      this.organizationOther.set(kept['organizationOther'] ?? '');
    }
    this.language.keepOnSwitch(SKILL_UNSAVED_KEY, () => ({
      name: this.name(),
      skill: this.skill() ?? '',
      other: this.other(),
      organization: this.organization() ?? '',
      organizationOther: this.organizationOther(),
    }));
    this.onboarding.organizationSuggestions().subscribe({
      next: (values) => this.suggestions.set(values),
      error: () => this.suggestions.set([]),
    });
  }

  protected label(code: string): string {
    return skillLabel(code);
  }

  protected orgLabel(code: string): string {
    return organizationLabel(code);
  }

  choose(code: string): void {
    this.skill.set(code);
    if (code !== 'other') {
      this.other.set('');
    }
    this.serverFieldError.set(null);
  }

  chooseOrganization(code: string): void {
    this.organization.set(code);
    if (code !== 'other') {
      this.organizationOther.set('');
    }
    this.serverFieldError.set(null);
  }

  onOrganizationOtherInput(value: string): void {
    this.organizationOther.set(value);
    this.serverFieldError.set(null);
  }

  submit(): void {
    const skill = this.skill();
    const organization = this.organization();
    if (!skill || !organization || !this.valid() || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.serverFieldError.set(null);
    this.onboarding
      .recordSkill(
        this.name().trim(),
        skill,
        skill === 'other' ? this.other() : null,
        organization,
        organization === 'other' ? this.organizationOther() : null,
      )
      .subscribe({
        next: () => {
          this.language.clearUnsaved(SKILL_UNSAVED_KEY);
          this.session.refresh().subscribe({
            next: () => {
              this.saving.set(false);
              void this.router.navigateByUrl('/welcome');
            },
            error: () => {
              this.saving.set(false);
              void this.router.navigateByUrl('/welcome');
            },
          });
        },
        error: (error: unknown) => {
          this.saving.set(false);
          const problem = problemOf(error);
          switch (problemSlug(problem)) {
            case 'step-not-pending':
              void this.router.navigateByUrl('/welcome', { replaceUrl: true });
              return;
            case 'invalid-request': {
              const errors: FieldError[] = problem?.errors ?? [];
              const field = errors.find(
                (e) => e.field === 'skillOther' || e.field === 'organizationOther' || e.field === 'organization',
              );
              if (field) {
                this.serverFieldError.set(fieldMessage(field) ?? unexpectedErrorMessage());
                return;
              }
              this.toast.error(
                errors.map((e) => fieldMessage(e)).find((m) => m !== undefined) ?? unexpectedErrorMessage(),
              );
              return;
            }
            default:
              this.toast.error(problemMessage(problem) ?? unexpectedErrorMessage());
          }
        },
      });
  }
}
