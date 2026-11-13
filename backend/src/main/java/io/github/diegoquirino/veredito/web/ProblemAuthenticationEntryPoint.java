package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.api.ProblemResponses;
import io.github.diegoquirino.veredito.api.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** A protected resource without a valid session: {@code 401 authentication-required}. */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemResponses problems;

    public ProblemAuthenticationEntryPoint(ProblemResponses problems) {
        this.problems = problems;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        DeniedAccessLog.record(request, HttpServletResponse.SC_UNAUTHORIZED);
        problems.write(response, problems.create(ProblemType.AUTHENTICATION_REQUIRED, request));
    }
}
