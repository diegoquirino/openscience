package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.domain.access.SendLimits;
import io.github.diegoquirino.veredito.persistence.AccessCodeRepository;
import io.github.diegoquirino.veredito.persistence.AccessSessionRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every hour, deletes the access codes older than the 24 hours the daily limit counts, and the
 * expired sessions (research section 14). Several instances may run it at once; the deletes are
 * idempotent.
 */
@Component
public class CleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CleanupJob.class);

    private final AccessCodeRepository codes;
    private final AccessSessionRepository sessions;
    private final Clock clock;

    public CleanupJob(AccessCodeRepository codes, AccessSessionRepository sessions, Clock clock) {
        this.codes = codes;
        this.sessions = sessions;
        this.clock = clock;
    }

    public record Removed(int codes, int sessions) {}

    @Scheduled(cron = "0 0 * * * *")
    public void scheduled() {
        try {
            run();
        } catch (DataAccessException e) {
            log.warn("Cleanup skipped, database unavailable: {}", e.getClass().getSimpleName());
        }
    }

    public Removed run() {
        Instant now = clock.instant();
        Removed removed = new Removed(codes.deleteCreatedBefore(now.minus(SendLimits.DAY)), sessions.deleteExpired(now));
        log.info("Cleanup removed {} access code(s) and {} expired session(s)", removed.codes(), removed.sessions());
        return removed;
    }
}
