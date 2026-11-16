import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, switchMap } from 'rxjs';
import { ENTRY_POINT_URL } from '../../core/entry-point.service';
import { HalResource, Links } from '../../core/hal';

/** Tasks of one role by bucket; an expired reservation is already counted as free by the server. */
export interface RoleProgress {
  free: number;
  inProgress: number;
  completed: number;
  completedPercent: number;
}

/** The overall row has a null system and batch; the others are one per system and import batch. */
export interface ProgressRow {
  system: string | null;
  batchId: number | null;
  batchFileName: string | null;
  items: number;
  blocks: number;
  role1: RoleProgress;
  role2: RoleProgress;
}

export interface Reservation {
  pseudonym: string;
  system: string;
  block: number;
  role: number;
  draftCount: number;
  expiresAt: string;
  expired: boolean;
}

export interface Contributor {
  pseudonym: string;
  completedTasks: number;
  shareOfCompleted: number;
}

export interface Participants {
  eligible: number;
  ineligible: number;
  declinedConsent: number;
  incomplete: number;
  bySkill: Record<string, number>;
  completedByPseudonym: Contributor[];
}

export interface LanguageRow {
  language: string;
  participantsPreferred: number;
  finalResponses: number;
}

export interface EmailQuota {
  emailsSentLast24h: number;
  dailyEmailLimit: number;
  emailsRemaining: number;
  lowRemaining: boolean;
}

/** The read-only snapshot of `GET /api/v1/dashboard` (spec 010); it carries no name or e-mail. */
export interface DashboardSnapshot extends HalResource {
  generatedAt: string;
  firstFreeQueuePosition: number | null;
  progress: ProgressRow[];
  reservations: Reservation[];
  participants: Participants;
  languages: LanguageRow[];
  emailQuota: EmailQuota;
}

/** The snapshot and the way to the import, which the server links only while no item exists. */
export interface DashboardView {
  snapshot: DashboardSnapshot;
  /** Address of `vd:batches` when present; the empty study shortcut toward Import. */
  batchesHref: string | null;
}

/** Follows `vd:dashboard` from the entry point; no address is ever built here (spec 010 contracts/web.md). */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  /** Reads the snapshot; calling it again is the Refresh, and it changes nothing on the server. */
  load(): Observable<DashboardView> {
    return this.http.get<HalResource>(ENTRY_POINT_URL).pipe(
      switchMap((entry) => {
        const href = Links.of(entry).href('dashboard');
        if (!href) {
          throw new Error('vd:dashboard missing');
        }
        return this.http.get<DashboardSnapshot>(href);
      }),
      map((snapshot) => ({
        snapshot,
        batchesHref: Links.of(snapshot).href('batches') ?? null,
      })),
    );
  }
}
