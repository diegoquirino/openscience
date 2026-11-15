package io.github.diegoquirino.veredito.domain.onboarding;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** The 13 skill codes of Appendix B.4, in display order; the same list as {@code ck_participant_professional_skill}. */
public enum ProfessionalSkill {
    DEVELOPER("developer"),
    SOFTWARE_ENGINEER("software_engineer"),
    SYSTEMS_ANALYST("systems_analyst"),
    REQUIREMENTS_ANALYST("requirements_analyst"),
    TEST_ANALYST("test_analyst"),
    TEST_ENGINEER("test_engineer"),
    SOFTWARE_ARCHITECT("software_architect"),
    TECH_LEAD("tech_lead"),
    PRODUCT_PROJECT_MANAGER("product_project_manager"),
    PROFESSOR_RESEARCHER("professor_researcher"),
    GRADUATE_STUDENT("graduate_student"),
    UNDERGRADUATE_STUDENT("undergraduate_student"),
    OTHER("other");

    private final String code;

    ProfessionalSkill(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<ProfessionalSkill> fromCode(String code) {
        return Arrays.stream(values()).filter(skill -> skill.code.equals(code)).findFirst();
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(ProfessionalSkill::code).toList();
    }
}
