package io.github.diegoquirino.veredito.domain.queue;

/** Stable slug when no task is handed out, or after submit for the completion screen. */
public enum QueueOutcome {
    NO_ITEMS_YET("no-items-yet"),
    DATABASE_COMPLETE("database-complete"),
    NONE_AVAILABLE_FOR_YOU("none-available-for-you"),
    MORE_AVAILABLE("more-available");

    private final String slug;

    QueueOutcome(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }
}
