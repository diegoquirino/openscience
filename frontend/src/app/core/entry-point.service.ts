import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { HalResource, Links } from './hal';

export type Environment = 'production' | 'staging' | 'development';

export interface EntryPoint {
  systemVersion: string | null;
  environment: Environment;
  available: boolean;
  links: Links;
}

interface EntryPointBody extends HalResource {
  systemVersion: string;
  environment: Environment;
  available: boolean;
}

export const ENTRY_POINT_URL = '/api/v1';

/** On any failure the system is shown as unavailable and the staging banner stays hidden. */
export const UNREACHABLE: EntryPoint = {
  systemVersion: null,
  environment: 'production',
  available: false,
  links: new Links(),
};

@Injectable({ providedIn: 'root' })
export class EntryPointService {
  private readonly http = inject(HttpClient);

  load(): Observable<EntryPoint> {
    return this.http.get<EntryPointBody>(ENTRY_POINT_URL).pipe(
      map((body) => ({
        systemVersion: body.systemVersion,
        environment: body.environment,
        available: body.available,
        links: Links.of(body),
      })),
      catchError(() => of(UNREACHABLE)),
    );
  }
}
