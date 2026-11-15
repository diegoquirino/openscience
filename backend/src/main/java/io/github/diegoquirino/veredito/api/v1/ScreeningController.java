package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.onboarding.ScreeningService;
import io.github.diegoquirino.veredito.onboarding.ScreeningService.ScreeningView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntryPointController.SCREENING)
@Tag(name = "Participation")
public class ScreeningController {

    private static final String EXAMPLE = """
            {
              "answer": "yes_academic",
              "eligible": true,
              "_links": {
                "self": { "href": "/api/v1/participants/me/screening" },
                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                "vd:screening-example": { "href": "/api/v1/screening-example" }
              }
            }""";

    private final ScreeningService screening;

    public ScreeningController(ScreeningService screening) {
        this.screening = screening;
    }

    @GetMapping(version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The participant's screening answer",
            description = """
                    The stored answer and eligibility; eligible is true for the two yes answers and false for \
                    no. The PUT template is present only while the pending step is screening: the answer is \
                    given once and no is permanent. Participant sessions only.""")
    @ApiResponse(
            responseCode = "200",
            description = "The stored answer, or nulls when not answered yet",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ScreeningModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ScreeningModel get(@AuthenticationPrincipal VereditoPrincipal caller) {
        return model(screening.view(participantId(caller)));
    }

    @PutMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "Answer the screening question",
            description = """
                    Stores the answer once. yes_professional and yes_academic make the participant eligible \
                    (the next onboarding read says participation); no makes the e-mail permanently ineligible. \
                    An identical repeat is 200; any other answer once stored, or before its step, is 409 \
                    step-not-pending.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ScreeningRequest.class),
                    examples = @ExampleObject(value = """
                            { "answer": "yes_academic" }""")))
    @ApiResponse(
            responseCode = "200",
            description = "The stored answer; Content-Location is the onboarding resource",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ScreeningModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "400",
            description = "answer missing (required) or not one of the three codes (option)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "409",
            description = "step-not-pending: the screening is not the pending step and the body differs from what "
                    + "is stored",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<ScreeningModel> put(
            @AuthenticationPrincipal VereditoPrincipal caller, @RequestBody(required = false) ScreeningRequest body) {
        long participantId = participantId(caller);
        ScreeningView view = screening.record(participantId, body == null ? null : body.answer());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LOCATION, EntryPointController.ONBOARDING)
                .body(model(view));
    }

    private static long participantId(VereditoPrincipal caller) {
        if (caller == null || caller.participantId() == null) {
            throw new ProblemException(ProblemType.AUTHENTICATION_REQUIRED);
        }
        return caller.participantId();
    }

    private static ScreeningModel model(ScreeningView view) {
        ScreeningModel model = new ScreeningModel(view);
        if (view.pending()) {
            model.add(Affordances.of(Link.of(EntryPointController.SCREENING))
                    .afford(HttpMethod.PUT)
                    .withName("default")
                    .withInput(ScreeningRequest.class)
                    .withInputMediaType(MediaType.APPLICATION_JSON)
                    .toLink());
        } else {
            model.add(Link.of(EntryPointController.SCREENING));
        }
        model.add(Link.of(EntryPointController.SCREENING_EXAMPLE, LinkRelations.SCREENING_EXAMPLE.curied()));
        return model;
    }
}
