package io.github.diegoquirino.veredito.access;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.access.Subject;
import io.github.diegoquirino.veredito.persistence.AccessCodeRepository;
import io.github.diegoquirino.veredito.persistence.AccessSessionRepository;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Research section 14: codes older than 24 hours and expired sessions go; the rest stays. */
class CleanupJobTest extends ApiTestBase {

    private static final PersonName ANA = new PersonName("Ana Souza");

    @Autowired
    CleanupJob job;

    @Autowired
    AccessCodeRepository codes;

    @Autowired
    AccessSessionRepository sessions;

    @Test
    void oldCodesAndExpiredSessionsAreDeletedAndTheRestStays() {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        long oldCode = code(now.minus(Duration.ofHours(24)).minusSeconds(1));
        long recentCode = code(now.minus(Duration.ofHours(23)));
        String expired = session(now.minusSeconds(1));
        String live = session(now.plus(Duration.ofHours(1)));

        CleanupJob.Removed removed = job.run();

        assertThat(removed.codes()).isGreaterThanOrEqualTo(1);
        assertThat(removed.sessions()).isGreaterThanOrEqualTo(1);
        assertThat(codeRows(oldCode)).isZero();
        assertThat(codeRows(recentCode)).isEqualTo(1);
        assertThat(sessionRows(expired)).isZero();
        assertThat(sessionRows(live)).isEqualTo(1);

        assertThat(job.run()).isEqualTo(new CleanupJob.Removed(0, 0));
        assertThat(codeRows(recentCode)).isEqualTo(1);
        assertThat(sessionRows(live)).isEqualTo(1);
    }

    @Test
    void theRemainingRowsGoOnceTheClockPassesTheirLimits() {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        long code = code(now);
        String session = session(now.plus(Duration.ofHours(24)));

        clock.advance(Duration.ofHours(23));
        job.run();
        assertThat(codeRows(code)).isEqualTo(1);
        assertThat(sessionRows(session)).isEqualTo(1);

        clock.advance(Duration.ofHours(1).plusSeconds(1));
        job.run();
        assertThat(codeRows(code)).isZero();
        assertThat(sessionRows(session)).isZero();
    }

    private long code(Instant createdAt) {
        EmailAddress email = EmailAddress.parse("cleanup-" + System.nanoTime() + "@example.com").orElseThrow();
        return codes.insert(email, ANA, "a".repeat(64), createdAt, createdAt.plus(Duration.ofMinutes(10)));
    }

    private String session(Instant expiresAt) {
        String hash = new TokenHasher().hash("cleanup-" + System.nanoTime());
        sessions.insert(hash, Subject.ADMINISTRATOR, null, expiresAt.minus(Duration.ofHours(24)), expiresAt);
        return hash;
    }

    private int codeRows(long id) {
        return jdbc.sql("SELECT COUNT(*) FROM access_code WHERE id = ?").param(id).query(Integer.class).single();
    }

    private int sessionRows(String hash) {
        return jdbc.sql("SELECT COUNT(*) FROM access_session WHERE token_hash = ?")
                .param(hash)
                .query(Integer.class)
                .single();
    }
}
