package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.domain.queue.NextTaskChooser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Queue tasks: free rows from import (005) and reservation lifecycle (007). */
@Repository
public class TaskRepository {

    private static final String SELECT = """
            SELECT id, block_id, role, queue_position, status, participant_id, reserved_at, expires_at, submitted_at
            FROM task
            """;

    private final JdbcClient jdbc;

    public TaskRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record TaskRow(
            long id,
            long blockId,
            int role,
            int queuePosition,
            String status,
            Long participantId,
            Instant reservedAt,
            Instant expiresAt,
            Instant submittedAt) {}

    /** Highest {@code queue_position} already stored, or {@code 0} when there are none. */
    public int maxQueuePosition() {
        return jdbc.sql("SELECT COALESCE(MAX(queue_position), 0) FROM task")
                .query(Integer.class)
                .single();
    }

    public long insertFree(long blockId, int role, int queuePosition) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO task (block_id, role, queue_position, status)
                        VALUES (?, ?, ?, 'free')""")
                .params(blockId, role, queuePosition)
                .update(keys);
        return keys.getKey().longValue();
    }

    public long countByBlock(long blockId) {
        return jdbc.sql("SELECT COUNT(*) FROM task WHERE block_id = ?")
                .param(blockId)
                .query(Long.class)
                .single();
    }

    public Optional<TaskRow> findById(long taskId) {
        return jdbc.sql(SELECT + " WHERE id = ?")
                .param(taskId)
                .query(TaskRepository::map)
                .optional();
    }

    public Optional<TaskRow> findByIdForUpdate(long taskId) {
        return jdbc.sql(SELECT + " WHERE id = ? FOR UPDATE")
                .param(taskId)
                .query(TaskRepository::map)
                .optional();
    }

    public Optional<TaskRow> findValidReservation(long participantId, Instant now) {
        return jdbc.sql(SELECT + """
                        WHERE participant_id = ? AND status = 'reserved' AND expires_at > ?
                        ORDER BY queue_position LIMIT 1""")
                .params(participantId, Timestamp.from(now))
                .query(TaskRepository::map)
                .optional();
    }

    public Set<Long> heldBlockIds(long participantId) {
        List<Long> ids = jdbc.sql("""
                        SELECT DISTINCT block_id FROM task
                        WHERE participant_id = ? AND status IN ('reserved', 'completed')""")
                .param(participantId)
                .query(Long.class)
                .list();
        return Set.copyOf(ids);
    }

    /** Free tasks and expired reservations, unlocked (for outcome hints). */
    public List<NextTaskChooser.Candidate> listCandidates(Instant now) {
        return jdbc.sql("""
                        SELECT id, block_id, role, queue_position, status, participant_id, expires_at
                        FROM task
                        WHERE status = 'free'
                           OR (status = 'reserved' AND expires_at <= ?)
                        ORDER BY queue_position""")
                .param(Timestamp.from(now))
                .query(TaskRepository::mapCandidate)
                .list();
    }

    /**
     * Free tasks and expired reservations, locked with {@code SKIP LOCKED} so concurrent reserves get
     * different rows.
     */
    public List<NextTaskChooser.Candidate> lockCandidatesForUpdateSkipLocked(Instant now) {
        return jdbc.sql("""
                        SELECT id, block_id, role, queue_position, status, participant_id, expires_at
                        FROM task
                        WHERE status = 'free'
                           OR (status = 'reserved' AND expires_at <= ?)
                        ORDER BY queue_position
                        FOR UPDATE SKIP LOCKED""")
                .param(Timestamp.from(now))
                .query(TaskRepository::mapCandidate)
                .list();
    }

    public void reserve(long taskId, long participantId, Instant reservedAt, Instant expiresAt) {
        int updated = jdbc.sql("""
                        UPDATE task SET status = 'reserved', participant_id = ?, reserved_at = ?,
                            expires_at = ?, submitted_at = NULL
                        WHERE id = ?""")
                .params(participantId, Timestamp.from(reservedAt), Timestamp.from(expiresAt), taskId)
                .update();
        if (updated != 1) {
            throw new IllegalStateException("Expected to reserve exactly one task, updated " + updated);
        }
    }

    public void renewExpiry(long taskId, Instant expiresAt) {
        jdbc.sql("UPDATE task SET expires_at = ? WHERE id = ? AND status = 'reserved'")
                .params(Timestamp.from(expiresAt), taskId)
                .update();
    }

    public void complete(long taskId, Instant submittedAt) {
        jdbc.sql("""
                        UPDATE task SET status = 'completed', submitted_at = ?
                        WHERE id = ? AND status = 'reserved'""")
                .params(Timestamp.from(submittedAt), taskId)
                .update();
    }

    /** Returns a reserved task to the free queue (spec 023 cancel). */
    public void releaseToFree(long taskId) {
        jdbc.sql("""
                        UPDATE task SET status = 'free', participant_id = NULL, reserved_at = NULL,
                            expires_at = NULL, submitted_at = NULL
                        WHERE id = ?""")
                .param(taskId)
                .update();
    }

    public long countByStatus(String status) {
        return jdbc.sql("SELECT COUNT(*) FROM task WHERE status = ?")
                .param(status)
                .query(Long.class)
                .single();
    }

    public long countAll() {
        return jdbc.sql("SELECT COUNT(*) FROM task").query(Long.class).single();
    }

    public boolean existsAnyItem() {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM item)").query(Boolean.class).single();
    }

    public Optional<TaskRow> findOwnExpiredReservation(long participantId, Instant now) {
        return jdbc.sql(SELECT + """
                        WHERE participant_id = ? AND status = 'reserved' AND expires_at <= ?
                        ORDER BY queue_position LIMIT 1""")
                .params(participantId, Timestamp.from(now))
                .query(TaskRepository::map)
                .optional();
    }

    private static TaskRow map(ResultSet rs, int row) throws SQLException {
        return new TaskRow(
                rs.getLong("id"),
                rs.getLong("block_id"),
                rs.getInt("role"),
                rs.getInt("queue_position"),
                rs.getString("status"),
                rs.getObject("participant_id") == null ? null : rs.getLong("participant_id"),
                toInstant(rs.getTimestamp("reserved_at")),
                toInstant(rs.getTimestamp("expires_at")),
                toInstant(rs.getTimestamp("submitted_at")));
    }

    private static NextTaskChooser.Candidate mapCandidate(ResultSet rs, int row) throws SQLException {
        return new NextTaskChooser.Candidate(
                rs.getLong("id"),
                rs.getLong("block_id"),
                rs.getInt("role"),
                rs.getInt("queue_position"),
                rs.getString("status"),
                rs.getObject("participant_id") == null ? null : rs.getLong("participant_id"),
                toInstant(rs.getTimestamp("expires_at")));
    }

    private static Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
