package io.github.diegoquirino.veredito.domain.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Skill and "Other" validation of Appendix B.4 (spec 006 FR-003, FR-014). */
class SkillRulesTest {

    @Test
    void thirteenCodesInTheOrderOfAppendixB4() {
        assertThat(ProfessionalSkill.codes()).containsExactly(
                "developer", "software_engineer", "systems_analyst", "requirements_analyst", "test_analyst",
                "test_engineer", "software_architect", "tech_lead", "product_project_manager",
                "professor_researcher", "graduate_student", "undergraduate_student", "other");
    }

    @ParameterizedTest
    @EnumSource(value = ProfessionalSkill.class, names = "OTHER", mode = EnumSource.Mode.EXCLUDE)
    void everyCodeButOtherIsValidWithoutDescription(ProfessionalSkill skill) {
        SkillChoice.Validation result = SkillChoice.validate(skill.code(), null);
        assertThat(result.valid()).isTrue();
        assertThat(result.choice()).isEqualTo(new SkillChoice(skill, null));
    }

    @Test
    void missingOrUnknownSkill() {
        assertThat(SkillChoice.validate(null, null).errors()).isEqualTo(Map.of("skill", "required"));
        assertThat(SkillChoice.validate("", null).errors()).isEqualTo(Map.of("skill", "required"));
        assertThat(SkillChoice.validate("wizard", null).errors()).isEqualTo(Map.of("skill", "option"));
        assertThat(SkillChoice.validate("Developer", null).errors()).isEqualTo(Map.of("skill", "option"));
    }

    @Test
    void otherNeedsANonBlankDescription() {
        assertThat(SkillChoice.validate("other", null).errors()).isEqualTo(Map.of("skillOther", "required"));
        assertThat(SkillChoice.validate("other", "").errors()).isEqualTo(Map.of("skillOther", "required"));
        assertThat(SkillChoice.validate("other", "   \t").errors()).isEqualTo(Map.of("skillOther", "required"));
    }

    @Test
    void otherIsStoredExactlyAsTyped() {
        SkillChoice.Validation result = SkillChoice.validate("other", "  Redator Técnico  ");
        assertThat(result.valid()).isTrue();
        assertThat(result.choice().other()).isEqualTo("  Redator Técnico  ");
    }

    @Test
    void limitIsOneHundredCodePointsNotUtf16Units() {
        assertThat(SkillChoice.validate("other", "a".repeat(100)).valid()).isTrue();
        assertThat(SkillChoice.validate("other", "a".repeat(101)).errors())
                .isEqualTo(Map.of("skillOther", "size"));

        String emojis100 = "\uD83D\uDE00".repeat(100);
        assertThat(emojis100.length()).isEqualTo(200);
        assertThat(SkillChoice.validate("other", emojis100).valid()).isTrue();
        assertThat(SkillChoice.validate("other", emojis100 + "x").errors())
                .isEqualTo(Map.of("skillOther", "size"));
    }

    @Test
    void descriptionWithAnotherSkillIsUnexpected() {
        assertThat(SkillChoice.validate("developer", "x").errors()).isEqualTo(Map.of("skillOther", "unexpected"));
    }

    @Test
    void errorsKeepFieldOrder() {
        assertThat(List.copyOf(SkillChoice.validate("wizard", "x").errors().keySet()))
                .containsExactly("skill", "skillOther");
    }

    @Test
    void screeningCodes() {
        assertThat(ScreeningAnswer.codes()).containsExactly("yes_professional", "yes_academic", "no");
        assertThat(ScreeningAnswer.fromCode("yes_academic")).contains(ScreeningAnswer.YES_ACADEMIC);
        assertThat(ScreeningAnswer.fromCode("maybe")).isEmpty();
        assertThat(ScreeningAnswer.YES_PROFESSIONAL.eligible()).isTrue();
        assertThat(ScreeningAnswer.YES_ACADEMIC.eligible()).isTrue();
        assertThat(ScreeningAnswer.NO.eligible()).isFalse();
    }
}
