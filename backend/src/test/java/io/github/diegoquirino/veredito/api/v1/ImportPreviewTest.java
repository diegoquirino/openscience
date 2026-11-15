package io.github.diegoquirino.veredito.api.v1;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** US1 of spec 005: CSV preview via vd:request-preview. */
class ImportPreviewTest extends ApiTestBase {

    @BeforeEach
    void clearStudyData() {
        clearStudyDatabase();
    }

    @Test
    void minitestPreviewHasZeroErrorsAndPlannedBlocks() throws IOException {
        Response preview = upload(administratorSession(), "minitest/veredito_minitest_vazio.csv");
        preview.then().statusCode(201)
                .header("Location", startsWith("/api/v1/batches/previews/"))
                .body("includedCount", equalTo(38))
                .body("ignoredCount", equalTo(0))
                .body("errorCount", equalTo(0))
                .body("blockCount", equalTo(4))
                .body("blockSizes", contains(10, 10, 10, 8))
                .body("_links", hasKey("vd:confirm"))
                .body("_templates.default.properties.name", contains("previewId"));

        String self = preview.path("_links.self.href");
        as(administratorSession()).get(self)
                .then().statusCode(200)
                .body("includedCount", equalTo(38))
                .body("blockSizes", contains(10, 10, 10, 8));
    }

    @Test
    void missingColumnIsCsvMissingColumn() throws IOException {
        upload(administratorSession(), "minitest/minitest_missing_kind_column.csv")
                .then().statusCode(400)
                .body("type", equalTo("urn:veredito:problem:csv-missing-column"))
                .body("column", equalTo("kind"));
    }

    @Test
    void nonMinitestInPilotIsMinitestOnly() throws IOException {
        upload(administratorSession(), "minitest/other_system_sample.csv")
                .then().statusCode(422)
                .body("type", equalTo("urn:veredito:problem:minitest-only"))
                .body("systems", contains("other-system"));
    }

    @Test
    void duplicatesClassifyIgnoredAndErrors() throws IOException {
        upload(administratorSession(), "minitest/minitest_duplicates.csv")
                .then().statusCode(201)
                .body("errorCount", greaterThan(0))
                .body("errors.code", org.hamcrest.Matchers.hasItem("duplicate-in-file"));
    }

    @Test
    void visitorIsUnauthorizedAndParticipantIsForbidden() throws IOException {
        byte[] bytes = resourceBytes("minitest/veredito_minitest_vazio.csv");
        request().multiPart("file", "veredito_minitest_vazio.csv", bytes, "text/csv")
                .post(EntryPointController.REQUEST_PREVIEW)
                .then().statusCode(401)
                .body("type", equalTo("urn:veredito:problem:authentication-required"));

        as(participantSession()).multiPart("file", "veredito_minitest_vazio.csv", bytes, "text/csv")
                .post(EntryPointController.REQUEST_PREVIEW)
                .then().statusCode(403)
                .body("type", equalTo("urn:veredito:problem:forbidden"));
    }

    @Test
    void unknownPreviewIsNotFound() {
        as(administratorSession()).get("/api/v1/batches/previews/999999")
                .then().statusCode(404)
                .body("type", equalTo("urn:veredito:problem:preview-not-found"));
    }

    @Test
    void missingFileIsInvalidRequest() {
        as(administratorSession()).multiPart("other", "x")
                .post(EntryPointController.REQUEST_PREVIEW)
                .then().statusCode(400)
                .body("type", equalTo("urn:veredito:problem:invalid-request"));
    }

    @Test
    void notUtf8IsCsvNotUtf8() {
        byte[] bytes = new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00};
        as(administratorSession()).multiPart("file", "bad.csv", bytes, "text/csv")
                .post(EntryPointController.REQUEST_PREVIEW)
                .then().statusCode(400)
                .body("type", equalTo("urn:veredito:problem:csv-not-utf8"));
    }

    @Test
    void emptyIncludedOmitsConfirm() {
        String header = String.join(",",
                "system", "edit_key", "uc", "kind", "field", "origin", "target",
                "origin_version", "target_version", "origin_usecase", "target_usecase");
        byte[] bytes = (header + "\n").getBytes(StandardCharsets.UTF_8);
        as(administratorSession()).multiPart("file", "header-only.csv", bytes, "text/csv")
                .post(EntryPointController.REQUEST_PREVIEW)
                .then().statusCode(201)
                .body("includedCount", equalTo(0))
                .body("_links", not(hasKey("vd:confirm")));
    }

    private Response upload(String token, String classpath) throws IOException {
        byte[] bytes = resourceBytes(classpath);
        String name = classpath.substring(classpath.lastIndexOf('/') + 1);
        return as(token).multiPart("file", name, bytes, "text/csv")
                .post(href(as(token).get("/api/v1"), "request-preview"));
    }

    private static byte[] resourceBytes(String classpath) throws IOException {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            return in.readAllBytes();
        }
    }
}
