package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The body of {@code PUT} on {@code vd:me}; {@code @NotNull} marks the HAL-FORMS property as required. */
@Schema(name = "PreferredLanguageRequest", description = "The new profile language.")
public record PreferredLanguageRequest(
        @NotNull
        @Schema(
                description = "pt-BR, en-US or es-ES.",
                example = "es-ES",
                allowableValues = {"pt-BR", "en-US", "es-ES"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String preferredLanguage) {}
