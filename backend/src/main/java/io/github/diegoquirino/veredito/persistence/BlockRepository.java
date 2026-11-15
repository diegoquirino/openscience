package io.github.diegoquirino.veredito.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Queue blocks formed on import confirm (spec 005). */
@Repository
public class BlockRepository {

    private final JdbcClient jdbc;

    public BlockRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record Block(long id, long batchId, int number, int size) {}

    /** Highest block number already stored, or {@code 0} when there are none. */
    public int maxNumber() {
        return jdbc.sql("SELECT COALESCE(MAX(number), 0) FROM block").query(Integer.class).single();
    }

    public long insert(long batchId, int number, int size) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO block (batch_id, number, size) VALUES (?, ?, ?)")
                .params(batchId, number, size)
                .update(keys);
        return keys.getKey().longValue();
    }

    public long countByBatch(long batchId) {
        return jdbc.sql("SELECT COUNT(*) FROM block WHERE batch_id = ?")
                .param(batchId)
                .query(Long.class)
                .single();
    }

    public List<Block> listByBatch(long batchId) {
        return jdbc.sql("SELECT id, batch_id, number, size FROM block WHERE batch_id = ? ORDER BY number")
                .param(batchId)
                .query(BlockRepository::map)
                .list();
    }

    public Optional<Block> findById(long id) {
        return jdbc.sql("SELECT id, batch_id, number, size FROM block WHERE id = ?")
                .param(id)
                .query(BlockRepository::map)
                .optional();
    }

    private static Block map(ResultSet rs, int row) throws SQLException {
        return new Block(rs.getLong("id"), rs.getLong("batch_id"), rs.getInt("number"), rs.getInt("size"));
    }
}
