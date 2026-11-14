package io.github.diegoquirino.veredito.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Rules and headers of {@code specs/003-deployment-foundation/contracts/web-routing.md}, and the root
 * page of {@code specs/004-email-code-access/contracts/web.md}.
 */
class WebRoutingTest extends ApiTestBase {

    private static final String NO_CACHE = "no-cache";
    private static final String IMMUTABLE = "max-age=31536000, public, immutable";

    @Test
    void rootServesTheLanguageChoicePage() {
        request().accept("text/html").get("/")
                .then().statusCode(200)
                .header("Cache-Control", NO_CACHE)
                .body(containsString("/pt-BR/"));
    }

    @Test
    void rootWithASavedLanguageGoesStraightToItsPrefix() {
        request().cookie("veredito-lang", "es-ES").get("/")
                .then().statusCode(302)
                .header("Location", "/es-ES/")
                .header("Cache-Control", NO_CACHE)
                .header("Vary", containsString("Cookie"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"xx-XX", "es-es", "pt"})
    void rootIgnoresAnUnsupportedSavedLanguage(String value) {
        request().accept("text/html").cookie("veredito-lang", value).get("/")
                .then().statusCode(200)
                .header("Vary", containsString("Cookie"))
                .body(containsString("/pt-BR/"));
    }

    @Test
    void rootWithoutASavedLanguageVariesByCookie() {
        request().accept("text/html").get("/")
                .then().statusCode(200)
                .header("Vary", containsString("Cookie"));
    }

    @Test
    void prefixWithoutTrailingSlashIsRedirectedPermanently() {
        request().get("/en-US")
                .then().statusCode(301)
                .header("Location", "/en-US/");
    }

    @Test
    void spaFallbackServesTheLocaleIndex() {
        request().accept("text/html").get("/es-ES/admin")
                .then().statusCode(200)
                .header("Cache-Control", NO_CACHE)
                .body(containsString("lang=\"es-ES\""));
        request().accept("text/html").get("/pt-BR/tasks/12")
                .then().statusCode(200)
                .header("Cache-Control", NO_CACHE)
                .body(containsString("lang=\"pt-BR\""));
    }

    @Test
    void prefixRootServesTheLocaleIndex() {
        request().accept("text/html").get("/en-US/")
                .then().statusCode(200)
                .header("Cache-Control", NO_CACHE)
                .body(containsString("lang=\"en-US\""));
    }

    @Test
    void hashedBundleIsImmutable() {
        request().accept("*/*").get("/en-US/main-TEST.js")
                .then().statusCode(200)
                .header("Cache-Control", IMMUTABLE);
    }

    @Test
    void missingFileWithExtensionIsPlainNotFound() {
        request().accept("*/*").get("/en-US/missing.js")
                .then().statusCode(404)
                .body(not(containsString("lang=")));
    }

    @Test
    void prefixInAnotherCaseIsRedirectedToTheCanonicalCase() {
        request().get("/en-us/admin?x=1")
                .then().statusCode(301)
                .header("Location", "/en-US/admin?x=1");
        request().get("/PT-br/")
                .then().statusCode(301)
                .header("Location", "/pt-BR/");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/fr-FR/", "/admin", "/xyz"})
    void anythingElseIsRedirectedToTheRoot(String path) {
        request().get(path)
                .then().statusCode(302)
                .header("Location", "/");
    }

    @Test
    void otherMethodsOutsideTheApiAreNotAllowed() {
        request().post("/pt-BR/")
                .then().statusCode(405)
                .header("Allow", containsString("GET"));
    }

    @Test
    void apiIsNotAffectedByTheFilter() {
        entryPoint()
                .then().statusCode(200)
                .contentType(containsString(HAL_FORMS))
                .body("_links.self.href", equalTo("/api/v1"));
    }
}
