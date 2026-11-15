package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * US6 of spec 007: each draft records the language of the request that saved it, taken from locale
 * resolution (Accept-Language), never from the body; reading or switching language does not renew.
 */
class TaskLanguageTest extends ApiTestBase {

    @BeforeEach
    void minitestImported() {
        clearStudyDatabase();
        importMinitestBatch();
    }

    @Test
    void draftsSavedInThreeLocalesRecordEachLanguage() {
        String a = eligibleParticipantSession();
        Response task = reserve(a);
        long first = itemId(a, task, 1);
        long second = itemId(a, task, 2);
        long third = itemId(a, task, 3);

        save(a, task, first, "pt-BR").then().statusCode(200);
        save(a, task, second, "en-US").then().statusCode(200);
        save(a, task, third, "es-ES").then().statusCode(200);

        as(a).get(itemUrl(task, 1)).then().body("draft.language", equalTo("pt-BR"));
        as(a).get(itemUrl(task, 2)).then().body("draft.language", equalTo("en-US"));
        as(a).get(itemUrl(task, 3)).then().body("draft.language", equalTo("es-ES"));
        assertThat(languages(task)).containsExactlyInAnyOrder("pt-BR", "en-US", "es-ES");

        // Saving the same item again in another language replaces only that draft's language.
        save(a, task, first, "es-ES").then().statusCode(200);
        as(a).get(itemUrl(task, 1)).then().body("draft.language", equalTo("es-ES"));
        as(a).get(itemUrl(task, 2)).then().body("draft.language", equalTo("en-US"));
    }

    @Test
    void languageOnlyFollowsTheHeaderNotTheBody() {
        String a = eligibleParticipantSession();
        Response task = reserve(a);
        long itemId = itemId(a, task, 1);

        Map<String, Object> body = body();
        body.put("language", "es-ES");
        as(a).header("Accept-Language", "en-US").contentType("application/json").body(body)
                .put(responseUrl(task, itemId))
                .then().statusCode(200);

        as(a).get(itemUrl(task, 1)).then().body("draft.language", equalTo("en-US"));
    }

    @Test
    void unsupportedLocaleFallsBackAndRegionalVariantsMapToSupportedTags() {
        String a = eligibleParticipantSession();
        Response task = reserve(a);
        long first = itemId(a, task, 1);
        long second = itemId(a, task, 2);

        save(a, task, first, "fr-FR").then().statusCode(200);
        save(a, task, second, "es-MX").then().statusCode(200);

        as(a).get(itemUrl(task, 1)).then().body("draft.language", equalTo("pt-BR"));
        as(a).get(itemUrl(task, 2)).then().body("draft.language", equalTo("es-ES"));
    }

    @Test
    void switchingLanguageAloneDoesNotRenewTheReservation() {
        String a = eligibleParticipantSession();
        Response task = reserve(a);
        String expiresAt = task.path("reservationExpiresAt");
        long itemId = itemId(a, task, 1);
        save(a, task, itemId, "pt-BR").then().statusCode(200);
        String afterSave = as(a).get("/api/v1/tasks/" + task.<Integer>path("id")).path("reservationExpiresAt");
        assertThat(afterSave).isNotNull();

        clock.advance(Duration.ofMinutes(3));
        for (String locale : new String[] {"en-US", "es-ES", "pt-BR"}) {
            as(a).header("Accept-Language", locale).get("/api/v1/tasks/" + task.<Integer>path("id"))
                    .then().statusCode(200)
                    .body("reservationExpiresAt", equalTo(afterSave));
            as(a).header("Accept-Language", locale).get(itemUrl(task, 1)).then().statusCode(200);
        }
        assertThat(expiresAt).isNotNull();
        // Reads in other languages left the stored language and the expiry untouched.
        as(a).get("/api/v1/tasks/" + task.<Integer>path("id"))
                .then().body("reservationExpiresAt", equalTo(afterSave));
        as(a).get(itemUrl(task, 1)).then().body("draft.language", equalTo("pt-BR"));
    }

    private java.util.List<String> languages(Response task) {
        return jdbc.sql("SELECT language FROM response WHERE task_id = ?")
                .param(task.<Integer>path("id"))
                .query(String.class)
                .list();
    }

    private Response save(String token, Response task, long itemId, String locale) {
        return as(token).header("Accept-Language", locale).contentType("application/json").body(body())
                .put(responseUrl(task, itemId));
    }

    private static Map<String, Object> body() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("changeImpact", "LOW");
        body.put("maintenanceOperation", "KEEP");
        return body;
    }

    private Response reserve(String token) {
        Response response = as(token).post(href(as(token).get("/api/v1"), "next-task"));
        response.then().statusCode(201);
        return response;
    }

    private static String itemUrl(Response task, int position) {
        return "/api/v1/tasks/" + task.<Integer>path("id") + "/items/" + position;
    }

    private static String responseUrl(Response task, long itemId) {
        return "/api/v1/tasks/" + task.<Integer>path("id") + "/responses/" + itemId;
    }

    private long itemId(String token, Response task, int position) {
        return as(token).get(itemUrl(task, position)).then().statusCode(200).extract().jsonPath().getLong("itemId");
    }
}
