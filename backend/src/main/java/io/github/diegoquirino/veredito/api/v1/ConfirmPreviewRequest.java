package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Body of {@code vd:confirm}: the preview to materialize into a batch. */
@Schema(name = "ConfirmPreviewRequest")
public record ConfirmPreviewRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "12") Long previewId) {}
