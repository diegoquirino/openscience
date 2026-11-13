package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.api.ProblemResponses;
import io.github.diegoquirino.veredito.api.ProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** In production, the API documentation is for the administrator only (contracts/api.md). */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ApiDocsAccessFilter extends OncePerRequestFilter {

    private final AdministratorSessionCheck administrator;
    private final ProblemResponses problems;

    public ApiDocsAccessFilter(AdministratorSessionCheck administrator, ProblemResponses problems) {
        this.administrator = administrator;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LocaleRoutingFilter.isDocumentation(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (administrator.isAdministrator(request)) {
            chain.doFilter(request, response);
            return;
        }
        problems.write(response, problems.create(ProblemType.AUTHENTICATION_REQUIRED, request));
    }
}
