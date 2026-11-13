package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.AccessCodeService;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/access-codes")
@Tag(name = "Access")
public class AccessCodeController {

    static final String SELF = "/api/v1/access-codes";
    static final String SIGN_IN = "/api/v1/access-sessions";

    private final AccessCodeService service;

    public AccessCodeController(AccessCodeService service) {
        this.service = service;
    }

    @PostMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Send a sign-in code by e-mail",
            description = """
                    Sends a 6-digit code to the e-mail, in the language of Accept-Language. The code is valid \
                    for 10 minutes and for 5 attempts; only the latest code of an e-mail counts. A new code for \
                    the same e-mail can be requested after 60 seconds, at most 5 times an hour, and the whole \
                    system sends at most config.daily_email_limit codes in 24 hours. The answer is 200 and not \
                    201: the code is not a resource the client can read back. It never tells whether the \
                    e-mail belongs to a participant.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AccessCodeRequest.class),
                    examples = @ExampleObject(value = """
                            { "name": "Ana Souza", "email": "Ana@Example.com " }""")))
    @ApiResponse(
            responseCode = "200",
            description = "The code was sent",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = AccessCodeModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "email": "ana@example.com",
                              "validityMinutes": 10,
                              "resendAfterSeconds": 60,
                              "_links": {
                                "self": { "href": "/api/v1/access-codes" },
                                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                                "vd:sign-in": { "href": "/api/v1/access-sessions" },
                                "vd:request-code": { "href": "/api/v1/access-codes" }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "400",
            description = "Name or e-mail missing or invalid; errors lists each field (name: required or size;"
                    + " email: required or email)",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:invalid-request",
                              "title": "Invalid request",
                              "status": 400,
                              "detail": "The request has missing or invalid data; see the list of errors.",
                              "instance": "/api/v1/access-codes",
                              "errors": [ { "field": "name", "code": "size" }, { "field": "email", "code": "email" } ]
                            }""")))
    @ApiResponse(
            responseCode = "429",
            description = "Too soon after the latest code (retryAfterSeconds), 5 codes for the e-mail in the last"
                    + " hour (retryAt), or the daily e-mail limit of the system; header Retry-After in the first two",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = {
                        @ExampleObject(name = "too soon", value = """
                                {
                                  "type": "urn:veredito:problem:code-resend-too-soon",
                                  "title": "Wait before asking for a new code",
                                  "status": 429,
                                  "detail": "A code was sent to this e-mail a moment ago. You can ask for a new one in 42 seconds.",
                                  "instance": "/api/v1/access-codes",
                                  "retryAfterSeconds": 42
                                }"""),
                        @ExampleObject(name = "hourly limit", value = """
                                {
                                  "type": "urn:veredito:problem:code-hourly-limit",
                                  "title": "Too many codes requested",
                                  "status": 429,
                                  "detail": "This e-mail asked for 5 codes in the last hour. You can ask again in about 37 minutes.",
                                  "instance": "/api/v1/access-codes",
                                  "retryAt": "2026-11-13T18:40:00Z"
                                }"""),
                        @ExampleObject(name = "daily limit", value = """
                                {
                                  "type": "urn:veredito:problem:daily-email-limit",
                                  "title": "Daily e-mail limit reached",
                                  "status": 429,
                                  "detail": "Veredito reached its daily limit of e-mails. Try again tomorrow.",
                                  "instance": "/api/v1/access-codes"
                                }""")
                    }))
    @ApiResponse(
            responseCode = "503",
            description = "The SMTP server refused the message or did not answer in time; no code is valid",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:code-not-sent",
                              "title": "Code not sent",
                              "status": 503,
                              "detail": "The e-mail with the code could not be sent. Try again in a few minutes.",
                              "instance": "/api/v1/access-codes"
                            }""")))
    public AccessCodeModel request(@RequestBody AccessCodeRequest body, Locale locale) {
        AccessCodeModel model = new AccessCodeModel(service.request(body.name(), body.email(), locale));
        model.add(Link.of(SELF));
        model.add(Link.of(SIGN_IN, LinkRelations.SIGN_IN.curied()));
        model.add(Link.of(SELF, LinkRelations.REQUEST_CODE.curied()));
        return model;
    }
}
