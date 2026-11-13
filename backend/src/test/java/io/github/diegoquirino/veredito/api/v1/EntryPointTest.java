package io.github.diegoquirino.veredito.api.v1;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
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
    @ValueSource(strings = {"vd:me", "vd:batches", "vd:exports", "vd:dashboard", "vd:participants"})
    void noAdministrativeOrParticipantLinkForAVisitor(String rel) {
        entryPoint().then().body("_links", not(hasKey(rel)));
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
    void requestCodeIsNotDeliveredBeforeSpec004() {
        String href = entryPoint().path("_links.'vd:request-code'.href");
        request().contentType("application/json").body("{}").post(href)
                .then().statusCode(404)
                .contentType(startsWith(PROBLEM_JSON))
                .body("type", equalTo("urn:veredito:problem:not-found"));
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
