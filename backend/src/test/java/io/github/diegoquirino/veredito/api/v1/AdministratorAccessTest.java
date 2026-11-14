package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** US2: the administrator signs in with a code like anyone else and gets the administrator session. */
class AdministratorAccessTest extends ApiTestBase {

    @Test
    void theAdministratorSignsInWithoutBecomingAParticipant() {
        Map<String, Object> saved = adminColumns();
        try {
            jdbc.sql("UPDATE config SET admin_name = NULL, admin_preferred_language = NULL WHERE id = 1").update();
            long participantsBefore = participantCount();

            String token = signIn("Diego Quirino", " DiegoQuirino@Gmail.com ", "es-ES");

            Response session = as(token).get(EntryPointController.SESSION);
            session.then().statusCode(200)
                    .body("subject", equalTo("administrator"))
                    .body("name", equalTo("Diego Quirino"))
                    .body("$", not(hasKey("pseudonym")))
                    .body("preferredLanguage", equalTo("es-ES"))
                    .body("_links.'vd:me'.href", equalTo(EntryPointController.ADMINISTRATOR_ME));
            assertThat(participantCount()).isEqualTo(participantsBefore);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM participant WHERE email = ?")
                            .param(ADMINISTRATOR_EMAIL)
                            .query(Long.class)
                            .single())
                    .isZero();

            clock.advance(Duration.ofSeconds(61));
            String second = signIn("Diego Q. Lima", ADMINISTRATOR_EMAIL, "en-US");

            assertThat(adminColumns())
                    .containsEntry("admin_name", "Diego Q. Lima")
                    .containsEntry("admin_preferred_language", "es-ES");
            assertThat(participantCount()).isEqualTo(participantsBefore);

            Response entryPoint = as(second).get("/api/v1");
            entryPoint.then().body("_links.'vd:me'.href", equalTo(EntryPointController.ADMINISTRATOR_ME));
            as(second).get(href(entryPoint, "me"))
                    .then().statusCode(200)
                    .body("name", equalTo("Diego Q. Lima"))
                    .body("email", equalTo(ADMINISTRATOR_EMAIL))
                    .body("preferredLanguage", equalTo("es-ES"))
                    .body("_links.self.href", equalTo(EntryPointController.ADMINISTRATOR_ME));
        } finally {
            jdbc.sql("UPDATE config SET admin_name = ?, admin_preferred_language = ? WHERE id = 1")
                    .params(saved.get("admin_name"), saved.get("admin_preferred_language"))
                    .update();
        }
    }

    @Test
    void aParticipantReadsTheOwnProfile() {
        String email = "profile-" + System.nanoTime() + "@example.com";
        String token = signIn("Ana Souza", email, "pt-BR");

        Response entryPoint = as(token).get("/api/v1");
        entryPoint.then().body("_links.'vd:me'.href", equalTo(EntryPointController.PARTICIPANT_ME));
        Response profile = as(token).get(href(entryPoint, "me"));
        profile.then().statusCode(200)
                .body("name", equalTo("Ana Souza"))
                .body("preferredLanguage", equalTo("pt-BR"))
                .body("$", not(hasKey("email")))
                .body("_links.self.href", equalTo(EntryPointController.PARTICIPANT_ME));
        assertThat(profile.<String>path("pseudonym")).isEqualTo(jdbc.sql(
                        "SELECT pseudonym FROM participant WHERE email = ?")
                .param(email)
                .query(String.class)
                .single());
        assertThat(profile.<String>path("registeredAt")).isNotBlank();
    }

    private Map<String, Object> adminColumns() {
        return jdbc.sql("SELECT admin_name, admin_preferred_language FROM config WHERE id = 1")
                .query()
                .singleRow();
    }

    private long participantCount() {
        return jdbc.sql("SELECT COUNT(*) FROM participant").query(Long.class).single();
    }
}
