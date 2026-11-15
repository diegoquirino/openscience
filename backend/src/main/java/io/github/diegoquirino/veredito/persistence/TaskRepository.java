package io.github.diegoquirino.veredito.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Free queue tasks created with each imported block (spec 005). */
@Repository
public class TaskRepository {

    private final JdbcClient jdbc;

    public TaskRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

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
}
