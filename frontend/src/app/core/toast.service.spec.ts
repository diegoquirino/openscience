import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ToastService, TOAST_DURATION_MS } from './toast.service';

describe('ToastService', () => {
  it('opens a typed snack bar for at least 10 seconds with Close', () => {
    const open = vi.fn(() => ({ onAction: () => ({ subscribe: vi.fn() }) }));
    TestBed.configureTestingModule({
      providers: [{ provide: MatSnackBar, useValue: { open } }],
    });
    const toast = TestBed.inject(ToastService);

    toast.error('Something failed');
    expect(open).toHaveBeenCalledWith(
      'Something failed',
      'Close',
      expect.objectContaining({
        duration: TOAST_DURATION_MS,
        panelClass: ['toast', 'toast-error'],
      }),
    );
    expect(TOAST_DURATION_MS).toBeGreaterThanOrEqual(10_000);

    toast.success('Done');
    expect(open).toHaveBeenLastCalledWith(
      'Done',
      'Close',
      expect.objectContaining({ panelClass: ['toast', 'toast-success'] }),
    );

    toast.warn('Careful');
    expect(open).toHaveBeenLastCalledWith(
      'Careful',
      'Close',
      expect.objectContaining({ panelClass: ['toast', 'toast-warn'] }),
    );

    toast.info('Note');
    expect(open).toHaveBeenLastCalledWith(
      'Note',
      'Close',
      expect.objectContaining({ panelClass: ['toast', 'toast-info'] }),
    );
  });

  it('supports a custom action label with the same duration', () => {
    const open = vi.fn(() => ({ onAction: () => ({ subscribe: vi.fn() }) }));
    TestBed.configureTestingModule({
      providers: [{ provide: MatSnackBar, useValue: { open } }],
    });
    TestBed.inject(ToastService).action('info', 'Reload?', 'Reload');
    expect(open).toHaveBeenCalledWith(
      'Reload?',
      'Reload',
      expect.objectContaining({
        duration: TOAST_DURATION_MS,
        panelClass: ['toast', 'toast-info'],
      }),
    );
  });
});
