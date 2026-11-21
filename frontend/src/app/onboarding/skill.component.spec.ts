import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { LanguageService } from '../core/language.service';
import { OnboardingService } from '../core/onboarding.service';
import { SessionService } from '../core/session.service';
import { ToastService } from '../core/toast.service';
import { SKILL_UNSAVED_KEY, SkillComponent } from './skill.component';

describe('SkillComponent', () => {
  const recordSkill = vi.fn();
  const refresh = vi.fn();
  const takeUnsaved = vi.fn();
  const keepOnSwitch = vi.fn();
  const clearUnsaved = vi.fn();

  function create(unsaved?: Record<string, string>) {
    takeUnsaved.mockReturnValue(unsaved);
    refresh.mockReturnValue(of(null));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: OnboardingService, useValue: { recordSkill } },
        { provide: SessionService, useValue: { refresh } },
        { provide: LanguageService, useValue: { takeUnsaved, keepOnSwitch, clearUnsaved } },
        {
          provide: ToastService,
          useValue: { error: vi.fn(), success: vi.fn(), warn: vi.fn(), info: vi.fn(), show: vi.fn(), action: vi.fn() },
        },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(SkillComponent);
    fixture.detectChanges();
    return { fixture, navigate, root: fixture.nativeElement as HTMLElement, component: fixture.componentInstance };
  }

  const proceed = (root: HTMLElement) => root.querySelector<HTMLButtonElement>('button.continue')!;

  beforeEach(() => vi.resetAllMocks());

  it('lists the 13 options and disables Continue until name and skill are chosen', () => {
    const { root } = create();
    expect(root.querySelectorAll('mat-radio-button')).toHaveLength(13);
    expect(proceed(root).disabled).toBe(true);
  });

  it('enables Continue for a common option and sends name and code without description', () => {
    recordSkill.mockReturnValue(of(undefined));
    const { fixture, navigate, root, component } = create();

    component.name.set('Ana Souza');
    component.choose('test_analyst');
    fixture.detectChanges();
    expect(proceed(root).disabled).toBe(false);
    proceed(root).click();

    expect(recordSkill).toHaveBeenCalledWith('Ana Souza', 'test_analyst', null);
    expect(clearUnsaved).toHaveBeenCalledWith(SKILL_UNSAVED_KEY);
    expect(refresh).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/welcome');
  });

  it('asks for a description for "other" within 100 characters', () => {
    const { fixture, root, component } = create();

    component.name.set('Ana Souza');
    component.choose('other');
    fixture.detectChanges();
    expect(root.querySelector('input.other-input')).not.toBeNull();
    expect(proceed(root).disabled).toBe(true);

    component.other.set('a'.repeat(100));
    fixture.detectChanges();
    expect(proceed(root).disabled).toBe(false);

    component.other.set('a'.repeat(101));
    fixture.detectChanges();
    expect(proceed(root).disabled).toBe(true);
    expect(root.textContent).toContain('101 / 100');
  });

  it('hides and clears the description when another option is chosen', () => {
    const { fixture, root, component } = create();
    component.choose('other');
    component.other.set('Technical writer');
    component.choose('developer');
    fixture.detectChanges();

    expect(component.other()).toBe('');
    expect(root.querySelector('input.other-input')).toBeNull();
  });

  it('sends the description as typed with "other"', () => {
    recordSkill.mockReturnValue(of(undefined));
    const { fixture, root, component } = create();
    component.name.set('Ana Souza');
    component.choose('other');
    component.other.set(' Technical writer ');
    fixture.detectChanges();

    proceed(root).click();

    expect(recordSkill).toHaveBeenCalledWith('Ana Souza', 'other', ' Technical writer ');
  });

  it('restores the choice kept across a language switch and registers the values to keep', () => {
    const { component } = create({ name: 'Ana', skill: 'other', other: 'Redator' });
    expect(component.name()).toBe('Ana');
    expect(component.skill()).toBe('other');
    expect(component.other()).toBe('Redator');
    expect(takeUnsaved).toHaveBeenCalledWith(SKILL_UNSAVED_KEY);

    const values = keepOnSwitch.mock.calls[0][1] as () => Record<string, string>;
    expect(values()).toEqual({ name: 'Ana', skill: 'other', other: 'Redator' });
  });
});
