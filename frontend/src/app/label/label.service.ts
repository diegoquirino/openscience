import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, concatMap, defaultIfEmpty, filter, from, map, of, switchMap, take } from 'rxjs';
import { HalResource, Links } from '../core/hal';
import { EntryPointService } from '../core/entry-point.service';
import { clockOffsetMs } from './countdown';

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

export interface ItemDraft {
  changeImpact: string | null;
  maintenanceOperation: string | null;
  comment: string | null;
  language: string | null;
}

/** An item of the held task with the caller's own draft (`null` when there is none). */
export interface ItemBody extends HalResource {
  position: number;
  itemId: number;
  system: string;
  uc: string;
  kind: string;
  field: string;
  originVersion: string | null;
  targetVersion: string | null;
  originUsecase: string | null;
  targetUsecase: string | null;
  origin: string | null;
  target: string | null;
  draft: ItemDraft | null;
}

export interface SaveResponseBody {
  changeImpact: string;
  maintenanceOperation: string;
  comment: string | null;
}

/** Raised by `currentTask` when the reserve call has no task to hand out. */
export class NoActiveTaskError extends Error {
  constructor(readonly outcome: string) {
    super(`no active task: ${outcome}`);
  }
}

const TASKS = '/api/v1/tasks';

@Injectable({ providedIn: 'root' })
export class LabelService {
  private readonly http = inject(HttpClient);
  private readonly entry = inject(EntryPointService);

  private readonly taskState = signal<TaskBody | null>(null);
  private readonly offsetState = signal(0);

  /** The task held by the caller as last seen (reserve or save). */
  readonly task = this.taskState.asReadonly();
  /** `server - client` clock offset in milliseconds, for the countdown (SC-005). */
  readonly clockOffsetMs = this.offsetState.asReadonly();

  reserve(): Observable<TaskBody | ReservationOutcomeBody> {
    return this.entry.load().pipe(
      switchMap((entry) => {
        const href = entry.links.href('next-task');
        if (!href) {
          throw new Error('vd:next-task missing');
        }
        return this.http.post<TaskBody | ReservationOutcomeBody>(href, null, { observe: 'response' });
      }),
      map((response) => this.accept(response)),
    );
  }

  /** The cached task, or the current reservation (the reserve call is idempotent for a holder). */
  currentTask(): Observable<TaskBody> {
    const cached = this.taskState();
    if (cached) {
      return of(cached);
    }
    return this.reserve().pipe(
      map((body) => {
        if (!this.isTask(body)) {
          throw new NoActiveTaskError(body.outcome);
        }
        return body;
      }),
    );
  }

  getItem(taskId: number, position: number): Observable<ItemBody> {
    const template = this.taskState()?.id === taskId ? Links.of(this.taskState()).href('items') : undefined;
    const href = template
      ? template.replace('{position}', String(position))
      : `${TASKS}/${taskId}/items/${position}`;
    return this.http.get<ItemBody>(href);
  }

  /** PUT of the draft of one item; `href` is the `vd:save-response` link of the item. */
  saveResponse(href: string, body: SaveResponseBody): Observable<TaskBody> {
    return this.http
      .put<TaskBody>(href, body, { observe: 'response' })
      .pipe(map((response) => this.accept(response) as TaskBody));
  }

  /**
   * The next position without a draft, looking after `after` first and wrapping around; `null`
   * when every item has one (the task then goes to review). Use `after = 0` to start at the top.
   */
  nextPending(task: TaskBody, after: number): Observable<number | null> {
    if (task.answeredCount >= task.size) {
      return of(null);
    }
    const order = [
      ...Array.from({ length: Math.max(0, task.size - after) }, (_, i) => after + 1 + i),
      ...Array.from({ length: Math.max(0, Math.min(after, task.size + 1) - 1) }, (_, i) => i + 1),
    ];
    return from(order).pipe(
      concatMap((position) => this.getItem(task.id, position)),
      filter((item) => item.draft === null),
      map((item) => item.position),
      take(1),
      defaultIfEmpty(null as number | null),
    );
  }

  isTask(body: TaskBody | ReservationOutcomeBody): body is TaskBody {
    return typeof (body as TaskBody).id === 'number' && (body as TaskBody).status !== undefined;
  }

  private accept(response: HttpResponse<TaskBody | ReservationOutcomeBody>): TaskBody | ReservationOutcomeBody {
    const body = response.body as TaskBody | ReservationOutcomeBody;
    if (this.isTask(body)) {
      this.taskState.set(body);
      this.offsetState.set(clockOffsetMs(body.serverNow, response.headers.get('Date'), Date.now()));
    } else {
      this.taskState.set(null);
    }
    return body;
  }
}
