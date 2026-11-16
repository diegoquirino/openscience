package io.github.diegoquirino.veredito.domain.dashboard;

/**
 * Tasks of one role by bucket.
 *
 * @param completedPercent share of completed tasks in percent, one decimal; {@code 0.0} without tasks
 */
public record TaskCounts(long free, long inProgress, long completed, double completedPercent) {

    public static final TaskCounts ZERO = of(0, 0, 0);

    public static TaskCounts of(long free, long inProgress, long completed) {
        long total = free + inProgress + completed;
        double percent = total == 0 ? 0.0 : Math.round(1000.0 * completed / total) / 10.0;
        return new TaskCounts(free, inProgress, completed, percent);
    }
}