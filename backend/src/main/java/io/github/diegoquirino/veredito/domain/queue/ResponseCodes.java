package io.github.diegoquirino.veredito.domain.queue;

import java.util.Optional;
import java.util.Set;

/** English category codes stored on each draft (Appendices B.1 and B.2). */
public final class ResponseCodes {

    public static final Set<String> CHANGE_IMPACT = Set.of("LOW", "HIGH");

    public static final Set<String> MAINTENANCE_OPERATION = Set.of(
            "KEEP", "UPDATE", "REMOVE", "CREATE", "MERGE", "SPLIT", "REASSIGN", "FLAG");

    public static final int COMMENT_MAX = 1000;

    private ResponseCodes() {}

    /** Stable reason slug for {@code invalid-request}, or empty when the draft is valid. */
    public static Optional<String> firstError(String changeImpact, String maintenanceOperation, String comment) {
        if (changeImpact == null || !CHANGE_IMPACT.contains(changeImpact)) {
            return Optional.of("invalid-change-impact");
        }
        if (maintenanceOperation == null || !MAINTENANCE_OPERATION.contains(maintenanceOperation)) {
            return Optional.of("invalid-maintenance-operation");
        }
        if (comment != null && comment.length() > COMMENT_MAX) {
            return Optional.of("comment-too-long");
        }
        return Optional.empty();
    }
}
