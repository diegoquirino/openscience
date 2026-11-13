package io.github.diegoquirino.veredito.domain.access;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Whether a new code may be sent to an e-mail (research section 5): the resend interval, the hourly
 * limit of the e-mail and the daily limit of all e-mails, checked in that order.
 */
public final class SendLimits {

    public static final Duration HOUR = Duration.ofHours(1);
    public static final Duration DAY = Duration.ofHours(24);

    private SendLimits() {}

    public record Rules(Duration resendInterval, int hourlyLimit, int dailyLimit) {

        public Rules {
            if (resendInterval.isNegative() || resendInterval.compareTo(HOUR) > 0 || hourlyLimit < 1
                    || dailyLimit < 0) {
                throw new IllegalArgumentException("Resend interval up to 1 hour, hourly limit at least 1");
            }
        }
    }

    public sealed interface Decision {

        record Allowed() implements Decision {}

        record TooSoon(long secondsLeft) implements Decision {}

        record HourlyLimit(Instant retryAt) implements Decision {}

        record DailyLimit() implements Decision {}
    }

    /**
     * @param sendsOfTheEmailLastHour send times of this e-mail in the hour before {@code now}
     * @param sendsLastDay sends to every e-mail in the 24 hours before {@code now}
     */
    public static Decision decide(List<Instant> sendsOfTheEmailLastHour, long sendsLastDay, Instant now, Rules rules) {
        List<Instant> sends = sendsOfTheEmailLastHour.stream().sorted().toList();
        if (!sends.isEmpty()) {
            Instant allowedFrom = sends.getLast().plus(rules.resendInterval());
            if (now.isBefore(allowedFrom)) {
                long millisLeft = Duration.between(now, allowedFrom).toMillis();
                return new Decision.TooSoon(Math.max(1, (millisLeft + 999) / 1000));
            }
        }
        if (sends.size() >= rules.hourlyLimit()) {
            Instant leavesTheWindow = sends.get(sends.size() - rules.hourlyLimit());
            return new Decision.HourlyLimit(leavesTheWindow.plus(HOUR));
        }
        if (sendsLastDay >= rules.dailyLimit()) {
            return new Decision.DailyLimit();
        }
        return new Decision.Allowed();
    }
}
