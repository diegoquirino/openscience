package io.github.diegoquirino.veredito.support;

import static io.restassured.RestAssured.given;
import static io.restassured.config.DecoderConfig.decoderConfig;
import static io.restassured.config.RedirectConfig.redirectConfig;

import io.github.diegoquirino.veredito.access.TokenGenerator;
import io.github.diegoquirino.veredito.access.TokenHasher;
import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.access.Subject;
import io.github.diegoquirino.veredito.persistence.AccessSessionRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import io.github.diegoquirino.veredito.web.CurrentSession;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Running application on a random port, backed by the MySQL and Mailpit containers, with a movable
 * clock and a random code hash key; redirects are never followed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({MySqlContainerConfig.class, MailpitContainerConfig.class, MovableClock.Config.class})
@TestPropertySource(properties = "veredito.access.code-hash-key=${random.uuid}${random.uuid}")
public abstract class ApiTestBase {

    public static final String HAL_FORMS = "application/prs.hal-forms+json";
    public static final String CURIE = "vd";
    public static final String ADMINISTRATOR_EMAIL = "diegoquirino@gmail.com";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected MovableClock clock;

    @Autowired
    protected MailpitClient mailpit;

    @Autowired
    private AccessSessionRepository sessions;

    @Autowired
    private ParticipantRepository participants;

    @Autowired
    private TransactionTemplate transaction;

    protected RequestSpecification api;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        clock.reset();
        api = new RequestSpecBuilder()
                .setPort(port)
                .setAccept(HAL_FORMS)
                .setConfig(RestAssuredConfig.config()
                        .redirect(redirectConfig().followRedirects(false))
                        .decoderConfig(decoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
                .build();
    }

    /** A request with the base specification, for paths outside the API too; the caller is a visitor. */
    protected RequestSpecification request() {
        return given().spec(api);
    }

    /** A request carrying the session cookie. */
    protected RequestSpecification as(String sessionToken) {
        return request().cookie(CurrentSession.COOKIE, sessionToken);
    }

    protected Response entryPoint() {
        return request().get("/api/v1");
    }

    /** Follows the relation {@code rel}; a name without a prefix is read as {@code vd:<rel>}. */
    protected Response follow(Response response, String rel) {
        return request().get(href(response, rel));
    }

    protected static String href(Response response, String rel) {
        String curied = rel.contains(":") || "self".equals(rel) ? rel : CURIE + ":" + rel;
        String href = response.path("_links.'" + curied + "'.href");
        if (href == null) {
            throw new AssertionError("Relation " + curied + " not found in " + response.asString());
        }
        return href;
    }

    /**
     * The whole sign-in from the entry point: {@code vd:request-code}, the code read from Mailpit,
     * then {@code vd:sign-in}. Returns the {@code veredito-session} token.
     */
    protected String signIn(String name, String email, String locale) {
        Response requested = request().header("Accept-Language", locale)
                .contentType("application/json")
                .body(Map.of("name", name, "email", email))
                .post(href(entryPoint(), "request-code"));
        requested.then().statusCode(200);
        String code = mailpit.latestCodeTo(EmailAddress.parse(email).orElseThrow().value());
        Response signedIn = request().header("Accept-Language", locale)
                .contentType("application/json")
                .body(Map.of("email", email, "code", code))
                .post(href(requested, "sign-in"));
        signedIn.then().statusCode(201);
        return signedIn.getCookie(CurrentSession.COOKIE);
    }

    /** A session of a new participant, written directly, without a code (no e-mail limit applies). */
    protected String participantSession() {
        EmailAddress email = EmailAddress.parse("participant-" + System.nanoTime() + "@example.com").orElseThrow();
        ParticipantProfile participant = transaction.execute(status ->
                participants.upsertOnSignIn(email, new PersonName("Test Participant"), "pt-BR", clock.instant()));
        return session(Subject.PARTICIPANT, participant.id());
    }

    /** A session of the administrator, written directly, without a code. */
    protected String administratorSession() {
        return session(Subject.ADMINISTRATOR, null);
    }

    private String session(Subject subject, Long participantId) {
        String token = new TokenGenerator().next();
        Instant now = clock.instant();
        sessions.insert(new TokenHasher().hash(token), subject, participantId, now, now.plus(Duration.ofHours(24)));
        return token;
    }
}
