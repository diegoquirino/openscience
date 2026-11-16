package io.github.diegoquirino.veredito.domain.dashboard;

/** The rolling 24-hour e-mail budget; {@code lowRemaining} flags fewer than {@value #LOW_THRESHOLD} left. */
public record EmailQuota(long emailsSentLast24h, int dailyEmailLimit, long emailsRemaining, boolean lowRemaining) {

    public static final int LOW_THRESHOLD = 10;

    public static EmailQuota of(long sentLast24h, int dailyEmailLimit) {
        long remaining = Math.max(0, dailyEmailLimit - sentLast24h);
        return new EmailQuota(sentLast24h, dailyEmailLimit, remaining, remaining < LOW_THRESHOLD);
    }
}