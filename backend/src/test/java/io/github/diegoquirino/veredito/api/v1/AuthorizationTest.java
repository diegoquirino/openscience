package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Every operation the OpenAPI document tags "Administration" refuses visitors and participants;
 * every "Participation" or "Tasks" operation refuses the administrator (and visitors get 401 via
 * SecurityConfig).
 */
@ExtendWith(OutputCaptureExtension.class)
class AuthorizationTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";
    private static final Set<String> METHODS = Set.of("get", "put", "post", "delete", "patch");
    private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\{[^}]+}");

    private record Operation(String method, String path) {

        Response call(RequestSpecification request) {
            if (!method.equals("GET") && !method.equals("DELETE")) {
                request = request.contentType("application/json").body("{}");
            }
            return request.request(method, path);
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    @Test
    void administrativeOperationsRefuseVisitorsAndParticipants(CapturedOutput output) {
        List<Operation> operations = operationsTagged("Administration");
        assertThat(operations).isNotEmpty();
        String participant = participantSession();
        String pseudonym = as(participant).get(EntryPointController.SESSION).path("pseudonym");
        String administrator = administratorSession();

        for (Operation operation : operations) {
            int logStart = output.getOut().length();
            operation.call(request())
                    .then().statusCode(401)
                    .body("type", equalTo(PROBLEM + "authentication-required"));
            operation.call(as(participant))
                    .then().statusCode(403)
                    .body("type", equalTo(PROBLEM + "forbidden"));
            String log = output.getOut().substring(logStart);
            assertThat(lines(log, "Denied 401 on administrative resource: caller=visitor pseudonym=- " + operation))
                    .as("WARN for the visitor on %s", operation)
                    .isEqualTo(1);
            assertThat(lines(log, "Denied 403 on administrative resource: caller=participant pseudonym=" + pseudonym
                            + " " + operation))
                    .as("WARN for the participant on %s", operation)
                    .isEqualTo(1);
            assertThat(log).doesNotContain(participant).doesNotContain("@");

            operation.call(as(administrator))
                    .then().statusCode(not(oneOf(401, 403)));
        }
        System.out.println("AuthorizationTest: " + operations.size()
                + " administrative operation(s) refused visitors and participants: " + operations);
    }

    @Test
    void theOperationsOfSpec009AreTaggedAdministration() {
        List<String> operations = operationsTagged("Administration").stream().map(Operation::toString).toList();
        assertThat(operations).contains(
                "GET /api/v1/exports",
                "GET /api/v1/exports/wide",
                "GET /api/v1/exports/long",
                "GET /api/v1/exports/completo",
                "GET /api/v1/exports/rater1",
                "GET /api/v1/exports/rater2");
    }

    @Test
    void participantAndTaskOperationsRefuseTheAdministrator() {
        List<Operation> operations = new ArrayList<>();
        operations.addAll(operationsTagged("Participation"));
        operations.addAll(operationsTagged("Tasks"));
        assertThat(operations).isNotEmpty();
        String administrator = administratorSession();
        String participant = eligibleParticipantSession();

        for (Operation operation : operations) {
            operation.call(request())
                    .then().statusCode(401)
                    .body("type", equalTo(PROBLEM + "authentication-required"));
            operation.call(as(administrator))
                    .then().statusCode(403)
                    .body("type", equalTo(PROBLEM + "forbidden"));
            operation.call(as(participant))
                    .then().statusCode(not(oneOf(401, 403)));
        }
        System.out.println("AuthorizationTest: " + operations.size()
                + " participant/task operation(s) refused visitors and the administrator: " + operations);
    }

    @Test
    void theOperationsOfSpec006AreTaggedParticipationAndTherefore() {
        List<String> operations = operationsTagged("Participation").stream().map(Operation::toString).toList();
        assertThat(operations).contains(
                "GET /api/v1/participants/me/onboarding",
                "GET /api/v1/consent-forms/current",
                "GET /api/v1/consent-forms/1/texts/1",
                "GET /api/v1/participants/me/consent",
                "PUT /api/v1/participants/me/consent",
                "GET /api/v1/participants/me/skill",
                "PUT /api/v1/participants/me/skill",
                "GET /api/v1/participants/me/screening",
                "PUT /api/v1/participants/me/screening",
                "GET /api/v1/screening-example");
    }

    private List<Operation> operationsTagged(String tag) {
        Response docs = request().accept("application/json").get("/v3/api-docs/v1");
        docs.then().statusCode(200);
        Map<String, Map<String, Map<String, Object>>> paths = docs.path("paths");
        List<Operation> operations = new ArrayList<>();
        paths.forEach((path, byMethod) -> byMethod.forEach((method, spec) -> {
            if (METHODS.contains(method) && spec.get("tags") instanceof List<?> tags && tags.contains(tag)) {
                operations.add(new Operation(
                        method.toUpperCase(Locale.ROOT), TEMPLATE_VARIABLE.matcher(path).replaceAll("1")));
            }
        }));
        return operations;
    }

    private static long lines(String log, String text) {
        return log.lines().filter(line -> line.contains(" WARN ") && line.endsWith(text)).count();
    }
}
