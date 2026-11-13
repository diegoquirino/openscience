import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NavigationError } from '@angular/router';
import { Subject } from 'rxjs';
import { isChunkLoadError, offerReloadOnChunkError, RELOAD_PAGE } from './chunk-error';

describe('chunk load error handler', () => {
  it('recognizes the chunk load messages of the browsers', () => {
    expect(isChunkLoadError(new TypeError('Failed to fetch dynamically imported module: /en-US/chunk-X.js'))).toBe(true);
    expect(isChunkLoadError(new TypeError('error loading dynamically imported module'))).toBe(true);
    expect(isChunkLoadError(new TypeError('Importing a module script failed.'))).toBe(true);
    expect(isChunkLoadError(new Error('Cannot match any routes'))).toBe(false);
  });

  it('offers a reload for a chunk load error and nothing for other errors', () => {
    const action = new Subject<void>();
    const open = vi.fn(() => ({ onAction: () => action }));
    const reload = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatSnackBar, useValue: { open } },
        { provide: RELOAD_PAGE, useValue: reload },
      ],
    });
    const failure = (error: unknown) => new NavigationError(1, '/participate', error);

    TestBed.runInInjectionContext(() => offerReloadOnChunkError(failure(new Error('Cannot match any routes'))));
    expect(open).not.toHaveBeenCalled();

    TestBed.runInInjectionContext(() =>
      offerReloadOnChunkError(failure(new TypeError('Failed to fetch dynamically imported module'))),
    );
    expect(open).toHaveBeenCalledWith('A new version is available; reload the page.', 'Reload');
    expect(reload).not.toHaveBeenCalled();

    action.next();
    expect(reload).toHaveBeenCalledOnce();
  });
});
