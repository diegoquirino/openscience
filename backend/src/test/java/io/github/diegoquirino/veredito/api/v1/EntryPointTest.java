package io.github.diegoquirino.veredito.api.v1;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** US5 scenario 2 and the error rows of contracts/api.md. */
class EntryPointTest extends ApiTestBase {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String PROBLEM_SCHEMA = "schemas/problem-details.json";

    @Test
    void visitorGetsOnlyTheVisitorLinks() {
        entryPoint()
                .then().statusCode(200)
                .body("_links.keySet()", containsInAnyOrder("self", "curies", "vd:request-code"))
                .body("_links.'vd:request-code'.href", equalTo("/api/v1/access-codes"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "vd:me", "vd:batches", "vd:exports", "vd:dashboard", "vd:participants", "vd:email-settings",
        "vd:onboarding", "vd:consent-form", "vd:next-task"
    })
    void noAdministrativeOrParticipantLinkForAVisitor(String rel) {
        entryPoint().then().body("_links", not(hasKey(rel)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-token", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void anUnknownOrMalformedCookieIsAVisitorNever401(String cookie) {
        as(cookie).get("/api/v1")
                .then().statusCode(200)
                .body("_links.keySet()", containsInAnyOrder("self", "curies", "vd:request-code"));
    }

    @Test
    void newParticipantGetsOnboardingAndConsentFormButNoNextTask() {
        as(participantSession()).get("/api/v1")
                .then().statusCode(200)
                .body("_links.keySet()", containsInAnyOrder(
                        "self", "curies", "vd:session", "vd:me", "vd:sign-out", "vd:onboarding", "vd:consent-form"))
                // profile is pending; consent-form link still advertised when a current form exists
                .body("_links.'vd:session'.href", equalTo("/api/v1/access-sessions/current"))
                .body("_links.'vd:me'.href", equalTo("/api/v1/participants/me"))
                .body("_links.'vd:sign-out'.href", equalTo("/api/v1/access-sessions/current"))
                .body("_links.'vd:onboarding'.href", equalTo("/api/v1/participants/me/onboarding"))
                .body("_links.'vd:consent-form'.href", equalTo("/api/v1/consent-forms/current"));
    }

    @Test
    void eligibleParticipantAlsoGetsNextTask() {
        as(eligibleParticipantSession()).get("/api/v1")
                .then().statusCode(200)
                .body("_links.keySet()", containsInAnyOrder(
                        "self", "curies", "vd:session", "vd:me", "vd:sign-out", "vd:onboarding", "vd:consent-form",
                        "vd:next-task"))
                .body("_links.'vd:next-task'.href", equalTo("/api/v1/tasks/reservations"));
    }

    @Test
    void consentFormLinkDisappearsWhileNoFormIsCurrentAndNextTaskWhenNoLongerEligible() {
        String token = eligibleParticipantSession();
        jdbc.sql("UPDATE consent_form SET published_at = NULL").update();
        as(token).get("/api/v1")
                .then().statusCode(200)
                .body("_links", not(hasKey("vd:consent-form")))
                .body("_links", not(hasKey("vd:next-task")))
                .body("_links", hasKey("vd:onboarding"));
    }

    @Test
    void administratorGetsTheAdministratorProfile() {
        as(administratorSession()).get("/api/v1")
                .then().statusCode(200)
                .body("_links.keySet()", containsInAnyOrder(
                        "self", "curies", "vd:session", "vd:me", "vd:sign-out",
                        "vd:batches", "vd:request-preview", "vd:exports", "vd:dashboard", "vd:participants",
                        "vd:email-settings", "vd:study-phase"))
                .body("_links.'vd:me'.href", equalTo("/api/v1/administrator/me"))
                .body("_links.'vd:dashboard'.href", equalTo("/api/v1/dashboard"))
                .body("_links.'vd:participants'.href", equalTo("/api/v1/participants"))
                .body("_links.'vd:email-settings'.href", equalTo("/api/v1/administrator/email-settings"))
                .body("_links.'vd:batches'.href", equalTo("/api/v1/batches"))
                .body("_links.'vd:request-preview'.href", equalTo("/api/v1/batches/previews"))
                .body("_links.'vd:study-phase'.href", equalTo("/api/v1/administrator/study-phase"));
    }

    @Test
    void anExpiredSessionIsAVisitor() {
        String token = participantSession();
        clock.advance(Duration.ofHours(24));
        as(token).get("/api/v1")
                .then().statusCode(200)
                .body("_links.keySet()", containsInAnyOrder("self", "curies", "vd:request-code"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sign-in", "session", "sign-out", "me", "batches", "request-preview", "study-phase", "confirm",
            "next-task", "reserve", "save-response", "submit", "items", "review", "onboarding", "consent-form",
            "consent-text", "consent", "skill", "screening", "screening-example", "exports", "export-wide", "export-long",
            "email-settings", "email-template",
    })
    void curieDocumentsTheAccessRelations(String rel) {
        request().get("/api/v1/rels/" + rel)
                .then().statusCode(200)
                .body("rel", equalTo("vd:" + rel))
                .body("description", notNullValue())
                .body("documentation", startsWith("/swagger-ui.html#/"));
    }

    @Test
    void reportsVersionAndEnvironment() {
        entryPoint()
                .then().statusCode(200)
                .body("systemVersion", notNullValue())
                .body("environment", oneOf("production", "staging", "development"))
                .body("available", equalTo(true));
    }

    @Test
    void curieDocumentsTheRequestCodeRelation() {
        Response entryPoint = entryPoint();
        String template = entryPoint.path("_links.curies[0].href");

        request().get(template.replace("{rel}", "request-code"))
                .then().statusCode(200)
                .body("rel", equalTo("vd:request-code"))
                .body("description", notNullValue())
                .body("method", equalTo("POST"))
                .body("documentation", startsWith("/swagger-ui.html#/"))
                .body("_links.self.href", equalTo("/api/v1/rels/request-code"));
    }

    @Test
    void unknownRelationIsNotFound() {
        request().get("/api/v1/rels/unknown")
                .then().statusCode(404)
                .contentType(startsWith(PROBLEM_JSON))
                .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                .body("type", equalTo("urn:veredito:problem:not-found"));
    }

    @Test
    void requestCodeAnswersAnEmptyRequestWithTheFieldErrors() {
        String href = entryPoint().path("_links.'vd:request-code'.href");
        request().contentType("application/json").body("{}").post(href)
                .then().statusCode(400)
                .contentType(startsWith(PROBLEM_JSON))
                .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                .body("type", equalTo("urn:veredito:problem:invalid-request"))
                .body("errors.field", contains("email"))
                .body("errors.code", contains("required"));
    }

    @Test
    void aParticipantSignedInWithACodeGetsTheParticipantLinksOnly() {
        String token = signIn("Ana Souza", "entry-" + System.nanoTime() + "@example.com", "pt-BR");

        as(token).get("/api/v1")
                .then().statusCode(200)
                .body("_links.keySet()", containsInAnyOrder(
                        "self", "curies", "vd:session", "vd:me", "vd:sign-out", "vd:onboarding", "vd:consent-form"))
                // profile is pending; consent-form link still advertised when a current form exists
                .body("_links.'vd:me'.href", equalTo("/api/v1/participants/me"))
                .body("_links", not(hasKey("vd:next-task")))
                .body("_links", not(hasKey("vd:request-code")));
    }

    @Test
    void unknownPathIsNotFound() {
        request().get("/api/v1/unknown")
                .then().statusCode(404)
                .contentType(startsWith(PROBLEM_JSON))
                .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                .body("type", equalTo("urn:veredito:problem:not-found"));
    }

    @Test
    void unsupportedMethodIsNotAllowed() {
        request().delete("/api/v1")
                .then().statusCode(405)
                .header("Allow", containsString("GET"))
                .contentType(startsWith(PROBLEM_JSON))
                .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                .body("type", equalTo("urn:veredito:problem:method-not-allowed"));
    }

    @Test
    void unsatisfiableAcceptIsNotAcceptable() {
        request().accept("text/csv").get("/api/v1")
                .then().statusCode(406)
                .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                .body("type", equalTo("urn:veredito:problem:not-acceptable"));
    }
}
