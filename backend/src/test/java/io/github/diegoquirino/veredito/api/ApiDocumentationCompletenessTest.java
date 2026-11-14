package io.github.diegoquirino.veredito.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * US5 scenario 4, FR-013 and SC-007: every operation of each OpenAPI group has a summary, a description,
 * an example in each success response with a body (a 204 documents none) and a Problem Details error response. Also writes the documents
 * to {@code target/openapi/} for CI.
 */
class ApiDocumentationCompletenessTest extends ApiTestBase {

    private static final List<String> GROUPS = List.of("v1", "index");
    private static final Set<String> METHODS = Set.of("get", "put", "post", "delete", "patch", "head", "options");
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetails";

    @Test
    void everyOperationIsDocumented() throws IOException {
        List<String> problems = new ArrayList<>();
        for (String group : GROUPS) {
            Response response = request().accept("application/json").get("/v3/api-docs/" + group);
            response.then().statusCode(200);
            Path output = Path.of("target", "openapi", "openapi-" + group + ".json");
            Files.createDirectories(output.getParent());
            Files.writeString(output, response.asString(), StandardCharsets.UTF_8);

            Map<String, Map<String, Object>> paths = response.path("paths");
            if (paths == null || paths.isEmpty()) {
                problems.add("group " + group + ": no operation");
                continue;
            }
            String prefix = "v1".equals(group) ? "/api/v1" : "/api";
            paths.keySet().stream()
                    .filter(path -> !path.equals(prefix) && !path.startsWith(prefix + "/"))
                    .forEach(path -> problems.add("group " + group + ": path " + path + " is outside " + prefix));
            paths.forEach((path, operations) -> operations.forEach((method, operation) -> {
                if (METHODS.contains(method)) {
                    check(method.toUpperCase(Locale.ROOT) + " " + path, asMap(operation), problems);
                }
            }));
        }
        assertTrue(problems.isEmpty(), () -> String.join(System.lineSeparator(), problems));
    }

    private static void check(String operation, Map<String, Object> spec, List<String> problems) {
        if (isBlank(spec.get("summary"))) {
            problems.add(operation + ": missing summary");
        }
        if (isBlank(spec.get("description"))) {
            problems.add(operation + ": missing description");
        }
        Map<String, Object> responses = asMap(spec.get("responses"));
        boolean documentedError = false;
        for (Map.Entry<String, Object> entry : responses.entrySet()) {
            String status = entry.getKey();
            Map<String, Object> content = asMap(asMap(entry.getValue()).get("content"));
            if (status.equals("204")) {
                if (!content.isEmpty()) {
                    problems.add(operation + ": response 204 documents a body");
                }
            } else if (status.startsWith("2")) {
                if (content.isEmpty()) {
                    problems.add(operation + ": response " + status + " has no content");
                }
                content.forEach((mediaType, body) -> {
                    Map<String, Object> media = asMap(body);
                    if (media.get("example") == null && asMap(media.get("examples")).isEmpty()) {
                        problems.add(operation + ": response " + status + " " + mediaType + " has no example");
                    }
                });
            }
            if (status.startsWith("4")) {
                Map<String, Object> problem = asMap(content.get(PROBLEM_JSON));
                if (PROBLEM_SCHEMA_REF.equals(asMap(problem.get("schema")).get("$ref"))) {
                    documentedError = true;
                } else {
                    problems.add(operation + ": response " + status + " is not " + PROBLEM_JSON + " with "
                            + PROBLEM_SCHEMA_REF);
                }
            }
        }
        if (!documentedError) {
            problems.add(operation + ": no 4xx response with Problem Details");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }
}
