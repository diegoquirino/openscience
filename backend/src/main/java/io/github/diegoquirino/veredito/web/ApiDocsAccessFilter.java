package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.api.ProblemDetailsAdvice;
import io.github.diegoquirino.veredito.api.ProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/** In production, the API documentation is for the administrator only (contracts/api.md). */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ApiDocsAccessFilter extends OncePerRequestFilter {

    private final AdministratorSessionCheck administrator;
    private final ProblemDetailsAdvice problems;
    private final JsonMapper json;

    public ApiDocsAccessFilter(AdministratorSessionCheck administrator, ProblemDetailsAdvice problems, JsonMapper json) {
        this.administrator = administrator;
        this.problems = problems;
        this.json = json;
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
        ProblemDetail problem = problems.create(ProblemType.AUTHENTICATION_REQUIRED, request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.getType().toString());
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("instance", problem.getInstance().toString());
        response.setStatus(problem.getStatus());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(body));
    }
}
