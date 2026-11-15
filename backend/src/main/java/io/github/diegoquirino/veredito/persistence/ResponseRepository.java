package io.github.diegoquirino.veredito.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Draft and final responses for a reserved or completed task (spec 007). */
@Repository
public class ResponseRepository {

    private static final String SELECT = """
            SELECT id, task_id, item_id, participant_id, state, change_impact, maintenance_operation,
                   comment, language, saved_at
            FROM response""";

    private final JdbcClient jdbc;

    public ResponseRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record ResponseRow(
            long id,
            long taskId,
            long itemId,
            long participantId,
            String state,
            String changeImpact,
            String maintenanceOperation,
            String comment,
            String language,
            Instant savedAt) {}

    public void upsertDraft(
            long taskId,
            long itemId,
            long participantId,
            String changeImpact,
            String maintenanceOperation,
            String comment,
            String language,
            Instant savedAt) {
        jdbc.sql("""
                        INSERT INTO response (
                            task_id, item_id, participant_id, state, change_impact, maintenance_operation,
                            comment, language, saved_at)
                        VALUES (?, ?, ?, 'draft', ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            participant_id = VALUES(participant_id),
                            state = 'draft',
                            change_impact = VALUES(change_impact),
                            maintenance_operation = VALUES(maintenance_operation),
                            comment = VALUES(comment),
                            language = VALUES(language),
                            saved_at = VALUES(saved_at)""")
                .params(
                        taskId,
                        itemId,
                        participantId,
                        changeImpact,
                        maintenanceOperation,
                        comment,
                        language,
                        Timestamp.from(savedAt))
                .update();
    }

    public void deleteByTaskId(long taskId) {
        jdbc.sql("DELETE FROM response WHERE task_id = ?").param(taskId).update();
    }

    public long countByTaskId(long taskId) {
        return jdbc.sql("SELECT COUNT(*) FROM response WHERE task_id = ?")
                .param(taskId)
                .query(Long.class)
                .single();
    }

    public List<ResponseRow> listByTaskId(long taskId) {
        return jdbc.sql(SELECT + " WHERE task_id = ? ORDER BY item_id")
                .param(taskId)
                .query(ResponseRepository::map)
                .list();
    }

    public Optional<ResponseRow> findByTaskAndItem(long taskId, long itemId) {
        return jdbc.sql(SELECT + " WHERE task_id = ? AND item_id = ?")
                .params(taskId, itemId)
                .query(ResponseRepository::map)
                .optional();
    }

    public void finalizeAll(long taskId) {
        jdbc.sql("UPDATE response SET state = 'final' WHERE task_id = ? AND state = 'draft'")
                .param(taskId)
                .update();
    }

    private static ResponseRow map(ResultSet rs, int row) throws SQLException {
        return new ResponseRow(
                rs.getLong("id"),
                rs.getLong("task_id"),
                rs.getLong("item_id"),
                rs.getLong("participant_id"),
                rs.getString("state"),
                rs.getString("change_impact"),
                rs.getString("maintenance_operation"),
                rs.getString("comment"),
                rs.getString("language"),
                rs.getTimestamp("saved_at").toInstant());
    }
}
