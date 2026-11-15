package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.importing.StudyPhaseService;
import io.github.diegoquirino.veredito.importing.StudyPhaseService.StudyPhaseView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/administrator/study-phase")
@Tag(name = "Administration")
public class StudyPhaseController {

    static final String SELF = "/api/v1/administrator/study-phase";

    private final StudyPhaseService phases;

    public StudyPhaseController(StudyPhaseService phases) {
        this.phases = phases;
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "Read the study phase",
            description = """
                    Returns pilot or collection, whether only Minitest may be imported, and whether a switch \
                    to collection is allowed (empty study database, not staging). Absent in staging (404).""")
    @ApiResponse(
            responseCode = "200",
            description = "Current phase",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = StudyPhaseModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "phase": "pilot",
                              "changedAt": null,
                              "switchAllowed": false,
                              "minitestOnly": true,
                              "_links": { "self": { "href": "/api/v1/administrator/study-phase" } }
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
            description = "Staging profile",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public StudyPhaseModel get() {
        return model(phases.current());
    }

    @PutMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Switch the study phase to collection",
            description = "Allowed only in production/development while still in pilot with an empty study database.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StudyPhaseRequest.class),
                    examples = @ExampleObject(value = "{ \"phase\": \"collection\" }")))
    @ApiResponse(
            responseCode = "200",
            description = "Switched",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = StudyPhaseModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "phase": "collection",
                              "changedAt": "2026-11-14T23:40:00Z",
                              "switchAllowed": false,
                              "minitestOnly": false,
                              "_links": { "self": { "href": "/api/v1/administrator/study-phase" } }
                            }""")))
    @ApiResponse(
            responseCode = "400",
            description = "phase is not collection",
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
            description = "Staging profile",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Switch not allowed",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public StudyPhaseModel put(@RequestBody StudyPhaseRequest body) {
        StudyPhaseService.requireCollectionPhase(body == null ? null : body.phase());
        return model(phases.switchToCollection());
    }

    private static StudyPhaseModel model(StudyPhaseView view) {
        StudyPhaseModel model = new StudyPhaseModel(view);
        if (view.switchAllowed()) {
            model.add(Affordances.of(Link.of(SELF))
                    .afford(HttpMethod.PUT)
                    .withName("default")
                    .withInput(StudyPhaseRequest.class)
                    .withInputMediaType(MediaType.APPLICATION_JSON)
                    .toLink());
        } else {
            model.add(Link.of(SELF));
        }
        return model;
    }
}
