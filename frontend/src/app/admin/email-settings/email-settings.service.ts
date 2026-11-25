import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { ENTRY_POINT_URL } from '../../core/entry-point.service';
import { HalResource, Links } from '../../core/hal';

export type InviteTemplateKind = 'inicial' | 'lembrete' | 'agradecimento';

export interface EmailSettings extends HalResource {
  caae: string | null;
  platformUrl: string;
  collectionStartAt: string | null;
  collectionEndAt: string | null;
  reminderAt: string | null;
}

export interface EmailTemplate extends HalResource {
  kind: InviteTemplateKind;
  subject: string;
  body: string;
  updatedAt: string;
}

export interface EmailSettingsPayload {
  caae: string | null;
  platformUrl: string;
  collectionStartAt: string | null;
  collectionEndAt: string | null;
  reminderAt: string | null;
}

/** Date (calendar) + clock time in the browser local zone, for Material datepicker + time input. */
export interface LocalDateTimeParts {
  date: Date | null;
  time: string;
}

/** Follows `vd:email-settings` / `vd:email-template` (specs 031–032). */
@Injectable({ providedIn: 'root' })
export class EmailSettingsService {
  private readonly http = inject(HttpClient);

  loadSettings(): Observable<EmailSettings> {
    return this.http.get<HalResource>(ENTRY_POINT_URL).pipe(
      switchMap((entry) => {
        const href = Links.of(entry).href('email-settings');
        if (!href) {
          throw new Error('vd:email-settings missing');
        }
        return this.http.get<EmailSettings>(href);
      }),
    );
  }

  saveSettings(settings: EmailSettings, payload: EmailSettingsPayload): Observable<EmailSettings> {
    const href = Links.of(settings).href('self');
    if (!href) {
      throw new Error('email-settings self missing');
    }
    return this.http.put<EmailSettings>(href, payload);
  }

  loadTemplate(settings: EmailSettings, kind: InviteTemplateKind): Observable<EmailTemplate> {
    const templated = Links.of(settings).href('email-template');
    if (!templated) {
      throw new Error('vd:email-template missing');
    }
    const href = templated.replace('{kind}', kind);
    return this.http.get<EmailTemplate>(href);
  }

  saveTemplate(template: EmailTemplate, subject: string, body: string): Observable<EmailTemplate> {
    const href = Links.of(template).href('self');
    if (!href) {
      throw new Error('email-template self missing');
    }
    return this.http.put<EmailTemplate>(href, { subject, body });
  }

  /** ISO instant ? local date + `HH:mm` for the Material datepicker / time input. */
  static toLocalParts(iso: string | null | undefined): LocalDateTimeParts {
    if (!iso) {
      return { date: null, time: '' };
    }
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return { date: null, time: '' };
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    return {
      date: new Date(date.getFullYear(), date.getMonth(), date.getDate()),
      time: `${pad(date.getHours())}:${pad(date.getMinutes())}`,
    };
  }

  /** Local date + `HH:mm` ? ISO-8601 UTC instant, or null when date is missing. */
  static fromLocalParts(date: Date | null, time: string): string | null {
    if (!date || Number.isNaN(date.getTime())) {
      return null;
    }
    const match = /^(\d{1,2}):(\d{2})$/.exec(time.trim());
    const hours = match ? Number(match[1]) : 0;
    const minutes = match ? Number(match[2]) : 0;
    const local = new Date(date.getFullYear(), date.getMonth(), date.getDate(), hours, minutes, 0, 0);
    if (Number.isNaN(local.getTime())) {
      return null;
    }
    return local.toISOString();
  }
}
