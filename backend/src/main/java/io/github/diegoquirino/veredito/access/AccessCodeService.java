package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.AccessProperties;
import io.github.diegoquirino.veredito.domain.access.AccessCodeFormat;
import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.access.SendLimits;
import io.github.diegoquirino.veredito.domain.access.SendLimits.Decision;
import io.github.diegoquirino.veredito.persistence.AccessCodeRepository;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** {@code vd:request-code}: checks the limits, stores the hashed code and sends it (research sections 4 to 6). */
@Service
public class AccessCodeService {

    private final AccessCodeRepository codes;
    private final ConfigRepository config;
    private final CodeHasher hasher;
    private final AccessCodeMailer mailer;
    private final AccessProperties properties;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AccessCodeService(
            AccessCodeRepository codes,
            ConfigRepository config,
            CodeHasher hasher,
            AccessCodeMailer mailer,
            AccessProperties properties,
            TransactionTemplate transaction,
            Clock clock) {
        this.codes = codes;
        this.config = config;
        this.hasher = hasher;
        this.mailer = mailer;
        this.properties = properties;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** What the person is told after the code left: the normalized e-mail and the timings. */
    public record RequestedCode(EmailAddress email, long validityMinutes, long resendAfterSeconds) {}

    private record PendingCode(long id, String code) {}

    public RequestedCode request(String rawName, String rawEmail, Locale locale) {
        Optional<PersonName> name = PersonName.parse(rawName);
        Optional<EmailAddress> email = EmailAddress.parse(rawEmail);
        Map<String, String> errors = new LinkedHashMap<>();
        if (name.isEmpty()) {
            errors.put("name", isBlank(rawName) ? "required" : "size");
        }
        if (email.isEmpty()) {
            errors.put("email", isBlank(rawEmail) ? "required" : "email");
        }
        if (!errors.isEmpty()) {
            throw ProblemException.invalidFields(errors);
        }
        PendingCode pending = transaction.execute(status -> store(name.get(), email.get()));
        try {
            mailer.send(name.get(), email.get(), pending.code(), locale);
        } catch (ProblemException notSent) {
            codes.delete(pending.id());
            throw notSent;
        }
        return new RequestedCode(
                email.get(), properties.codeValidity().toMinutes(), properties.resendInterval().toSeconds());
    }

    /** Runs with the {@code config} row locked, so two requests at a limit cannot both pass. */
    private PendingCode store(PersonName name, EmailAddress email) {
        config.lockForUpdate();
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        SendLimits.Rules rules = new SendLimits.Rules(
                properties.resendInterval(), properties.hourlyLimit(), config.dailyEmailLimit());
        Decision decision = SendLimits.decide(
                codes.sendTimesSince(email, now.minus(SendLimits.HOUR)),
                codes.countAllSince(now.minus(SendLimits.DAY)),
                now,
                rules);
        switch (decision) {
            case Decision.TooSoon tooSoon -> throw new ProblemException(
                            ProblemType.CODE_RESEND_TOO_SOON, tooSoon.secondsLeft())
                    .with("retryAfterSeconds", tooSoon.secondsLeft())
                    .header("Retry-After", String.valueOf(tooSoon.secondsLeft()));
            case Decision.HourlyLimit hourly -> throw hourlyLimit(hourly.retryAt(), now);
            case Decision.DailyLimit _ -> throw new ProblemException(ProblemType.DAILY_EMAIL_LIMIT);
            case Decision.Allowed _ -> {}
        }
        String code = AccessCodeFormat.generate(random);
        long id = codes.insert(email, name, hasher.hash(email, code), now, now.plus(properties.codeValidity()));
        return new PendingCode(id, code);
    }

    private static ProblemException hourlyLimit(Instant retryAt, Instant now) {
        Instant wholeSecond = retryAt.truncatedTo(ChronoUnit.SECONDS);
        Instant at = wholeSecond.equals(retryAt) ? retryAt : wholeSecond.plusSeconds(1);
        long seconds = Math.max(1, Duration.between(now, at).toSeconds());
        long minutes = (seconds + 59) / 60;
        return new ProblemException(ProblemType.CODE_HOURLY_LIMIT, minutes)
                .with("retryAt", at.toString())
                .header("Retry-After", String.valueOf(seconds));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
