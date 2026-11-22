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
import java.util.List;
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
@RequestMapping
@Tag(name = "Participation")
public class SkillController {

    private static final String EXAMPLE = """
            {
              "name": "Ana Souza",
              "skill": "other",
              "skillOther": "Technical writer",
              "organization": "other",
              "organizationOther": "Acme Ltd",
              "_links": {
                "self": { "href": "/api/v1/participants/me/skill" },
                "vd:organization-suggestions": { "href": "/api/v1/participants/me/organization-suggestions" },
                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ]
              }
            }""";

    private final SkillService skills;

    public SkillController(SkillService skills) {
        this.skills = skills;
    }

    @GetMapping(path = EntryPointController.SKILL, version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The participant's name, skill and organization",
            description = """
                    The stored display name, skill, organization and free-text fields when other. The PUT \
                    template is present while the pending step is profile and remains available afterwards \
                    for edits (specs 014 and 025). Participant sessions only.""")
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

    @PutMapping(
            path = EntryPointController.SKILL,
            version = "v1",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "Save name, professional skill and organization",
            description = """
                    Stores the display name, one skill code and one organization code. With other, the \
                    matching free-text field is required, not blank and at most 100 characters. While the \
                    pending step is profile, name, skill and organization are required. Afterwards the same \
                    PUT updates the profile. An identical repeat is 200.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SkillRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "name": "Ana Souza",
                              "skill": "other",
                              "skillOther": "Technical writer",
                              "organization": "ufcg"
                            }""")))
    @ApiResponse(
            responseCode = "200",
            description = "The stored answer; Content-Location is the onboarding resource",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = SkillModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "400",
            description = "Validation errors on name, skill, skillOther, organization or organizationOther",
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
            description = "step-not-pending",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<SkillModel> put(
            @AuthenticationPrincipal VereditoPrincipal caller, @RequestBody(required = false) SkillRequest body) {
        long participantId = participantId(caller);
        SkillRequest request = body == null ? new SkillRequest(null, null, null, null, null) : body;
        SkillView view = skills.record(
                participantId,
                request.name(),
                request.skill(),
                request.skillOther(),
                request.organization(),
                request.organizationOther());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LOCATION, EntryPointController.ONBOARDING)
                .body(model(view));
    }

    @GetMapping(path = EntryPointController.ORGANIZATION_SUGGESTIONS, version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "Autocomplete suggestions for organization other",
            description = """
                    Distinct organizationOther values already saved by other participants with \
                    organization = other (spec 025).""")
    @ApiResponse(responseCode = "200", description = "Suggestion strings")
    public OrganizationSuggestionsModel suggestions(@AuthenticationPrincipal VereditoPrincipal caller) {
        long id = participantId(caller);
        List<String> values = skills.organizationSuggestions(id);
        OrganizationSuggestionsModel model = new OrganizationSuggestionsModel(values);
        model.add(Link.of(EntryPointController.ORGANIZATION_SUGGESTIONS).withSelfRel());
        return model;
    }

    private static long participantId(VereditoPrincipal caller) {
        if (caller == null || caller.participantId() == null) {
            throw new ProblemException(ProblemType.AUTHENTICATION_REQUIRED);
        }
        return caller.participantId();
    }

    private static SkillModel model(SkillView view) {
        SkillModel model = new SkillModel(view);
        model.add(Affordances.of(Link.of(EntryPointController.SKILL))
                .afford(HttpMethod.PUT)
                .withName("default")
                .withInput(SkillRequest.class)
                .withInputMediaType(MediaType.APPLICATION_JSON)
                .toLink());
        model.add(Link.of(EntryPointController.ORGANIZATION_SUGGESTIONS, LinkRelations.ORGANIZATION_SUGGESTIONS.curied()));
        return model;
    }
}
