package io.github.diegoquirino.veredito.invite;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository;
import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository.InviteSettings;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.BulkInviteRecipient;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every minute: when the clock has reached the configured reminder / collection-end instants, send
 * lembrete / agradecimento once to every eligible participant (spec 033).
 */
@Component
public class InviteBulkSendJob {

    private static final Logger log = LoggerFactory.getLogger(InviteBulkSendJob.class);

    private final InviteSettingsRepository settings;
    private final ParticipantRepository participants;
    private final InviteMailer mailer;
    private final Clock clock;

    public InviteBulkSendJob(
            InviteSettingsRepository settings,
            ParticipantRepository participants,
            InviteMailer mailer,
            Clock clock) {
        this.settings = settings;
        this.participants = participants;
        this.mailer = mailer;
        this.clock = clock;
    }

    public record Sent(int lembrete, int agradecimento) {}

    @Scheduled(cron = "0 * * * * *")
    public void scheduled() {
        try {
            run();
        } catch (DataAccessException e) {
            log.warn("Invite bulk send skipped, database unavailable: {}", e.getClass().getSimpleName());
        }
    }

    public Sent run() {
        Instant now = clock.instant();
        InviteSettings invite = settings.load();
        int lembrete = 0;
        int agradecimento = 0;
        if (due(invite.reminderAt(), now)) {
            lembrete = sendDue(
                    InviteTemplateKind.LEMBRETE,
                    participants.findPendingLembrete(),
                    now,
                    participants::claimInviteLembrete,
                    participants::clearInviteLembrete);
        }
        if (due(invite.collectionEndAt(), now)) {
            agradecimento = sendDue(
                    InviteTemplateKind.AGRADECIMENTO,
                    participants.findPendingAgradecimento(),
                    now,
                    participants::claimInviteAgradecimento,
                    participants::clearInviteAgradecimento);
        }
        if (lembrete > 0 || agradecimento > 0) {
            log.info("Invite bulk send delivered {} lembrete and {} agradecimento", lembrete, agradecimento);
        }
        return new Sent(lembrete, agradecimento);
    }

    private int sendDue(
            InviteTemplateKind kind,
            List<BulkInviteRecipient> recipients,
            Instant now,
            Claim claim,
            Clear clear) {
        int sent = 0;
        for (BulkInviteRecipient row : recipients) {
            EmailAddress to = EmailAddress.parse(row.email()).orElse(null);
            if (to == null) {
                continue;
            }
            if (!claim.apply(row.id(), now)) {
                continue;
            }
            String nome = Objects.requireNonNullElse(row.name(), "");
            if (mailer.send(kind, to, nome)) {
                sent++;
            } else {
                clear.apply(row.id());
            }
        }
        return sent;
    }

    private static boolean due(Instant when, Instant now) {
        return when != null && !now.isBefore(when);
    }

    @FunctionalInterface
    private interface Claim {
        boolean apply(long id, Instant now);
    }

    @FunctionalInterface
    private interface Clear {
        void apply(long id);
    }
}
