package io.github.diegoquirino.veredito.api;

import io.github.diegoquirino.veredito.api.LocaleConfig.SupportedLocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the localized Problem Details of every type, for {@link ProblemDetailsAdvice} and for the
 * filters that answer before Spring MVC (documentation access, security).
 */
@Component
public class ProblemResponses {

    private final MessageSource messages;
    private final SupportedLocaleResolver localeResolver;
    private final JsonMapper json;

    public ProblemResponses(MessageSource messages, SupportedLocaleResolver localeResolver, JsonMapper json) {
        this.messages = messages;
        this.localeResolver = localeResolver;
        this.json = json;
    }

    /** A localized problem of the given type for the current request. */
    public ProblemDetail create(ProblemType type, HttpServletRequest request, Object... detailArgs) {
        var locale = localeResolver.resolveLocale(request);
        ProblemDetail problem = ProblemDetail.forStatus(type.status());
        problem.setType(type.type());
        problem.setTitle(messages.getMessage(type.titleKey(), null, locale));
        problem.setDetail(messages.getMessage(type.detailKey(), detailArgs, locale));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    /** Writes the problem as {@code application/problem+json}, outside Spring MVC. */
    public void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.getType().toString());
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("instance", problem.getInstance().toString());
        if (problem.getProperties() != null) {
            body.putAll(problem.getProperties());
        }
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(body));
    }
}
