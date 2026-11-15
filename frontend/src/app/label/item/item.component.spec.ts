import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { LanguageService } from '../../core/language.service';
import { ItemBody, LabelService, TaskBody } from '../label.service';
import { LabelItemComponent, unsavedKey } from './item.component';

const task: TaskBody = {
  id: 41,
  blockNumber: 1,
  role: 1,
  status: 'reserved',
  size: 3,
  answeredCount: 0,
  reservationExpiresAt: new Date(Date.now() + 10 * 60_000).toISOString(),
  serverNow: new Date().toISOString(),
};

const item: ItemBody = {
  position: 2,
  itemId: 9002,
  system: 'minitest',
  uc: 'UC01',
  kind: 'MOD',
  field: 'step',
  originVersion: '1.1',
  targetVersion: '1.2',
  originUsecase: 'Line one\nLine two',
  targetUsecase: 'Changed',
  origin: 'Old step',
  target: 'New step',
  draft: null,
  _links: { 'vd:save-response': { href: '/api/v1/tasks/41/responses/9002' } },
};

describe('LabelItemComponent', () => {
  const saveResponse = vi.fn();
  const nextPending = vi.fn();
  const clearUnsaved = vi.fn();
  const takeUnsaved = vi.fn();
  const keepOnSwitch = vi.fn();

  function create(
    options: { draft?: ItemBody['draft']; unsaved?: Record<string, string>; reservationExpiresAt?: string } = {},
  ) {
    const held = options.reservationExpiresAt ? { ...task, reservationExpiresAt: options.reservationExpiresAt } : task;
    takeUnsaved.mockReturnValue(options.unsaved);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: LabelService,
          useValue: {
            task: signal(held),
            clockOffsetMs: signal(0),
            currentTask: () => of(held),
            getItem: () => of({ ...item, draft: options.draft ?? null }),
            saveResponse,
            nextPending,
          },
        },
        { provide: LanguageService, useValue: { takeUnsaved, clearUnsaved, keepOnSwitch } },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ position: '2' })) } },
      ],
    });
    const fixture = TestBed.createComponent(LabelItemComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => vi.resetAllMocks());

  it('shows the item texts with line breaks and keeps Continue disabled until both questions are answered', async () => {
    const fixture = create();
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.textContent).toContain('2 of 3');
    expect(root.textContent).toContain('minitest');
    expect(root.textContent).toContain('Line one\nLine two');
    const button = root.querySelector('button[type=submit]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    fixture.componentInstance['setImpact']('HIGH');
    fixture.detectChanges();
    expect(button.disabled).toBe(true);

    fixture.componentInstance['setOperation']('UPDATE');
    fixture.detectChanges();
    expect(button.disabled).toBe(false);
  });

  it('restores unsaved choices over the stored draft', async () => {
    const fixture = create({
      draft: { changeImpact: 'LOW', maintenanceOperation: 'KEEP', comment: 'saved', language: 'pt-BR' },
      unsaved: { impact: 'HIGH', operation: 'FLAG', comment: 'typed' },
    });
    await fixture.whenStable();

    expect(takeUnsaved).toHaveBeenCalledWith(unsavedKey(41, 2));
    expect(fixture.componentInstance['impact']()).toBe('HIGH');
    expect(fixture.componentInstance['operation']()).toBe('FLAG');
    expect(fixture.componentInstance['comment']()).toBe('typed');
    expect(keepOnSwitch).toHaveBeenCalledWith(unsavedKey(41, 2), expect.any(Function));
  });

  it('saves the draft and moves to the next pending item', async () => {
    saveResponse.mockReturnValue(of({ ...task, answeredCount: 1 }));
    nextPending.mockReturnValue(of(3));
    const fixture = create({
      draft: { changeImpact: 'LOW', maintenanceOperation: 'KEEP', comment: null, language: 'pt-BR' },
    });
    await fixture.whenStable();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.componentInstance['submit']();

    expect(saveResponse).toHaveBeenCalledWith('/api/v1/tasks/41/responses/9002', {
      changeImpact: 'LOW',
      maintenanceOperation: 'KEEP',
      comment: null,
    });
    expect(clearUnsaved).toHaveBeenCalledWith(unsavedKey(41, 2));
    expect(navigate).toHaveBeenCalledWith(['/label/items', 3]);
  });

  it('shows no expiry warning while time is left', async () => {
    const fixture = create();
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('[role=timer]')?.textContent).toContain('0');
    expect(root.querySelector('[role=timer].expired')).toBeNull();
    expect(root.querySelector('p.warning')).toBeNull();
  });

  it('warns when the countdown is at zero and keeps the unsaved choices (T026)', async () => {
    const fixture = create({
      reservationExpiresAt: new Date(Date.now() - 5_000).toISOString(),
      unsaved: { impact: 'HIGH', operation: 'FLAG', comment: 'typed' },
    });
    await fixture.whenStable();
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('[role=timer]')?.textContent).toContain('00:00');
    expect(root.querySelector('[role=timer].expired')).not.toBeNull();
    expect(root.querySelector('p.warning[role=alert]')).not.toBeNull();
    expect(fixture.componentInstance['impact']()).toBe('HIGH');
    expect(fixture.componentInstance['operation']()).toBe('FLAG');
    expect(fixture.componentInstance['comment']()).toBe('typed');
    const button = root.querySelector('button[type=submit]') as HTMLButtonElement;
    expect(button.disabled).toBe(false);
  });

  it('goes to the review when no item is pending', async () => {
    saveResponse.mockReturnValue(of({ ...task, answeredCount: 3 }));
    nextPending.mockReturnValue(of(null));
    const fixture = create({
      draft: { changeImpact: 'HIGH', maintenanceOperation: 'MERGE', comment: 'x', language: 'en-US' },
    });
    await fixture.whenStable();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.componentInstance['submit']();

    expect(navigate).toHaveBeenCalledWith(['/label/review']);
  });
});
