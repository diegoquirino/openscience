import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { LabelService } from '../label.service';
import { ItemClock } from './item-clock';
import { UseCaseDialogComponent, UseCaseDialogData } from './usecase-dialog.component';
import { MISSING_USECASE_TEXT } from './usecase-text';

const TEXT = [
  'usecase "Login User" {',
  '  basic {',
  '    step 1 emailUser "launches the login screen"   <- edited',
  '    step 2 system "presents a form"',
  '  }',
  '}',
].join('\n');

describe('UseCaseDialogComponent', () => {
  function open(overrides: Partial<UseCaseDialogData> = {}) {
    TestBed.configureTestingModule({
      providers: [
        ItemClock,
        {
          provide: LabelService,
          useValue: {
            task: signal({ reservationExpiresAt: new Date(Date.now() + 5 * 60_000).toISOString() }),
            clockOffsetMs: signal(0),
          },
        },
        { provide: MAT_DIALOG_DATA, useFactory: () => ({
          side: 'origin',
          uc: 'LoginTest',
          version: 'v1.1',
          text: TEXT,
          position: 2,
          size: 10,
          clock: TestBed.inject(ItemClock),
          ...overrides,
        }) },
      ],
    });
    const fixture = TestBed.createComponent(UseCaseDialogComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('titles the previous version with the use case and the stored version label', () => {
    const root = open().nativeElement as HTMLElement;
    expect(root.querySelector('.dialog-title')?.textContent?.trim()).toBe('LoginTest · version v1.1 (previous)');
  });

  it('titles the modified version', () => {
    const root = open({ side: 'target', version: 'v1.2' }).nativeElement as HTMLElement;
    expect(root.querySelector('.dialog-title')?.textContent?.trim()).toBe('LoginTest · version v1.2 (modified)');
  });

  it('shows one line per text line, highlights the edited one and never shows the marker', () => {
    const root = open().nativeElement as HTMLElement;
    const lines = Array.from(root.querySelectorAll<HTMLElement>('.usecase-line'));

    expect(lines).toHaveLength(6);
    expect(lines.map((line) => line.classList.contains('edited'))).toEqual([false, false, true, false, false, false]);
    expect(lines[2].classList.contains('highlight-before')).toBe(true);
    expect(lines[2].classList.contains('highlight-after')).toBe(false);
    expect(lines[2].textContent).toBe('step 1 emailUser "launches the login screen"');
    expect(lines[2].style.paddingLeft).toBe('4ch');
    expect(lines[1].style.paddingLeft).toBe('2ch');
    expect(root.textContent).not.toContain('<- edited');
  });

  it('uses the after highlight color on the modified version', () => {
    const root = open({ side: 'target' }).nativeElement as HTMLElement;
    const edited = root.querySelector('.usecase-line.edited') as HTMLElement;
    expect(edited.classList.contains('highlight-after')).toBe(true);
    expect(edited.classList.contains('highlight-before')).toBe(false);
  });

  it('highlights CLARET keywords and quoted strings', () => {
    const root = open().nativeElement as HTMLElement;
    const keywords = Array.from(root.querySelectorAll('.claret-keyword')).map((node) => node.textContent);
    const strings = Array.from(root.querySelectorAll('.claret-string')).map((node) => node.textContent);
    expect(keywords).toEqual(expect.arrayContaining(['usecase', 'basic', 'step']));
    expect(strings).toContain('"Login User"');
    expect(strings).toContain('"launches the login screen"');
  });

  it('shows the counter and the countdown in its header', () => {
    const root = open().nativeElement as HTMLElement;
    expect(root.querySelector('.counter')?.textContent?.trim()).toBe('2 of 10');
    expect(root.querySelector('[role=timer]')?.textContent).toContain('05:00');
  });

  it('shows the localized message instead of the pipeline text, without highlighting', () => {
    const root = open({ text: MISSING_USECASE_TEXT }).nativeElement as HTMLElement;

    expect(root.querySelector('.missing-usecase')?.textContent).toBe('(this use case does not exist in this version)');
    expect(root.querySelector('.usecase-line')).toBeNull();
    expect(root.textContent).not.toContain('the use case does not exist in this version)');
  });

  it('scrolls the body to the first edited line when it opens and stays at the top without one', async () => {
    const edited = open();
    const body = (edited.nativeElement as HTMLElement).querySelector('.body') as HTMLElement;
    const line = body.querySelector('.usecase-line.edited') as HTMLElement;
    Object.defineProperty(line, 'offsetTop', { value: 900 });
    Object.defineProperty(body, 'clientHeight', { value: 300 });
    await edited.whenStable();
    (edited.componentInstance as unknown as { scrollToFirstEdited(): void })['scrollToFirstEdited']();
    expect(body.scrollTop).toBe(800);

    TestBed.resetTestingModule();
    const plain = open({ text: 'usecase "A" {\n}' });
    await plain.whenStable();
    expect(((plain.nativeElement as HTMLElement).querySelector('.body') as HTMLElement).scrollTop).toBe(0);
  });
});
