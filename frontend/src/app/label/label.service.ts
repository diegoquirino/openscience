import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, concatMap, defaultIfEmpty, filter, forkJoin, from, map, of, switchMap, take, tap } from 'rxjs';
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

/** The held task and its items (with drafts) in position order. */
export interface ReviewData {
  task: TaskBody;
  items: ItemBody[];
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

  /**
   * Everything the review shows: the task as the server has it now (`GET` of the task) and each of
   * its items with the caller's draft (`GET` of each item, in position order).
   */
  loadDraftsForReview(taskId: number): Observable<ReviewData> {
    return this.http.get<TaskBody>(`${TASKS}/${taskId}`, { observe: 'response' }).pipe(
      map((response) => this.accept(response) as TaskBody),
      switchMap((task) =>
        (task.size > 0
          ? forkJoin(Array.from({ length: task.size }, (_, i) => this.getItem(task.id, i + 1)))
          : of([] as ItemBody[])
        ).pipe(map((items) => ({ task, items }))),
      ),
    );
  }

  /**
   * `POST` of `vd:submit` of the task (the contract path when the link is absent: the server then
   * answers `block-incomplete`). The held task is forgotten on success: the next reserve starts over.
   */
  submit(task: TaskBody): Observable<TaskBody> {
    const href = Links.of(task).href('submit') ?? `${TASKS}/${task.id}/submission`;
    return this.http.post<TaskBody>(href, null).pipe(
      tap(() => this.taskState.set(null)),
    );
  }

  /**
   * `DELETE` of `vd:cancel-reservation`: discards drafts of this task and frees it (spec 023).
   * The held task is forgotten on success so the next reserve starts over.
   */
  cancel(task: TaskBody): Observable<void> {
    const href = Links.of(task).href('cancel-reservation') ?? `${TASKS}/${task.id}`;
    return this.http.delete<void>(href).pipe(tap(() => this.taskState.set(null)));
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
