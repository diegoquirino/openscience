package io.github.diegoquirino.veredito.domain.exporting;

import java.time.Instant;

/**
 * What one completed role contributes to an item row: the participant and the final response. A role that
 * is not completed (or has no final response for the item) is {@code null} in the row builders.
 *
 * @param anonymized whether the participant was anonymized; name and e-mail then export as the marker
 * @param consentAccepted whether the participant accepted the consent form (version, language and time follow)
 */
public record ExportRole(
        String pseudonym,
        String name,
        String email,
        boolean anonymized,
        boolean consentAccepted,
        String consentVersion,
        String consentLanguage,
        Instant consentDecidedAt,
        String professionalSkill,
        String organization,
        String organizationOther,
        String changeImpact,
        String maintenanceOperation,
        String comment,
        String responseLanguage) {}
