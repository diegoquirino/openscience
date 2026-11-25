import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, switchMap } from 'rxjs';
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

/** Follows `vd:email-settings` / `vd:email-template` (spec 031). */
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

  /** ISO instant ? value for `<input type="datetime-local">` in the browser local zone. */
  static toLocalInput(iso: string | null | undefined): string {
    if (!iso) {
      return '';
    }
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  /** Local datetime-local string ? ISO-8601 UTC instant, or null when empty. */
  static fromLocalInput(value: string): string | null {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    const date = new Date(trimmed);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    return date.toISOString();
  }
}
