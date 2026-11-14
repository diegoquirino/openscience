package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Rows of {@code access_code}, one per code actually sent (specs/004-email-code-access/data-model.md). */
@Repository
public class AccessCodeRepository {

    private final JdbcClient jdbc;

    public AccessCodeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record StoredCode(
            long id, String name, String codeHash, Instant createdAt, Instant expiresAt, int attempts, Instant usedAt) {}

    public long insert(EmailAddress email, PersonName name, String codeHash, Instant createdAt, Instant expiresAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO access_code (email, name, code_hash, created_at, expires_at, attempts)
                        VALUES (?, ?, ?, ?, ?, 0)""")
                .params(email.value(), name.value(), codeHash, Timestamp.from(createdAt), Timestamp.from(expiresAt))
                .update(keys);
        return keys.getKey().longValue();
    }

    /** The most recent code of the e-mail; inside a transaction, its row stays locked until the end. */
    public Optional<StoredCode> latestFor(EmailAddress email) {
        return jdbc.sql("""
                        SELECT id, name, code_hash, created_at, expires_at, attempts, used_at
                        FROM access_code WHERE email = ?
                        ORDER BY created_at DESC, id DESC LIMIT 1 FOR UPDATE""")
                .param(email.value())
                .query(AccessCodeRepository::storedCode)
                .optional();
    }

    /** The hashes of the e-mail's codes other than {@code latestId}: the codes a newer one replaced. */
    public List<String> replacedHashes(EmailAddress email, long latestId) {
        return jdbc.sql("SELECT code_hash FROM access_code WHERE email = ? AND id <> ?")
                .params(email.value(), latestId)
                .query(String.class)
                .list();
    }

    public List<Instant> sendTimesSince(EmailAddress email, Instant since) {
        return jdbc.sql("SELECT created_at FROM access_code WHERE email = ? AND created_at > ? ORDER BY created_at")
                .params(email.value(), Timestamp.from(since))
                .query((rs, row) -> rs.getTimestamp(1).toInstant())
                .list();
    }

    public long countAllSince(Instant since) {
        return jdbc.sql("SELECT COUNT(*) FROM access_code WHERE created_at > ?")
                .param(Timestamp.from(since))
                .query(Long.class)
                .single();
    }

    public void incrementAttempts(long id) {
        jdbc.sql("UPDATE access_code SET attempts = attempts + 1 WHERE id = ?").param(id).update();
    }

    /** Marks the code used; {@code false} when another request used it first. */
    public boolean markUsed(long id, Instant now) {
        return jdbc.sql("UPDATE access_code SET used_at = ? WHERE id = ? AND used_at IS NULL")
                .params(Timestamp.from(now), id)
                .update() == 1;
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM access_code WHERE id = ?").param(id).update();
    }

    public int deleteCreatedBefore(Instant instant) {
        return jdbc.sql("DELETE FROM access_code WHERE created_at < ?").param(Timestamp.from(instant)).update();
    }

    private static StoredCode storedCode(ResultSet rs, int row) throws SQLException {
        Timestamp usedAt = rs.getTimestamp("used_at");
        return new StoredCode(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("code_hash"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getInt("attempts"),
                usedAt == null ? null : usedAt.toInstant());
    }
}
