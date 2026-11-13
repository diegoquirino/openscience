package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.AccessProperties;
import io.github.diegoquirino.veredito.domain.access.AccessCodeFormat;
import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.access.Subject;
import io.github.diegoquirino.veredito.persistence.AccessCodeRepository;
import io.github.diegoquirino.veredito.persistence.AccessCodeRepository.StoredCode;
import io.github.diegoquirino.veredito.persistence.AccessSessionRepository;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** {@code vd:sign-in} and {@code vd:session}: validates the code and opens the session (research sections 3, 4 and 8). */
@Service
public class SignInService {

    private final AccessCodeRepository codes;
    private final AccessSessionRepository sessions;
    private final ParticipantRepository participants;
    private final ConfigRepository config;
    private final CodeHasher codeHasher;
    private final TokenGenerator tokens;
    private final TokenHasher tokenHasher;
    private final AccessProperties properties;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public SignInService(
            AccessCodeRepository codes,
            AccessSessionRepository sessions,
            ParticipantRepository participants,
            ConfigRepository config,
            CodeHasher codeHasher,
            TokenGenerator tokens,
            TokenHasher tokenHasher,
            AccessProperties properties,
            TransactionTemplate transaction,
            Clock clock) {
        this.codes = codes;
        this.sessions = sessions;
        this.participants = participants;
        this.config = config;
        this.codeHasher = codeHasher;
        this.tokens = tokens;
        this.tokenHasher = tokenHasher;
        this.properties = properties;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** The raw session token, which only the cookie carries, and the session it opened. */
    public record SignedIn(String token, SessionView session) {}

    private sealed interface Outcome {

        record Success(SignedIn signedIn) implements Outcome {}

        record WrongCode(int attemptsLeft) implements Outcome {}

        record DeadCode() implements Outcome {}
    }

    /**
     * A wrong code is counted in a transaction that commits, and only then refused; the answers never
     * tell whether the e-mail belongs to someone.
     */
    public SignedIn signIn(String rawEmail, String rawCode, Locale locale) {
        Optional<EmailAddress> email = EmailAddress.parse(rawEmail);
        String code = AccessCodeFormat.normalize(rawCode);
        Map<String, String> errors = new LinkedHashMap<>();
        if (email.isEmpty()) {
            errors.put("email", rawEmail == null || rawEmail.isBlank() ? "required" : "email");
        }
        if (code.isEmpty()) {
            errors.put("code", "required");
        }
        if (!errors.isEmpty()) {
            throw ProblemException.invalidFields(errors);
        }
        Outcome outcome = transaction.execute(status -> attempt(email.get(), code, locale.toLanguageTag()));
        return switch (outcome) {
            case Outcome.Success success -> success.signedIn();
            case Outcome.WrongCode wrong -> throw new ProblemException(ProblemType.INVALID_CODE, wrong.attemptsLeft())
                    .with("attemptsLeft", wrong.attemptsLeft());
            case Outcome.DeadCode _ -> throw new ProblemException(ProblemType.CODE_EXPIRED);
        };
    }

    /** The session of a valid principal, with the profile as it is now. */
    public SessionView current(VereditoPrincipal principal) {
        if (principal.isAdministrator()) {
            return SessionView.administrator(config.adminProfile(), principal.expiresAt());
        }
        return participants.findById(principal.participantId())
                .map(profile -> SessionView.participant(profile, principal.expiresAt()))
                .orElseThrow(() -> new ProblemException(ProblemType.AUTHENTICATION_REQUIRED));
    }

    private Outcome attempt(EmailAddress email, String code, String language) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Optional<StoredCode> latest = codes.latestFor(email);
        if (latest.isEmpty() || isDead(latest.get(), now)) {
            return new Outcome.DeadCode();
        }
        StoredCode stored = latest.get();
        if (!AccessCodeFormat.isWellFormed(code) || !codeHasher.matches(stored.codeHash(), email, code)) {
            codes.incrementAttempts(stored.id());
            int attemptsLeft = properties.maxAttempts() - stored.attempts() - 1;
            return attemptsLeft > 0 ? new Outcome.WrongCode(attemptsLeft) : new Outcome.DeadCode();
        }
        if (!codes.markUsed(stored.id(), now)) {
            return new Outcome.DeadCode();
        }
        PersonName name = new PersonName(stored.name());
        String token = tokens.next();
        Instant expiresAt = now.plus(properties.sessionDuration());
        SessionView session;
        if (isAdministrator(email)) {
            config.updateAdminSignIn(name, language);
            sessions.insert(tokenHasher.hash(token), Subject.ADMINISTRATOR, null, now, expiresAt);
            session = SessionView.administrator(config.adminProfile(), expiresAt);
        } else {
            ParticipantProfile participant = participants.upsertOnSignIn(email, name, language, now);
            sessions.insert(tokenHasher.hash(token), Subject.PARTICIPANT, participant.id(), now, expiresAt);
            session = SessionView.participant(participant, expiresAt);
        }
        return new Outcome.Success(new SignedIn(token, session));
    }

    /** Codes stored before V3 have no name and cannot open a session. */
    private boolean isDead(StoredCode code, Instant now) {
        return code.usedAt() != null
                || !now.isBefore(code.expiresAt())
                || code.attempts() >= properties.maxAttempts()
                || code.name() == null;
    }

    private boolean isAdministrator(EmailAddress email) {
        return EmailAddress.parse(config.adminEmail()).filter(email::equals).isPresent();
    }
}
