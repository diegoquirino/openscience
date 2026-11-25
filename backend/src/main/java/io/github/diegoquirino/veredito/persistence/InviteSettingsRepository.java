package io.github.diegoquirino.veredito.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class InviteSettingsRepository {

    private final JdbcClient jdbc;

    public InviteSettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record InviteSettings(
            String caae,
            String platformUrl,
            Instant collectionStartAt,
            Instant collectionEndAt,
            Instant reminderAt) {}

    public InviteSettings load() {
        return jdbc.sql("""
                        SELECT invite_caae, invite_platform_url, invite_collection_start_at,
                               invite_collection_end_at, invite_reminder_at
                        FROM config WHERE id = 1""")
                .query((rs, row) -> new InviteSettings(
                        rs.getString("invite_caae"),
                        rs.getString("invite_platform_url"),
                        instant(rs.getTimestamp("invite_collection_start_at")),
                        instant(rs.getTimestamp("invite_collection_end_at")),
                        instant(rs.getTimestamp("invite_reminder_at"))))
                .single();
    }

    public void save(InviteSettings settings, Instant now) {
        jdbc.sql("""
                        UPDATE config SET
                            invite_caae = ?,
                            invite_platform_url = ?,
                            invite_collection_start_at = ?,
                            invite_collection_end_at = ?,
                            invite_reminder_at = ?,
                            updated_at = ?
                        WHERE id = 1""")
                .params(
                        settings.caae(),
                        settings.platformUrl(),
                        ts(settings.collectionStartAt()),
                        ts(settings.collectionEndAt()),
                        ts(settings.reminderAt()),
                        Timestamp.from(now))
                .update();
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static Timestamp ts(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
