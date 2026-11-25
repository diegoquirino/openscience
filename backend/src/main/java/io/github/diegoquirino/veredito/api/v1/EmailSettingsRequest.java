package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "EmailSettingsRequest", description = "Invite send settings; date-times are ISO-8601 instants (UTC).")
public record EmailSettingsRequest(
        @Schema(example = "CAAE 12345678.9.0000.0000") String caae,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://diegoquirino.cloud")
                String platformUrl,
        @Schema(example = "2026-12-21T03:00:00Z") Instant collectionStartAt,
        @Schema(example = "2027-01-09T02:59:00Z") Instant collectionEndAt,
        @Schema(example = "2026-12-28T15:00:00Z") Instant reminderAt) {}
