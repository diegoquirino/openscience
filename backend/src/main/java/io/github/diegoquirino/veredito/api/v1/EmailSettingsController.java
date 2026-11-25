package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.invite.InviteSettingsService;
import io.github.diegoquirino.veredito.invite.InviteTemplateKind;
import io.github.diegoquirino.veredito.persistence.EmailTemplateRepository.Template;
import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository.InviteSettings;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrator invite e-mail settings and templates (spec 031); date-times are UTC instants. */
@RestController
@RequestMapping("/api/v1/administrator")
@Tag(name = "Administration")
public class EmailSettingsController {

    static final String SETTINGS = "/api/v1/administrator/email-settings";
    static final String TEMPLATE = "/api/v1/administrator/email-templates/{kind}";

    private final InviteSettingsService invite;

    public EmailSettingsController(InviteSettingsService invite) {
        this.invite = invite;
    }

    @GetMapping(path = "/email-settings", version = "v1")
    @Operation(summary = "Read invite e-mail send settings (CAAE, URL, collection and reminder date-times)")
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = EmailSettingsModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "caae": "CAAE [numero]",
                              "platformUrl": "https://diegoquirino.cloud",
                              "collectionStartAt": "2026-12-21T03:00:00Z",
                              "collectionEndAt": "2027-01-09T02:59:00Z",
                              "reminderAt": "2026-12-28T15:00:00Z",
                              "_links": {
                                "self": { "href": "/api/v1/administrator/email-settings" },
                                "vd:email-template": {
                                  "href": "/api/v1/administrator/email-templates/{kind}",
                                  "templated": true
                                }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "401",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public EmailSettingsModel getSettings() {
        return settingsModel(invite.viewSettings());
    }

    @PutMapping(path = "/email-settings", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Save invite send settings; collection end must be after start when both are set")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = HAL_FORMS, schema = @Schema(implementation = EmailSettingsModel.class)))
    @ApiResponse(
            responseCode = "400",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "401",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public EmailSettingsModel putSettings(@RequestBody EmailSettingsRequest body) {
        EmailSettingsRequest request = body == null
                ? new EmailSettingsRequest(null, null, null, null, null)
                : body;
        return settingsModel(invite.saveSettings(
                request.caae(),
                request.platformUrl(),
                request.collectionStartAt(),
                request.collectionEndAt(),
                request.reminderAt()));
    }

    @GetMapping(path = "/email-templates/{kind}", version = "v1")
    @Operation(summary = "Read one invite template (inicial, lembrete or agradecimento)")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = HAL_FORMS, schema = @Schema(implementation = EmailTemplateModel.class)))
    @ApiResponse(
            responseCode = "404",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public EmailTemplateModel getTemplate(@PathVariable String kind) {
        return templateModel(invite.viewTemplate(requireKind(kind)));
    }

    @PutMapping(path = "/email-templates/{kind}", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Replace subject and body of one invite template")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = HAL_FORMS, schema = @Schema(implementation = EmailTemplateModel.class)))
    @ApiResponse(
            responseCode = "400",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public EmailTemplateModel putTemplate(@PathVariable String kind, @RequestBody EmailTemplateRequest body) {
        EmailTemplateRequest request = body == null ? new EmailTemplateRequest(null, null) : body;
        return templateModel(invite.saveTemplate(requireKind(kind), request.subject(), request.body()));
    }

    private static InviteTemplateKind requireKind(String raw) {
        return InviteTemplateKind.fromCode(raw)
                .orElseThrow(() -> new ProblemException(ProblemType.NOT_FOUND));
    }

    private EmailSettingsModel settingsModel(InviteSettings settings) {
        EmailSettingsModel model = new EmailSettingsModel(settings);
        model.add(Affordances.of(Link.of(SETTINGS))
                .afford(HttpMethod.PUT)
                .withName("default")
                .withInput(EmailSettingsRequest.class)
                .withInputMediaType(MediaType.APPLICATION_JSON)
                .toLink());
        model.add(Link.of(TEMPLATE, LinkRelations.EMAIL_TEMPLATE.curied()));
        return model;
    }

    private EmailTemplateModel templateModel(Template template) {
        EmailTemplateModel model = new EmailTemplateModel(template);
        String self = TEMPLATE.replace("{kind}", template.kind());
        model.add(Affordances.of(Link.of(self))
                .afford(HttpMethod.PUT)
                .withName("default")
                .withInput(EmailTemplateRequest.class)
                .withInputMediaType(MediaType.APPLICATION_JSON)
                .toLink());
        model.add(Link.of(SETTINGS, LinkRelations.EMAIL_SETTINGS.curied()));
        return model;
    }
}
