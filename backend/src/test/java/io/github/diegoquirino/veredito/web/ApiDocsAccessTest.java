package io.github.diegoquirino.veredito.web;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ActiveProfiles;

/** US5 scenario 3: the API documentation is open in staging and refused in production. */
class ApiDocsAccessTest {

    @Nested
    @ActiveProfiles("prod")
    class Production extends ApiTestBase {

        @ParameterizedTest
        @ValueSource(strings = {"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs/v1", "/v3/api-docs/index"})
        void documentationRequiresTheAdministrator(String path) {
            request().header("Accept-Language", "en-US").get(path)
                    .then().statusCode(401)
                    .contentType(startsWith("application/problem+json"))
                    .header("Content-Language", "en-US")
                    .body(matchesJsonSchemaInClasspath("schemas/problem-details.json"))
                    .body("type", equalTo("urn:veredito:problem:authentication-required"))
                    .body("title", equalTo("Authentication required"))
                    .body("instance", equalTo(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api", "/api/v1"})
        void theApiItselfStaysOpen(String path) {
            request().get(path).then().statusCode(200);
        }
    }

    @Nested
    @ActiveProfiles("staging")
    class Staging extends ApiTestBase {

        @ParameterizedTest
        @ValueSource(strings = {"/swagger-ui.html", "/v3/api-docs/v1", "/v3/api-docs/index"})
        void documentationIsOpen(String path) {
            request().redirects().follow(true).accept("*/*").get(path)
                    .then().statusCode(200);
        }
    }
}
