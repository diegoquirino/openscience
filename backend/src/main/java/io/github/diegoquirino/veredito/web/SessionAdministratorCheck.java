package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Reads the session cookie itself, because {@link ApiDocsAccessFilter} runs before the Spring
 * Security filter chain fills the security context.
 */
@Component
public class SessionAdministratorCheck implements AdministratorSessionCheck {

    private final CurrentSession currentSession;

    public SessionAdministratorCheck(CurrentSession currentSession) {
        this.currentSession = currentSession;
    }

    @Override
    public boolean isAdministrator(HttpServletRequest request) {
        try {
            return currentSession.resolve(request).map(VereditoPrincipal::isAdministrator).orElse(false);
        } catch (DataAccessException unavailable) {
            return false;
        }
    }
}
