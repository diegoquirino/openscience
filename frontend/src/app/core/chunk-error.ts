import { DOCUMENT, inject, InjectionToken } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NavigationError } from '@angular/router';

/** Messages of Chromium, Firefox, Safari and webpack when a lazy chunk is gone after a deployment. */
const CHUNK_LOAD_FAILURE =
  /ChunkLoadError|Failed to fetch dynamically imported module|error loading dynamically imported module|Importing a module script failed/i;

export const RELOAD_PAGE = new InjectionToken<() => void>('reload page', {
  providedIn: 'root',
  factory: () => {
    const document = inject(DOCUMENT);
    return () => document.location.reload();
  },
});

export function isChunkLoadError(error: unknown): boolean {
  const text = error instanceof Error ? `${error.name} ${error.message}` : String(error);
  return CHUNK_LOAD_FAILURE.test(text);
}

/** Router navigation error handler; runs in the injection context. */
export function offerReloadOnChunkError(navigationError: NavigationError): void {
  if (!isChunkLoadError(navigationError.error)) {
    return;
  }
  const reload = inject(RELOAD_PAGE);
  inject(MatSnackBar)
    .open(
      $localize`:@@newVersion.message:A new version is available; reload the page.`,
      $localize`:@@newVersion.reload:Reload`,
    )
    .onAction()
    .subscribe(() => reload());
}
