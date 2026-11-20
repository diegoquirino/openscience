import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PROBLEM_TYPE_PREFIX } from '../../core/problem-messages';
import { ItemBody, LabelService, TaskBody } from '../label.service';
import { LabelReviewComponent } from './review.component';

const task: TaskBody = {
  id: 41,
  blockNumber: 1,
  role: 1,
  status: 'reserved',
  size: 2,
  answeredCount: 2,
  reservationExpiresAt: new Date(Date.now() + 10 * 60_000).toISOString(),
  _links: { 'vd:submit': { href: '/api/v1/tasks/41/submission' } },
};

function item(position: number, draft: ItemBody['draft']): ItemBody {
  return {
    position,
    itemId: 9000 + position,
    system: 'minitest',
    uc: 'UC01',
    kind: 'MOD',
    field: 'step',
    originVersion: '1',
    targetVersion: '2',
    originUsecase: null,
    targetUsecase: null,
    origin: 'a',
    target: 'b',
    draft,
  };
}

const answered = (position: number, comment: string | null) =>
  item(position, { changeImpact: 'HIGH', maintenanceOperation: 'UPDATE', comment, language: 'en-US' });

describe('LabelReviewComponent', () => {
  const submit = vi.fn();

  function create(items: ItemBody[]) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: LabelService,
          useValue: {
            task: signal(task),
            currentTask: () => of(task),
            loadDraftsForReview: () => of({ task, items }),
            submit,
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(LabelReviewComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => vi.resetAllMocks());

  it('lists every item with impact, operation, comment and an edit link', async () => {
    const fixture = create([answered(1, 'because'), answered(2, null)]);
    await fixture.whenStable();
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;

    const rows = root.querySelectorAll('li.item');
    expect(rows.length).toBe(2);
    expect(rows[0].querySelector('.impact')?.textContent).toContain('Yes (high impact)');
    expect(rows[0].querySelector('.operation')?.textContent).toContain('Update');
    expect(rows[0].querySelector('.comment')?.textContent).toContain('because');
    expect(rows[1].querySelector('.comment')?.textContent).toContain('—');
    expect(rows[1].querySelector('a.edit')?.getAttribute('href')).toBe('/label/items/2');
    expect((root.querySelector('button.submit') as HTMLButtonElement).disabled).toBe(false);
  });

  it('disables Submit and points to the first pending item when an item has no draft', async () => {
    const fixture = create([answered(1, null), item(2, null)]);
    await fixture.whenStable();
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;

    expect((root.querySelector('button.submit') as HTMLButtonElement).disabled).toBe(true);
    expect(root.querySelector('li.item.pending')).not.toBeNull();
    expect(fixture.componentInstance['firstPending']()).toBe(2);
  });

  it('submits and goes to the done screen with the outcome', async () => {
    submit.mockReturnValue(of({ ...task, status: 'completed', outcome: 'more-available' }));
    const fixture = create([answered(1, null), answered(2, null)]);
    await fixture.whenStable();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.componentInstance['submit']();

    expect(submit).toHaveBeenCalledWith(task);
    expect(navigate).toHaveBeenCalledWith(['/label/done'], { queryParams: { outcome: 'more-available' } });
  });

  it('jumps to firstPendingPosition on block-incomplete', async () => {
    submit.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { type: `${PROBLEM_TYPE_PREFIX}block-incomplete`, firstPendingPosition: 2 },
          }),
      ),
    );
    const fixture = create([answered(1, null), answered(2, null)]);
    await fixture.whenStable();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.componentInstance['submit']();

    expect(navigate).toHaveBeenCalledWith(['/label/items', 2]);
  });

  it('shows the message of a reservation that ran out', async () => {
    submit.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({ status: 409, error: { type: `${PROBLEM_TYPE_PREFIX}reservation-expired` } }),
      ),
    );
    const fixture = create([answered(1, null), answered(2, null)]);
    await fixture.whenStable();

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('p.error')?.textContent).toContain('ran out');
  });
});
