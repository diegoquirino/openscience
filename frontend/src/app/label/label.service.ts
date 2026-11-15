import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { HalResource } from '../core/hal';
import { EntryPointService } from '../core/entry-point.service';

export interface TaskBody extends HalResource {
  id: number;
  blockNumber: number;
  role: number;
  status: string;
  size: number;
  answeredCount: number;
  reservationExpiresAt?: string;
  reservationMinutes?: number;
  outcome?: string | null;
  serverNow?: string;
}

export interface ReservationOutcomeBody extends HalResource {
  outcome: string;
  id?: number;
}

@Injectable({ providedIn: 'root' })
export class LabelService {
  private readonly http = inject(HttpClient);
  private readonly entry = inject(EntryPointService);

  reserve(): Observable<TaskBody | ReservationOutcomeBody> {
    return this.entry.load().pipe(
      switchMap((entry) => {
        const href = entry.links.href('next-task');
        if (!href) {
          throw new Error('vd:next-task missing');
        }
        return this.http.post<TaskBody | ReservationOutcomeBody>(href, null);
      }),
    );
  }

  isTask(body: TaskBody | ReservationOutcomeBody): body is TaskBody {
    return typeof (body as TaskBody).id === 'number' && (body as TaskBody).status !== undefined;
  }
}
