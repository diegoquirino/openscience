package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of {@code PUT} on {@code vd:skill}; the codes are English in every language. */
@Schema(name = "SkillRequest", description = "The professional skill of the participant (Appendix B.4).")
public record SkillRequest(
        @NotNull
        @Schema(
                description = "One of the 13 skill codes.",
                example = "test_analyst",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String skill,
        @Size(max = 100)
        @Schema(
                description = "Required only with skill = other; at most 100 characters, stored as typed.",
                example = "Technical writer",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String skillOther) {}