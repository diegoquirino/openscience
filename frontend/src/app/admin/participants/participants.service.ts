import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, switchMap } from 'rxjs';
import { ENTRY_POINT_URL } from '../../core/entry-point.service';
import { HalResource, Links } from '../../core/hal';

export interface ReservedSummary {
  block: number;
  role: number;
  expiresAt: string;
}

export interface ParticipantCard extends HalResource {
  id: number;
  pseudonym: string;
  email: string | null;
  preferredLanguage: string | null;
  anonymizedAt: string | null;
  completedTasks: number;
  reserved: ReservedSummary | null;
  finalResponseCount: number;
  draftResponseCount: number;
  tasksReturnableOnDiscard: number;
  emailCanBeSent: boolean;
}

export interface AnonymizationResult {
  pseudonym: string;
  option: string;
  emailSent: boolean;
  finalResponsesDiscarded: number;
  tasksReturned: number;
  occurredAt: string;
}

/** Follows `vd:participants` / `vd:anonymize` from HAL (spec 011 contracts/web.md). */
@Injectable({ providedIn: 'root' })
export class ParticipantsService {
  private readonly http = inject(HttpClient);

  search(params: { email?: string; pseudonym?: string }): Observable<ParticipantCard[]> {
    return this.http.get<HalResource>(ENTRY_POINT_URL).pipe(
      switchMap((entry) => {
        const base = Links.of(entry).href('participants');
        if (!base) {
          throw new Error('vd:participants missing');
        }
        const query = params.email
          ? `?email=${encodeURIComponent(params.email)}`
          : `?pseudonym=${encodeURIComponent(params.pseudonym ?? '')}`;
        return this.http.get<{ participants: ParticipantCard[] }>(base + query);
      }),
      map((body) => body.participants ?? []),
    );
  }

  anonymize(
    card: ParticipantCard,
    option: 'keep_responses' | 'discard_responses',
  ): Observable<AnonymizationResult> {
    const href = Links.of(card).href('anonymize');
    if (!href) {
      throw new Error('vd:anonymize missing');
    }
    return this.http.post<AnonymizationResult>(href, { option });
  }
}
