import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ToastService } from '../../core/toast.service';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from './dashboard.service';

const ENTRY = { _links: { 'vd:dashboard': { href: '/api/v1/dashboard' } } };

const SNAPSHOT = {
  generatedAt: '2026-11-16T12:00:00Z',
  firstFreeQueuePosition: 3,
  progress: [
    {
      system: null,
      batchId: null,
      batchFileName: null,
      items: 38,
      blocks: 4,
      role1: { free: 2, inProgress: 1, completed: 1, completedPercent: 25 },
      role2: { free: 4, inProgress: 0, completed: 0, completedPercent: 0 },
    },
    {
      system: 'minitest',
      batchId: 1,
      batchFileName: 'veredito_minitest_vazio.csv',
      items: 38,
      blocks: 4,
      role1: { free: 2, inProgress: 1, completed: 1, completedPercent: 25 },
      role2: { free: 4, inProgress: 0, completed: 0, completedPercent: 0 },
    },
  ],
  reservations: [
    { pseudonym: 'R-0002', system: 'minitest', block: 3, role: 2, draftCount: 1, expiresAt: '2026-11-16T11:55:00Z', expired: true },
    { pseudonym: 'R-0001', system: 'minitest', block: 2, role: 1, draftCount: 3, expiresAt: '2026-11-16T12:40:00Z', expired: false },
  ],
  participants: {
    eligible: 3,
    ineligible: 1,
    declinedConsent: 1,
    incomplete: 2,
    bySkill: { test_analyst: 3, developer: 2 },
    completedByPseudonym: [{ pseudonym: 'R-0001', completedTasks: 3, shareOfCompleted: 0.75 }],
  },
  languages: [
    { language: 'pt-BR', participantsPreferred: 2, finalResponses: 10 },
    { language: 'en-US', participantsPreferred: 1, finalResponses: 0 },
    { language: 'es-ES', participantsPreferred: 0, finalResponses: 0 },
  ],
  emailQuota: { emailsSentLast24h: 6, dailyEmailLimit: 15, emailsRemaining: 9, lowRemaining: true },
  _links: { self: { href: '/api/v1/dashboard' } },
};

const EMPTY = {
  generatedAt: '2026-11-16T12:00:00Z',
  firstFreeQueuePosition: null,
  progress: [
    {
      system: null,
      batchId: null,
      batchFileName: null,
      items: 0,
      blocks: 0,
      role1: { free: 0, inProgress: 0, completed: 0, completedPercent: 0 },
      role2: { free: 0, inProgress: 0, completed: 0, completedPercent: 0 },
    },
  ],
  reservations: [],
  participants: { eligible: 0, ineligible: 0, declinedConsent: 0, incomplete: 0, bySkill: {}, completedByPseudonym: [] },
  languages: [
    { language: 'pt-BR', participantsPreferred: 0, finalResponses: 0 },
    { language: 'en-US', participantsPreferred: 0, finalResponses: 0 },
    { language: 'es-ES', participantsPreferred: 0, finalResponses: 0 },
  ],
  emailQuota: { emailsSentLast24h: 0, dailyEmailLimit: 450, emailsRemaining: 450, lowRemaining: false },
  _links: { self: { href: '/api/v1/dashboard' }, 'vd:batches': { href: '/api/v1/batches' } },
};

