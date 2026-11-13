package io.github.diegoquirino.veredito.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

/** OpenAPI description of the Problem Details written by {@link ProblemDetailsAdvice}; never instantiated. */
@Schema(name = "ProblemDetails", description = "RFC 9457 problem. title and detail follow Accept-Language; type never changes.")
public record ProblemDetailsSchema(
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "urn:veredito:problem:not-found") String type,
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "Resource not found") String title,
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "404") int status,
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "Nothing exists at this address.") String detail,
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "/api/v1/unknown") String instance,
        @Schema(description = "Only in unsupported-api-version.", example = "[\"v1\"]") List<String> supportedVersions,
        @Schema(description = "Only in internal-error; also written to the server log.", format = "uuid") String incidentId,
        @Schema(description = "Only in invalid-request.") List<FieldError> errors) {

    @Schema(name = "ProblemDetailsFieldError")
    public record FieldError(
            @Schema(requiredMode = RequiredMode.REQUIRED, example = "email") String field,
            @Schema(requiredMode = RequiredMode.REQUIRED, example = "NotBlank") String code) {}
}
