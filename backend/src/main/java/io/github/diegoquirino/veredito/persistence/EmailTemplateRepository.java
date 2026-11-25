package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.invite.InviteTemplateKind;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EmailTemplateRepository {

    private final JdbcClient jdbc;

    public EmailTemplateRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record Template(String kind, String subject, String body, Instant updatedAt) {}

    public Template require(InviteTemplateKind kind) {
        return jdbc.sql("SELECT kind, subject, body, updated_at FROM email_template WHERE kind = ?")
                .param(kind.code())
                .query((rs, row) -> new Template(
                        rs.getString("kind"),
                        rs.getString("subject"),
                        rs.getString("body"),
                        rs.getTimestamp("updated_at").toInstant()))
                .single();
    }

    public void update(InviteTemplateKind kind, String subject, String body, Instant now) {
        jdbc.sql("""
                        UPDATE email_template SET subject = ?, body = ?, updated_at = ?
                        WHERE kind = ?""")
                .params(subject, body, Timestamp.from(now), kind.code())
                .update();
    }
}
