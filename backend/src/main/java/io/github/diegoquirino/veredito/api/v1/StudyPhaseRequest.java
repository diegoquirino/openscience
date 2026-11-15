package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "StudyPhaseRequest")
public record StudyPhaseRequest(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "collection") String phase) {}
