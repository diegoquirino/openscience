package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.domain.onboarding.ConsentFormRules;
import io.github.diegoquirino.veredito.domain.onboarding.ConsentFormRules.FormSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reads the consent forms; the application never inserts, updates or deletes them (data comes from Flyway). */
@Repository
public class ConsentFormRepository {

    private final JdbcClient jdbc;

    public ConsentFormRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record ConsentFormRow(long id, String version, String referenceLanguage, Instant publishedAt) {}

    public record ConsentTextRow(
            long formId, String version, String language, String referenceLanguage, String text, Instant publishedAt) {}

    /** Published at or before {@code now}, with the three texts; the latest publication wins (data-model.md). */
    public Optional<ConsentFormRow> findCurrent(Instant now) {
        return jdbc.sql("""
                        SELECT cf.id, cf.version, cf.reference_language, cf.published_at
                        FROM consent_form cf
                        WHERE cf.published_at IS NOT NULL
                          AND cf.published_at <= ?
                          AND (SELECT COUNT(*) FROM consent_form_text t WHERE t.consent_form_id = cf.id) = 3
                        ORDER BY cf.published_at DESC, cf.id DESC
                        LIMIT 1""")
                .param(Timestamp.from(now))
                .query(ConsentFormRepository::form)
                .optional();
    }

    public Optional<ConsentFormRow> findById(long id) {
        return jdbc.sql("SELECT id, version, reference_language, published_at FROM consent_form WHERE id = ?")
                .param(id)
                .query(ConsentFormRepository::form)
                .optional();
    }

    /** One text of one published and complete version; empty for an unknown, unpublished or incomplete one. */
    public Optional<ConsentTextRow> findText(String version, String language, Instant now) {
        return jdbc.sql("""
                        SELECT cf.id, cf.version, t.language, cf.reference_language, t.text, cf.published_at
                        FROM consent_form cf
                        JOIN consent_form_text t ON t.consent_form_id = cf.id
                        WHERE cf.version = ? AND t.language = ?
                          AND cf.published_at IS NOT NULL
                          AND cf.published_at <= ?
                          AND (SELECT COUNT(*) FROM consent_form_text c WHERE c.consent_form_id = cf.id) = 3""")
                .params(version, language, Timestamp.from(now))
                .query((rs, row) -> new ConsentTextRow(
                        rs.getLong("id"),
                        rs.getString("version"),
                        rs.getString("language"),
                        rs.getString("reference_language"),
                        rs.getString("text"),
                        rs.getTimestamp("published_at").toInstant()))
                .optional();
    }

    /** Every form with the languages of its texts, for {@link ConsentFormRules}. */
    public List<FormSnapshot> listSnapshots() {
        return jdbc.sql("""
                        SELECT cf.id, cf.version, cf.published_at,
                               COALESCE(GROUP_CONCAT(t.language ORDER BY t.language SEPARATOR ','), '') AS languages
                        FROM consent_form cf
                        LEFT JOIN consent_form_text t ON t.consent_form_id = cf.id
                        GROUP BY cf.id, cf.version, cf.published_at
                        ORDER BY cf.id""")
                .query((rs, row) -> snapshot(rs))
                .list();
    }

    /** Published versions that lack a language; never current (FR-012). */
    public List<FormSnapshot> listIncompletePublished(Instant now) {
        return ConsentFormRules.incompletePublished(listSnapshots(), now);
    }

    private static FormSnapshot snapshot(ResultSet rs) throws SQLException {
        Timestamp published = rs.getTimestamp("published_at");
        String languages = rs.getString("languages");
        return new FormSnapshot(
                rs.getLong("id"),
                rs.getString("version"),
                published == null ? null : published.toInstant(),
                languages.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(languages.split(","))));
    }

    private static ConsentFormRow form(ResultSet rs, int row) throws SQLException {
        Timestamp published = rs.getTimestamp("published_at");
        return new ConsentFormRow(
                rs.getLong("id"),
                rs.getString("version"),
                rs.getString("reference_language"),
                published == null ? null : published.toInstant());
    }
}
