package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body of {@code vd:save-response}; codes are validated by the domain so every slip is {@code invalid-request}. */
@Schema(name = "SaveResponseRequest")
public record SaveResponseRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"LOW", "HIGH"}, example = "HIGH")
                String changeImpact,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        allowableValues = {"KEEP", "UPDATE", "REMOVE", "CREATE", "MERGE", "SPLIT", "REASSIGN", "FLAG"},
                        example = "UPDATE")
                String maintenanceOperation,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 1000, example = "optional note")
                String comment) {}
