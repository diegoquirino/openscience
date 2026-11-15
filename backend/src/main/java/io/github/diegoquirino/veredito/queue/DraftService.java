package io.github.diegoquirino.veredito.queue;

import io.github.diegoquirino.veredito.api.LocaleConfig;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.queue.ResponseCodes;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import io.github.diegoquirino.veredito.persistence.ItemRepository;
import io.github.diegoquirino.veredito.persistence.ItemRepository.Item;
import io.github.diegoquirino.veredito.persistence.ResponseRepository;
import io.github.diegoquirino.veredito.persistence.ResponseRepository.ResponseRow;
import io.github.diegoquirino.veredito.persistence.TaskRepository;
import io.github.diegoquirino.veredito.persistence.TaskRepository.TaskRow;
import io.github.diegoquirino.veredito.queue.TaskReservationService.TaskView;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads items of a held task and saves drafts, renewing the reservation (spec 007 US2). */
@Service
public class DraftService {

    private final TaskRepository tasks;
    private final ItemRepository items;
    private final ResponseRepository responses;
    private final ConfigRepository config;
    private final EligibilityGate eligibility;
    private final TaskReservationService reservations;
    private final Clock clock;

    public DraftService(
            TaskRepository tasks,
            ItemRepository items,
            ResponseRepository responses,
            ConfigRepository config,
            EligibilityGate eligibility,
            TaskReservationService reservations,
            Clock clock) {
        this.tasks = tasks;
        this.items = items;
        this.responses = responses;
        this.config = config;
        this.eligibility = eligibility;
        this.reservations = reservations;
        this.clock = clock;
    }

    /** An item of the task's block with the caller's own response, when there is one. */
    public record ItemView(long taskId, TaskRow task, int position, Item item, ResponseRow draft) {}

    @Transactional(readOnly = true)
    public ItemView item(long participantId, long taskId, int position) {
        // Same ownership rules as GET /tasks/{id}: foreign or unknown tasks are 404.
        TaskView view = reservations.requireOwnedTask(participantId, taskId);
        if (position < 1 || position > view.size()) {
            throw new ProblemException(ProblemType.NOT_FOUND);
        }
        Item item = items.findByBlockAndPosition(view.task().blockId(), position)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        ResponseRow draft = responses.findByTaskAndItem(taskId, item.id()).orElse(null);
        return new ItemView(taskId, view.task(), position, item, draft);
    }

    /**
     * Saves or replaces the draft for one item and renews the reservation. Accepted only from the
     * holder, or from the owner of an expired reservation nobody took (FR-004).
     */
    @Transactional
    public TaskView saveDraft(
            long participantId,
            long taskId,
            long itemId,
            String changeImpact,
            String maintenanceOperation,
            String comment,
            Locale locale) {
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
            throw new ProblemException(ProblemType.TASK_ALREADY_SUBMITTED);
        }
        if (task.expiresAt() == null || !task.expiresAt().isAfter(now)) {
            // Own expired reservation nobody took: renewable unless the caller already holds a new one.
            Optional<TaskRow> other = tasks.findValidReservation(participantId, now);
            if (other.isPresent() && other.get().id() != task.id()) {
                throw new ProblemException(ProblemType.RESERVATION_EXPIRED);
            }
        }

        Item item = items.findById(itemId)
                .filter(found -> found.blockId() == task.blockId())
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));

        Optional<String> invalid = ResponseCodes.firstError(changeImpact, maintenanceOperation, comment);
        if (invalid.isPresent()) {
            String code = invalid.get();
            throw ProblemException.invalidFields(Map.of(fieldOf(code), code));
        }

        responses.upsertDraft(
                task.id(),
                item.id(),
                participantId,
                changeImpact,
                maintenanceOperation,
                comment,
                languageTag(locale),
                now);
        tasks.renewExpiry(task.id(), now.plus(config.reservationMinutes(), ChronoUnit.MINUTES));
        TaskRow renewed = tasks.findById(task.id()).orElseThrow();
        return reservations.view(renewed, now, null);
    }

    private static String fieldOf(String code) {
        return switch (code) {
            case "invalid-change-impact" -> "changeImpact";
            case "invalid-maintenance-operation" -> "maintenanceOperation";
            default -> "comment";
        };
    }

    /** One of the supported tags, defaulting to pt-BR. */
    static String languageTag(Locale locale) {
        if (locale != null) {
            for (Locale supported : LocaleConfig.SUPPORTED) {
                if (supported.equals(locale)) {
                    return supported.toLanguageTag();
                }
            }
            for (Locale supported : LocaleConfig.SUPPORTED) {
                if (supported.getLanguage().equals(locale.getLanguage())) {
                    return supported.toLanguageTag();
                }
            }
        }
        return LocaleConfig.PT_BR.toLanguageTag();
    }
}
