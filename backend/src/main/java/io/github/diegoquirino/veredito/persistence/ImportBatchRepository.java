package io.github.diegoquirino.veredito.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Confirmed import batches (spec 005 writes; schema from 003). */
@Repository
public class ImportBatchRepository {

    private static final String SELECT =
            "SELECT id, file_name, imported_at, included_count, ignored_count, error_count FROM import_batch";

    private final JdbcClient jdbc;

    public ImportBatchRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record Batch(
            long id,
            String fileName,
            Instant importedAt,
            int includedCount,
            int ignoredCount,
            int errorCount) {}

    public long insert(
            String fileName, Instant importedAt, int includedCount, int ignoredCount, int errorCount) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO import_batch (file_name, imported_at, included_count, ignored_count, error_count)
                        VALUES (?, ?, ?, ?, ?)""")
                .params(
                        fileName,
                        Timestamp.from(importedAt),
                        includedCount,
                        ignoredCount,
                        errorCount)
                .update(keys);
        return keys.getKey().longValue();
    }

    public Optional<Batch> findById(long id) {
        return jdbc.sql(SELECT + " WHERE id = ?").param(id).query(ImportBatchRepository::map).optional();
    }

    public List<Batch> listNewestFirst() {
        return jdbc.sql(SELECT + " ORDER BY imported_at DESC, id DESC")
                .query(ImportBatchRepository::map)
                .list();
    }

    private static Batch map(ResultSet rs, int row) throws SQLException {
        return new Batch(
                rs.getLong("id"),
                rs.getString("file_name"),
                rs.getTimestamp("imported_at").toInstant(),
                rs.getInt("included_count"),
                rs.getInt("ignored_count"),
                rs.getInt("error_count"));
    }
}
