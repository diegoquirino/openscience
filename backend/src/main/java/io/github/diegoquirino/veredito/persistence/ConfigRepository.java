package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.domain.access.PersonName;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The single {@code config} row: administrator, limits and study phase. */
@Repository
public class ConfigRepository {

    private final JdbcClient jdbc;

    public ConfigRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record AdminProfile(String name, String email, String preferredLanguage) {}

    /** Serializes the code requests until the end of the current transaction (research section 5). */
    public void lockForUpdate() {
        jdbc.sql("SELECT id FROM config WHERE id = 1 FOR UPDATE").query(Integer.class).single();
    }

    public String adminEmail() {
        return jdbc.sql("SELECT admin_email FROM config WHERE id = 1").query(String.class).single();
    }

    public int dailyEmailLimit() {
        return jdbc.sql("SELECT daily_email_limit FROM config WHERE id = 1").query(Integer.class).single();
    }

    public int blockSize() {
        return jdbc.sql("SELECT block_size FROM config WHERE id = 1").query(Integer.class).single();
    }

    public int reservationMinutes() {
        return jdbc.sql("SELECT reservation_minutes FROM config WHERE id = 1")
                .query(Integer.class)
                .single();
    }

    public String studyPhase() {
        return jdbc.sql("SELECT study_phase FROM config WHERE id = 1").query(String.class).single();
    }

    public Instant studyPhaseChangedAt() {
        return jdbc.sql("SELECT study_phase_changed_at FROM config WHERE id = 1")
                .query((rs, row) -> {
                    Timestamp value = rs.getTimestamp("study_phase_changed_at");
                    return value == null ? null : value.toInstant();
                })
                .optional()
                .orElse(null);
    }

    /** Sets the study phase (e.g. {@code collection}) and the change timestamp. */
    public void updateStudyPhase(String phase, Instant now) {
        jdbc.sql("""
                        UPDATE config SET study_phase = ?, study_phase_changed_at = ?, updated_at = ?
                        WHERE id = 1""")
                .params(phase, Timestamp.from(now), Timestamp.from(now))
                .update();
    }

    /** Keeps the latest name; the language is set only at the first sign-in. */
    public void updateAdminSignIn(PersonName name, String languageIfNull) {
        jdbc.sql("""
                        UPDATE config SET admin_name = ?,
                            admin_preferred_language = COALESCE(admin_preferred_language, ?)
                        WHERE id = 1""")
                .params(name.value(), languageIfNull)
                .update();
    }

    /** Updates the administrator display name (profile menu, spec 014). */
    public void updateAdminName(PersonName name) {
        jdbc.sql("UPDATE config SET admin_name = ? WHERE id = 1").param(name.value()).update();
    }

    public AdminProfile adminProfile() {
        return jdbc.sql("SELECT admin_name, admin_email, admin_preferred_language FROM config WHERE id = 1")
                .query((rs, row) -> new AdminProfile(
                        rs.getString("admin_name"), rs.getString("admin_email"), rs.getString("admin_preferred_language")))
                .single();
    }

    public void updateAdminPreferredLanguage(String language) {
        jdbc.sql("UPDATE config SET admin_preferred_language = ? WHERE id = 1").param(language).update();
    }
}
