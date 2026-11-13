package io.github.diegoquirino.veredito.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every error under {@code /api} into the Problem Details of
 * {@code specs/003-deployment-foundation/contracts/problem-details.md}.
 */
@RestControllerAdvice
public class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

    public static final List<String> SUPPORTED_VERSIONS = List.of("v1");

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

    private final ProblemResponses problems;

    public ProblemDetailsAdvice(ProblemResponses problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<Object> handleProblem(ProblemException ex, WebRequest request) {
        return handleExceptionInternal(ex, null, ex.headers(), ex.type().status(), request);
    }

    @ExceptionHandler({CannotGetJdbcConnectionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<Object> handleDatabaseUnavailable(Exception ex, WebRequest request) {
        log.warn("Database unavailable: {}", ex.getMessage());
        return handleExceptionInternal(ex, null, new HttpHeaders(), ProblemType.SERVICE_UNAVAILABLE.status(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        return handleExceptionInternal(ex, null, new HttpHeaders(), ProblemType.INTERNAL_ERROR.status(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpServletRequest servletRequest = ((NativeWebRequest) request).getNativeRequest(HttpServletRequest.class);
        ProblemDetail problem = switch (ex) {
            case ProblemException e -> fromException(e, servletRequest);
            case InvalidApiVersionException e -> unsupportedVersion(e, servletRequest);
            case NoResourceFoundException e -> notFound(servletRequest);
            case NoHandlerFoundException e -> notFound(servletRequest);
            case HttpRequestMethodNotSupportedException e ->
                    create(ProblemType.METHOD_NOT_ALLOWED, servletRequest, e.getMethod());
            case HttpMediaTypeNotAcceptableException e -> create(ProblemType.NOT_ACCEPTABLE, servletRequest);
            case MethodArgumentNotValidException e -> invalidRequest(servletRequest, status, fieldErrors(e));
            case HttpMessageNotReadableException e ->
                    invalidRequest(servletRequest, status, List.of(Map.of("field", "body", "code", "unreadable")));
            case CannotGetJdbcConnectionException e -> create(ProblemType.SERVICE_UNAVAILABLE, servletRequest);
            case DataAccessResourceFailureException e -> create(ProblemType.SERVICE_UNAVAILABLE, servletRequest);
            default -> byStatus(ex, status, servletRequest);
        };
        return super.handleExceptionInternal(ex, problem, headers, HttpStatusCode.valueOf(problem.getStatus()), request);
    }

    /** Builds a localized problem of the given type for the current request. */
    public ProblemDetail create(ProblemType type, HttpServletRequest request, Object... detailArgs) {
        return problems.create(type, request, detailArgs);
    }

    private ProblemDetail fromException(ProblemException ex, HttpServletRequest request) {
        ProblemDetail problem = create(ex.type(), request, ex.detailArgs());
        ex.properties().forEach(problem::setProperty);
        return problem;
    }

    private ProblemDetail notFound(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return ApiVersioningConfig.unsupportedVersionOf(path)
                .map(version -> unsupportedVersion(version, request))
                .orElseGet(() -> create(ProblemType.NOT_FOUND, request));
    }

    private ProblemDetail unsupportedVersion(InvalidApiVersionException ex, HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return unsupportedVersion(ApiVersioningConfig.versionSegmentOf(path).orElse(ex.getVersion()), request);
    }

    private ProblemDetail unsupportedVersion(String version, HttpServletRequest request) {
        String requested = version == null ? "" : version;
        if (!requested.startsWith("v")) {
            requested = "v" + requested;
        }
        ProblemDetail problem = create(
                ProblemType.UNSUPPORTED_API_VERSION, request, requested, String.join(", ", SUPPORTED_VERSIONS));
        problem.setProperty("supportedVersions", SUPPORTED_VERSIONS);
        return problem;
    }

    private ProblemDetail invalidRequest(
            HttpServletRequest request, HttpStatusCode status, List<Map<String, String>> errors) {
        ProblemDetail problem = create(ProblemType.INVALID_REQUEST, request);
        problem.setStatus(status.value());
        problem.setProperty("errors", errors);
        return problem;
    }

    private ProblemDetail byStatus(Exception ex, HttpStatusCode status, HttpServletRequest request) {
        if (status.value() == ProblemType.NOT_FOUND.status().value()) {
            return create(ProblemType.NOT_FOUND, request);
        }
        if (status.value() == ProblemType.SERVICE_UNAVAILABLE.status().value()) {
            return create(ProblemType.SERVICE_UNAVAILABLE, request);
        }
        if (status.is4xxClientError()) {
            return invalidRequest(request, status, List.of());
        }
        String incidentId = UUID.randomUUID().toString();
        log.error("Incident {} on {} {}", incidentId, request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail problem = create(ProblemType.INTERNAL_ERROR, request, incidentId);
        problem.setProperty("incidentId", incidentId);
        return problem;
    }

    private static List<Map<String, String>> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "code", String.valueOf(error.getCode())))
                .toList();
    }
}
