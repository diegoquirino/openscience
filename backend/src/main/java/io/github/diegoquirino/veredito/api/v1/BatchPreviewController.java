package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.importing.PreviewService;
import io.github.diegoquirino.veredito.importing.PreviewService.PreviewView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.Locale;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/batches/previews")
@Tag(name = "Administration")
public class BatchPreviewController {

    static final String COLLECTION = "/api/v1/batches/previews";
    static final String BATCHES = "/api/v1/batches";

    private final PreviewService previews;

    public BatchPreviewController(PreviewService previews) {
        this.previews = previews;
    }

    @PostMapping(version = "v1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a CSV and receive an import preview",
            description = """
                    Classifies every data row as included, ignored or error, plans the queue blocks and \
                    stores a temporary preview (30 minutes). Study tables are not written. When at least \
                    one row is included, the answer offers vd:confirm with a HAL-FORMS template.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    @ApiResponse(
            responseCode = "201",
            description = "Preview created",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ImportPreviewModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 12,
                              "fileName": "veredito_minitest_vazio.csv",
                              "expiresAt": "2026-11-14T23:30:00Z",
                              "includedCount": 38,
                              "ignoredCount": 0,
                              "errorCount": 0,
                              "blockCount": 4,
                              "blockSizes": [10, 10, 10, 8],
                              "extraColumns": [],
                              "ignored": [],
                              "errors": [],
                              "_links": {
                                "self": { "href": "/api/v1/batches/previews/12" },
                                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                                "vd:confirm": { "href": "/api/v1/batches" },
                                "vd:batches": { "href": "/api/v1/batches" }
                              },
                              "_templates": {
                                "default": {
                                  "title": "Confirm import",
                                  "method": "POST",
                                  "contentType": "application/json",
                                  "properties": [
                                    { "name": "previewId", "type": "number", "required": true, "value": 12 }
                                  ]
                                }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "400",
            description = "Missing file, not UTF-8, unreadable CSV or missing required column",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = {
                        @ExampleObject(name = "missing column", value = """
                                {
                                  "type": "urn:veredito:problem:csv-missing-column",
                                  "title": "Required CSV column missing",
                                  "status": 400,
                                  "detail": "The CSV is missing a required column.",
                                  "instance": "/api/v1/batches/previews",
                                  "column": "kind"
                                }"""),
                        @ExampleObject(name = "not utf-8", value = """
                                {
                                  "type": "urn:veredito:problem:csv-not-utf8",
                                  "title": "CSV is not UTF-8",
                                  "status": 400,
                                  "detail": "The uploaded file is not valid UTF-8.",
                                  "instance": "/api/v1/batches/previews"
                                }""")
                    }))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "422",
            description = "Non-Minitest systems while staging or pilot",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:minitest-only",
                              "title": "Only Minitest may be imported",
                              "status": 422,
                              "detail": "This environment accepts only the minitest system.",
                              "instance": "/api/v1/batches/previews",
                              "systems": ["other"]
                            }""")))
    public ResponseEntity<ImportPreviewModel> create(
            @RequestPart(value = "file", required = false) MultipartFile file, Locale locale) {
        PreviewView view = previews.create(file, locale);
        ImportPreviewModel model = model(view);
        return ResponseEntity.created(URI.create(self(view.id()))).body(model);
    }

    @GetMapping(path = "/{id}", version = "v1")
    @Operation(
            summary = "Read an import preview",
            description = "Returns the same body as the create answer while the preview exists and has not expired.")
    @ApiResponse(
            responseCode = "200",
            description = "Preview still valid",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ImportPreviewModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 12,
                              "fileName": "veredito_minitest_vazio.csv",
                              "expiresAt": "2026-11-14T23:30:00Z",
                              "includedCount": 38,
                              "ignoredCount": 0,
                              "errorCount": 0,
                              "blockCount": 4,
                              "blockSizes": [10, 10, 10, 8],
                              "extraColumns": [],
                              "ignored": [],
                              "errors": [],
                              "_links": {
                                "self": { "href": "/api/v1/batches/previews/12" },
                                "vd:confirm": { "href": "/api/v1/batches" },
                                "vd:batches": { "href": "/api/v1/batches" }
                              }
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
            description = "Unknown or expired preview",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:preview-not-found",
                              "title": "Import preview not found",
                              "status": 404,
                              "detail": "This import preview does not exist or has expired.",
                              "instance": "/api/v1/batches/previews/99"
                            }""")))
    public ImportPreviewModel get(@PathVariable long id, Locale locale) {
        return model(previews.get(id, locale));
    }

    private static ImportPreviewModel model(PreviewView view) {
        ImportPreviewModel model = new ImportPreviewModel(view);
        model.add(Link.of(self(view.id())));
        model.add(Link.of(BATCHES, LinkRelations.BATCHES.curied()));
        if (view.includedCount() > 0) {
            model.add(Affordances.of(Link.of(BATCHES, LinkRelations.CONFIRM.curied()))
                    .afford(HttpMethod.POST)
                    .withName("default")
                    .withInput(ConfirmPreviewRequest.class)
                    .withInputMediaType(MediaType.APPLICATION_JSON)
                    .toLink());
        }
        return model;
    }

    private static String self(long id) {
        return COLLECTION + "/" + id;
    }
}
