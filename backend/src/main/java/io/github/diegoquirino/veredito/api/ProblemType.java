package io.github.diegoquirino.veredito.api;

import java.net.URI;
import org.springframework.http.HttpStatus;

/** Catalog of Problem Details types; the slug and the URI never change with the language. */
public enum ProblemType {
    UNSUPPORTED_API_VERSION("unsupported-api-version", HttpStatus.BAD_REQUEST),
    AUTHENTICATION_REQUIRED("authentication-required", HttpStatus.UNAUTHORIZED),
    NOT_FOUND("not-found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("method-not-allowed", HttpStatus.METHOD_NOT_ALLOWED),
    NOT_ACCEPTABLE("not-acceptable", HttpStatus.NOT_ACCEPTABLE),
    INVALID_REQUEST("invalid-request", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("internal-error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("service-unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    CODE_RESEND_TOO_SOON("code-resend-too-soon", HttpStatus.TOO_MANY_REQUESTS),
    CODE_HOURLY_LIMIT("code-hourly-limit", HttpStatus.TOO_MANY_REQUESTS),
    DAILY_EMAIL_LIMIT("daily-email-limit", HttpStatus.TOO_MANY_REQUESTS),
    CODE_NOT_SENT("code-not-sent", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_CODE("invalid-code", HttpStatus.UNPROCESSABLE_CONTENT),
    CODE_EXPIRED("code-expired", HttpStatus.UNPROCESSABLE_CONTENT),
    FORBIDDEN("forbidden", HttpStatus.FORBIDDEN);

    public static final String URN_PREFIX = "urn:veredito:problem:";

    private final String slug;
    private final HttpStatus status;

    ProblemType(String slug, HttpStatus status) {
        this.slug = slug;
        this.status = status;
    }

    public String slug() {
        return slug;
    }

    public HttpStatus status() {
        return status;
    }

    public URI type() {
        return URI.create(URN_PREFIX + slug);
    }

    public String titleKey() {
        return "problem." + slug + ".title";
    }

    public String detailKey() {
        return "problem." + slug + ".detail";
    }
}
