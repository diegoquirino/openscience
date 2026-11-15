package io.github.diegoquirino.veredito.queue;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.queue.NextTaskChooser;
import io.github.diegoquirino.veredito.domain.queue.QueueOutcome;
import io.github.diegoquirino.veredito.persistence.BlockRepository;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import io.github.diegoquirino.veredito.persistence.ResponseRepository;
import io.github.diegoquirino.veredito.persistence.TaskRepository;
import io.github.diegoquirino.veredito.persistence.TaskRepository.TaskRow;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reserve the next free or expired task for an eligible participant (spec 007 US1). */
@Service
public class TaskReservationService {

    private final TaskRepository tasks;
    private final ResponseRepository responses;
    private final BlockRepository blocks;
    private final ConfigRepository config;
    private final EligibilityGate eligibility;
    private final Clock clock;

    public TaskReservationService(
            TaskRepository tasks,
            ResponseRepository responses,
            BlockRepository blocks,
            ConfigRepository config,
            EligibilityGate eligibility,
            Clock clock) {
        this.tasks = tasks;
        this.responses = responses;
        this.blocks = blocks;
        this.config = config;
        this.eligibility = eligibility;
        this.clock = clock;
    }

    public sealed interface Result permits Result.Reserved, Result.NoTask {
        record Reserved(TaskView task, boolean created) implements Result {}

        record NoTask(QueueOutcome outcome) implements Result {}
    }

    public record TaskView(
            TaskRow task,
            int blockNumber,
            int size,
            long answeredCount,
            int reservationMinutes,
            Instant serverNow,
            QueueOutcome outcome) {}

    @Transactional
    public Result reserve(long participantId) {
        requireEligible(participantId);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

        Optional<TaskRow> current = tasks.findValidReservation(participantId, now);
        if (current.isPresent()) {
            return new Result.Reserved(view(current.get(), now, null), false);
        }

        config.lockForUpdate();
        int minutes = config.reservationMinutes();
        Set<Long> held = tasks.heldBlockIds(participantId);
        List<NextTaskChooser.Candidate> candidates = tasks.lockCandidatesForUpdateSkipLocked(now);
        Optional<NextTaskChooser.Candidate> chosen = NextTaskChooser.choose(candidates, held, now);
        if (chosen.isEmpty()) {
            return new Result.NoTask(outcomeWhenNone());
        }

        NextTaskChooser.Candidate pick = chosen.get();
        if (pick.participantId() != null && !pick.participantId().equals(participantId)) {
            responses.deleteByTaskId(pick.taskId());
        }
        Instant expires = now.plus(minutes, ChronoUnit.MINUTES);
        try {
            tasks.reserve(pick.taskId(), participantId, now, expires);
        } catch (DataIntegrityViolationException ex) {
            throw new ProblemException(ProblemType.TASK_CONFLICT);
        }
        TaskRow reserved = tasks.findById(pick.taskId()).orElseThrow();
        return new Result.Reserved(view(reserved, now, null), true);
    }

    @Transactional(readOnly = true)
    public TaskView requireOwnedTask(long participantId, long taskId) {
        requireEligible(participantId);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        TaskRow task = tasks.findById(taskId).orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        if (task.participantId() == null || task.participantId() != participantId) {
            throw new ProblemException(ProblemType.NOT_FOUND);
        }
        QueueOutcome outcome = "completed".equals(task.status()) ? outcomeAfterComplete(participantId, now) : null;
        return view(task, now, outcome);
    }

    private void requireEligible(long participantId) {
        if (!eligibility.isEligible(participantId)) {
            throw eligibility.refusal(participantId);
        }
    }

    private QueueOutcome outcomeWhenNone() {
        if (!tasks.existsAnyItem()) {
            return QueueOutcome.NO_ITEMS_YET;
        }
        if (tasks.countByStatus("completed") == tasks.countAll()) {
            return QueueOutcome.DATABASE_COMPLETE;
        }
        return QueueOutcome.NONE_AVAILABLE_FOR_YOU;
    }

    private QueueOutcome outcomeAfterComplete(long participantId, Instant now) {
        Set<Long> held = tasks.heldBlockIds(participantId);
        if (NextTaskChooser.choose(tasks.listCandidates(now), held, now).isPresent()) {
            return QueueOutcome.MORE_AVAILABLE;
        }
        return outcomeWhenNone();
    }

    /** Builds the task representation for an already loaded row; shared with the draft service. */
    public TaskView view(TaskRow task, Instant now, QueueOutcome outcome) {
        var block = blocks.findById(task.blockId()).orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        long answered = responses.countByTaskId(task.id());
        int minutes = config.reservationMinutes();
        return new TaskView(task, block.number(), block.size(), answered, minutes, now, outcome);
    }
}
