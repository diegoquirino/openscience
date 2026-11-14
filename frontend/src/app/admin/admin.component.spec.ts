import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Links } from '../core/hal';
import { SessionService } from '../core/session.service';
import { AdminComponent } from './admin.component';

describe('AdminComponent', () => {
  it('shows the administrator name and the four functions, all disabled', async () => {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: [provideHttpClient()] });
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
    const items = Array.from(element.querySelectorAll<HTMLButtonElement>('mat-action-list button'));
    expect(items.map((item) => item.textContent?.trim())).toEqual(['Import', 'Export', 'Dashboard', 'Participants']);
    expect(items.every((item) => item.disabled)).toBe(true);
  });
});
