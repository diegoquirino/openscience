package io.github.diegoquirino.veredito.web;

import io.github.diegoquirino.veredito.api.ProblemResponses;
import io.github.diegoquirino.veredito.api.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** A valid session without permission for the resource: {@code 403 forbidden}. */
@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemResponses problems;

    public ProblemAccessDeniedHandler(ProblemResponses problems) {
        this.problems = problems;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        DeniedAccessLog.record(request, HttpServletResponse.SC_FORBIDDEN);
        problems.write(response, problems.create(ProblemType.FORBIDDEN, request));
    }
}
