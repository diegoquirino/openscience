import { LiveAnnouncer } from '@angular/cdk/a11y';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { LabelService, TaskBody } from '../label.service';
import { ItemClock, URGENT_SECONDS } from './item-clock';

function task(secondsLeft: number): TaskBody {
  return {
    id: 1,
    blockNumber: 1,
    role: 1,
    status: 'reserved',
    size: 10,
    answeredCount: 0,
    reservationExpiresAt: new Date(Date.now() + secondsLeft * 1000).toISOString(),
  };
}

describe('ItemClock', () => {
  const announce = vi.fn();
  const current = signal<TaskBody | null>(null);

  function create(secondsLeft: number): ItemClock {
    current.set(task(secondsLeft));
    TestBed.configureTestingModule({
      providers: [
        ItemClock,
        { provide: LabelService, useValue: { task: current, clockOffsetMs: signal(0) } },
        { provide: LiveAnnouncer, useValue: { announce } },
      ],
    });
    return TestBed.inject(ItemClock);
  }

  beforeEach(() => {
    vi.resetAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => vi.useRealTimers());

  it('shows mm:ss and is neither urgent nor expired with time to spare', () => {
    const clock = create(600);
    TestBed.tick();

    expect(clock.countdown()).toBe('10:00');
    expect(clock.urgent()).toBe(false);
    expect(clock.expired()).toBe(false);
    expect(announce).not.toHaveBeenCalled();
  });

  it('turns urgent in the last 2 minutes and announces it only once', () => {
    const clock = create(URGENT_SECONDS + 3);
    TestBed.tick();
    expect(clock.urgent()).toBe(false);

    vi.advanceTimersByTime(4000);
    TestBed.tick();
    expect(clock.urgent()).toBe(true);
    expect(announce).toHaveBeenCalledTimes(1);
    expect(announce).toHaveBeenCalledWith('Less than 2 minutes left in this reservation.', 'polite');

    vi.advanceTimersByTime(30_000);
    TestBed.tick();
    expect(clock.urgent()).toBe(true);
    expect(announce).toHaveBeenCalledTimes(1);
  });

  it('announces when the screen opens with less than 2 minutes left', () => {
    const clock = create(90);
    TestBed.tick();

    expect(clock.urgent()).toBe(true);
    expect(announce).toHaveBeenCalledTimes(1);
  });

  it('is expired, not urgent, at zero', () => {
    const clock = create(1);
    TestBed.tick();
    vi.advanceTimersByTime(2000);
    TestBed.tick();

    expect(clock.countdown()).toBe('00:00');
    expect(clock.expired()).toBe(true);
    expect(clock.urgent()).toBe(false);
  });

  it('announces again after a save renewed the reservation and it runs low again', () => {
    const clock = create(60);
    TestBed.tick();
    expect(announce).toHaveBeenCalledTimes(1);

    current.set(task(20 * 60));
    TestBed.tick();
    expect(clock.urgent()).toBe(false);

    current.set(task(100));
    TestBed.tick();
    expect(announce).toHaveBeenCalledTimes(2);
  });
});
