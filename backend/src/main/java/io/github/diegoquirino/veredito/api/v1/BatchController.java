package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.importing.BatchImportService;
import io.github.diegoquirino.veredito.importing.BatchImportService.ConfirmResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.Locale;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batches")
@Tag(name = "Administration")
public class BatchController {

    static final String COLLECTION = "/api/v1/batches";

    private final BatchImportService batches;

    public BatchController(BatchImportService batches) {
        this.batches = batches;
    }

    @PostMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Confirm an import preview",
            description = """
                    Re-checks uniqueness, then stores items, blocks and free queue tasks in one transaction \
                    when at least one row is new. A second confirm that finds nothing new answers 200 without \
                    a new batch.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ConfirmPreviewRequest.class),
                    examples = @ExampleObject(value = "{ \"previewId\": 12 }")))
    @ApiResponse(
            responseCode = "201",
            description = "Batch stored",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ImportBatchModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 3,
                              "fileName": "veredito_minitest_vazio.csv",
                              "importedAt": "2026-11-14T23:05:00Z",
                              "includedCount": 38,
                              "ignoredCount": 0,
                              "errorCount": 0,
                              "blockCount": 4,
                              "blockSizes": [10, 10, 10, 8],
                              "queuePositions": { "from": 1, "to": 8 },
                              "systems": ["minitest"],
                              "ignored": [],
                              "_links": {
                                "self": { "href": "/api/v1/batches/3" },
                                "vd:batches": { "href": "/api/v1/batches" }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "200",
            description = "Nothing new to store after uniqueness re-check",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ImportBatchModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "fileName": "veredito_minitest_vazio.csv",
                              "includedCount": 0,
                              "ignoredCount": 38,
                              "errorCount": 0,
                              "blockCount": 0,
                              "blockSizes": [],
                              "systems": ["minitest"],
                              "ignored": [],
                              "_links": { "vd:batches": { "href": "/api/v1/batches" } }
                            }""")))
    @ApiResponse(
            responseCode = "400",
            description = "Missing previewId",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Unknown or expired preview",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<ImportBatchModel> confirm(@RequestBody ConfirmPreviewRequest body, Locale locale) {
        ConfirmResult result = batches.confirm(body == null ? null : body.previewId(), locale);
        ImportBatchModel model = model(result);
        if (result.created()) {
            return ResponseEntity.created(URI.create(COLLECTION + "/" + result.id())).body(model);
        }
        return ResponseEntity.ok(model);
    }

    @GetMapping(path = "/{id}", version = "v1")
    @Operation(
            summary = "Read a confirmed import batch",
            description = "Returns the summary of a stored batch: counts, block sizes, queue positions and systems.")
    @ApiResponse(
            responseCode = "200",
            description = "Batch found",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ImportBatchModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 3,
                              "fileName": "veredito_minitest_vazio.csv",
                              "importedAt": "2026-11-14T23:05:00Z",
                              "includedCount": 38,
                              "blockCount": 4,
                              "blockSizes": [10, 10, 10, 8],
                              "queuePositions": { "from": 1, "to": 8 },
                              "systems": ["minitest"],
                              "_links": { "self": { "href": "/api/v1/batches/3" } }
                            }""")))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Unknown batch",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ImportBatchModel get(@PathVariable long id, Locale locale) {
        return model(batches.get(id, locale));
    }

    private static ImportBatchModel model(ConfirmResult result) {
        ImportBatchModel model = new ImportBatchModel(result);
        if (result.id() != null) {
            model.add(Link.of(COLLECTION + "/" + result.id()));
        }
        model.add(Link.of(COLLECTION, LinkRelations.BATCHES.curied()));
        return model;
    }
}