describe('DashboardComponent', () => {
  let http: HttpTestingController;

  function setup() {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ToastService,
          useValue: { error: vi.fn(), success: vi.fn(), warn: vi.fn(), info: vi.fn(), show: vi.fn(), action: vi.fn() },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    return fixture;
  }

  async function load(fixture: ReturnType<typeof setup>, snapshot: object = SNAPSHOT) {
    http.expectOne('/api/v1').flush(ENTRY);
    await Promise.resolve();
    http.expectOne('/api/v1/dashboard').flush(snapshot);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const text = (fixture: ReturnType<typeof setup>, selector: string): string | undefined =>
    (fixture.nativeElement as HTMLElement).querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();

  const cells = (fixture: ReturnType<typeof setup>, selector: string): string[] =>
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll(`${selector} > th, ${selector} > td`)).map(
      (cell) => cell.textContent?.replace(/\s+/g, ' ').trim() ?? '',
    );

  it('follows vd:dashboard and shows the overall row first with the buckets and percentages', async () => {
    const fixture = setup();
    await load(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1 span')?.textContent?.trim()).toBe('Dashboard');
    const rows = Array.from(element.querySelectorAll('[data-testid="panel-progress"] tbody tr'));
    expect(rows.map((row) => row.getAttribute('data-row'))).toEqual(['overall', 'system']);
    const overall = rows[0];
    expect(overall.querySelector('th')?.textContent?.trim()).toBe('All systems');
    expect(overall.querySelector('[data-col="role1-free"]')?.textContent).toBe('2');
    expect(overall.querySelector('[data-col="role1-in-progress"]')?.textContent).toBe('1');
    expect(overall.querySelector('[data-col="role1-completed"]')?.textContent).toBe('1');
    expect(overall.querySelector('[data-col="role1-percent"]')?.textContent).toBe('25%');
    expect(overall.querySelector('[data-col="role2-free"]')?.textContent).toBe('4');
    expect(rows[1].textContent).toContain('minitest');
    expect(rows[1].textContent).toContain('veredito_minitest_vazio.csv');
    expect(text(fixture, '[data-testid="first-free"]')).toBe('First free position in the queue: 3');
    expect(element.querySelector('[data-testid="dashboard-empty"]')).toBeNull();
  });

  it('lists the reservations by pseudonym and labels the expired one', async () => {
    const fixture = setup();
    await load(fixture);

    const rows = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="reservation-row"]'),
    );
    expect(rows.length).toBe(2);
    expect(rows[0].querySelector('th')?.textContent?.trim()).toBe('R-0002');
    expect(rows[0].querySelector('[data-testid="expired-label"]')?.textContent?.trim()).toBe('Expired');
    expect(rows[1].querySelector('th')?.textContent?.trim()).toBe('R-0001');
    expect(rows[1].querySelector('[data-testid="expired-label"]')).toBeNull();
  });

  it('shows participants, skills with their labels, shares, languages and the quota', async () => {
    const fixture = setup();
    await load(fixture);

    expect(text(fixture, '[data-testid="participants-eligible"]')).toBe('3');
    expect(text(fixture, '[data-testid="participants-ineligible"]')).toBe('1');
    expect(text(fixture, '[data-testid="participants-declined"]')).toBe('1');
    expect(text(fixture, '[data-testid="participants-incomplete"]')).toBe('2');
    expect(text(fixture, '[data-skill="test_analyst"] span')).toBe('Test analyst / QA');
    expect(text(fixture, '[data-skill="test_analyst"] strong')).toBe('3');
    expect(text(fixture, '[data-skill="developer"] span')).toBe('Software developer');
    expect(text(fixture, '[data-skill="developer"] strong')).toBe('2');
    expect(
      Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="contributor-row"] > *')).map((cell) =>
        cell.textContent?.trim(),
      ),
    ).toEqual(['R-0001', '3', '75%']);
    const languages = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('[data-language]'),
    ).map((row) => row.getAttribute('data-language'));
    expect(languages).toEqual(['pt-BR', 'en-US', 'es-ES']);
    expect(cells(fixture, '[data-language="pt-BR"]')).toEqual(['pt-BR', '2', '10']);
    expect(cells(fixture, '[data-language="es-ES"]')).toEqual(['es-ES', '0', '0']);
    expect(text(fixture, '[data-testid="quota-sent"]')).toBe('6');
    expect(text(fixture, '[data-testid="quota-limit"]')).toBe('15');
  });

  it('highlights the remaining e-mails only when few are left', async () => {
    const low = setup();
    await load(low);

    const remaining = (low.nativeElement as HTMLElement).querySelector('[data-testid="quota-remaining"]');
    expect(remaining?.classList.contains('low')).toBe(true);
    expect(text(low, '[data-testid="quota-low"]')).toBe('Fewer than 10 e-mails remain in the daily budget.');
  });

  it('does not highlight a comfortable quota', async () => {
    const fixture = setup();
    await load(fixture, { ...SNAPSHOT, emailQuota: { ...SNAPSHOT.emailQuota, emailsRemaining: 444, lowRemaining: false } });

    const remaining = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="quota-remaining"]');
    expect(remaining?.classList.contains('low')).toBe(false);
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="quota-low"]')).toBeNull();
  });

  it('shows no name or e-mail anywhere on the screen', async () => {
    const fixture = setup();
    await load(fixture);

    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(content).not.toContain('@');
    expect(content).toContain('R-0001');
  });

  it('shows the zeros and a shortcut to the import for an empty study', async () => {
    const fixture = setup();
    await load(fixture, EMPTY);

    const element = fixture.nativeElement as HTMLElement;
    expect(text(fixture, '[data-testid="dashboard-empty"]')).toContain('No items have been imported yet');
    expect(element.querySelector('[data-testid="dashboard-import"]')?.getAttribute('href')).toContain('/admin/import');
    expect(text(fixture, '[data-testid="first-free"]')).toBe('No free task is left in the queue.');
    expect(text(fixture, '[data-testid="panel-reservations"]')).toContain('No task is reserved right now.');
    expect(text(fixture, '[data-testid="panel-participants"]')).toContain('No task has been completed yet.');
    expect(element.querySelectorAll('[data-language]').length).toBe(3);
  });

  it('Refresh reads the same link again and replaces the numbers without reloading', async () => {
    const fixture = setup();
    await load(fixture);
    expect(text(fixture, '[data-row="overall"] [data-col="role1-completed"]')).toBe('1');

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[data-testid="dashboard-refresh"]')?.click();
    await load(fixture, {
      ...SNAPSHOT,
      progress: [
        { ...SNAPSHOT.progress[0], role1: { free: 1, inProgress: 1, completed: 2, completedPercent: 50 } },
        SNAPSHOT.progress[1],
      ],
    });

    expect(text(fixture, '[data-row="overall"] [data-col="role1-completed"]')).toBe('2');
    expect(text(fixture, '[data-row="overall"] [data-col="role1-percent"]')).toBe('50%');
  });

  it('shows a problem message when the snapshot cannot be read', async () => {
    const fixture = setup();
    const toast = TestBed.inject(ToastService) as unknown as { error: ReturnType<typeof vi.fn> };
    http.expectOne('/api/v1').flush(ENTRY);
    await Promise.resolve();
    http.expectOne('/api/v1/dashboard').flush('oops', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(toast.error).toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="panel-progress"]')).toBeNull();
  });
});

describe('DashboardService', () => {
  it('follows vd:dashboard and exposes the batches link of the empty study', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(DashboardService);
    const http = TestBed.inject(HttpTestingController);

    const result = firstValueFrom(service.load());
    http.expectOne('/api/v1').flush(ENTRY);
    http.expectOne('/api/v1/dashboard').flush(EMPTY);
    const view = await result;

    expect(view.batchesHref).toBe('/api/v1/batches');
    expect(view.snapshot.progress[0].items).toBe(0);
  });

  it('fails when the entry point offers no vd:dashboard', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(DashboardService);
    const http = TestBed.inject(HttpTestingController);

    const result = firstValueFrom(service.load()).catch((error: Error) => error.message);
    http.expectOne('/api/v1').flush({ _links: {} });

    expect(await result).toBe('vd:dashboard missing');
  });
});
