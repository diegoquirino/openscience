import { Component, signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { BUILD_VERSION } from '../build-info';
import { EntryPointService } from '../core/entry-point.service';
import { Links } from '../core/hal';
import { LanguageService } from '../core/language.service';
import { Session, SessionService } from '../core/session.service';
import { ShellComponent } from './shell.component';

@Component({ template: '' })
class BlankPage {}

const SIGNED_IN: Session = {
  subject: 'participant',
  name: 'Ana Souza',
  pseudonym: 'R-0001',
  preferredLanguage: 'en-US',
  expiresAt: '2026-11-14T17:03:00Z',
  links: new Links(),
};

describe('ShellComponent', () => {
  let switchTo: ReturnType<typeof vi.fn>;
  let signOut: ReturnType<typeof vi.fn>;
  let session: WritableSignal<Session | null>;

  beforeEach(async () => {
    const available = { systemVersion: '0.3.0', environment: 'production', available: true, links: new Links() };
    switchTo = vi.fn(() => of(undefined));
    session = signal<Session | null>(null);
    signOut = vi.fn(() => {
      session.set(null);
      return of(undefined);
    });
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([{ path: '**', component: BlankPage }]),
        { provide: EntryPointService, useValue: { load: () => of(available) } },
        { provide: LanguageService, useValue: { current: 'en-US', switchTo } },
        { provide: SessionService, useValue: { session, signOut } },
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

  it('shows only the flag of the current language on the header switch, named for screen readers', async () => {
    const element = (await render()).nativeElement as HTMLElement;
    const button = element.querySelector('header button.language')!;
    expect(button.textContent?.trim()).toBe('');
    expect(button.querySelector('app-flag svg')?.getAttribute('viewBox')).toBe('0 0 38 20');
    expect(button.getAttribute('aria-label')).toBe('Language: English (US)');
  });

  it('shows neither a name nor "Sign out" to a visitor', async () => {
    const element = (await render()).nativeElement as HTMLElement;
    expect(element.querySelector('header .name')).toBeNull();
    expect(element.querySelector('header button.sign-out')).toBeNull();
  });

  it('shows the name and "Sign out" with their icons when signed in', async () => {
    session.set(SIGNED_IN);
    const element = (await render()).nativeElement as HTMLElement;
    const name = element.querySelector('header .name')!;
    const signOutButton = element.querySelector('header button.sign-out')!;
    expect(name.textContent?.trim()).toBe('Ana Souza');
    expect(name.querySelector('svg.icon')?.getAttribute('aria-hidden')).toBe('true');
    expect(signOutButton.textContent?.trim()).toBe('Sign out');
    expect(signOutButton.querySelector('svg.icon')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('signs out and opens the home page', async () => {
    session.set(SIGNED_IN);
    const fixture = await render('/welcome');
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('header button.sign-out')!.click();
    await fixture.whenStable();

    expect(signOut).toHaveBeenCalledOnce();
    expect(TestBed.inject(Router).url).toBe('/');
    expect((fixture.nativeElement as HTMLElement).querySelector('header button.sign-out')).toBeNull();
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
    expect(options.map((option) => option.querySelector('app-flag svg')?.getAttribute('viewBox'))).toEqual([
      '0 0 20 14',
      '0 0 38 20',
      '0 0 3 2',
    ]);
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
