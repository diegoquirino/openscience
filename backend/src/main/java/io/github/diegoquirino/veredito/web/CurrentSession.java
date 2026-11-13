package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.access.TokenHasher;
import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.persistence.AccessSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The session of the {@code veredito-session} cookie, looked up once per request. Shared by the
 * security filter and by the documentation filter, which runs before Spring Security.
 */
@Component
public class CurrentSession {

    public static final String COOKIE = "veredito-session";

    private static final String ATTRIBUTE = CurrentSession.class.getName();
    private static final Pattern TOKEN_SHAPE = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final AccessSessionRepository sessions;
    private final TokenHasher hasher;
    private final Clock clock;

    public CurrentSession(AccessSessionRepository sessions, TokenHasher hasher, Clock clock) {
        this.sessions = sessions;
        this.hasher = hasher;
        this.clock = clock;
    }

    /** The caller's valid session; empty for a missing, malformed, unknown or expired cookie. */
    @SuppressWarnings("unchecked")
    public Optional<VereditoPrincipal> resolve(HttpServletRequest request) {
        Object cached = request.getAttribute(ATTRIBUTE);
        if (cached instanceof Optional<?> known) {
            return (Optional<VereditoPrincipal>) known;
        }
        Optional<VereditoPrincipal> principal = token(request).map(hasher::hash).flatMap(hash -> sessions
                .findValid(hash, clock.instant())
                .map(session -> new VereditoPrincipal(
                        session.subject(), session.participantId(), session.pseudonym(), session.expiresAt(), hash)));
        request.setAttribute(ATTRIBUTE, principal);
        return principal;
    }

    private static Optional<String> token(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE.equals(cookie.getName()) && TOKEN_SHAPE.matcher(cookie.getValue()).matches()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
