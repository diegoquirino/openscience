package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.access.SessionView;
import io.github.diegoquirino.veredito.access.SignInService;
import io.github.diegoquirino.veredito.access.SignInService.SignedIn;
import io.github.diegoquirino.veredito.access.VereditoPrincipal;
import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.config.AccessProperties;
import io.github.diegoquirino.veredito.config.OpenApiConfig;
import io.github.diegoquirino.veredito.domain.access.Subject;
import io.github.diegoquirino.veredito.web.CurrentSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.Locale;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/access-sessions")
@Tag(name = "Access")
public class AccessSessionController {

    static final String CURRENT = "/api/v1/access-sessions/current";

    private static final String PARTICIPANT_SESSION_EXAMPLE = """
            {
              "subject": "participant",
              "name": "Ana Souza",
              "pseudonym": "R-0001",
              "preferredLanguage": "es-ES",
              "expiresAt": "2026-11-14T17:03:00Z",
              "_links": {
                "self": { "href": "/api/v1/access-sessions/current" },
                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                "vd:me": { "href": "/api/v1/participants/me" },
                "vd:sign-out": { "href": "/api/v1/access-sessions/current" }
              }
            }""";

    private static final String ADMINISTRATOR_SESSION_EXAMPLE = """
            {
              "subject": "administrator",
              "name": "Diego Quirino",
              "preferredLanguage": "pt-BR",
              "expiresAt": "2026-11-14T17:03:00Z",
              "_links": {
                "self": { "href": "/api/v1/access-sessions/current" },
                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                "vd:me": { "href": "/api/v1/administrator/me" },
                "vd:sign-out": { "href": "/api/v1/access-sessions/current" }
              }
            }""";

    private final SignInService service;
    private final AccessProperties properties;

    public AccessSessionController(SignInService service, AccessProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Sign in with the code received by e-mail",
            description = """
                    Checks the code against the latest code of the e-mail; spaces and hyphens are ignored. \
                    The right code signs the person in: the first sign-in of an e-mail registers the \
                    participant with the next pseudonym (R-0001, R-0002, ...); the administrator's e-mail opens \
                    an administrator session and registers no participant. The session lasts 24 hours and \
                    travels only in the HttpOnly cookie veredito-session. A wrong code counts as an attempt; \
                    after 5 attempts, or once expired, used or replaced, the code is dead. No answer tells \
                    whether the e-mail belongs to a participant.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SignInRequest.class),
                    examples = @ExampleObject(value = """
                            { "email": "ana@example.com", "code": "123 456" }""")))
    @ApiResponse(
            responseCode = "201",
            description = "Signed in; the body is the session",
            headers = {
                @Header(name = "Location", description = "/api/v1/access-sessions/current"),
                @Header(
                        name = "Set-Cookie",
                        description = "veredito-session=<token>; Path=/; Max-Age=86400; HttpOnly; Secure;"
                                + " SameSite=Lax")
            },
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = SessionModel.class),
                    examples = {
                        @ExampleObject(name = "participant", value = PARTICIPANT_SESSION_EXAMPLE),
                        @ExampleObject(name = "administrator", value = ADMINISTRATOR_SESSION_EXAMPLE)
                    }))
    @ApiResponse(
            responseCode = "400",
            description = "E-mail or code missing (email: required or email; code: required)",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:invalid-request",
                              "title": "Invalid request",
                              "status": 400,
                              "detail": "The request has missing or invalid data; see the list of errors.",
                              "instance": "/api/v1/access-sessions",
                              "errors": [ { "field": "code", "code": "required" } ]
                            }""")))
    @ApiResponse(
            responseCode = "422",
            description = "Wrong code on a live code (attemptsLeft from 1 to 4), or a dead code: expired, used,"
                    + " invalidated by 5 wrong attempts, replaced by a newer code, or no code for this e-mail",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = {
                        @ExampleObject(name = "wrong code", value = """
                                {
                                  "type": "urn:veredito:problem:invalid-code",
                                  "title": "Wrong code",
                                  "status": 422,
                                  "detail": "The code does not match. Attempts left: 4.",
                                  "instance": "/api/v1/access-sessions",
                                  "attemptsLeft": 4
                                }"""),
                        @ExampleObject(name = "dead code", value = """
                                {
                                  "type": "urn:veredito:problem:code-expired",
                                  "title": "Code no longer valid",
                                  "status": 422,
                                  "detail": "This code expired, was already used or was replaced by a newer one. Ask for a new code.",
                                  "instance": "/api/v1/access-sessions"
                                }""")
                    }))
    public ResponseEntity<SessionModel> signIn(@RequestBody SignInRequest body, Locale locale) {
        SignedIn signedIn = service.signIn(body.email(), body.code(), locale);
        ResponseCookie cookie = ResponseCookie.from(CurrentSession.COOKIE, signedIn.token())
                .path("/")
                .maxAge(properties.sessionDuration())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();
        return ResponseEntity.created(URI.create(CURRENT))
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(model(signedIn.session()));
    }

    @GetMapping(path = "/current", version = "v1")
    @SecurityRequirement(name = OpenApiConfig.SESSION_SCHEME)
    @Operation(
            summary = "The current session",
            description = """
                    Who is signed in (participant or administrator), the latest name typed, the pseudonym of a \
                    participant, the profile language and the end of the session. The client opens the screens \
                    in the profile language and follows vd:me and vd:sign-out from here.""")
    @ApiResponse(
            responseCode = "200",
            description = "The session",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = SessionModel.class),
                    examples = {
                        @ExampleObject(name = "participant", value = PARTICIPANT_SESSION_EXAMPLE),
                        @ExampleObject(name = "administrator", value = ADMINISTRATOR_SESSION_EXAMPLE)
                    }))
    @ApiResponse(
            responseCode = "401",
            description = "No valid session: no cookie, or an unknown or expired one",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetailsSchema.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "urn:veredito:problem:authentication-required",
                              "title": "Authentication required",
                              "status": 401,
                              "detail": "Sign in to access this resource.",
                              "instance": "/api/v1/access-sessions/current"
                            }""")))
    public SessionModel current(@AuthenticationPrincipal VereditoPrincipal caller) {
        return model(service.current(caller));
    }

    private static SessionModel model(SessionView session) {
        SessionModel model = new SessionModel(session);
        model.add(Link.of(CURRENT));
        model.add(Link.of(
                session.subject() == Subject.ADMINISTRATOR
                        ? EntryPointController.ADMINISTRATOR_ME
                        : EntryPointController.PARTICIPANT_ME,
                LinkRelations.ME.curied()));
        model.add(Link.of(CURRENT, LinkRelations.SIGN_OUT.curied()));
        return model;
    }
}
