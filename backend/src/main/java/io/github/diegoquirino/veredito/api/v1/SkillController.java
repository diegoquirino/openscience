package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.onboarding.SkillService;
import io.github.diegoquirino.veredito.onboarding.SkillService.SkillView;
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
@RequestMapping(EntryPointController.SKILL)
@Tag(name = "Participation")
public class SkillController {

    private static final String EXAMPLE = """
            {
              "skill": "other",
              "skillOther": "Technical writer",
              "_links": {
                "self": { "href": "/api/v1/participants/me/skill" },
                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ]
              }
            }""";

    private final SkillService skills;

    public SkillController(SkillService skills) {
        this.skills = skills;
    }

    @GetMapping(version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The participant's professional skill",
            description = """
                    The stored skill code and, with other, the description typed. The PUT template lists the \
                    13 codes of Appendix B.4 and is present only while the pending step is skill: the skill \
                    is asked once. Participant sessions only.""")
    @ApiResponse(
            responseCode = "200",
            description = "The stored answer, or nulls when not answered yet",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = SkillModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public SkillModel get(@AuthenticationPrincipal VereditoPrincipal caller) {
        return model(skills.view(participantId(caller)));
    }

    @PutMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "Choose the professional skill",
            description = """
                    Stores one skill code once. With other, skillOther is required, not blank and at most 100 \
                    characters; it is stored exactly as typed. skillOther with any other code is refused. An \
                    identical repeat is 200; any other answer once the skill is stored, or before its step, \
                    is 409 step-not-pending.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SkillRequest.class),
                    examples = @ExampleObject(value = """
                            { "skill": "other", "skillOther": "Technical writer" }""")))
    @ApiResponse(
            responseCode = "200",
            description = "The stored answer; Content-Location is the onboarding resource",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = SkillModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "400",
            description = "skill missing (required) or unknown (option); skillOther required, too long (size) or "
                    + "unexpected",
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
            description = "step-not-pending: the skill is not the pending step and the body differs from what is "
                    + "stored",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<SkillModel> put(
            @AuthenticationPrincipal VereditoPrincipal caller, @RequestBody(required = false) SkillRequest body) {
        long participantId = participantId(caller);
        SkillRequest request = body == null ? new SkillRequest(null, null) : body;
        SkillView view = skills.record(participantId, request.skill(), request.skillOther());
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

    private static SkillModel model(SkillView view) {
        SkillModel model = new SkillModel(view);
        if (view.pending()) {
            model.add(Affordances.of(Link.of(EntryPointController.SKILL))
                    .afford(HttpMethod.PUT)
                    .withName("default")
                    .withInput(SkillRequest.class)
                    .withInputMediaType(MediaType.APPLICATION_JSON)
                    .toLink());
        } else {
            model.add(Link.of(EntryPointController.SKILL));
        }
        return model;
    }
}
