import { BreakpointObserver } from '@angular/cdk/layout';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { LanguageService } from '../../core/language.service';
import { ItemBody, LabelService, TaskBody } from '../label.service';
import { LabelItemComponent, unsavedKey } from './item.component';
import { UseCaseDialogComponent } from './usecase-dialog.component';

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
    options: {
      draft?: ItemBody['draft'];
      unsaved?: Record<string, string>;
      reservationExpiresAt?: string;
      item?: Partial<ItemBody>;
    } = {},
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
            getItem: () => of({ ...item, ...options.item, draft: options.draft ?? null }),
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

  function blocks(root: HTMLElement) {
    return Array.from(root.querySelectorAll<HTMLElement>('.column')).map((column) => ({
      side: column.dataset['side'],
      title: column.querySelector('.block-title')?.textContent?.trim() ?? null,
      text: column.querySelector('.text')?.textContent ?? null,
      removed: Array.from(column.querySelectorAll('del')).map((e) => e.textContent),
      added: Array.from(column.querySelectorAll('ins')).map((e) => e.textContent),
    }));
  }

  it('shows the header and a MOD edit side by side with the changed words marked', async () => {
    const fixture = create({ item: { origin: 'launches the screen\nand waits', target: 'displays the screen\nand waits' } });
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.location')?.textContent).toBe('Basic flow step · Modification');
    expect(root.querySelector('.system')?.textContent).toBe('minitest');
    expect(root.querySelector('.uc')?.textContent).toBe('UC01');
    expect(blocks(root)).toEqual([
      {
        side: 'origin',
        title: 'Before',
        text: 'launches the screen\nand waits',
        removed: ['launches'],
        added: [],
      },
      {
        side: 'target',
        title: 'After',
        text: 'displays the screen\nand waits',
        removed: [],
        added: ['displays'],
      },
    ]);
    expect(root.querySelector('.no-difference')).toBeNull();
  });

  it('shows only the added text of an ADD and only the removed text of a DEL', async () => {
    const add = create({ item: { kind: 'ADD', origin: '', target: 'exception 3 "New"\n  step 1' } });
    await add.whenStable();
    expect(blocks(add.nativeElement)).toEqual([
      { side: 'origin', title: null, text: null, removed: [], added: [] },
      { side: 'target', title: 'Added text', text: 'exception 3 "New"\n  step 1', removed: [], added: ['exception 3 "New"\n  step 1'] },
    ]);

    TestBed.resetTestingModule();
    const del = create({ item: { kind: 'DEL', origin: 'exception 5 "Old"', target: '' } });
    await del.whenStable();
    expect(blocks(del.nativeElement)).toEqual([
      { side: 'origin', title: 'Removed text', text: 'exception 5 "Old"', removed: ['exception 5 "Old"'], added: [] },
      { side: 'target', title: null, text: null, removed: [], added: [] },
    ]);
  });

  it('shows the requirements link before and after with (none) for the empty side', async () => {
    const fixture = create({ item: { kind: 'META', field: 'requirements', origin: '', target: 'REQ-07' } });
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.location')?.textContent).toBe('Requirements link · Metadata');
    expect(blocks(root)).toEqual([
      { side: 'origin', title: 'Requirements link: before', text: '(none)', removed: [], added: [] },
      { side: 'target', title: 'Requirements link: after', text: 'REQ-07', removed: [], added: ['REQ-07'] },
    ]);
  });

  it('warns that nothing differs when origin equals target, and shows an unknown field as is', async () => {
    const fixture = create({ item: { kind: 'META', field: 'custom_field', origin: 'REQ-07', target: 'REQ-07' } });
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.no-difference')?.textContent?.trim()).toBe('No text difference in this part');
    expect(root.querySelector('.location')?.textContent).toBe('custom_field · Metadata');
  });

  it('keeps Continue disabled until both questions are answered', async () => {
    const fixture = create();
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.textContent).toContain('2 of 3');
    expect(root.querySelector('.counter')?.textContent?.trim()).toBe('2 of 3');
    const button = root.querySelector('button[type=submit]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    fixture.componentInstance['setImpact']('HIGH');
    fixture.detectChanges();
    expect(button.disabled).toBe(true);

    fixture.componentInstance['setOperation']('UPDATE');
    fixture.detectChanges();
    expect(button.disabled).toBe(false);
  });

  it('opens the full use case of each version in a dialog and leaves the choices untouched (T015)', async () => {
    const fixture = create({
      draft: { changeImpact: 'LOW', maintenanceOperation: 'KEEP', comment: 'mine', language: 'pt-BR' },
    });
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    const dialog = TestBed.inject(MatDialog);
    const open = vi.spyOn(dialog, 'open').mockReturnValue(undefined as never);
    vi.spyOn(TestBed.inject(BreakpointObserver), 'isMatched').mockReturnValue(false);

    const buttons = Array.from(root.querySelectorAll<HTMLButtonElement>('button.open-usecase'));
    expect(buttons.map((button) => button.textContent?.trim())).toEqual([
      'View full use case (1.1)',
      'View full use case (1.2)',
    ]);

    buttons[0].click();
    buttons[1].click();

    const [previous, modified] = open.mock.calls;
    expect(previous[0]).toBe(UseCaseDialogComponent);
    expect(previous[1]).toMatchObject({
      width: '960px',
      restoreFocus: buttons[0],
      data: { side: 'origin', uc: 'UC01', version: '1.1', text: 'Line one\nLine two', position: 2, size: 3 },
    });
    expect(modified[1]).toMatchObject({
      restoreFocus: buttons[1],
      data: { side: 'target', version: '1.2', text: 'Changed' },
    });
    expect(fixture.componentInstance['impact']()).toBe('LOW');
    expect(fixture.componentInstance['operation']()).toBe('KEEP');
    expect(fixture.componentInstance['comment']()).toBe('mine');
  });

  it('opens the dialog full screen on a narrow screen and puts the button of an ADD in the first column', async () => {
    const fixture = create({ item: { kind: 'ADD', origin: '', target: 'new flow' } });
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    const open = vi.spyOn(TestBed.inject(MatDialog), 'open').mockReturnValue(undefined as never);
    vi.spyOn(TestBed.inject(BreakpointObserver), 'isMatched').mockReturnValue(true);

    const first = root.querySelector<HTMLButtonElement>('.column[data-side=origin] button.open-usecase');
    expect(first).not.toBeNull();
    expect(root.querySelector('.column[data-side=origin] .block')).toBeNull();
    first?.click();

    expect(open.mock.calls[0][1]).toMatchObject({ width: '100vw', height: '100dvh', panelClass: 'usecase-dialog-full' });
  });

  it('shows both statements with the explanation of every option and the precedence rule (T018)', async () => {
    const fixture = create();
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('.impact legend')?.textContent).toBe(
      'Does this edit change the behavior that the use case specifies?',
    );
    expect(root.querySelector('.operation legend')?.textContent).toBe(
      'What should be done with the manual test cases that exercise this part?',
    );
    expect(root.querySelectorAll('.impact input[type=radio]')).toHaveLength(2);
    expect(root.querySelectorAll('.operation input[type=radio]')).toHaveLength(8);

    const explanation = (id: string) => root.querySelector(`#${id}`)?.textContent;
    expect(explanation('impact-LOW')).toContain('does not change the observable behavior');
    expect(explanation('impact-HIGH')).toContain('so vague that the tester cannot decide');
    expect(explanation('operation-FLAG')).toContain('needs human analysis');

    // The accessible name of a radio stays its short label; the explanation is its description.
    const high = root.querySelector<HTMLInputElement>('.impact input[type=radio][value=HIGH]');
    expect(high?.getAttribute('aria-describedby')).toBe('impact-HIGH');
    expect(root.querySelector('.operation label[for$="-input"]')).not.toBeNull();
    expect(Array.from(root.querySelectorAll('.operation mat-radio-button label')).map((l) => l.textContent?.trim())).toEqual([
      'Keep',
      'Update',
      'Remove',
      'Create',
      'Merge',
      'Split',
      'Reassign',
      'Flag',
    ]);

    expect(root.querySelector('.precedence .intro')?.textContent).toBe(
      'If more than one applies, choose the one with the greatest consequence:',
    );
    expect(root.querySelector('.precedence .order')?.textContent).toBe(
      'Flag > Remove > Merge > Split > Create > Update > Reassign > Keep',
    );
  });

  it('restores unsaved choices over the stored draft', async () => {    const fixture = create({
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

  it('offers a new block when the save finds the block gone (T026)', async () => {
    saveResponse.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const fixture = create({
      draft: { changeImpact: 'LOW', maintenanceOperation: 'KEEP', comment: null, language: 'pt-BR' },
    });
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('a.new-block')).toBeNull();

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(root.querySelector('p.error[role=alert]')).not.toBeNull();
    expect(root.querySelector('a.new-block')?.getAttribute('href')).toBe('/label');
    expect(clearUnsaved).not.toHaveBeenCalled();
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
