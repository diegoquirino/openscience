package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Logs each refusal on an administrative resource (research section 9): caller kind, pseudonym,
 * method and path; never the e-mail, the cookie or the token.
 */
final class DeniedAccessLog {

    private static final Logger log = LoggerFactory.getLogger(DeniedAccessLog.class);

    private DeniedAccessLog() {}

    static void record(HttpServletRequest request, int status) {
        if (!SecurityConfig.ADMINISTRATION.matches(request)) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication();
        String caller = "visitor";
        String pseudonym = "-";
        if (authentication != null && authentication.getPrincipal() instanceof VereditoPrincipal principal) {
            caller = principal.subject().code();
            pseudonym = principal.pseudonym() == null ? "-" : principal.pseudonym();
        }
        log.warn("Denied {} on administrative resource: caller={} pseudonym={} {} {}",
                status, caller, pseudonym, request.getMethod(), request.getRequestURI());
    }
}
