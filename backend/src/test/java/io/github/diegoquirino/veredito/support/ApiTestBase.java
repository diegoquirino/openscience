package io.github.diegoquirino.veredito.support;

import static io.restassured.RestAssured.given;
import static io.restassured.config.DecoderConfig.decoderConfig;
import static io.restassured.config.RedirectConfig.redirectConfig;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Running application on a random port, backed by the MySQL container; redirects are never followed. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlContainerConfig.class)
public abstract class ApiTestBase {

    public static final String HAL_FORMS = "application/prs.hal-forms+json";
    public static final String CURIE = "vd";

    @Value("${local.server.port}")
    private int port;

    protected RequestSpecification api;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        api = new RequestSpecBuilder()
                .setPort(port)
                .setAccept(HAL_FORMS)
                .setConfig(RestAssuredConfig.config()
                        .redirect(redirectConfig().followRedirects(false))
                        .decoderConfig(decoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
                .build();
    }

    /** A request with the base specification, for paths outside the API too. */
    protected RequestSpecification request() {
        return given().spec(api);
    }

    protected Response entryPoint() {
        return request().get("/api/v1");
    }

    /** Follows the relation {@code rel}; a name without a prefix is read as {@code vd:<rel>}. */
    protected Response follow(Response response, String rel) {
        String curied = rel.contains(":") || "self".equals(rel) ? rel : CURIE + ":" + rel;
        String href = response.path("_links.'" + curied + "'.href");
        if (href == null) {
            throw new AssertionError("Relation " + curied + " not found in " + response.asString());
        }
        return request().get(href);
    }
}
