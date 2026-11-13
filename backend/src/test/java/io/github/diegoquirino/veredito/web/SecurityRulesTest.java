package io.github.diegoquirino.veredito.web;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** The path rules of contracts/access.md ("Authorization rules"), before the resources exist. */
class SecurityRulesTest extends ApiTestBase {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String PROBLEM_SCHEMA = "schemas/problem-details.json";
    private static final String ADMINISTRATOR_ME = "/api/v1/administrator/me";
    private static final String PARTICIPANT_ME = "/api/v1/participants/me";
    private static final String SESSION = "/api/v1/access-sessions/current";

    @Test
    void protectedResourcesAnswer401ToAVisitor() {
        for (String path : new String[] {ADMINISTRATOR_ME, PARTICIPANT_ME, SESSION}) {
            request().header("Accept-Language", "es-ES").get(path)
                    .then().statusCode(401)
                    .contentType(startsWith(PROBLEM_JSON))
                    .header("Content-Language", "es-ES")
                    .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                    .body("type", equalTo("urn:veredito:problem:authentication-required"))
                    .body("title", equalTo("Autenticaci\u00f3n requerida"))
                    .body("instance", equalTo(path));
        }
    }

    @Test
    void aParticipantIsForbiddenFromAdministrativeResources() {
        as(participantSession()).header("Accept-Language", "en-US").get(ADMINISTRATOR_ME)
                .then().statusCode(403)
                .contentType(startsWith(PROBLEM_JSON))
                .body(matchesJsonSchemaInClasspath(PROBLEM_SCHEMA))
                .body("type", equalTo("urn:veredito:problem:forbidden"))
                .body("title", equalTo("Access denied"));
    }

    @Test
    void theAdministratorIsForbiddenFromParticipantResources() {
        as(administratorSession()).get(PARTICIPANT_ME)
                .then().statusCode(403)
                .body("type", equalTo("urn:veredito:problem:forbidden"))
                .body("title", equalTo("Acesso negado"));
    }

    @Test
    void anExpiredSessionIsRefusedLikeNoSession() {
        String token = administratorSession();
        clock.advance(Duration.ofHours(24).plusSeconds(1));
        as(token).get(ADMINISTRATOR_ME)
                .then().statusCode(401)
                .body("type", equalTo("urn:veredito:problem:authentication-required"));
    }

    @Test
    void publicResourcesStayOpen() {
        request().get("/api/v1").then().statusCode(200);
        request().get("/api/v1/rels/request-code").then().statusCode(200);
        request().get("/api").then().statusCode(200);
    }
}
