package io.github.diegoquirino.veredito.dashboard;

import io.github.diegoquirino.veredito.domain.dashboard.DashboardProgress.BlockFact;
import io.github.diegoquirino.veredito.domain.dashboard.DashboardProgress.TaskFact;
import io.github.diegoquirino.veredito.domain.dashboard.ParticipantTotals;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The read-only JDBC queries of spec 010. Name and e-mail columns of {@code participant} are never selected,
 * and anonymized participants are left out of every person-related tally.
 */
@Repository
public class DashboardQuery {

    /** A reserved task, active or expired, with the drafts saved so far. */
    public record ReservationRow(
            String pseudonym, String system, int block, int role, long draftCount, Instant expiresAt) {}

    private final JdbcClient jdbc;

    public DashboardQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Items per system, batch and block, in queue order. */
    public List<BlockFact> blocks() {
        return jdbc.sql("""
                        SELECT i.`system` AS item_system, b.batch_id, ib.file_name, b.id AS block_id,
                               COUNT(*) AS items
                        FROM item i
                        JOIN block b ON b.id = i.block_id
                        JOIN import_batch ib ON ib.id = b.batch_id
                        GROUP BY i.`system`, b.batch_id, ib.file_name, b.id, b.number
                        ORDER BY b.number, MIN(i.position)""")
                .query((rs, row) -> new BlockFact(
                        rs.getString("item_system"),
                        rs.getLong("batch_id"),
                        rs.getString("file_name"),
                        rs.getLong("block_id"),
                        rs.getInt("items")))
                .list();
    }

    public List<TaskFact> tasks() {
        return jdbc.sql("SELECT block_id, role, queue_position, status, expires_at FROM task ORDER BY queue_position")
                .query((rs, row) -> new TaskFact(
                        rs.getLong("block_id"),
                        rs.getInt("role"),
                        rs.getInt("queue_position"),
                        rs.getString("status"),
                        instant(rs, "expires_at")))
                .list();
    }

    /** Every reserved task by expiry, oldest first; the system is that of the block's first item. */
    public List<ReservationRow> reservations() {
        return jdbc.sql("""
                        SELECT p.pseudonym, MIN(i.`system`) AS item_system, b.number AS block_number, t.role,
                               t.expires_at,
                               (SELECT COUNT(*) FROM response r WHERE r.task_id = t.id AND r.state = 'draft')
                                   AS draft_count
                        FROM task t
                        JOIN block b ON b.id = t.block_id
                        JOIN item i ON i.block_id = b.id
                        JOIN participant p ON p.id = t.participant_id
                        WHERE t.status = 'reserved'
                        GROUP BY t.id, p.pseudonym, b.number, t.role, t.expires_at
                        ORDER BY t.expires_at, t.id""")
                .query((rs, row) -> new ReservationRow(
                        rs.getString("pseudonym"),
                        rs.getString("item_system"),
                        rs.getInt("block_number"),
                        rs.getInt("role"),
                        rs.getLong("draft_count"),
                        instant(rs, "expires_at")))
                .list();
    }

    /** Onboarding columns of every non-anonymized participant. */
    public List<ParticipantTotals.Fact> participants() {
        return jdbc.sql("""
                        SELECT pseudonym, preferred_language, consent_decision, screening_answer, eligible,
                               professional_skill
                        FROM participant
                        WHERE anonymized_at IS NULL""")
                .query((rs, row) -> new ParticipantTotals.Fact(
                        rs.getString("pseudonym"),
                        rs.getString("preferred_language"),
                        rs.getString("consent_decision"),
                        rs.getString("screening_answer"),
                        nullableBoolean(rs, "eligible"),
                        rs.getString("professional_skill")))
                .list();
    }

    /** Completed tasks per pseudonym of non-anonymized participants. */
    public Map<String, Long> completedByPseudonym() {
        return counts("""
                SELECT p.pseudonym AS label, COUNT(*) AS total
                FROM task t JOIN participant p ON p.id = t.participant_id
                WHERE t.status = 'completed' AND p.anonymized_at IS NULL
                GROUP BY p.pseudonym""");
    }

    /** Non-anonymized participants per preferred language. */
    public Map<String, Long> preferredLanguages() {
        return counts("""
                SELECT preferred_language AS label, COUNT(*) AS total
                FROM participant
                WHERE anonymized_at IS NULL AND preferred_language IS NOT NULL
                GROUP BY preferred_language""");
    }

    /** Final responses per response language; drafts never count. */
    public Map<String, Long> finalResponseLanguages() {
        return counts("""
                SELECT language AS label, COUNT(*) AS total
                FROM response
                WHERE state = 'final'
                GROUP BY language""");
    }

    private Map<String, Long> counts(String sql) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.sql(sql).query((rs, row) -> {
            counts.put(rs.getString("label"), rs.getLong("total"));
            return null;
        }).list();
        return counts;
    }

    private static Boolean nullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}