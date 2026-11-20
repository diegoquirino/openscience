package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of {@code PUT} on administrator {@code vd:me}. Language alone keeps the header switch;
 * name alone updates the profile menu (spec 014). At least one field is required.
 */
@Schema(name = "PreferredLanguageRequest", description = "Administrator name and/or profile language.")
public record PreferredLanguageRequest(
        @Schema(
                description = "Display name, 2 to 120 characters after trimming.",
                example = "Diego Quirino",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,
        @Schema(
                description = "pt-BR, en-US or es-ES.",
                example = "es-ES",
                allowableValues = {"pt-BR", "en-US", "es-ES"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String preferredLanguage) {}
