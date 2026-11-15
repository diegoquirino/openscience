package io.github.diegoquirino.veredito.importing;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.importing.BlockPlan;
import io.github.diegoquirino.veredito.domain.importing.ImportClassifier;
import io.github.diegoquirino.veredito.domain.importing.ImportClassifier.ClassifiedRow;
import io.github.diegoquirino.veredito.domain.importing.ImportClassifier.Result;
import io.github.diegoquirino.veredito.importing.PreviewService.RowIssue;
import io.github.diegoquirino.veredito.persistence.BlockRepository;
import io.github.diegoquirino.veredito.persistence.BlockRepository.Block;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import io.github.diegoquirino.veredito.persistence.ImportBatchRepository;
import io.github.diegoquirino.veredito.persistence.ImportPreviewRepository;
import io.github.diegoquirino.veredito.persistence.ImportPreviewRepository.Preview;
import io.github.diegoquirino.veredito.persistence.ItemRepository;
import io.github.diegoquirino.veredito.persistence.ItemRepository.Item;
import io.github.diegoquirino.veredito.persistence.ItemRepository.NewItem;
import io.github.diegoquirino.veredito.persistence.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Confirms a preview into batches, blocks, items and free tasks (spec 005 US2). */
@Service
public class BatchImportService {

    private final ImportPreviewRepository previews;
    private final ImportBatchRepository batches;
    private final BlockRepository blocks;
    private final ItemRepository items;
    private final TaskRepository tasks;
    private final ConfigRepository config;
    private final JsonMapper json;
    private final MessageSource messages;
    private final Clock clock;
    private final JdbcClient jdbc;

    public BatchImportService(
            ImportPreviewRepository previews,
            ImportBatchRepository batches,
            BlockRepository blocks,
            ItemRepository items,
            TaskRepository tasks,
            ConfigRepository config,
            JsonMapper json,
            MessageSource messages,
            Clock clock,
            JdbcClient jdbc) {
        this.previews = previews;
        this.batches = batches;
        this.blocks = blocks;
        this.items = items;
        this.tasks = tasks;
        this.config = config;
        this.json = json;
        this.messages = messages;
        this.clock = clock;
        this.jdbc = jdbc;
    }

    public record QueuePositions(int from, int to) {}

    public record ConfirmResult(
            Long id,
            String fileName,
            Instant importedAt,
            int includedCount,
            int ignoredCount,
            int errorCount,
            int blockCount,
            List<Integer> blockSizes,
            QueuePositions queuePositions,
            List<String> systems,
            List<RowIssue> ignored,
            boolean created) {}

