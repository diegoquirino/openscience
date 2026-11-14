import { HttpClient } from '@angular/common/http';
import { DestroyRef, DOCUMENT, inject, Injectable, InjectionToken, LOCALE_ID } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, map, Observable, of } from 'rxjs';
import { SessionService } from './session.service';

/** Supported locales, each labeled in its own language. */
export const LOCALES = [
  { code: 'pt-BR', label: 'Português (Brasil)' },
  { code: 'en-US', label: 'English (US)' },
  { code: 'es-ES', label: 'Español (España)' },
] as const;

export type Locale = (typeof LOCALES)[number]['code'];

export function isLocale(value: string): value is Locale {
  return LOCALES.some((option) => option.code === value);
}

export const LANGUAGE_COOKIE = 'veredito-lang';
const ONE_YEAR_SECONDS = 31_536_000;
const UNSAVED_PREFIX = 'veredito-unsaved:';

/** A full page load, which is what switches the build (each locale is a separate build). */
export const LOAD_PAGE = new InjectionToken<(url: string) => void>('load page', {
  providedIn: 'root',
  factory: () => {
    const document = inject(DOCUMENT);
    return (url: string) => document.location.assign(url);
  },
});

export type UnsavedValues = Record<string, string>;

/**
 * The language of the interface (contracts/web.md): the cookie `veredito-lang`, the header switch
 * and the values typed but not saved, which survive the reload under the new prefix.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly document = inject(DOCUMENT);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = inject(SessionService);
  private readonly loadPage = inject(LOAD_PAGE);
  readonly current = inject(LOCALE_ID);
  private unsaved: { route: string; values: () => UnsavedValues } | undefined;

  setCookie(locale: string): void {
    this.document.cookie = `${LANGUAGE_COOKIE}=${locale}; Path=/; Max-Age=${ONE_YEAR_SECONDS}; SameSite=Lax`;
  }

  /**
   * Saves the profile language when signed in, keeps the unsaved values of the open form, then
   * opens the same route under the new prefix. A failed profile update does not stop the switch.
   */
  switchTo(locale: Locale): Observable<void> {
    const href = this.session.session()?.links.href('me');
    const profile = href
      ? this.http.put(href, { preferredLanguage: locale }).pipe(
          map(() => undefined),
          catchError(() => of(undefined)),
        )
      : of(undefined);
    return profile.pipe(
      map(() => {
        if (this.unsaved) {
          this.saveUnsaved(this.unsaved.route, this.unsaved.values());
        }
        this.open(locale, this.router.url);
      }),
    );
  }

  /** Sets the cookie and loads `route` (without prefix) under the prefix of `locale`. */
  open(locale: string, route: string): void {
    this.setCookie(locale);
    this.loadPage(`/${locale}${route}`);
  }

  /**
   * Registers the values a form would lose on a language switch, until the form is destroyed;
   * call it in the form's constructor.
   */
  keepOnSwitch(route: string, values: () => UnsavedValues): void {
    const entry = { route, values };
    this.unsaved = entry;
    inject(DestroyRef).onDestroy(() => {
      if (this.unsaved === entry) {
        this.unsaved = undefined;
      }
    });
  }

  saveUnsaved(route: string, values: UnsavedValues): void {
    sessionStorage.setItem(UNSAVED_PREFIX + route, JSON.stringify(values));
  }

  /** The unsaved values of `route`, removed from storage as they are read. */
  takeUnsaved(route: string): UnsavedValues | undefined {
    const key = UNSAVED_PREFIX + route;
    const raw = sessionStorage.getItem(key);
    sessionStorage.removeItem(key);
    if (!raw) {
      return undefined;
    }
    try {
      const parsed: unknown = JSON.parse(raw);
      return parsed && typeof parsed === 'object' ? (parsed as UnsavedValues) : undefined;
    } catch {
      return undefined;
    }
  }

  clearUnsaved(route: string): void {
    sessionStorage.removeItem(UNSAVED_PREFIX + route);
  }
}
