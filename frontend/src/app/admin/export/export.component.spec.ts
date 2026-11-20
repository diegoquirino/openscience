import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ToastService } from '../../core/toast.service';
import { ExportComponent } from './export.component';
import { filenameOf, ExportService, problemOfBlob } from './export.service';
import { FileSaver } from './file-saver';

const ENTRY = { _links: { 'vd:exports': { href: '/api/v1/exports' } } };

const INDEX = {
  systems: ['minitest', 'other sys'],
  _links: {
    self: { href: '/api/v1/exports' },
    'vd:export-wide': { href: '/api/v1/exports/wide' },
    'vd:export-long': { href: '/api/v1/exports/long' },
    'vd:export-completo': [
      { href: '/api/v1/exports/completo?system=minitest' },
      { href: '/api/v1/exports/completo?system=other+sys' },
    ],
    'vd:export-rater1': [
      { href: '/api/v1/exports/rater1?system=minitest' },
      { href: '/api/v1/exports/rater1?system=other+sys' },
    ],
    'vd:export-rater2': [
      { href: '/api/v1/exports/rater2?system=minitest' },
      { href: '/api/v1/exports/rater2?system=other+sys' },
    ],
  },
};

class FakeSaver {
  saved: { filename: string; blob: Blob }[] = [];
  save(filename: string, blob: Blob): void {
    this.saved.push({ filename, blob });
  }
}

