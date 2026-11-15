import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { isApiUrl } from './accept-language.interceptor';
import { problemOf, problemSlug } from './problem-messages';

/**
 * A `403 not-eligible` from any API call means the participant is not at the labeling step (the
 * consent form changed, for example): open the resolver, which sends them to the pending step
 * (spec 006, contracts/web.md "Guards").
 */
export const notEligibleInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiUrl(request.url)) {
    return next(request);
  }
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 403 &&
        problemSlug(problemOf(error)) === 'not-eligible'
      ) {
        void router.navigateByUrl('/welcome');
      }
      return throwError(() => error);
    }),
  );
};
