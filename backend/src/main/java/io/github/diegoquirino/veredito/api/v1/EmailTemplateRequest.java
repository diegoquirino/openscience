package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EmailTemplateRequest")
public record EmailTemplateRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String subject,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String body) {}
