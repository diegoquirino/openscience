import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { from, map, Observable, switchMap } from 'rxjs';
import { ENTRY_POINT_URL } from '../../core/entry-point.service';
import { HalResource, Links } from '../../core/hal';
import { Problem } from '../../core/problem-messages';

/** The three downloads of one system, each with the href the server gave. */
export interface SystemExports {
  system: string;
  completoHref: string | null;
  rater1Href: string | null;
  rater2Href: string | null;
}

export interface ExportIndex {
  systems: SystemExports[];
  wideHref: string | null;
  longHref: string | null;
}

export interface CsvFile {
  filename: string;
  blob: Blob;
}

interface ExportIndexBody extends HalResource {
  systems: string[];
}

/** Follows `vd:exports` from the entry point; no address is ever built here (spec 009 contracts/web.md). */
@Injectable({ providedIn: 'root' })
export class ExportService {
  private readonly http = inject(HttpClient);

  index(): Observable<ExportIndex> {
    return this.http.get<HalResource>(ENTRY_POINT_URL).pipe(
      switchMap((entry) => {
        const href = Links.of(entry).href('exports');
        if (!href) {
          throw new Error('vd:exports missing');
        }
        return this.http.get<ExportIndexBody>(href);
      }),
      map(toIndex),
    );
  }

  /** Downloads a CSV by its HAL href; the file name comes from `Content-Disposition`. */
  download(href: string): Observable<CsvFile> {
    return this.http
      .get(href, {
        observe: 'response',
        responseType: 'blob',
        headers: { Accept: 'text/csv, application/problem+json' },
      })
      .pipe(
        map((response) => ({
          filename: filenameOf(response.headers.get('Content-Disposition')) ?? fallbackName(href),
          blob: response.body as Blob,
        })),
      );
  }
}

/** The Problem Details of a failed download, whose body arrives as a blob. */
export function problemOfBlob(error: unknown): Observable<Problem | undefined> {
  const body: unknown = error instanceof HttpErrorResponse ? error.error : undefined;
  if (!(body instanceof Blob)) {
    return from(Promise.resolve(undefined));
  }
  return from(
    body.text().then((text) => {
      try {
        const parsed = JSON.parse(text) as Problem;
        return typeof parsed.type === 'string' ? parsed : undefined;
      } catch {
        return undefined;
      }
    }),
  );
}

/** `filename="veredito_wide.csv"` from a `Content-Disposition` value. */
export function filenameOf(disposition: string | null): string | null {
  if (!disposition) {
    return null;
  }
  const quoted = /filename="([^"]+)"/.exec(disposition);
  if (quoted) {
    return quoted[1];
  }
  return /filename=([^;]+)/.exec(disposition)?.[1].trim() ?? null;
}

function fallbackName(href: string): string {
  const path = href.split('?')[0];
  return `${path.substring(path.lastIndexOf('/') + 1) || 'export'}.csv`;
}

function toIndex(body: ExportIndexBody): ExportIndex {
  const links = Links.of(body);
  const byRel = (rel: string): Map<string, string> => {
    const hrefs = new Map<string, string>();
    for (const link of links.all(rel)) {
      const system = new URL(link.href, 'http://localhost').searchParams.get('system');
      if (system !== null) {
        hrefs.set(system, link.href);
      }
    }
    return hrefs;
  };
  const completo = byRel('export-completo');
  const rater1 = byRel('export-rater1');
  const rater2 = byRel('export-rater2');
  return {
    wideHref: links.href('export-wide') ?? null,
    longHref: links.href('export-long') ?? null,
    systems: (body.systems ?? []).map((system) => ({
      system,
      completoHref: completo.get(system) ?? null,
      rater1Href: rater1.get(system) ?? null,
      rater2Href: rater2.get(system) ?? null,
    })),
  };
}