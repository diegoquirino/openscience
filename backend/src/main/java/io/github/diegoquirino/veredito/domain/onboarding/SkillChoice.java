package io.github.diegoquirino.veredito.domain.onboarding;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A validated skill answer. {@code other} is kept exactly as typed (no trimming, no case change), and is
 * present only with {@link ProfessionalSkill#OTHER}.
 */
public record SkillChoice(ProfessionalSkill skill, String other) {

    /** MySQL {@code VARCHAR(100)} counts characters, so the limit is counted in code points. */
    public static final int OTHER_MAX_CODE_POINTS = 100;

    /** The outcome of {@link #validate}: a choice, or one error code per field (insertion order). */
    public record Validation(SkillChoice choice, Map<String, String> errors) {

        public boolean valid() {
            return errors.isEmpty();
        }
    }

    public static Validation validate(String skill, String other) {
        Map<String, String> errors = new LinkedHashMap<>();
        ProfessionalSkill parsed = null;
        if (skill == null || skill.isEmpty()) {
            errors.put("skill", "required");
        } else {
            parsed = ProfessionalSkill.fromCode(skill).orElse(null);
            if (parsed == null) {
                errors.put("skill", "option");
            }
        }
        if (parsed == ProfessionalSkill.OTHER) {
            if (other == null || other.isBlank()) {
                errors.put("skillOther", "required");
            } else if (other.codePointCount(0, other.length()) > OTHER_MAX_CODE_POINTS) {
                errors.put("skillOther", "size");
            }
        } else if (other != null) {
            errors.put("skillOther", "unexpected");
        }
        if (!errors.isEmpty()) {
            return new Validation(null, errors);
        }
        return new Validation(new SkillChoice(parsed, parsed == ProfessionalSkill.OTHER ? other : null), Map.of());
    }
}
