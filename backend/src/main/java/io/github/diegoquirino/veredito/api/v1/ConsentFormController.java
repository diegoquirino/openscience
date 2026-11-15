package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.onboarding.ConsentService;
import io.github.diegoquirino.veredito.onboarding.ConsentService.ConsentText;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consent-forms")
@Tag(name = "Participation")
public class ConsentFormController {

    private static final String EXAMPLE = """
            {
              "version": "v1",
              "language": "en-US",
              "referenceLanguage": "pt-BR",
              "translation": true,
              "text": "INFORMED CONSENT FORM\\n\\nThis is a translation; ...",
              "publishedAt": "2026-11-20T12:00:00Z",
              "_links": {
                "self": { "href": "/api/v1/consent-forms/current" },
                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                "vd:consent-text": { "href": "/api/v1/consent-forms/v1/texts/pt-BR" },
                "vd:consent": { "href": "/api/v1/participants/me/consent" }
              }
            }""";

    private final ConsentService consent;

    public ConsentFormController(ConsentService consent) {
        this.consent = consent;
    }

    @GetMapping(path = "/current", version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The current consent form in the language of the request",
            description = """
                    The latest published version that has the three texts (pt-BR, en-US, es-ES), in the \
                    language resolved from Accept-Language; Content-Language states it. translation is true \
                    when the language is not the reference language (pt-BR), and vd:consent-text then points \
                    to the pt-BR text of the same version. Participant sessions only.""")
    @ApiResponse(
            responseCode = "200",
            description = "The current version",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ConsentFormModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No consent form is current: the study is not open yet",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ConsentFormModel current(Locale locale) {
        ConsentText text = consent.currentText(locale.toLanguageTag());
        ConsentFormModel model = new ConsentFormModel(text);
        model.add(Link.of(EntryPointController.CONSENT_FORM_CURRENT));
        model.add(Link.of(textPath(text.version(), text.referenceLanguage()), LinkRelations.CONSENT_TEXT.curied()));
        model.add(Link.of(EntryPointController.CONSENT, LinkRelations.CONSENT.curied()));
        return model;
    }

    @GetMapping(path = "/{version}/texts/{language}", version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "One language of one consent form version",
            description = """
                    One text of one published and complete version, whatever the request locale. This is how \
                    the pt-BR reference is read from an en-US or es-ES screen without changing the interface \
                    language; the version in the path pins what the participant is shown. Content-Language is \
                    the language of the text. Participant sessions only.""")
    @ApiResponse(
            responseCode = "200",
            description = "The text of that version and language",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ConsentFormModel.class),
                    examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The administrator's session",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Unknown, unpublished or incomplete version, or a language outside the three",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<ConsentFormModel> text(@PathVariable String version, @PathVariable String language) {
        ConsentText text = consent.text(version, language);
        ConsentFormModel model = new ConsentFormModel(text);
        model.add(Link.of(textPath(text.version(), text.language())));
        model.add(Link.of(EntryPointController.CONSENT, LinkRelations.CONSENT.curied()));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_LANGUAGE, text.language()).body(model);
    }

    private static String textPath(String version, String language) {
        return "/api/v1/consent-forms/" + version + "/texts/" + language;
    }
}
