import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Links } from '../core/hal';
import { SessionService } from '../core/session.service';
import { AdminComponent } from './admin.component';

describe('AdminComponent', () => {
  it('shows the administrator name with Import and Export enabled and the other functions disabled', async () => {
    TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [provideHttpClient(), provideRouter([])],
    });
    TestBed.inject(SessionService).session.set({
      subject: 'administrator',
      name: 'Diego Quirino',
      pseudonym: null,
      preferredLanguage: 'pt-BR',
      expiresAt: '2026-11-14T17:03:00Z',
      links: Links.of({ _links: {} }),
    });

    const fixture = TestBed.createComponent(AdminComponent);
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Administration');
    expect(element.textContent).toContain('Signed in as Diego Quirino');
    const links = Array.from(element.querySelectorAll<HTMLAnchorElement>('mat-nav-list a'));
    expect(links.map((link) => link.textContent?.trim())).toEqual(['Import', 'Export']);
    expect(links[0].getAttribute('href')).toContain('/admin/import');
    expect(links[1].getAttribute('href')).toContain('/admin/export');
    const disabled = Array.from(element.querySelectorAll<HTMLButtonElement>('mat-nav-list button'));
    expect(disabled.map((item) => item.textContent?.trim())).toEqual(['Dashboard', 'Participants']);
    expect(disabled.every((item) => item.disabled)).toBe(true);
    expect(element.querySelector('.soon')?.textContent).toBe(
      'Dashboard and participants will be available in the next releases.',
    );
  });
});
