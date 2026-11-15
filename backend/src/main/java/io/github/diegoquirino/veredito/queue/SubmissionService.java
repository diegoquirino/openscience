package io.github.diegoquirino.veredito.queue;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.persistence.ItemRepository;
import io.github.diegoquirino.veredito.persistence.ItemRepository.Item;
import io.github.diegoquirino.veredito.persistence.ResponseRepository;
import io.github.diegoquirino.veredito.persistence.TaskRepository;
import io.github.diegoquirino.veredito.persistence.TaskRepository.TaskRow;
import io.github.diegoquirino.veredito.queue.TaskReservationService.TaskView;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Submits a block: every item answered, drafts become final, the task completes (spec 007 US3). */
@Service
public class SubmissionService {

    private final TaskRepository tasks;
    private final ItemRepository items;
    private final ResponseRepository responses;
    private final EligibilityGate eligibility;
    private final TaskReservationService reservations;
    private final Clock clock;

    public SubmissionService(
            TaskRepository tasks,
            ItemRepository items,
            ResponseRepository responses,
            EligibilityGate eligibility,
            TaskReservationService reservations,
            Clock clock) {
        this.tasks = tasks;
        this.items = items;
        this.responses = responses;
        this.eligibility = eligibility;
        this.reservations = reservations;
        this.clock = clock;
    }

    /**
     * Completes the task of the caller. Accepted from the holder, or from the owner of an expired
     * reservation nobody took (FR-004); submitting an already completed task changes nothing (FR-007).
     */
    @Transactional
    public TaskView submit(long participantId, long taskId) {
        if (!eligibility.isEligible(participantId)) {
            throw new ProblemException(ProblemType.NOT_ELIGIBLE);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

        TaskRow task = tasks.findByIdForUpdate(taskId).orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        if (task.participantId() == null || task.participantId() != participantId) {
            // Never reveal tasks of the other rater, nor tasks reclaimed by someone else.
            throw new ProblemException(ProblemType.NOT_FOUND);
        }
        if ("completed".equals(task.status())) {
            return reservations.requireOwnedTask(participantId, taskId);
        }
        if (task.expiresAt() == null || !task.expiresAt().isAfter(now)) {
            Optional<TaskRow> other = tasks.findValidReservation(participantId, now);
            if (other.isPresent() && other.get().id() != task.id()) {
                throw new ProblemException(ProblemType.RESERVATION_EXPIRED);
            }
        }

        List<Item> block = items.listByBlockOrdered(task.blockId());
        for (Item item : block) {
            if (responses.findByTaskAndItem(task.id(), item.id()).isEmpty()) {
                throw new ProblemException(ProblemType.BLOCK_INCOMPLETE)
                        .with("firstPendingPosition", item.position());
            }
        }

        responses.finalizeAll(task.id());
        tasks.complete(task.id(), now);
        return reservations.requireOwnedTask(participantId, taskId);
    }
}
