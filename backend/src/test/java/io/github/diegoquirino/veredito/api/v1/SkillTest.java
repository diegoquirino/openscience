package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.domain.onboarding.ProfessionalSkill;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec 013: name + skill before consent; spec 014: updates stay allowed afterwards. */
class SkillTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    /** A participant with a display name but no skill yet (profile step). */
    private String atProfileStep() {
        return participantSession();
    }

    private Response putSkill(String token, String locale, String name, String skill, String other) {
        Map<String, Object> body = new HashMap<>();
        if (name != null) {
            body.put("name", name);
        }
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
    void theTemplateListsThe13CodesWhileTheProfileStepIsPending() {
        String token = atProfileStep();
        Response pending = as(token).get(EntryPointController.SKILL);
        pending.then().statusCode(200)
                .body("skill", nullValue())
                .body("skillOther", nullValue())
                .body("_templates.default.method", equalTo("PUT"))
                .body("_templates.default.properties.find { it.name == 'skill' }.options.inline",
                        equalTo(ProfessionalSkill.codes()));

        putSkill(token, "pt-BR", "Ana Souza", "developer", null).then().statusCode(200);
        as(token).get(EntryPointController.SKILL)
                .then().statusCode(200)
                .body("name", equalTo("Ana Souza"))
                .body("skill", equalTo("developer"))
                .body("_templates.default.method", equalTo("PUT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US", "es-ES"})
    void theSameCodeIsStoredInEveryLanguage(String locale) {
        String token = atProfileStep();
        long id = participantIdOf(token);

        putSkill(token, locale, "Ana Souza", "test_analyst", null)
                .then().statusCode(200)
                .header("Content-Location", equalTo("/api/v1/participants/me/onboarding"))
                .body("skill", equalTo("test_analyst"))
                .body("skillOther", nullValue());

        assertThat(jdbc.sql("SELECT professional_skill FROM participant WHERE id = ?").param(id)
                .query(String.class).single()).isEqualTo("test_analyst");
        assertThat(as(token).get(href(as(token).get("/api/v1"), "onboarding")).<String>path("step"))
                .isEqualTo("consent");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "developer", "software_engineer", "systems_analyst", "requirements_analyst", "test_analyst",
        "test_engineer", "software_architect", "tech_lead", "product_project_manager", "professor_researcher",
        "graduate_student", "undergraduate_student"
    })
    void everyCommonCodeIsAccepted(String skill) {
        putSkill(atProfileStep(), "en-US", "Ana Souza", skill, null)
                .then().statusCode(200).body("skill", equalTo(skill));
    }

    @Test
    void otherIsStoredExactlyAsTyped() {
        String token = atProfileStep();
        long id = participantIdOf(token);
        String typed = "  Technical   Writer  ";

        putSkill(token, "en-US", "Ana Souza", "other", typed)
                .then().statusCode(200)
                .body("skill", equalTo("other"))
                .body("skillOther", equalTo(typed));

        assertThat(jdbc.sql("SELECT professional_skill_other FROM participant WHERE id = ?").param(id)
                .query(String.class).single()).isEqualTo(typed);
    }

    @Test
    void otherWithoutADescriptionIsRefused() {
        String token = atProfileStep();
        putSkill(token, "pt-BR", "Ana Souza", "other", null)
                .then().statusCode(400)
                .body("type", equalTo(PROBLEM + "invalid-request"))
                .body("errors.field", contains("skillOther"))
                .body("errors.code", contains("required"));
        putSkill(token, "pt-BR", "Ana Souza", "other", "   ")
                .then().statusCode(400).body("errors.code", contains("required"));
    }

    @Test
    void otherLongerThan100CodePointsIsRefusedButExactly100IsAccepted() {
        String token = atProfileStep();
        putSkill(token, "pt-BR", "Ana Souza", "other", "x".repeat(101))
                .then().statusCode(400)
                .body("errors.field", contains("skillOther"))
                .body("errors.code", contains("size"));
        putSkill(token, "pt-BR", "Ana Souza", "other", "\uD83D\uDE00".repeat(101))
                .then().statusCode(400).body("errors.code", contains("size"));

        putSkill(token, "pt-BR", "Ana Souza", "other", "\uD83D\uDE00".repeat(100)).then().statusCode(200);
    }

    @Test
    void invalidCodesAndUnexpectedDescriptionsAreFieldErrors() {
        String token = atProfileStep();
        putSkill(token, "pt-BR", "Ana Souza", null, null)
                .then().statusCode(400)
                .body("errors.field", contains("skill")).body("errors.code", contains("required"));
        putSkill(token, "pt-BR", "Ana Souza", "wizard", null)
                .then().statusCode(400)
                .body("errors.field", contains("skill")).body("errors.code", contains("option"));
        putSkill(token, "pt-BR", "Ana Souza", "developer", "extra")
                .then().statusCode(400)
                .body("errors.field", contains("skillOther")).body("errors.code", contains("unexpected"));
        putSkill(token, "pt-BR", null, "developer", null)
                .then().statusCode(400)
                .body("errors.field", contains("name")).body("errors.code", contains("required"));
    }

    @Test
    void anIdenticalRepeatIsIdempotentAndAChangeUpdatesTheProfile() {
        String token = atProfileStep();
        putSkill(token, "pt-BR", "Ana Souza", "other", "Writer").then().statusCode(200);

        putSkill(token, "en-US", "Ana Souza", "other", "Writer")
                .then().statusCode(200).body("skillOther", equalTo("Writer"));
        putSkill(token, "pt-BR", "Ana Souza", "developer", null)
                .then().statusCode(200)
                .body("skill", equalTo("developer"));
        as(token).get(EntryPointController.SKILL)
                .then().statusCode(200)
                .body("skill", equalTo("developer"))
                .body("skillOther", nullValue());
    }
}
