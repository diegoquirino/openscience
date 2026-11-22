package io.github.diegoquirino.veredito.domain.onboarding;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A validated organization answer (spec 025). {@code other} is kept as typed and is present only with
 * {@link OrganizationAffiliation#OTHER}.
 */
public record OrganizationChoice(OrganizationAffiliation organization, String other) {

    public static final int OTHER_MAX_CODE_POINTS = 100;

    public record Validation(OrganizationChoice choice, Map<String, String> errors) {

        public boolean valid() {
            return errors.isEmpty();
        }
    }

    public static Validation validate(String organization, String other) {
        Map<String, String> errors = new LinkedHashMap<>();
        OrganizationAffiliation parsed = null;
        if (organization == null || organization.isEmpty()) {
            errors.put("organization", "required");
        } else {
            parsed = OrganizationAffiliation.fromCode(organization).orElse(null);
            if (parsed == null) {
                errors.put("organization", "option");
            }
        }
        if (parsed == OrganizationAffiliation.OTHER) {
            if (other == null || other.isBlank()) {
                errors.put("organizationOther", "required");
            } else if (other.codePointCount(0, other.length()) > OTHER_MAX_CODE_POINTS) {
                errors.put("organizationOther", "size");
            }
        } else if (other != null) {
            errors.put("organizationOther", "unexpected");
        }
        if (!errors.isEmpty()) {
            return new Validation(null, errors);
        }
        return new Validation(
                new OrganizationChoice(parsed, parsed == OrganizationAffiliation.OTHER ? other : null), Map.of());
    }
}
