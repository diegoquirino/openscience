import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { isApiUrl } from './accept-language.interceptor';
import { SessionService } from './session.service';

/** A `401` from any API call means the session is gone: forget it and open the sign-in form. */
export const unauthorizedInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiUrl(request.url)) {
    return next(request);
  }
  const session = inject(SessionService);
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        session.clear();
        void router.navigateByUrl('/participate');
      }
      return throwError(() => error);
    }),
  );
};
