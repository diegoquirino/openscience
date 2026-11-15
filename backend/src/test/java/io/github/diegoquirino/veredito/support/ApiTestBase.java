package io.github.diegoquirino.veredito.support;

import static io.restassured.RestAssured.given;
import static io.restassured.config.DecoderConfig.decoderConfig;
import static io.restassured.config.RedirectConfig.redirectConfig;

import io.github.diegoquirino.veredito.access.TokenGenerator;
import io.github.diegoquirino.veredito.access.TokenHasher;
import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.access.Subject;
import io.github.diegoquirino.veredito.domain.onboarding.ConsentDecision;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import io.github.diegoquirino.veredito.persistence.ConsentFormRepository;
import io.github.diegoquirino.veredito.persistence.AccessSessionRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import io.github.diegoquirino.veredito.web.CurrentSession;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Running application on a random port, backed by the MySQL and Mailpit containers, with a movable
 * clock and a random code hash key; redirects are never followed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
    MySqlContainerConfig.class,
    MailpitContainerConfig.class,
    MovableClock.Config.class,
    SwitchableMailSender.Config.class
})
@TestPropertySource(properties = {
    "veredito.access.code-hash-key=${random.uuid}${random.uuid}",
    // The fixture consent form of spec 006 lives in db/staging, which production never loads.
    "spring.flyway.locations=classpath:db/migration,classpath:db/staging"
})
public abstract class ApiTestBase {

    public static final String HAL_FORMS = "application/prs.hal-forms+json";
    public static final String CURIE = "vd";
    public static final String ADMINISTRATOR_EMAIL = "diegoquirino@gmail.com";
    public static final String SIGN_IN = "/api/v1/access-sessions";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected MovableClock clock;

    @Autowired
    protected MailpitClient mailpit;

    @Autowired
    protected SwitchableMailSender mailSender;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    private AccessSessionRepository sessions;

    @Autowired
    private ParticipantRepository participants;

    @Autowired
    private ConsentFormRepository consentForms;

    @Autowired
    private TransactionTemplate transaction;

