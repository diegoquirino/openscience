import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Links } from '../core/hal';
import { SessionService } from '../core/session.service';
import { AdminComponent } from './admin.component';

describe('AdminComponent', () => {
  it('shows the administrator name with Import, Export, Dashboard and Participants enabled', async () => {
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
    expect(element.querySelector('h1 span')?.textContent?.trim()).toBe('Administration');
    expect(element.textContent).toContain('Signed in as Diego Quirino');
    const links = Array.from(element.querySelectorAll<HTMLAnchorElement>('a.action-card'));
    expect(links.map((link) => link.querySelector('.action-card-label')?.textContent?.trim())).toEqual([
      'Import',
      'Export',
      'Dashboard',
      'Anonymization of Participants',
      'E-mail send settings',
    ]);
    expect(links[0].getAttribute('href')).toContain('/admin/import');
    expect(links[1].getAttribute('href')).toContain('/admin/export');
    expect(links[2].getAttribute('href')).toContain('/admin/dashboard');
    expect(links[3].getAttribute('href')).toContain('/admin/participants');
    expect(links[4].getAttribute('href')).toContain('/admin/email-settings');
    expect(element.querySelectorAll('mat-icon.action-card-icon').length).toBe(5);
  });
});
