import { Injectable, inject } from '@angular/core';
import { MatSnackBar, MatSnackBarRef, TextOnlySnackBar } from '@angular/material/snack-bar';

/** Transient feedback kinds shown as coloured toasts (spec 019). */
export type ToastKind = 'error' | 'warn' | 'info' | 'success';

/** Minimum visible time for action/API feedback toasts. */
export const TOAST_DURATION_MS = 10_000;

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly snackBar = inject(MatSnackBar);

  private readonly closeLabel = $localize`:@@toast.close:Close`;

  show(kind: ToastKind, message: string): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, this.closeLabel, {
      duration: TOAST_DURATION_MS,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['toast', `toast-${kind}`],
    });
  }

  error(message: string): MatSnackBarRef<TextOnlySnackBar> {
    return this.show('error', message);
  }

  warn(message: string): MatSnackBarRef<TextOnlySnackBar> {
    return this.show('warn', message);
  }

  info(message: string): MatSnackBarRef<TextOnlySnackBar> {
    return this.show('info', message);
  }

  success(message: string): MatSnackBarRef<TextOnlySnackBar> {
    return this.show('success', message);
  }

  /**
   * Toast with a custom action label (e.g. Reload after a chunk load failure).
   * Same duration and info styling as other toasts; the action dismisses the bar.
   */
  action(kind: ToastKind, message: string, actionLabel: string): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, actionLabel, {
      duration: TOAST_DURATION_MS,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['toast', `toast-${kind}`],
    });
  }
}
