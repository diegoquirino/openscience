package io.github.diegoquirino.veredito.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Temporary CSV preview rows until confirm or expiry (spec 005). */
@Repository
public class ImportPreviewRepository {

    private static final String SELECT = """
            SELECT id, file_name, created_at, expires_at, included_count, ignored_count, error_count,
                   block_count, block_size, extra_columns_json, included_json, ignored_json, errors_json,
                   block_sizes_json
            FROM import_preview""";

    private final JdbcClient jdbc;

    public ImportPreviewRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record Preview(
            long id,
            String fileName,
            Instant createdAt,
            Instant expiresAt,
            int includedCount,
            int ignoredCount,
            int errorCount,
            int blockCount,
            int blockSize,
            String extraColumnsJson,
            String includedJson,
            String ignoredJson,
            String errorsJson,
            String blockSizesJson) {}

    public long insert(
            String fileName,
            Instant createdAt,
            Instant expiresAt,
            int includedCount,
            int ignoredCount,
            int errorCount,
            int blockCount,
            int blockSize,
            String extraColumnsJson,
            String includedJson,
            String ignoredJson,
            String errorsJson,
            String blockSizesJson) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO import_preview (
                            file_name, created_at, expires_at, included_count, ignored_count, error_count,
                            block_count, block_size, extra_columns_json, included_json, ignored_json,
                            errors_json, block_sizes_json)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(
                        fileName,
                        Timestamp.from(createdAt),
                        Timestamp.from(expiresAt),
                        includedCount,
                        ignoredCount,
                        errorCount,
                        blockCount,
                        blockSize,
                        extraColumnsJson,
                        includedJson,
                        ignoredJson,
                        errorsJson,
                        blockSizesJson)
                .update(keys);
        return keys.getKey().longValue();
    }

    /** Preview that still exists and has not expired at {@code now}. */
    public Optional<Preview> findValid(long id, Instant now) {
        return jdbc.sql(SELECT + " WHERE id = ? AND expires_at > ?")
                .params(id, Timestamp.from(now))
                .query(ImportPreviewRepository::map)
                .optional();
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM import_preview WHERE id = ?").param(id).update();
    }

    public int deleteExpired(Instant now) {
        return jdbc.sql("DELETE FROM import_preview WHERE expires_at <= ?")
                .param(Timestamp.from(now))
                .update();
    }

    private static Preview map(ResultSet rs, int row) throws SQLException {
        return new Preview(
                rs.getLong("id"),
                rs.getString("file_name"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getInt("included_count"),
                rs.getInt("ignored_count"),
                rs.getInt("error_count"),
                rs.getInt("block_count"),
                rs.getInt("block_size"),
                rs.getString("extra_columns_json"),
                rs.getString("included_json"),
                rs.getString("ignored_json"),
                rs.getString("errors_json"),
                rs.getString("block_sizes_json"));
    }
}
