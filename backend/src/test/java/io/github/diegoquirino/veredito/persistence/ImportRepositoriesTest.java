package io.github.diegoquirino.veredito.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.domain.importing.BlockPlan;
import io.github.diegoquirino.veredito.domain.importing.ImportClassifier;
import io.github.diegoquirino.veredito.persistence.ImportPreviewRepository.Preview;
import io.github.diegoquirino.veredito.persistence.ItemRepository.NewItem;
import io.github.diegoquirino.veredito.support.MySqlContainerConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** Repositories of spec 005 against MySQL 9.7 (preview and confirm persistence). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MySqlContainerConfig.class)
class ImportRepositoriesTest {

    @Autowired
    ImportPreviewRepository previews;

    @Autowired
    ImportBatchRepository batches;

    @Autowired
    BlockRepository blocks;

    @Autowired
    ItemRepository items;

    @Autowired
    TaskRepository tasks;

    @Autowired
    ConfigRepository config;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcClient jdbc;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Test
    void previewRoundTripAndExpiry() {
        Instant expires = now.plus(30, ChronoUnit.MINUTES);
        long id = previews.insert(
                "sample.csv",
                now,
                expires,
                2,
                1,
                0,
                1,
                10,
                "[]",
                "[{\"system\":\"minitest\"}]",
                "[{\"line\":3,\"code\":\"already-imported\"}]",
                "[]",
                "[2]");

        Preview found = previews.findValid(id, now).orElseThrow();
        assertThat(found.fileName()).isEqualTo("sample.csv");
        assertThat(found.includedCount()).isEqualTo(2);
        assertThat(found.ignoredJson()).contains("already-imported");
        assertThat(previews.findValid(id, expires)).isEmpty();

        assertThat(previews.deleteExpired(expires.plusSeconds(1))).isEqualTo(1);
        assertThat(previews.findValid(id, now)).isEmpty();
    }

    @Test
    void elevenItemsBecomeTwoBlocksAndFourContiguousFreeTasks() {
        transaction.executeWithoutResult(status -> {
            config.lockForUpdate();
            int blockSize = config.blockSize();
            assertThat(blockSize).isEqualTo(10);
            assertThat(config.studyPhase()).isEqualTo("pilot");

            List<Map<String, String>> rows = elevenRows();
            assertThat(ImportClassifier.classify(rows, items.existingKeys()).included()).hasSize(11);
            List<Integer> sizes = BlockPlan.sizes(11, blockSize);
            assertThat(sizes).containsExactly(10, 1);

            Instant importedAt = now;
            long batchId = batches.insert("eleven.csv", importedAt, 11, 0, 0);
            int nextNumber = blocks.maxNumber() + 1;
            int nextQueue = tasks.maxQueuePosition() + 1;
            List<NewItem> toInsert = new ArrayList<>();
            int offset = 0;
            for (int size : sizes) {
                long blockId = blocks.insert(batchId, nextNumber++, size);
                tasks.insertFree(blockId, 1, nextQueue++);
                tasks.insertFree(blockId, 2, nextQueue++);
                for (int position = 1; position <= size; position++) {
                    Map<String, String> row = rows.get(offset++);
                    toInsert.add(new NewItem(
                            row.get("system"),
                            row.get("edit_key"),
                            row.get("uc"),
                            row.get("kind"),
                            row.get("field"),
                            row.get("origin"),
                            row.get("target"),
                            row.get("origin_version"),
                            row.get("target_version"),
                            row.get("origin_usecase"),
                            row.get("target_usecase"),
                            batchId,
                            blockId,
                            position));
                }
            }
            items.insert(toInsert);

            assertThat(blocks.countByBatch(batchId)).isEqualTo(2);
            assertThat(blocks.listByBatch(batchId))
                    .extracting(BlockRepository.Block::size)
                    .containsExactly(10, 1);
            assertThat(items.count()).isEqualTo(11);
            assertThat(items.findByBatch(batchId)).hasSize(11);
            assertThat(items.existingKeys()).hasSize(11);

            List<Integer> positions = jdbc.sql("""
                            SELECT queue_position FROM task
                            WHERE block_id IN (SELECT id FROM block WHERE batch_id = ?)
                            ORDER BY queue_position""")
                    .param(batchId)
                    .query(Integer.class)
                    .list();
            assertThat(positions).containsExactly(1, 2, 3, 4);
            assertThat(tasks.countByBlock(blocks.listByBatch(batchId).get(0).id())).isEqualTo(2);

            assertThat(batches.findById(batchId).orElseThrow().fileName()).isEqualTo("eleven.csv");
            assertThat(batches.listNewestFirst()).extracting(ImportBatchRepository.Batch::id).contains(batchId);
        });
    }

    private static List<Map<String, String>> elevenRows() {
        List<Map<String, String>> rows = new ArrayList<>(11);
        for (int i = 1; i <= 11; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("system", "minitest");
            row.put("edit_key", "key-" + i);
            row.put("uc", "UC-" + i);
            row.put("kind", "MOD");
            row.put("field", "description");
            row.put("origin", "origin " + i);
            row.put("target", "target " + i);
            row.put("origin_version", "1.0");
            row.put("target_version", "2.0");
            row.put("origin_usecase", "ou " + i);
            row.put("target_usecase", "tu " + i);
            rows.add(row);
        }
        return rows;
    }
}
