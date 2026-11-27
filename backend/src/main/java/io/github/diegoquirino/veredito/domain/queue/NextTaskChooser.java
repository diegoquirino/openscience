package io.github.diegoquirino.veredito.domain.queue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Picks the first free or expired task the participant can label, in queue order. Own valid
 * reservations are resumed by the service before calling this chooser. Spec 042 also skips tasks
 * whose block shares any {@code item.system} with systems the participant already holds.
 */
public final class NextTaskChooser {

    private NextTaskChooser() {}

    /**
     * @param ordered candidates already sorted by {@code queue_position} ascending
     * @param heldBlockIds blocks where the participant already has any role (reserved or completed)
     * @param heldSystems systems of those held blocks (spec 042)
     */
    public static Optional<Candidate> choose(
            List<Candidate> ordered, Set<Long> heldBlockIds, Set<String> heldSystems, Instant now) {
        for (Candidate candidate : ordered) {
            if (heldBlockIds.contains(candidate.blockId())) {
                continue;
            }
            if (!Collections.disjoint(heldSystems, candidate.systems())) {
                continue;
            }
            if (isFree(candidate) || isExpired(candidate, now)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static boolean isFree(Candidate candidate) {
        return "free".equals(candidate.status());
    }

    private static boolean isExpired(Candidate candidate, Instant now) {
        return "reserved".equals(candidate.status())
                && candidate.expiresAt() != null
                && !candidate.expiresAt().isAfter(now);
    }

    /** Snapshot used by the chooser; mirrors the locked rows from persistence plus item systems. */
    public record Candidate(
            long taskId,
            long blockId,
            int role,
            int queuePosition,
            String status,
            Long participantId,
            Instant expiresAt,
            Set<String> systems) {}
}
