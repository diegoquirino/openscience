import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { provideRouter, withNavigationErrorHandler } from '@angular/router';
import { routes } from './app.routes';
import { acceptLanguageInterceptor } from './core/accept-language.interceptor';
import { offerReloadOnChunkError } from './core/chunk-error';
import { unauthorizedInterceptor } from './core/unauthorized.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withNavigationErrorHandler(offerReloadOnChunkError)),
    provideHttpClient(withFetch(), withInterceptors([acceptLanguageInterceptor, unauthorizedInterceptor])),
    { provide: MAT_DATE_LOCALE, useFactory: () => inject(LOCALE_ID) },
  ],
};
