package io.github.diegoquirino.veredito.domain.dashboard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Queue progress: an overall row, then one row per system and batch (research section 3). */
public final class DashboardProgress {

    /** One system's items in one block; {@code blocks} come in queue order. */
    public record BlockFact(String system, long batchId, String batchFileName, long blockId, int items) {}

    /** One task of the queue. */
    public record TaskFact(long blockId, int role, int queuePosition, String status, Instant expiresAt) {}

    /**
     * One progress row; {@code system}, {@code batchId} and {@code batchFileName} are {@code null} in the
     * overall row.
     */
    public record Row(
            String system,
            Long batchId,
            String batchFileName,
            long items,
            long blocks,
            TaskCounts role1,
            TaskCounts role2) {}

    /** The rows and the lowest queue position that is free or reclaimable ({@code null} when none). */
    public record Result(List<Row> rows, Integer firstFreeQueuePosition) {}

    private record Key(String system, long batchId) {}

    private DashboardProgress() {}

    public static Result build(List<BlockFact> blocks, List<TaskFact> tasks, Instant now) {
        Map<Key, List<BlockFact>> groups = new LinkedHashMap<>();
        for (BlockFact block : blocks) {
            groups.computeIfAbsent(new Key(block.system(), block.batchId()), key -> new ArrayList<>()).add(block);
        }
        List<Row> rows = new ArrayList<>();
        Set<Long> allBlocks = new HashSet<>();
        blocks.forEach(block -> allBlocks.add(block.blockId()));
        rows.add(row(null, null, null, blocks, allBlocks, tasks, now));
        groups.forEach((key, members) -> {
            Set<Long> ids = new HashSet<>();
            members.forEach(block -> ids.add(block.blockId()));
            rows.add(row(key.system(), key.batchId(), members.get(0).batchFileName(), members, ids, tasks, now));
        });
        Integer firstFree = null;
        for (TaskFact task : tasks) {
            if (TaskBucket.of(task.status(), task.expiresAt(), now) == TaskBucket.FREE
                    && (firstFree == null || task.queuePosition() < firstFree)) {
                firstFree = task.queuePosition();
            }
        }
        return new Result(List.copyOf(rows), firstFree);
    }

    private static Row row(
            String system,
            Long batchId,
            String fileName,
            List<BlockFact> members,
            Set<Long> blockIds,
            List<TaskFact> tasks,
            Instant now) {
        long items = members.stream().mapToLong(BlockFact::items).sum();
        return new Row(system, batchId, fileName, items, blockIds.size(),
                counts(1, blockIds, tasks, now), counts(2, blockIds, tasks, now));
    }

    private static TaskCounts counts(int role, Set<Long> blockIds, List<TaskFact> tasks, Instant now) {
        long[] tally = new long[TaskBucket.values().length];
        for (TaskFact task : tasks) {
            if (task.role() == role && blockIds.contains(task.blockId())) {
                tally[TaskBucket.of(task.status(), task.expiresAt(), now).ordinal()]++;
            }
        }
        return TaskCounts.of(
                tally[TaskBucket.FREE.ordinal()],
                tally[TaskBucket.IN_PROGRESS.ordinal()],
                tally[TaskBucket.COMPLETED.ordinal()]);
    }
}