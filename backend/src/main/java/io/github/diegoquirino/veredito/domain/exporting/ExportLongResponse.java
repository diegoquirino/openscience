package io.github.diegoquirino.veredito.domain.exporting;

import java.time.Instant;

/** One final response with the participant and the block timestamps, for the long table. */
public record ExportLongResponse(
        String system,
        String editKey,
        int blockNumber,
        int role,
        String pseudonym,
        String professionalSkill,
        String participantPreferredLanguage,
        String consentLanguage,
        String changeImpact,
        String maintenanceOperation,
        String comment,
        String responseLanguage,
        Instant reservedAt,
        Instant submittedAt) {}