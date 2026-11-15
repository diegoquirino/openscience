import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, of, switchMap } from 'rxjs';
import { ENTRY_POINT_URL } from '../../core/entry-point.service';
import { HalResource, Links } from '../../core/hal';

export interface RowIssue {
  line: number;
  code: string;
  reason: string;
  system: string;
  editKey: string;
}

export interface ImportPreview {
  id: number;
  fileName: string;
  expiresAt: string;
  includedCount: number;
  ignoredCount: number;
  errorCount: number;
  blockCount: number;
  blockSizes: number[];
  extraColumns: string[];
  ignored: RowIssue[];
  errors: RowIssue[];
  confirmHref: string | null;
}

export interface ImportBatch {
  id: number | null;
  fileName: string;
  importedAt: string | null;
  includedCount: number;
  ignoredCount: number;
  errorCount: number;
  blockCount: number;
  blockSizes: number[];
  queuePositions: { from: number; to: number } | null;
  systems: string[];
}

export interface StudyPhase {
  phase: 'pilot' | 'collection';
  changedAt: string | null;
  switchAllowed: boolean;
  minitestOnly: boolean;
  selfHref: string;
}

interface PreviewBody extends HalResource {
  id: number;
  fileName: string;
  expiresAt: string;
  includedCount: number;
  ignoredCount: number;
  errorCount: number;
  blockCount: number;
  blockSizes: number[];
  extraColumns: string[];
  ignored: RowIssue[];
  errors: RowIssue[];
}

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);

  requestPreview(file: File): Observable<ImportPreview> {
    return this.http.get<HalResource>(ENTRY_POINT_URL).pipe(
      switchMap((entry) => {
        const href = Links.of(entry).href('request-preview');
        if (!href) {
          throw new Error('vd:request-preview missing');
        }
        const body = new FormData();
        body.append('file', file, file.name);
        return this.http.post<PreviewBody>(href, body);
      }),
      map(toPreview),
    );
  }

  confirm(preview: ImportPreview): Observable<ImportBatch> {
    const href = preview.confirmHref;
    if (!href) {
      throw new Error('vd:confirm missing');
    }
    return this.http.post<BatchBody>(href, { previewId: preview.id }).pipe(map(toBatch));
  }

  listBatches(): Observable<ImportBatch[]> {
    return this.http.get<HalResource>(ENTRY_POINT_URL).pipe(
      switchMap((entry) => {
        const href = Links.of(entry).href('batches');
        if (!href) {
          throw new Error('vd:batches missing');
        }
        return this.http.get<BatchListBody>(href);
      }),
      map((body) => {
        const embedded = body._embedded?.['vd:batches'];
        const rows = Array.isArray(embedded) ? embedded : embedded ? [embedded] : [];
        return rows.map(toBatch);
      }),
    );
  }

  studyPhase(): Observable<StudyPhase | null> {
    return this.http.get<HalResource>(ENTRY_POINT_URL).pipe(
      switchMap((entry) => {
        const href = Links.of(entry).href('study-phase');
        return href
          ? this.http.get<StudyPhaseBody>(href).pipe(map((body) => toStudyPhase(body, href)))
          : of(null);
      }),
    );
  }

  switchToCollection(selfHref: string): Observable<StudyPhase> {
    return this.http
      .put<StudyPhaseBody>(selfHref, { phase: 'collection' })
      .pipe(map((body) => toStudyPhase(body, selfHref)));
  }
}

interface StudyPhaseBody extends HalResource {
  phase: StudyPhase['phase'];
  changedAt?: string | null;
  switchAllowed: boolean;
  minitestOnly: boolean;
}

interface BatchBody extends HalResource {
  id?: number;
  fileName: string;
  importedAt?: string;
  includedCount: number;
  ignoredCount: number;
  errorCount: number;
  blockCount: number;
  blockSizes: number[];
  queuePositions?: { from: number; to: number };
  systems: string[];
}

interface BatchListBody extends HalResource {
  _embedded?: { 'vd:batches'?: BatchBody | BatchBody[] };
}

function toPreview(body: PreviewBody): ImportPreview {
  return {
    id: body.id,
    fileName: body.fileName,
    expiresAt: body.expiresAt,
    includedCount: body.includedCount,
    ignoredCount: body.ignoredCount,
    errorCount: body.errorCount,
    blockCount: body.blockCount,
    blockSizes: body.blockSizes ?? [],
    extraColumns: body.extraColumns ?? [],
    ignored: body.ignored ?? [],
    errors: body.errors ?? [],
    confirmHref: Links.of(body).href('confirm') ?? null,
  };
}

function toBatch(body: BatchBody): ImportBatch {
  return {
    id: body.id ?? null,
    fileName: body.fileName,
    importedAt: body.importedAt ?? null,
    includedCount: body.includedCount,
    ignoredCount: body.ignoredCount,
    errorCount: body.errorCount,
    blockCount: body.blockCount,
    blockSizes: body.blockSizes ?? [],
    queuePositions: body.queuePositions ?? null,
    systems: body.systems ?? [],
  };
}

function toStudyPhase(body: StudyPhaseBody, fallbackHref: string): StudyPhase {
  return {
    phase: body.phase,
    changedAt: body.changedAt ?? null,
    switchAllowed: body.switchAllowed,
    minitestOnly: body.minitestOnly,
    selfHref: Links.of(body).href('self') ?? fallbackHref,
  };
}
