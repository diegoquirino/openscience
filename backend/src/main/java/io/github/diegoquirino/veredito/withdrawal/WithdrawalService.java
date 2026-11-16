package io.github.diegoquirino.veredito.withdrawal;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.withdrawal.WithdrawalImpact;
import io.github.diegoquirino.veredito.domain.withdrawal.WithdrawalOption;
import io.github.diegoquirino.veredito.withdrawal.WithdrawalQuery.ParticipantRow;
import io.github.diegoquirino.veredito.withdrawal.WithdrawalQuery.ReservedSummary;
import io.github.diegoquirino.veredito.withdrawal.WithdrawalQuery.TaskRef;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Search and anonymize participants for LGPD withdrawal (spec 011). */
@Service
public class WithdrawalService {

    public record ParticipantCard(
            long id,
            String pseudonym,
            String email,
            String preferredLanguage,
            Instant anonymizedAt,
            long completedTasks,
            ReservedSummary reserved,
            WithdrawalImpact impact,
            boolean emailCanBeSent) {}

    public record AnonymizationResult(
            String pseudonym,
            WithdrawalOption option,
            boolean emailSent,
            int finalResponsesDiscarded,
            int tasksReturned,
            Instant occurredAt,
            long participantId) {}

    private final WithdrawalQuery query;
    private final WithdrawalMailer mailer;
    private final EmailQuotaService quota;
    private final Clock clock;

    public WithdrawalService(
            WithdrawalQuery query, WithdrawalMailer mailer, EmailQuotaService quota, Clock clock) {
        this.query = query;
        this.mailer = mailer;
        this.quota = quota;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ParticipantCard> search(Optional<String> email, Optional<String> pseudonym) {
        boolean hasEmail = email.filter(s -> !s.isBlank()).isPresent();
        boolean hasPseudonym = pseudonym.filter(s -> !s.isBlank()).isPresent();
        if (hasEmail == hasPseudonym) {
            throw new ProblemException(ProblemType.INVALID_REQUEST)
                    .with("reason", "exactly one of email or pseudonym is required");
        }
        Optional<ParticipantRow> row = hasEmail
                ? query.findByEmail(email.get().trim())
                : query.findByPseudonym(pseudonym.get().trim());
        return row.map(this::card).stream().toList();
    }

    @Transactional(readOnly = true)
    public ParticipantCard requireCard(long id) {
        ParticipantRow row = query.findById(id).orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        return card(row);
    }

    @Transactional
    public AnonymizationResult anonymize(long id, WithdrawalOption option) {
        ParticipantRow peek = query.findById(id).orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        if (peek.anonymizedAt() != null) {
            throw new ProblemException(ProblemType.ALREADY_ANONYMIZED);
        }
        boolean emailSent = false;
        if (quota.canSend()) {
            mailer.send(peek.email(), peek.name(), option, localeOf(peek.preferredLanguage()));
            emailSent = true;
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        ParticipantRow row = query.findByIdForUpdate(id)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        if (row.anonymizedAt() != null) {
            throw new ProblemException(ProblemType.ALREADY_ANONYMIZED);
        }
        String email = row.email();
        List<TaskRef> tasks = query.lockTasksOfParticipant(id);
        int finalsBefore = (int) query.countResponses(id, "final");
        query.deleteDraftsOfParticipant(id);

        int tasksReturned = 0;
        for (TaskRef task : tasks) {
            if ("reserved".equals(task.status())) {
                query.releaseTaskToFree(task.id());
            } else if (option.discardsResponses() && "completed".equals(task.status())) {
                query.releaseTaskToFree(task.id());
                tasksReturned++;
            }
        }
        int finalsDiscarded = 0;
        if (option.discardsResponses()) {
            finalsDiscarded = query.deleteAllResponsesOfParticipant(id);
            if (finalsDiscarded == 0) {
                finalsDiscarded = finalsBefore;
            }
        }

        query.deleteSessions(id);
        query.deleteAccessCodes(email);
        query.anonymize(id, now);
        query.insertLog(
                now, row.pseudonym(), option.code(), emailSent, finalsDiscarded, tasksReturned, id);
        return new AnonymizationResult(
                row.pseudonym(), option, emailSent, finalsDiscarded, tasksReturned, now, id);
    }

    private ParticipantCard card(ParticipantRow row) {
        WithdrawalImpact impact = WithdrawalImpact.of(
                query.countResponses(row.id(), "final"),
                query.countResponses(row.id(), "draft"),
                query.countCompletedTasks(row.id()));
        return new ParticipantCard(
                row.id(),
                row.pseudonym(),
                row.anonymizedAt() == null ? row.email() : null,
                row.preferredLanguage(),
                row.anonymizedAt(),
                query.countCompletedTasks(row.id()),
                query.findReserved(row.id()).orElse(null),
                impact,
                row.anonymizedAt() == null && quota.canSend());
    }

    private static Locale localeOf(String preferred) {
        if (preferred == null || preferred.isBlank()) {
            return Locale.forLanguageTag("pt-BR");
        }
        return Locale.forLanguageTag(preferred);
    }
}
