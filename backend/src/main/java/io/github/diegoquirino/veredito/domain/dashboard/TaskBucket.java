package io.github.diegoquirino.veredito.domain.dashboard;

import java.time.Instant;

/**
 * Where a task counts in the progress numbers (research section 3): an expired reservation is free again,
 * exactly as {@code NextTaskChooser} reclaims it.
 */
public enum TaskBucket {
    FREE,
    IN_PROGRESS,
    COMPLETED;

    /** {@code status} is the {@code task.status} column; {@code expiresAt} matters only while reserved. */
    public static TaskBucket of(String status, Instant expiresAt, Instant now) {
        return switch (status) {
            case "completed" -> COMPLETED;
            case "reserved" -> expiresAt != null && expiresAt.isAfter(now) ? IN_PROGRESS : FREE;
            default -> FREE;
        };
    }
}