package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.access.Pseudonym;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The participant data that sign-in reads and writes (research section 8). */
@Repository
public class ParticipantRepository {

    /** Placeholder pseudonyms start with a character that {@link Pseudonym} never uses. */
    private static final String PLACEHOLDER_PREFIX = "~";

    private static final String SELECT =
            "SELECT id, pseudonym, name, registered_at, preferred_language FROM participant";

    private final JdbcClient jdbc;
    private final SecureRandom random = new SecureRandom();

    public ParticipantRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record ParticipantProfile(
            long id, String pseudonym, String name, Instant registeredAt, String preferredLanguage) {}

    /**
     * Creates the participant at the first sign-in or updates the name at the next ones; must run in a
     * transaction. A new row gets {@code R-<id>} and the language in use; an existing row keeps its
     * pseudonym, and its language when it has one.
     */
    public ParticipantProfile upsertOnSignIn(EmailAddress email, PersonName name, String language, Instant now) {
        // InnoDB spends an auto-increment value on every INSERT ... ON DUPLICATE KEY UPDATE, even when it
        // updates; a returning person must not leave a gap in the pseudonyms (FR-006). The plain read takes
        // no gap lock, so two concurrent first sign-ins still meet in the upsert below.
        Optional<Long> existing = jdbc.sql("SELECT id FROM participant WHERE email = ?")
                .param(email.value())
                .query(Long.class)
                .optional();
        if (existing.isPresent()) {
            jdbc.sql("""
                            UPDATE participant SET name = ?, preferred_language = COALESCE(preferred_language, ?)
                            WHERE id = ?""")
                    .params(name.value(), language, existing.get())
                    .update();
            return jdbc.sql(SELECT + " WHERE id = ?")
                    .param(existing.get())
                    .query(ParticipantRepository::profile)
                    .single();
        }
        String placeholder = placeholder();
        jdbc.sql("""
                        INSERT INTO participant (pseudonym, name, email, registered_at, preferred_language)
                        VALUES (?, ?, ?, ?, ?) AS typed
                        ON DUPLICATE KEY UPDATE
                            name = typed.name,
                            preferred_language = COALESCE(participant.preferred_language, typed.preferred_language)""")
                .params(placeholder, name.value(), email.value(), Timestamp.from(now), language)
                .update();
        ParticipantProfile row = jdbc.sql(SELECT + " WHERE email = ? FOR UPDATE")
                .param(email.value())
                .query(ParticipantRepository::profile)
                .single();
        if (!placeholder.equals(row.pseudonym())) {
            return row;
        }
        String pseudonym = Pseudonym.of(row.id());
        jdbc.sql("UPDATE participant SET pseudonym = ? WHERE id = ?").params(pseudonym, row.id()).update();
        return new ParticipantProfile(row.id(), pseudonym, row.name(), row.registeredAt(), row.preferredLanguage());
    }

    public Optional<ParticipantProfile> findById(long id) {
        return jdbc.sql(SELECT + " WHERE id = ?").param(id).query(ParticipantRepository::profile).optional();
    }

    public void updatePreferredLanguage(long id, String language) {
        jdbc.sql("UPDATE participant SET preferred_language = ? WHERE id = ?").params(language, id).update();
    }

    private String placeholder() {
        byte[] bytes = new byte[7];
        random.nextBytes(bytes);
        return PLACEHOLDER_PREFIX + HexFormat.of().formatHex(bytes);
    }

    private static ParticipantProfile profile(ResultSet rs, int row) throws SQLException {
        return new ParticipantProfile(
                rs.getLong("id"),
                rs.getString("pseudonym"),
                rs.getString("name"),
                rs.getTimestamp("registered_at").toInstant(),
                rs.getString("preferred_language"));
    }
}
