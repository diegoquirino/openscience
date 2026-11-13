package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.domain.access.Subject;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Rows of {@code access_session}; the token itself is never stored, only its SHA-256. */
@Repository
public class AccessSessionRepository {

    private final JdbcClient jdbc;

    public AccessSessionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** A session that has not expired, with the participant's pseudonym when there is one. */
    public record ValidSession(Subject subject, Long participantId, String pseudonym, Instant expiresAt) {}

    public void insert(String tokenHash, Subject subject, Long participantId, Instant createdAt, Instant expiresAt) {
        jdbc.sql("""
                        INSERT INTO access_session (token_hash, subject, participant_id, created_at, expires_at)
                        VALUES (?, ?, ?, ?, ?)""")
                .params(tokenHash, subject.code(), participantId, Timestamp.from(createdAt), Timestamp.from(expiresAt))
                .update();
    }

    public Optional<ValidSession> findValid(String tokenHash, Instant now) {
        return jdbc.sql("""
                        SELECT s.subject, s.participant_id, p.pseudonym, s.expires_at
                        FROM access_session s LEFT JOIN participant p ON p.id = s.participant_id
                        WHERE s.token_hash = ? AND s.expires_at > ?""")
                .params(tokenHash, Timestamp.from(now))
                .query((rs, row) -> new ValidSession(
                        Subject.fromCode(rs.getString("subject")),
                        rs.getObject("participant_id", Long.class),
                        rs.getString("pseudonym"),
                        rs.getTimestamp("expires_at").toInstant()))
                .optional();
    }

    public int delete(String tokenHash) {
        return jdbc.sql("DELETE FROM access_session WHERE token_hash = ?").param(tokenHash).update();
    }

    public int deleteExpired(Instant now) {
        return jdbc.sql("DELETE FROM access_session WHERE expires_at <= ?").param(Timestamp.from(now)).update();
    }
}