    @Transactional
    public ConfirmResult confirm(Long previewId, Locale locale) {
        if (previewId == null) {
            throw ProblemException.invalidFields(Map.of("previewId", "required"));
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        config.lockForUpdate();
        Preview preview = previews.findValid(previewId, now)
                .orElseThrow(() -> new ProblemException(ProblemType.PREVIEW_NOT_FOUND));

        List<Map<String, String>> candidates = readIncluded(preview.includedJson());
        Result classified = ImportClassifier.classify(candidates, items.existingKeys());
        List<ClassifiedRow> ignoredRows = new ArrayList<>(classified.ignored());
        ignoredRows.addAll(readStoredIssues(preview.ignoredJson()));
        List<ClassifiedRow> errorRows = readStoredIssues(preview.errorsJson());

        if (classified.included().isEmpty()) {
            return new ConfirmResult(
                    null,
                    preview.fileName(),
                    null,
                    0,
                    ignoredRows.size(),
                    errorRows.size(),
                    0,
                    List.of(),
                    null,
                    systemsOf(candidates),
                    localize(ignoredRows, locale),
                    false);
        }

        int blockSize = config.blockSize();
        List<Integer> sizes = BlockPlan.sizes(classified.included().size(), blockSize);
        long batchId = batches.insert(
                preview.fileName(),
                now,
                classified.included().size(),
                ignoredRows.size(),
                errorRows.size());

        int nextNumber = blocks.maxNumber() + 1;
        int nextQueue = tasks.maxQueuePosition() + 1;
        int queueFrom = nextQueue;
        List<NewItem> toInsert = new ArrayList<>(classified.included().size());
        int offset = 0;
        for (int size : sizes) {
            long blockId = blocks.insert(batchId, nextNumber++, size);
            tasks.insertFree(blockId, 1, nextQueue++);
            tasks.insertFree(blockId, 2, nextQueue++);
            for (int position = 1; position <= size; position++) {
                Map<String, String> row = classified.included().get(offset++);
                toInsert.add(new NewItem(
                        value(row, "system"),
                        value(row, "edit_key"),
                        value(row, "uc"),
                        value(row, "kind"),
                        value(row, "field"),
                        value(row, "origin"),
                        value(row, "target"),
                        value(row, "origin_version"),
                        value(row, "target_version"),
                        value(row, "origin_usecase"),
                        value(row, "target_usecase"),
                        batchId,
                        blockId,
                        position));
            }
        }
        items.insert(toInsert);
        previews.delete(preview.id());

        return new ConfirmResult(
                batchId,
                preview.fileName(),
                now,
                classified.included().size(),
                ignoredRows.size(),
                errorRows.size(),
                sizes.size(),
                sizes,
                new QueuePositions(queueFrom, nextQueue - 1),
                systemsOf(classified.included()),
                localize(ignoredRows, locale),
                true);
    }

    @Transactional(readOnly = true)
    public ConfirmResult get(long batchId, Locale locale) {
        var batch = batches.findById(batchId).orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
        List<Block> batchBlocks = blocks.listByBatch(batchId);
        List<Integer> sizes = batchBlocks.stream().map(Block::size).toList();
        List<Item> batchItems = items.findByBatch(batchId);
        List<String> systems = batchItems.stream()
                .map(Item::system)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
        List<Integer> positions = jdbc.sql("""
                        SELECT queue_position FROM task
                        WHERE block_id IN (SELECT id FROM block WHERE batch_id = ?)
                        ORDER BY queue_position""")
                .param(batchId)
                .query(Integer.class)
                .list();
        QueuePositions queue = positions.isEmpty()
                ? null
                : new QueuePositions(positions.getFirst(), positions.getLast());
        return new ConfirmResult(
                batch.id(),
                batch.fileName(),
                batch.importedAt(),
                batch.includedCount(),
                batch.ignoredCount(),
                batch.errorCount(),
                sizes.size(),
                sizes,
                queue,
                systems,
                List.of(),
                true);
    }

    private List<Map<String, String>> readIncluded(String payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> raw = json.readerForListOf(Map.class).readValue(payload);
            List<Map<String, String>> rows = new ArrayList<>(raw.size());
            for (Map<String, Object> map : raw) {
                Map<String, String> row = new LinkedHashMap<>();
                map.forEach((k, v) -> row.put(k, v == null ? "" : String.valueOf(v)));
                rows.add(row);
            }
            return rows;
        } catch (JacksonException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private List<ClassifiedRow> readStoredIssues(String payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> raw = json.readerForListOf(Map.class).readValue(payload);
            List<ClassifiedRow> rows = new ArrayList<>(raw.size());
            for (Map<String, Object> map : raw) {
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("system", stringOf(map.get("system")));
                Object editKey = map.get("editKey") != null ? map.get("editKey") : map.get("edit_key");
                fields.put("edit_key", stringOf(editKey));
                int line = ((Number) map.get("line")).intValue();
                rows.add(new ClassifiedRow(line, fields, stringOf(map.get("code"))));
            }
            return rows;
        } catch (JacksonException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private List<RowIssue> localize(List<ClassifiedRow> rows, Locale locale) {
        List<RowIssue> issues = new ArrayList<>(rows.size());
        for (ClassifiedRow row : rows) {
            issues.add(new RowIssue(
                    row.line(),
                    row.code(),
                    messages.getMessage("import.reason." + row.code(), null, locale),
                    value(row.fields(), "system"),
                    value(row.fields(), "edit_key")));
        }
        return List.copyOf(issues);
    }

    private static List<String> systemsOf(List<Map<String, String>> rows) {
        Set<String> systems = new LinkedHashSet<>();
        for (Map<String, String> row : rows) {
            String system = value(row, "system");
            if (!system.isEmpty()) {
                systems.add(system);
            }
        }
        return List.copyOf(systems);
    }

    private static String value(Map<String, String> row, String column) {
        String raw = row.get(column);
        return raw == null ? "" : raw;
    }

    private static String stringOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
