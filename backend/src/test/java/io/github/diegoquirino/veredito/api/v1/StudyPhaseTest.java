package io.github.diegoquirino.veredito.api.v1;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

/** US4 of spec 005: pilot to collection switch. */
class StudyPhaseTest extends ApiTestBase {

    @BeforeEach
    void resetPhaseAndData() {
        jdbc.sql("DELETE FROM task").update();
        jdbc.sql("DELETE FROM item").update();
        jdbc.sql("DELETE FROM block").update();
        jdbc.sql("DELETE FROM import_batch").update();
        jdbc.sql("DELETE FROM import_preview").update();
        jdbc.sql("DELETE FROM response").update();
        jdbc.sql("DELETE FROM access_session WHERE subject = 'participant'").update();
        jdbc.sql("DELETE FROM participant").update();
        jdbc.sql("UPDATE config SET study_phase = 'pilot', study_phase_changed_at = NULL WHERE id = 1").update();
    }

    @Test
    void getPhaseAndRefuseSwitchWhenItemsPresent() throws IOException {
        String token = administratorSession();
        as(token).get(EntryPointController.STUDY_PHASE)
                .then().statusCode(200)
                .body("phase", equalTo("pilot"))
                .body("minitestOnly", equalTo(true));

        Response preview = as(token)
                .multiPart(
                        "file",
                        "veredito_minitest_vazio.csv",
                        new ClassPathResource("minitest/veredito_minitest_vazio.csv").getInputStream().readAllBytes(),
                        "text/csv")
                .post(EntryPointController.REQUEST_PREVIEW);
        preview.then().statusCode(201);
        as(token)
                .contentType("application/json")
                .body(Map.of("previewId", preview.<Integer>path("id")))
                .post(EntryPointController.BATCHES)
                .then().statusCode(201);

        as(token)
                .contentType("application/json")
                .body(Map.of("phase", "collection"))
                .put(EntryPointController.STUDY_PHASE)
                .then().statusCode(409)
                .body("type", equalTo("urn:veredito:problem:phase-switch-not-allowed"));
    }

    @Test
    void switchToCollectionThenAcceptsOtherSystems() throws IOException {
        String token = administratorSession();
        as(token)
                .contentType("application/json")
                .body(Map.of("phase", "collection"))
                .put(EntryPointController.STUDY_PHASE)
                .then().statusCode(200)
                .body("phase", equalTo("collection"))
                .body("changedAt", notNullValue())
                .body("minitestOnly", equalTo(false))
                .body("switchAllowed", equalTo(false));

        as(token)
                .multiPart(
                        "file",
                        "other_system_sample.csv",
                        new ClassPathResource("minitest/other_system_sample.csv").getInputStream().readAllBytes(),
                        "text/csv")
                .post(EntryPointController.REQUEST_PREVIEW)
                .then().statusCode(201);
    }
}

@TestPropertySource(properties = "veredito.environment=staging")
class StudyPhaseStagingTest extends ApiTestBase {

    @Test
    void stagingHasNoStudyPhaseResource() {
        String token = administratorSession();
        as(token).get("/api/v1")
                .then().statusCode(200)
                .body("_links.'vd:study-phase'.href", org.hamcrest.Matchers.nullValue());
        as(token).get(EntryPointController.STUDY_PHASE)
                .then().statusCode(404)
                .body("type", equalTo("urn:veredito:problem:not-found"));
    }
}
