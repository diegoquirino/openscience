package io.github.diegoquirino.veredito.invite;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends the inicial invite once after a complete profile save (spec 031). Runs after commit so a
 * failed SMTP does not roll back the profile; {@code invite_inicial_sent_at} stays null for retry.
 */
@Component
public class InviteInicialDispatcher {

    private static final Logger log = LoggerFactory.getLogger(InviteInicialDispatcher.class);

    private final ParticipantRepository participants;
    private final InviteMailer mailer;
    private final Clock clock;

    public InviteInicialDispatcher(ParticipantRepository participants, InviteMailer mailer, Clock clock) {
        this.participants = participants;
        this.mailer = mailer;
        this.clock = clock;
    }

    /** Schedules {@link #trySend(long)} after the surrounding transaction commits. */
    public void scheduleAfterCommit(long participantId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    trySend(participantId);
                }
            });
        } else {
            trySend(participantId);
        }
    }

    void trySend(long participantId) {
        Optional<ParticipantRepository.InviteRecipient> recipient = participants.findInviteRecipient(participantId);
        if (recipient.isEmpty()) {
            return;
        }
        ParticipantRepository.InviteRecipient row = recipient.get();
        if (row.inicialSentAt() != null || row.email() == null || row.email().isBlank()) {
            return;
        }
        if (row.name() == null || row.name().isBlank()) {
            return;
        }
        if (!participants.claimInviteInicial(participantId, clock.instant())) {
            return;
        }
        EmailAddress to = EmailAddress.parse(row.email()).orElse(null);
        if (to == null) {
            participants.clearInviteInicial(participantId);
            return;
        }
        boolean sent = mailer.send(InviteTemplateKind.INICIAL, to, row.name());
        if (!sent) {
            participants.clearInviteInicial(participantId);
            log.info("Inicial invite left pending for participant {}", participantId);
        }
    }
}
