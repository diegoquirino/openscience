package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.domain.onboarding.ProfessionalSkill;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec 006 US2: one skill code, "Other" needs a description of at most 100 characters (FR-003, FR-005, FR-014). */
class SkillTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    /** A participant who accepted the current consent form and is at the skill step. */
    private String atSkillStep() {
        String token = participantSession();
        acceptCurrentConsent(participantIdOf(token), "pt-BR");
        return token;
    }

    private Response putSkill(String token, String locale, String skill, String other) {
        Map<String, Object> body = new HashMap<>();
        if (skill != null) {
            body.put("skill", skill);
        }
        if (other != null) {
            body.put("skillOther", other);
        }
        return as(token).header("Accept-Language", locale).contentType("application/json").body(body)
                .put(EntryPointController.SKILL);
    }

    @Test
    void theTemplateListsThe13CodesAndOnlyWhileTheStepIsPending() {
        String token = atSkillStep();
        Response pending = as(token).get(EntryPointController.SKILL);
        pending.then().statusCode(200)
                .body("skill", nullValue())
                .body("skillOther", nullValue())
                .body("_templates.default.method", equalTo("PUT"))
                .body("_templates.default.properties.find { it.name == 'skill' }.options.inline",
                        equalTo(ProfessionalSkill.codes()));

        putSkill(token, "pt-BR", "developer", null).then().statusCode(200);
        as(token).get(EntryPointController.SKILL)
                .then().statusCode(200)
                .body("skill", equalTo("developer"))
                .body("$", not(hasKey("_templates")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US", "es-ES"})
    void theSameCodeIsStoredInEveryLanguage(String locale) {
        String token = atSkillStep();
        long id = participantIdOf(token);

        putSkill(token, locale, "test_analyst", null)
                .then().statusCode(200)
                .header("Content-Location", equalTo("/api/v1/participants/me/onboarding"))
                .body("skill", equalTo("test_analyst"))
                .body("skillOther", nullValue());

        assertThat(jdbc.sql("SELECT professional_skill FROM participant WHERE id = ?").param(id)
                .query(String.class).single()).isEqualTo("test_analyst");
        assertThat(as(token).get(href(as(token).get("/api/v1"), "onboarding")).<String>path("step"))
                .isEqualTo("screening");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "developer", "software_engineer", "systems_analyst", "requirements_analyst", "test_analyst",
        "test_engineer", "software_architect", "tech_lead", "product_project_manager", "professor_researcher",
        "graduate_student", "undergraduate_student"
    })
    void everyCommonCodeIsAccepted(String skill) {
        putSkill(atSkillStep(), "en-US", skill, null).then().statusCode(200).body("skill", equalTo(skill));
    }

    @Test
    void otherIsStoredExactlyAsTyped() {
        String token = atSkillStep();
        long id = participantIdOf(token);
        String typed = "  Technical   Writer  ";

        putSkill(token, "en-US", "other", typed)
                .then().statusCode(200)
                .body("skill", equalTo("other"))
                .body("skillOther", equalTo(typed));

        assertThat(jdbc.sql("SELECT professional_skill_other FROM participant WHERE id = ?").param(id)
                .query(String.class).single()).isEqualTo(typed);
    }

    @Test
    void otherWithoutADescriptionIsRefused() {
        String token = atSkillStep();
        putSkill(token, "pt-BR", "other", null)
                .then().statusCode(400)
                .body("type", equalTo(PROBLEM + "invalid-request"))
                .body("errors.field", contains("skillOther"))
                .body("errors.code", contains("required"));
        putSkill(token, "pt-BR", "other", "   ")
                .then().statusCode(400).body("errors.code", contains("required"));
    }

    @Test
    void otherLongerThan100CodePointsIsRefusedButExactly100IsAccepted() {
        String token = atSkillStep();
        putSkill(token, "pt-BR", "other", "x".repeat(101))
                .then().statusCode(400)
                .body("errors.field", contains("skillOther"))
                .body("errors.code", contains("size"));
        putSkill(token, "pt-BR", "other", "\uD83D\uDE00".repeat(101))
                .then().statusCode(400).body("errors.code", contains("size"));

        putSkill(token, "pt-BR", "other", "\uD83D\uDE00".repeat(100)).then().statusCode(200);
    }

    @Test
    void invalidCodesAndUnexpectedDescriptionsAreFieldErrors() {
        String token = atSkillStep();
        putSkill(token, "pt-BR", null, null)
                .then().statusCode(400)
                .body("errors.field", contains("skill")).body("errors.code", contains("required"));
        putSkill(token, "pt-BR", "wizard", null)
                .then().statusCode(400)
                .body("errors.field", contains("skill")).body("errors.code", contains("option"));
        putSkill(token, "pt-BR", "developer", "extra")
                .then().statusCode(400)
                .body("errors.field", contains("skillOther")).body("errors.code", contains("unexpected"));
    }

    @Test
    void withoutTheConsentTheSkillStepIsNotPending() {
        String token = participantSession();
        putSkill(token, "en-US", "developer", null)
                .then().statusCode(409)
                .body("type", equalTo(PROBLEM + "step-not-pending"))
                .body("step", equalTo("consent"));
        as(token).get(EntryPointController.SKILL).then().body("$", not(hasKey("_templates")));
    }

    @Test
    void theSkillIsAskedOnceAnIdenticalRepeatIsIdempotent() {
        String token = atSkillStep();
        putSkill(token, "pt-BR", "other", "Writer").then().statusCode(200);

        putSkill(token, "en-US", "other", "Writer").then().statusCode(200).body("skillOther", equalTo("Writer"));
        putSkill(token, "pt-BR", "developer", null)
                .then().statusCode(409)
                .body("type", equalTo(PROBLEM + "step-not-pending"))
                .body("step", equalTo("screening"));
        putSkill(token, "pt-BR", "other", "Another")
                .then().statusCode(409).body("type", equalTo(PROBLEM + "step-not-pending"));
        assertThat(List.of(as(token).get(EntryPointController.SKILL).<String>path("skillOther")))
                .containsExactly("Writer");
    }
}
