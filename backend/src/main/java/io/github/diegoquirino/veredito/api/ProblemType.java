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
    FORBIDDEN("forbidden", HttpStatus.FORBIDDEN),
    CSV_NOT_UTF8("csv-not-utf8", HttpStatus.BAD_REQUEST),
    CSV_INVALID("csv-invalid", HttpStatus.BAD_REQUEST),
    CSV_MISSING_COLUMN("csv-missing-column", HttpStatus.BAD_REQUEST),
    MINITEST_ONLY("minitest-only", HttpStatus.UNPROCESSABLE_CONTENT),
    PREVIEW_NOT_FOUND("preview-not-found", HttpStatus.NOT_FOUND),
    PHASE_SWITCH_NOT_ALLOWED("phase-switch-not-allowed", HttpStatus.CONFLICT),
    NOT_ELIGIBLE("not-eligible", HttpStatus.FORBIDDEN),
    RESERVATION_EXPIRED("reservation-expired", HttpStatus.CONFLICT),
    BLOCK_INCOMPLETE("block-incomplete", HttpStatus.CONFLICT),
    TASK_ALREADY_SUBMITTED("task-already-submitted", HttpStatus.CONFLICT),
    TASK_CONFLICT("task-conflict", HttpStatus.CONFLICT),
    STUDY_NOT_OPEN("study-not-open", HttpStatus.CONFLICT),
    CONSENT_FORM_OUTDATED("consent-form-outdated", HttpStatus.CONFLICT),
    STEP_NOT_PENDING("step-not-pending", HttpStatus.CONFLICT),
    SYSTEM_NOT_FOUND("system-not-found", HttpStatus.NOT_FOUND),
    ALREADY_ANONYMIZED("already-anonymized", HttpStatus.CONFLICT);

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
