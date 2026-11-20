package io.github.diegoquirino.veredito.withdrawal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC reads and writes for LGPD withdrawal (spec 011). */
@Repository
public class WithdrawalQuery {

    private final JdbcClient jdbc;

    public WithdrawalQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record ParticipantRow(
            long id,
            String pseudonym,
            String name,
            String email,
            String preferredLanguage,
            Instant anonymizedAt) {}

    public record ReservedSummary(int block, int role, Instant expiresAt) {}

    public record TaskRef(long id, String status) {}

    public Optional<ParticipantRow> findByIdForUpdate(long id) {
        return jdbc.sql("""
                        SELECT id, pseudonym, name, email, preferred_language, anonymized_at
                        FROM participant WHERE id = ? FOR UPDATE""")
                .param(id)
                .query(WithdrawalQuery::mapParticipant)
                .optional();
    }

    public Optional<ParticipantRow> findById(long id) {
        return jdbc.sql("""
                        SELECT id, pseudonym, name, email, preferred_language, anonymized_at
                        FROM participant WHERE id = ?""")
                .param(id)
                .query(WithdrawalQuery::mapParticipant)
                .optional();
    }

    public Optional<ParticipantRow> findByEmail(String email) {
        return jdbc.sql("""
                        SELECT id, pseudonym, name, email, preferred_language, anonymized_at
                        FROM participant WHERE LOWER(email) = ?""")
                .param(email.toLowerCase(Locale.ROOT))
                .query(WithdrawalQuery::mapParticipant)
                .optional();
    }

    public Optional<ParticipantRow> findByPseudonym(String pseudonym) {
        return jdbc.sql("""
                        SELECT id, pseudonym, name, email, preferred_language, anonymized_at
                        FROM participant WHERE pseudonym = ?""")
                .param(pseudonym)
                .query(WithdrawalQuery::mapParticipant)
                .optional();
    }

    /** Every participant (identifiable and already anonymized), newest first. */
    public List<ParticipantRow> listAll() {
        return jdbc.sql("""
                        SELECT id, pseudonym, name, email, preferred_language, anonymized_at
                        FROM participant
                        ORDER BY id DESC""")
                .query(WithdrawalQuery::mapParticipant)
                .list();
    }

    public void updatePseudonym(long participantId, String pseudonym) {
        jdbc.sql("UPDATE participant SET pseudonym = ? WHERE id = ?")
                .params(pseudonym, participantId)
                .update();
    }

    public long countCompletedTasks(long participantId) {
        return jdbc.sql("SELECT COUNT(*) FROM task WHERE participant_id = ? AND status = 'completed'")
                .param(participantId)
                .query(Long.class)
                .single();
    }

    public long countResponses(long participantId, String state) {
        return jdbc.sql("SELECT COUNT(*) FROM response WHERE participant_id = ? AND state = ?")
                .params(participantId, state)
                .query(Long.class)
                .single();
    }

    public Optional<ReservedSummary> findReserved(long participantId) {
        return jdbc.sql("""
                        SELECT b.number AS block_number, t.role, t.expires_at
                        FROM task t JOIN block b ON b.id = t.block_id
                        WHERE t.participant_id = ? AND t.status = 'reserved'
                        ORDER BY t.queue_position LIMIT 1""")
                .param(participantId)
                .query((rs, row) -> new ReservedSummary(
                        rs.getInt("block_number"),
                        rs.getInt("role"),
                        rs.getTimestamp("expires_at").toInstant()))
                .optional();
    }

    public List<TaskRef> lockTasksOfParticipant(long participantId) {
        return jdbc.sql("""
                        SELECT id, status FROM task WHERE participant_id = ? ORDER BY queue_position FOR UPDATE""")
                .param(participantId)
                .query((rs, row) -> new TaskRef(rs.getLong("id"), rs.getString("status")))
                .list();
    }

    public void deleteDraftsOfParticipant(long participantId) {
        jdbc.sql("DELETE FROM response WHERE participant_id = ? AND state = 'draft'")
                .param(participantId)
                .update();
    }

    public int deleteAllResponsesOfParticipant(long participantId) {
        return jdbc.sql("DELETE FROM response WHERE participant_id = ?")
                .param(participantId)
                .update();
    }

    public void releaseTaskToFree(long taskId) {
        jdbc.sql("""
                        UPDATE task SET status = 'free', participant_id = NULL, reserved_at = NULL,
                            expires_at = NULL, submitted_at = NULL
                        WHERE id = ?""")
                .param(taskId)
                .update();
    }

    public void deleteSessions(long participantId) {
        jdbc.sql("DELETE FROM access_session WHERE participant_id = ?")
                .param(participantId)
                .update();
    }

    public void deleteAccessCodes(String email) {
        jdbc.sql("DELETE FROM access_code WHERE email = ?").param(email).update();
    }

    public void anonymize(long participantId, Instant at) {
        jdbc.sql("""
                        UPDATE participant SET name = NULL, email = NULL, anonymized_at = ?
                        WHERE id = ?""")
                .params(Timestamp.from(at), participantId)
                .update();
    }

    public void insertLog(
            Instant occurredAt,
            String pseudonym,
            String optionCode,
            boolean emailSent,
            int finalsDiscarded,
            int tasksReturned,
            long participantId) {
        jdbc.sql("""
                        INSERT INTO withdrawal_log (
                            occurred_at, pseudonym, option_code, email_sent,
                            final_responses_discarded, tasks_returned, participant_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?)""")
                .params(
                        Timestamp.from(occurredAt),
                        pseudonym,
                        optionCode,
                        emailSent,
                        finalsDiscarded,
                        tasksReturned,
                        participantId)
                .update();
    }

    public long countEmailSentSince(Instant since) {
        return jdbc.sql("SELECT COUNT(*) FROM withdrawal_log WHERE email_sent = TRUE AND occurred_at > ?")
                .param(Timestamp.from(since))
                .query(Long.class)
                .single();
    }

    private static ParticipantRow mapParticipant(ResultSet rs, int row) throws SQLException {
        Timestamp anonymized = rs.getTimestamp("anonymized_at");
        return new ParticipantRow(
                rs.getLong("id"),
                rs.getString("pseudonym"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("preferred_language"),
                anonymized == null ? null : anonymized.toInstant());
    }
}
