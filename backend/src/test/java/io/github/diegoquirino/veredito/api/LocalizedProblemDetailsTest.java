package io.github.diegoquirino.veredito.api;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** US6 scenario 5 and FR-016: the same Problem Details in the language of {@code Accept-Language}. */
class LocalizedProblemDetailsTest extends ApiTestBase {

    private static final String NO_HEADER = "<none>";

    @ParameterizedTest(name = "Accept-Language {0} gives {1}")
    @CsvSource({
        "pt-BR,    pt-BR, messages_pt_BR.properties",
        "en-US,    en-US, messages.properties",
        "es-ES,    es-ES, messages_es_ES.properties",
        "es,       es-ES, messages_es_ES.properties",
        "fr-FR,    pt-BR, messages_pt_BR.properties",
        "<none>,   pt-BR, messages_pt_BR.properties"
    })
    void unsupportedVersionIsLocalized(String acceptLanguage, String expectedLocale, String bundle) throws IOException {
        Properties texts = load(bundle);
        Locale locale = Locale.forLanguageTag(expectedLocale);
        String title = texts.getProperty("problem.unsupported-api-version.title");
        String detail = new MessageFormat(texts.getProperty("problem.unsupported-api-version.detail"), locale)
                .format(new Object[] {"v9", "v1"});

        RequestSpecification request = request();
        if (!NO_HEADER.equals(acceptLanguage)) {
            request.header("Accept-Language", acceptLanguage);
        }
        request.get("/api/v9/tasks")
                .then().statusCode(400)
                .contentType(startsWith("application/problem+json"))
                .header("Content-Language", expectedLocale)
                .body(matchesJsonSchemaInClasspath("schemas/problem-details.json"))
                .body("type", equalTo("urn:veredito:problem:unsupported-api-version"))
                .body("supportedVersions", contains("v1"))
                .body("title", equalTo(title))
                .body("detail", equalTo(detail));
    }

    private static Properties load(String name) throws IOException {
        try (InputStream in = LocalizedProblemDetailsTest.class.getClassLoader().getResourceAsStream(name);
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        }
    }
}
