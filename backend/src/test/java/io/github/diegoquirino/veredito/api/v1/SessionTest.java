package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.github.diegoquirino.veredito.access.TokenHasher;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** US3 and SC-004: the session lasts 24 hours on the same browser and ends on "Sign out". */
class SessionTest extends ApiTestBase {

    private static final String AUTHENTICATION_REQUIRED = "urn:veredito:problem:authentication-required";

    @Test
    void theCookieKeepsThePersonSignedInForADayWithoutAnotherEmail() {
        String email = "session-" + System.nanoTime() + "@example.com";
        String token = signIn("Ana Souza", email, "pt-BR");
        assertThat(mailpit.countTo(email)).isEqualTo(1);

        clock.advance(Duration.ofHours(24).minusMinutes(1));

        as(token).get(href(as(token).get("/api/v1"), "session"))
                .then().statusCode(200)
                .body("subject", equalTo("participant"))
                .body("name", equalTo("Ana Souza"));
        assertThat(mailpit.countTo(email)).isEqualTo(1);
    }

    @Test
    void after24HoursTheCookieOpensNothing() {
        String token = participantSession();

        clock.advance(Duration.ofHours(24));

        as(token).get(EntryPointController.SESSION)
                .then().statusCode(401)
                .body("type", equalTo(AUTHENTICATION_REQUIRED));
        as(token).get("/api/v1")
                .then().statusCode(200)
                .body("_links.'vd:request-code'.href", notNullValue())
                .body("_links.'vd:session'", nullValue())
                .body("_links.'vd:sign-out'", nullValue());
    }

    @Test
    void signingOutDeletesTheSessionAndClearsTheCookie() {
        for (String token : new String[] {participantSession(), administratorSession()}) {
            String signOut = href(as(token).get(EntryPointController.SESSION), "sign-out");

            Response response = as(token).delete(signOut);

            response.then().statusCode(204);
            assertThat(response.getHeader("Set-Cookie"))
                    .startsWith("veredito-session=;")
                    .contains("Path=/", "Max-Age=0", "HttpOnly", "Secure", "SameSite=Lax");
            assertThat(sessionRows(new TokenHasher().hash(token))).isZero();
            as(token).get(EntryPointController.SESSION)
                    .then().statusCode(401)
                    .body("type", equalTo(AUTHENTICATION_REQUIRED));
            as(token).delete(signOut).then().statusCode(401);
        }
    }

    @Test
    void signingOutWithoutASessionIsRefused() {
        request().delete(EntryPointController.SESSION)
                .then().statusCode(401)
                .body("type", equalTo(AUTHENTICATION_REQUIRED));
    }

    @Test
    void onlyTheHashOfTheTokenIsStored() {
        String token = signIn("Ana Souza", "hash-" + System.nanoTime() + "@example.com", "pt-BR");

        String hash = new TokenHasher().hash(token);
        assertThat(hash).isNotEqualTo(token).hasSize(64);
        assertThat(sessionRows(hash)).isEqualTo(1);
        assertThat(sessionRows(token)).isZero();
    }

    private int sessionRows(String tokenHash) {
        return jdbc.sql("SELECT COUNT(*) FROM access_session WHERE token_hash = ?")
                .param(tokenHash)
                .query(Integer.class)
                .single();
    }
}
