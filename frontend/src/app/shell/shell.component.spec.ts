import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { BUILD_VERSION } from '../build-info';
import { EntryPointService } from '../core/entry-point.service';
import { Links } from '../core/hal';
import { LanguageService } from '../core/language.service';
import { ShellComponent } from './shell.component';

@Component({ template: '' })
class BlankPage {}

describe('ShellComponent', () => {
  let switchTo: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    const available = { systemVersion: '0.3.0', environment: 'production', available: true, links: new Links() };
    switchTo = vi.fn(() => of(undefined));
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([{ path: '**', component: BlankPage }]),
        { provide: EntryPointService, useValue: { load: () => of(available) } },
        { provide: LanguageService, useValue: { current: 'en-US', switchTo } },
      ],
    }).compileComponents();
  });

  async function render(url = '/') {
    const fixture = TestBed.createComponent(ShellComponent);
    await TestBed.inject(Router).navigateByUrl(url);
    await fixture.whenStable();
    return fixture;
  }

  async function openMenu() {
    const fixture = await render('/participate');
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('header button.language')!.click();
    await fixture.whenStable();
    return { fixture, options: Array.from(document.querySelectorAll<HTMLButtonElement>('[mat-menu-item]')) };
  }

  it('shows Veredito in the header', async () => {
    const element = (await render()).nativeElement as HTMLElement;
    expect(element.querySelector('header')?.textContent).toContain('Veredito');
  });

  it('shows only the build version in the footer', async () => {
    const element = (await render()).nativeElement as HTMLElement;
    expect(element.querySelector('footer')?.textContent?.trim()).toBe(`Version ${BUILD_VERSION}`);
    expect(element.querySelector('footer a')).toBeNull();
  });

  it('names the current language on the header switch', async () => {
    const element = (await render()).nativeElement as HTMLElement;
    const button = element.querySelector('header button.language')!;
    expect(button.textContent?.trim()).toBe('English (US)');
    expect(button.getAttribute('aria-label')).toBe('Language: English (US)');
  });

  it('lists each language in its own language and marks the current one', async () => {
    const { options } = await openMenu();
    expect(options.map((option) => option.textContent?.replace('✓', '').trim())).toEqual([
      'Português (Brasil)',
      'English (US)',
      'Español (España)',
    ]);
    expect(options.map((option) => option.getAttribute('lang'))).toEqual(['pt-BR', 'en-US', 'es-ES']);
    expect(options.map((option) => option.getAttribute('aria-current'))).toEqual([null, 'true', null]);
  });

  it('switches to the chosen language', async () => {
    const { options } = await openMenu();
    options[2].click();
    expect(switchTo).toHaveBeenCalledWith('es-ES');
  });

  it('does nothing when the current language is chosen', async () => {
    const { options } = await openMenu();
    options[1].click();
    expect(switchTo).not.toHaveBeenCalled();
  });
});