    protected RequestSpecification api;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        clock.reset();
        mailSender.failing(false);
        resetConsentForms();
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
        Response requested = requestCode(name, email, locale);
        requested.then().statusCode(200);
        Response signedIn = sendCode(email, mailpit.latestCodeTo(normalized(email)), locale);
        signedIn.then().statusCode(201);
        return signedIn.getCookie(CurrentSession.COOKIE);
    }

    /** Follows {@code vd:request-code} from the entry point. */
    protected Response requestCode(String name, String email, String locale) {
        return request().header("Accept-Language", locale)
                .contentType("application/json")
                .body(Map.of("name", name, "email", email))
                .post(href(entryPoint(), "request-code"));
    }

    /**
     * Posts a code to {@code vd:sign-in}; AccessTest checks that every {@code vd:request-code} answer
     * links this address, so the tests can also send a code for an e-mail that never asked for one.
     */
    protected Response sendCode(String email, String code, String locale) {
        return request().header("Accept-Language", locale)
                .contentType("application/json")
                .body(Map.of("email", email, "code", code))
                .post(SIGN_IN);
    }

    protected static String normalized(String email) {
        return EmailAddress.parse(email).orElseThrow().value();
    }

    /**
     * A session of a new participant, written directly, without a code (no e-mail limit applies). The
     * participant has done no onboarding step, so the eligibility gate refuses it; see
     * {@link #eligibleParticipantSession()}.
     */
    protected String participantSession() {
        return session(Subject.PARTICIPANT, newParticipant().id());
    }

    /**
     * A session of a new participant who already accepted the current consent form, chose a skill and
     * answered the screening with {@code yes_professional}: the gate lets it reserve and label blocks.
     */
    protected String eligibleParticipantSession() {
        long id = newParticipant().id();
        acceptCurrentConsent(id, "pt-BR");
        seedSkill(id, "test_analyst", null);
        seedScreening(id, ScreeningAnswer.YES_PROFESSIONAL);
        return session(Subject.PARTICIPANT, id);
    }

    /** Signs in with a code like {@link #signIn}, then completes the onboarding straight in the tables. */
    protected String eligibleSignIn(String name, String email, String locale) {
        String token = signIn(name, email, locale);
        long id = participantIdOf(token);
        acceptCurrentConsent(id, "pt-BR");
        seedSkill(id, "test_analyst", null);
        seedScreening(id, ScreeningAnswer.YES_PROFESSIONAL);
        return token;
    }

    /** The participant id behind a session token, found through the pseudonym of {@code vd:session}. */
    protected long participantIdOf(String sessionToken) {
        String pseudonym = as(sessionToken).get("/api/v1/access-sessions/current").path("pseudonym");
        return jdbc.sql("SELECT id FROM participant WHERE pseudonym = ?").param(pseudonym).query(Long.class).single();
    }

    /** Writes an acceptance of the current consent form straight to the table, in the given language. */
    protected void acceptCurrentConsent(long participantId, String language) {
        recordConsent(participantId, ConsentDecision.ACCEPTED, language);
    }

    protected void recordConsent(long participantId, ConsentDecision decision, String language) {
        long formId = consentForms.findCurrent(clock.instant()).orElseThrow().id();
        participants.recordConsent(participantId, formId, decision, language, clock.instant());
    }

    protected void seedSkill(long participantId, String skill, String other) {
        participants.recordSkill(participantId, skill, other);
    }

    protected void seedScreening(long participantId, ScreeningAnswer answer) {
        participants.recordScreening(participantId, answer);
    }

    /**
     * Puts the consent forms back as the staging fixture leaves them: only {@code fixture-1}, published in
     * 2000. Tests insert, unpublish or accept other versions, so every test starts from this state.
     */
    protected void resetConsentForms() {
        jdbc.sql("""
                        UPDATE participant SET consent_form_id = NULL, consent_decision = NULL,
                               consent_language = NULL, consent_decided_at = NULL
                        WHERE consent_form_id IN (SELECT id FROM consent_form WHERE version <> 'fixture-1')""")
                .update();
        jdbc.sql("""
                        DELETE FROM consent_form_text
                        WHERE consent_form_id IN (SELECT id FROM consent_form WHERE version <> 'fixture-1')""")
                .update();
        jdbc.sql("DELETE FROM consent_form WHERE version <> 'fixture-1'").update();
        jdbc.sql("UPDATE consent_form SET published_at = '2000-01-01 00:00:00.000000' WHERE version = 'fixture-1'")
                .update();
    }

    /** Inserts a consent form with one text per language given; {@code publishedAt} may be null (a draft). */
    protected void insertConsentForm(String version, Instant publishedAt, String... languages) {
        jdbc.sql("""
                        INSERT INTO consent_form (version, reference_language, created_at, published_at)
                        VALUES (?, 'pt-BR', ?, ?)""")
                .params(version, Timestamp.from(clock.instant()), publishedAt == null ? null : Timestamp.from(publishedAt))
                .update();
        long id = jdbc.sql("SELECT id FROM consent_form WHERE version = ?").param(version).query(Long.class).single();
        for (String language : languages) {
            jdbc.sql("INSERT INTO consent_form_text (consent_form_id, language, text) VALUES (?, ?, ?)")
                    .params(id, language, "Text of " + version + " in " + language)
                    .update();
        }
    }

    protected void insertCompleteConsentForm(String version, Instant publishedAt) {
        insertConsentForm(version, publishedAt, "pt-BR", "en-US", "es-ES");
    }

    private ParticipantProfile newParticipant() {
        EmailAddress email = EmailAddress.parse("participant-" + System.nanoTime() + "@example.com").orElseThrow();
        return transaction.execute(status ->
                participants.upsertOnSignIn(email, new PersonName("Test Participant"), "pt-BR", clock.instant()));
    }
    /** A session of the administrator, written directly, without a code. */
    protected String administratorSession() {
        return session(Subject.ADMINISTRATOR, null);
    }

    /** Empties the study tables (responses, tasks, items, blocks, batches) so a test starts from scratch. */
    protected void clearStudyDatabase() {
        jdbc.sql("DELETE FROM response").update();
        jdbc.sql("DELETE FROM task").update();
        jdbc.sql("DELETE FROM item").update();
        jdbc.sql("DELETE FROM block").update();
        jdbc.sql("DELETE FROM import_batch").update();
        jdbc.sql("DELETE FROM import_preview").update();
    }

    /** Imports the empty Minitest CSV as the administrator: 4 blocks of 10 items, 8 tasks. */
    protected void importMinitestBatch() {
        String token = administratorSession();
        try (InputStream in = new ClassPathResource("minitest/veredito_minitest_vazio.csv").getInputStream()) {
            Response preview = as(token)
                    .multiPart("file", "veredito_minitest_vazio.csv", in.readAllBytes(), "text/csv")
                    .post("/api/v1/batches/previews");
            preview.then().statusCode(201);
            as(token)
                    .contentType("application/json")
                    .body(Map.of("previewId", preview.<Integer>path("id")))
                    .post("/api/v1/batches")
                    .then().statusCode(201);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String session(Subject subject, Long participantId) {
        String token = new TokenGenerator().next();
        Instant now = clock.instant();
        sessions.insert(new TokenHasher().hash(token), subject, participantId, now, now.plus(Duration.ofHours(24)));
        return token;
    }
}
