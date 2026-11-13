package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.api.ProblemResponses;
import io.github.diegoquirino.veredito.api.ProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts the caller of a valid {@code veredito-session} cookie in the security context; any other
 * request stays anonymous. Created by {@link SecurityConfig}, never registered as a servlet filter.
 */
public class SessionCookieFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionCookieFilter.class);

    private final CurrentSession currentSession;
    private final ProblemResponses problems;

    public SessionCookieFilter(CurrentSession currentSession, ProblemResponses problems) {
        this.currentSession = currentSession;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            currentSession.resolve(request).ifPresent(principal -> {
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
                SecurityContext context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.getContextHolderStrategy().setContext(context);
            });
        } catch (DataAccessException e) {
            log.warn("Database unavailable while reading the session: {}", e.getClass().getSimpleName());
            problems.write(response, problems.create(ProblemType.SERVICE_UNAVAILABLE, request));
            return;
        }
        chain.doFilter(request, response);
    }
}
