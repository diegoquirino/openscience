package io.github.diegoquirino.veredito.api.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/** Edge case "Database deleted or unreachable": the entry point still answers, as unavailable. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:mysql://127.0.0.1:1/veredito",
            "spring.datasource.hikari.initialization-fail-timeout=-1",
            "spring.flyway.enabled=false"
        })
class DatabaseUnavailableTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void entryPointReportsUnavailableWithoutTheRequestCodeLink() {
        given().port(port).accept(ApiTestBase.HAL_FORMS)
                .when().get("/api/v1")
                .then().statusCode(200)
                .body("available", equalTo(false))
                .body("_links", not(hasKey("vd:request-code")))
                .body("_links.self.href", equalTo("/api/v1"));
    }
}
