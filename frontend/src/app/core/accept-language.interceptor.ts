import { HttpInterceptorFn } from '@angular/common/http';
import { inject, LOCALE_ID } from '@angular/core';

/** Every API call carries the locale of this build, so Problem Details come in the same language. */
export const acceptLanguageInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiUrl(request.url)) {
    return next(request);
  }
  const locale = inject(LOCALE_ID);
  return next(request.clone({ setHeaders: { 'Accept-Language': locale } }));
};

export function isApiUrl(url: string): boolean {
  const path = url.startsWith('http') ? new URL(url).pathname : url;
  return path === '/api' || path.startsWith('/api/');
}