describe('ExportComponent', () => {
  let http: HttpTestingController;
  let saver: FakeSaver;

  function setup() {
    saver = new FakeSaver();
    TestBed.configureTestingModule({
      imports: [ExportComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: FileSaver, useValue: saver },
        {
          provide: ToastService,
          useValue: { error: vi.fn(), success: vi.fn(), warn: vi.fn(), info: vi.fn(), show: vi.fn(), action: vi.fn() },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ExportComponent);
    fixture.detectChanges();
    return fixture;
  }

  async function load(fixture: ReturnType<typeof setup>, index: object = INDEX) {
    http.expectOne('/api/v1').flush(ENTRY);
    await Promise.resolve();
    http.expectOne('/api/v1/exports').flush(index);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const button = (fixture: ReturnType<typeof setup>, selector: string): HTMLButtonElement =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(selector) as HTMLButtonElement;

  it('follows vd:exports and offers wide, long and the three files of each system', async () => {
    const fixture = setup();
    await load(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Export');
    expect(button(fixture, '[data-testid="export-wide"]').textContent?.trim()).toBe('Download wide table');
    expect(button(fixture, '[data-testid="export-long"]').textContent?.trim()).toBe('Download long table');
    const groups = Array.from(element.querySelectorAll('.system'));
    expect(groups.map((group) => group.getAttribute('data-system'))).toEqual(['minitest', 'other sys']);
    expect(groups[0].querySelector('h2')?.textContent?.trim()).toBe('System minitest');
    expect(
      Array.from(groups[0].querySelectorAll('button')).map((item) => item.textContent?.trim()),
    ).toEqual(['Download completo', 'Download rater 1', 'Download rater 2']);
    expect(element.querySelector('.empty')).toBeNull();
  });

  it('downloads the wide table with the file name of Content-Disposition', async () => {
    const fixture = setup();
    await load(fixture);

    button(fixture, '[data-testid="export-wide"]').click();
    const request = http.expectOne('/api/v1/exports/wide');
    expect(request.request.responseType).toBe('blob');
    expect(request.request.headers.get('Accept')).toContain('text/csv');
    request.flush(new Blob(['system,edit_key\n']), {
      headers: { 'Content-Disposition': 'attachment; filename="veredito_wide.csv"' },
    });
    await fixture.whenStable();

    expect(saver.saved.map((file) => file.filename)).toEqual(['veredito_wide.csv']);
  });

  it('uses the concrete href of each system without building one', async () => {
    const fixture = setup();
    await load(fixture);

    const second = (fixture.nativeElement as HTMLElement).querySelectorAll('.system')[1];
    second.querySelector<HTMLButtonElement>('[data-testid="export-rater2"]')?.click();
    http
      .expectOne('/api/v1/exports/rater2?system=other+sys')
      .flush(new Blob(['x']), { headers: { 'Content-Disposition': 'attachment; filename="t1_other_sys_rater2.csv"' } });
    await fixture.whenStable();

    expect(saver.saved[0].filename).toBe('t1_other_sys_rater2.csv');
  });

  it('shows the empty state and still offers wide and long when there is no system', async () => {
    const fixture = setup();
    await load(fixture, {
      systems: [],
      _links: { 'vd:export-wide': { href: '/api/v1/exports/wide' }, 'vd:export-long': { href: '/api/v1/exports/long' } },
    });

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.empty')?.textContent?.trim()).toBe(
      'No items have been imported yet, so there are no per-system files.',
    );
    expect(button(fixture, '[data-testid="export-wide"]').disabled).toBe(false);
    expect(button(fixture, '[data-testid="export-long"]').disabled).toBe(false);
    expect(element.querySelectorAll('.system').length).toBe(0);
  });

  it('shows the message of a problem returned by a download', async () => {
    const fixture = setup();
    await load(fixture);

    button(fixture, '[data-testid="export-completo"]').click();
    http.expectOne('/api/v1/exports/completo?system=minitest').flush(
      new Blob([JSON.stringify({ type: 'urn:veredito:problem:system-not-found', system: 'minitest' })]),
      { status: 404, statusText: 'Not Found' },
    );
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
    fixture.detectChanges();

    const toast = TestBed.inject(ToastService) as unknown as { error: ReturnType<typeof vi.fn> };
    expect(toast.error).toHaveBeenCalledWith('There are no items for the system minitest');
    expect(saver.saved).toEqual([]);
  });
});

describe('export helpers', () => {
  it('reads the file name of Content-Disposition', () => {
    expect(filenameOf('attachment; filename="veredito_wide.csv"')).toBe('veredito_wide.csv');
    expect(filenameOf('attachment; filename=t1_x_rater1.csv')).toBe('t1_x_rater1.csv');
    expect(filenameOf(null)).toBeNull();
    expect(filenameOf('inline')).toBeNull();
  });

  it('reads the Problem Details out of a blob error and ignores anything else', async () => {
    const withBody = new HttpErrorResponse({
      status: 404,
      error: new Blob([JSON.stringify({ type: 'urn:veredito:problem:system-not-found' })]),
    });
    const notJson = new HttpErrorResponse({ status: 500, error: new Blob(['oops']) });
    const noBody = new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') });

    const results: unknown[] = [];
    for (const error of [withBody, notJson, noBody, new Error('x')]) {
      results.push(await new Promise((resolve) => problemOfBlob(error).subscribe(resolve)));
    }

    expect(results).toEqual([{ type: 'urn:veredito:problem:system-not-found' }, undefined, undefined, undefined]);
  });

  it('groups the per-system relations by their system query', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(ExportService);
    const http = TestBed.inject(HttpTestingController);

    const result = firstValueFrom(service.index());
    http.expectOne('/api/v1').flush(ENTRY);
    http.expectOne('/api/v1/exports').flush(INDEX);
    const view = await result;

    expect(view.wideHref).toBe('/api/v1/exports/wide');
    expect(view.longHref).toBe('/api/v1/exports/long');
    expect(view.systems.map((s) => [s.system, s.completoHref, s.rater1Href, s.rater2Href])).toEqual([
      [
        'minitest',
        '/api/v1/exports/completo?system=minitest',
        '/api/v1/exports/rater1?system=minitest',
        '/api/v1/exports/rater2?system=minitest',
      ],
      [
        'other sys',
        '/api/v1/exports/completo?system=other+sys',
        '/api/v1/exports/rater1?system=other+sys',
        '/api/v1/exports/rater2?system=other+sys',
      ],
    ]);
  });
});