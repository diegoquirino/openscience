package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Body of {@code PUT} on {@code vd:screening}. */
@Schema(name = "ScreeningRequest", description = "The answer to the screening question (Appendix B.3).")
public record ScreeningRequest(
        @NotNull
        @Schema(
                description = "yes_professional, yes_academic or no.",
                allowableValues = {"yes_professional", "yes_academic", "no"},
                example = "yes_academic",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String answer) {}