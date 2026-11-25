import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  LOCALE_ID,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerIntl } from '@angular/material/datepicker';
import { provideRouter, withNavigationErrorHandler } from '@angular/router';
import { routes } from './app.routes';
import { acceptLanguageInterceptor } from './core/accept-language.interceptor';
import { offerReloadOnChunkError } from './core/chunk-error';
import { LanguageService } from './core/language.service';
import { LocalizedDatepickerIntl } from './core/localized-datepicker-intl';
import { notEligibleInterceptor } from './core/not-eligible.interceptor';
import { unauthorizedInterceptor } from './core/unauthorized.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withNavigationErrorHandler(offerReloadOnChunkError)),
    provideHttpClient(withFetch(), withInterceptors([acceptLanguageInterceptor, unauthorizedInterceptor, notEligibleInterceptor])),
    { provide: MAT_DATE_LOCALE, useFactory: () => inject(LOCALE_ID) },
    provideNativeDateAdapter(),
    { provide: MatDatepickerIntl, useClass: LocalizedDatepickerIntl },
    provideAppInitializer(() => inject(LanguageService).setCookie(inject(LOCALE_ID))),
  ],
};
