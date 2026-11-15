package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.domain.importing.ImportClassifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Study items written on import confirm (spec 005). */
@Repository
public class ItemRepository {

    private static final String SELECT = """
            SELECT id, `system`, edit_key, uc, kind, field, origin, target, origin_version, target_version,
                   origin_usecase, target_usecase, batch_id, block_id, position
            FROM item""";

    private final JdbcClient jdbc;

    public ItemRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record Item(
            long id,
            String system,
            String editKey,
            String uc,
            String kind,
            String field,
            String origin,
            String target,
            String originVersion,
            String targetVersion,
            String originUsecase,
            String targetUsecase,
            long batchId,
            long blockId,
            int position) {}

    /**
     * Keys already in the database, in the {@link ImportClassifier#key(Map)} format
     * ({@code system + "\\0" + edit_key}).
     */
    public Set<String> existingKeys() {
        List<Map<String, Object>> rows = jdbc.sql("SELECT `system`, edit_key FROM item").query().listOfRows();
        Set<String> keys = new HashSet<>(rows.size());
        for (Map<String, Object> row : rows) {
            keys.add(row.get("system") + "\0" + row.get("edit_key"));
        }
        return Set.copyOf(keys);
    }

    public void insert(Collection<NewItem> items) {
        for (NewItem item : items) {
            jdbc.sql("""
                            INSERT INTO item (
                                `system`, edit_key, uc, kind, field, origin, target,
                                origin_version, target_version, origin_usecase, target_usecase,
                                batch_id, block_id, position)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                    .params(
                            item.system(),
                            item.editKey(),
                            item.uc(),
                            item.kind(),
                            item.field(),
                            blankToNull(item.origin()),
                            blankToNull(item.target()),
                            item.originVersion(),
                            item.targetVersion(),
                            item.originUsecase(),
                            item.targetUsecase(),
                            item.batchId(),
                            item.blockId(),
                            item.position())
                    .update();
        }
    }

    public long count() {
        return jdbc.sql("SELECT COUNT(*) FROM item").query(Long.class).single();
    }

    public List<Item> findByBatch(long batchId) {
        return jdbc.sql(SELECT + " WHERE batch_id = ? ORDER BY block_id, position")
                .param(batchId)
                .query(ItemRepository::map)
                .list();
    }

    public List<Item> listByBlockOrdered(long blockId) {
        return jdbc.sql(SELECT + " WHERE block_id = ? ORDER BY position")
                .param(blockId)
                .query(ItemRepository::map)
                .list();
    }

    public Optional<Item> findByBlockAndPosition(long blockId, int position) {
        return jdbc.sql(SELECT + " WHERE block_id = ? AND position = ?")
                .params(blockId, position)
                .query(ItemRepository::map)
                .optional();
    }

    public record NewItem(
            String system,
            String editKey,
            String uc,
            String kind,
            String field,
            String origin,
            String target,
            String originVersion,
            String targetVersion,
            String originUsecase,
            String targetUsecase,
            long batchId,
            long blockId,
            int position) {}

    private static String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static Item map(ResultSet rs, int row) throws SQLException {
        return new Item(
                rs.getLong("id"),
                rs.getString("system"),
                rs.getString("edit_key"),
                rs.getString("uc"),
                rs.getString("kind"),
                rs.getString("field"),
                rs.getString("origin"),
                rs.getString("target"),
                rs.getString("origin_version"),
                rs.getString("target_version"),
                rs.getString("origin_usecase"),
                rs.getString("target_usecase"),
                rs.getLong("batch_id"),
                rs.getLong("block_id"),
                rs.getInt("position"));
    }
}
