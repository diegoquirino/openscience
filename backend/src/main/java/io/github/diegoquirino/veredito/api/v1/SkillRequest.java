package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Body of {@code PUT} on {@code vd:skill} / profile (name + skill + organization, specs 013–025). */
@Schema(name = "SkillRequest", description = "Name, professional skill and organization of the participant.")
public record SkillRequest(
        @Schema(
                description = "Display name, 2 to 120 characters after trimming.",
                example = "Ana Souza",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
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
        String skillOther,
        @Schema(
                description = "Organization code: tjpb, ufcg or other.",
                example = "ufcg",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String organization,
        @Size(max = 100)
        @Schema(
                description = "Required only with organization = other; at most 100 characters, stored as typed.",
                example = "Acme Ltd",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String organizationOther) {}
