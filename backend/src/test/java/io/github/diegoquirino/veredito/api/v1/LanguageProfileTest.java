package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.github.diegoquirino.veredito.support.Bundles;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** FR-012 and FR-013: the profile language, set at the first sign-in, wins afterwards and changes through vd:me. */
class LanguageProfileTest extends ApiTestBase {

    private static final String PROBLEM = "urn:veredito:problem:";

    @Test
    void theFirstSignInStoresTheLanguageInUseAndTheProfileWinsAfterwards() {
        String email = "language-" + System.nanoTime() + "@example.com";
        signIn("Ana Souza", email, "es-ES");
        assertThat(storedLanguage(email)).isEqualTo("es-ES");

        clock.advance(Duration.ofSeconds(61));
        String token = signIn("Ana Souza", email, "en-US");

        assertThat(storedLanguage(email)).isEqualTo("es-ES");
        as(token).get(EntryPointController.SESSION)
                .then().statusCode(200)
                .body("preferredLanguage", equalTo("es-ES"));
    }

    @Test
    void theCodeEmailFollowsTheLanguageInUseNotTheProfile() {
        String email = "mail-language-" + System.nanoTime() + "@example.com";
        signIn("Ana Souza", email, "es-ES");

        clock.advance(Duration.ofSeconds(61));
        requestCode("Ana Souza", email, "en-US").then().statusCode(200);

        Properties english = Bundles.load(Bundles.fileOf("en-US"));
        assertThat(mailpit.countTo(email)).isEqualTo(2);
        assertThat(mailpit.latestTo(email).subject()).isEqualTo(english.getProperty("mail.access-code.subject"));
    }

    @Test
    void aParticipantChangesTheProfileLanguage() {
        String email = "switch-" + System.nanoTime() + "@example.com";
        String token = signIn("Ana Souza", email, "pt-BR");

        changeLanguage(token, EntryPointController.PARTICIPANT_ME, "en-US")
                .then().statusCode(200)
                .body("preferredLanguage", equalTo("en-US"));
        changeLanguage(token, EntryPointController.PARTICIPANT_ME, "en-US").then().statusCode(200);

        assertThat(storedLanguage(email)).isEqualTo("en-US");
        as(token).get(EntryPointController.SESSION).then().body("preferredLanguage", equalTo("en-US"));
    }

    @Test
    void aLanguageOutsideTheThreeIsRefused() {
        String token = participantSession();

        for (String value : new String[] {"fr-FR", "es-es", "en"}) {
            changeLanguage(token, EntryPointController.PARTICIPANT_ME, value)
                    .then().statusCode(400)
                    .body("type", equalTo(PROBLEM + "invalid-request"))
                    .body("errors.field", contains("preferredLanguage"))
                    .body("errors.code", contains("locale"));
        }
        as(token).contentType("application/json").body(Map.of()).put(EntryPointController.PARTICIPANT_ME)
                .then().statusCode(400)
                .body("errors.code", contains("required"));
    }

    @Test
    void theAdministratorChangesTheProfileLanguage() {
        Object saved = jdbc.sql("SELECT admin_preferred_language FROM config WHERE id = 1")
                .query()
                .singleRow()
                .get("admin_preferred_language");
        try {
            changeLanguage(administratorSession(), EntryPointController.ADMINISTRATOR_ME, "es-ES")
                    .then().statusCode(200)
                    .body("preferredLanguage", equalTo("es-ES"));

            assertThat(jdbc.sql("SELECT admin_preferred_language FROM config WHERE id = 1")
                            .query(String.class)
                            .single())
                    .isEqualTo("es-ES");
        } finally {
            jdbc.sql("UPDATE config SET admin_preferred_language = ? WHERE id = 1").param(saved).update();
        }
    }

    @Test
    void bothProfilesOfferTheLanguageTemplate() {
        assertLanguageTemplate(as(participantSession()).get(EntryPointController.PARTICIPANT_ME));
        assertLanguageTemplate(as(administratorSession()).get(EntryPointController.ADMINISTRATOR_ME));
    }

    private static void assertLanguageTemplate(Response profile) {
        profile.then().statusCode(200)
                .body("_templates.default.method", equalTo("PUT"))
                .body("_templates.default.properties.name", contains("preferredLanguage"))
                .body("_templates.default.properties[0].required", equalTo(true))
                .body("_templates.default.properties[0].options.inline", contains("pt-BR", "en-US", "es-ES"))
                .body("_templates.default.properties[0].options.minItems", equalTo(1))
                .body("_templates.default.properties[0].options.maxItems", equalTo(1));
    }

    private Response changeLanguage(String token, String href, String language) {
        return as(token).contentType("application/json")
                .body(Map.of("preferredLanguage", language))
                .put(href);
    }

    private String storedLanguage(String email) {
        return jdbc.sql("SELECT preferred_language FROM participant WHERE email = ?")
                .param(email)
                .query(String.class)
                .single();
    }
}
