package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingStep;
import io.github.diegoquirino.veredito.onboarding.ConsentService;
import io.github.diegoquirino.veredito.onboarding.ConsentService.ConsentView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
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
@RequestMapping(EntryPointController.CONSENT)
@Tag(name = "Participation")
public class ConsentDecisionController {

    private static final String EXAMPLE = """
            {
              "decision": "accepted",
              "consentFormVersion": "v1",
              "language": "en-US",
              "decidedAt": "2026-11-20T17:03:00Z",
              "current": true,
              "_links": {
                "self": { "href": "/api/v1/participants/me/consent" },
                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                "vd:consent-form": { "href": "/api/v1/consent-forms/current" }
              }
            }""";

    private final ConsentService consent;

    public ConsentDecisionController(ConsentService consent) {
        this.consent = consent;
    }

    @GetMapping(version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The participant's last decision on the consent form",
            description = """
                    The last recorded decision, its version, the language of the text shown and the time. \
                    Nothing recorded yet: every field is null and current is false. current is true when the \
                    recorded version is the current one. The PUT template is present only while the pending \
                    step is consent. Participant sessions only.""")
    @ApiResponse(
            responseCode = "200",
            description = "The last decision",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ConsentDecisionModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ConsentDecisionModel get(@AuthenticationPrincipal VereditoPrincipal caller) {
        return model(consent.view(participantId(caller)));
    }

    @PutMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "Accept or refuse the current consent form",
            description = """
                    Records the decision on the current version, with the request locale as the language of \
                    the text shown and the current instant. Only the last decision is kept: a refusal can \
                    become an acceptance, and an acceptance of an older version is replaced by one of the \
                    current version. Repeating the same decision is 200 and changes nothing. Refusing a \
                    version already accepted is 409 step-not-pending (withdrawal is a different feature).""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ConsentDecisionRequest.class),
                    examples = @ExampleObject(value = """
                            { "decision": "accepted", "consentFormVersion": "v1" }""")))
    @ApiResponse(
            responseCode = "200",
            description = "The refreshed decision; Content-Location is the onboarding resource",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ConsentDecisionModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "400",
            description = "decision missing (required) or not accepted/refused (option); consentFormVersion missing",
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
            description = "study-not-open, consent-form-outdated (currentVersion) or step-not-pending (step)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<ConsentDecisionModel> put(
            @AuthenticationPrincipal VereditoPrincipal caller,
            @RequestBody(required = false) ConsentDecisionRequest body,
            Locale locale) {
        long participantId = participantId(caller);
        ConsentDecisionRequest request = body == null ? new ConsentDecisionRequest(null, null) : body;
        ConsentView view = consent.record(
                participantId, request.decision(), request.consentFormVersion(), locale.toLanguageTag());
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

    private static ConsentDecisionModel model(ConsentView view) {
        ConsentDecisionModel model = new ConsentDecisionModel(view);
        if (view.step() == OnboardingStep.CONSENT) {
            model.add(Affordances.of(Link.of(EntryPointController.CONSENT))
                    .afford(HttpMethod.PUT)
                    .withName("default")
                    .withInput(ConsentDecisionRequest.class)
                    .withInputMediaType(MediaType.APPLICATION_JSON)
                    .toLink());
        } else {
            model.add(Link.of(EntryPointController.CONSENT));
        }
        model.add(Link.of(EntryPointController.CONSENT_FORM_CURRENT, LinkRelations.CONSENT_FORM.curied()));
        return model;
    }
}
