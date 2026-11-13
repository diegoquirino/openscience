package io.github.diegoquirino.veredito.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** US5 scenarios 1 and 5: the version index and the version boundary of every link. */
class ApiIndexTest extends ApiTestBase {

    @Test
    void indexListsVersionOneWithItsLink() {
        request().get("/api")
                .then().statusCode(200)
                .contentType(startsWith(HAL_FORMS))
                .body("versions.version", contains("v1"))
                .body("versions.deprecated", contains(false))
                .body("_links.self.href", equalTo("/api"))
                .body("_links.'vd:v1'.href", equalTo("/api/v1"))
                .body("_links.curies[0].href", equalTo("/api/v1/rels/{rel}"));
    }

    @Test
    void unsupportedVersionIsRefused() {
        request().get("/api/v9")
                .then().statusCode(400)
                .contentType(startsWith("application/problem+json"))
                .body("type", equalTo("urn:veredito:problem:unsupported-api-version"))
                .body("supportedVersions", contains("v1"));
    }

    @Test
    void everyLinkOfVersionOneStaysUnderVersionOne() {
        Response entryPoint = entryPoint();
        List<String> hrefs = new ArrayList<>(hrefsOf(entryPoint));
        hrefs.addAll(hrefsOf(follow(entryPoint, "self")));
        String relation = entryPoint.path("_links.curies[0].href");
        hrefs.addAll(hrefsOf(request().get(relation.replace("{rel}", "request-code"))));

        assertThat(hrefs).isNotEmpty().allSatisfy(href -> assertThat(href).startsWith("/api/v1"));
    }

    private static List<String> hrefsOf(Response response) {
        Map<String, Object> links = response.path("_links");
        List<String> hrefs = new ArrayList<>();
        links.values().forEach(value -> {
            if (value instanceof List<?> list) {
                list.forEach(item -> hrefs.add(String.valueOf(((Map<?, ?>) item).get("href"))));
            } else {
                hrefs.add(String.valueOf(((Map<?, ?>) value).get("href")));
            }
        });
        return hrefs;
    }
}
