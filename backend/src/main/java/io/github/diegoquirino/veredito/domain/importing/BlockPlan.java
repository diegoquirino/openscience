package io.github.diegoquirino.veredito.domain.importing;

import java.util.ArrayList;
import java.util.List;

/** Splits included items into consecutive blocks of {@code blockSize} (FR-005). */
public final class BlockPlan {

    private BlockPlan() {}

    /** Block sizes in order; empty when there are no included items. */
    public static List<Integer> sizes(int includedCount, int blockSize) {
        if (includedCount < 0) {
            throw new IllegalArgumentException("includedCount must be >= 0");
        }
        if (blockSize < 1) {
            throw new IllegalArgumentException("blockSize must be >= 1");
        }
        if (includedCount == 0) {
            return List.of();
        }
        List<Integer> sizes = new ArrayList<>();
        int remaining = includedCount;
        while (remaining > 0) {
            int size = Math.min(blockSize, remaining);
            sizes.add(size);
            remaining -= size;
        }
        return List.copyOf(sizes);
    }
}
