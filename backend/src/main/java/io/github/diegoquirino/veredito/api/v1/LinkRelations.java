package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.api.HypermediaConfig;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpMethod;

/**
 * Every {@code vd:} relation the API emits, documented at {@code /api/v1/rels/{rel}}. Later specs add
 * theirs here; descriptions are English documentation for developers, not user-facing text.
 */
public final class LinkRelations {

    public static final Relation V1 = new Relation(
            "v1", "Entry point of version 1 of the API.", HttpMethod.GET, "/swagger-ui.html#/Entry%20point");
    public static final Relation REQUEST_CODE = new Relation(
            "request-code", "Request a sign-in code by e-mail.", HttpMethod.POST, "/swagger-ui.html#/Access");
    public static final Relation SIGN_IN = new Relation(
            "sign-in", "Validate an access code and sign in.", HttpMethod.POST, "/swagger-ui.html#/Access");
    public static final Relation SESSION = new Relation(
            "session", "The current session: who is signed in, the profile language and the expiry.",
            HttpMethod.GET, "/swagger-ui.html#/Access");
    public static final Relation SIGN_OUT = new Relation(
            "sign-out", "End the current session.", HttpMethod.DELETE, "/swagger-ui.html#/Access");
    public static final Relation ME = new Relation(
            "me", "Own profile; PUT changes the preferred language.", HttpMethod.GET,
            "/swagger-ui.html#/Participation");
    public static final Relation BATCHES = new Relation(
            "batches", "List of confirmed import batches, newest first.", HttpMethod.GET,
            "/swagger-ui.html#/Administration");
    public static final Relation REQUEST_PREVIEW = new Relation(
            "request-preview", "Upload a CSV and receive an import preview.", HttpMethod.POST,
            "/swagger-ui.html#/Administration");
    public static final Relation STUDY_PHASE = new Relation(
            "study-phase", "Read or switch the study phase (pilot / collection).", HttpMethod.GET,
            "/swagger-ui.html#/Administration");
    public static final Relation CONFIRM = new Relation(
            "confirm", "Confirm a preview and store items, blocks and free tasks.", HttpMethod.POST,
            "/swagger-ui.html#/Administration");
    public static final Relation NEXT_TASK = new Relation(
            "next-task", "Request the next free task (block-role) from the queue.", HttpMethod.POST,
            "/swagger-ui.html#/Tasks");
    public static final Relation RESERVE = new Relation(
            "reserve", "Reserve a task for the signed-in participant.", HttpMethod.POST,
            "/swagger-ui.html#/Tasks");
    public static final Relation SAVE_RESPONSE = new Relation(
            "save-response", "Save or replace a draft response for one item.", HttpMethod.PUT,
            "/swagger-ui.html#/Tasks");
    public static final Relation SUBMIT = new Relation(
            "submit", "Submit every draft of the reserved task as final.", HttpMethod.POST,
            "/swagger-ui.html#/Tasks");
    public static final Relation ITEMS = new Relation(
            "items", "Item of a reserved task by position in the block.", HttpMethod.GET,
            "/swagger-ui.html#/Tasks");
    public static final Relation REVIEW = new Relation(
            "review", "Reserved task ready for review of all drafts.", HttpMethod.GET,
            "/swagger-ui.html#/Tasks");

    public static final Relation ONBOARDING = new Relation(
            "onboarding", "The step the participant is at: consent form, skill, screening or participation.",
            HttpMethod.GET, "/swagger-ui.html#/Participation");
    public static final Relation CONSENT_FORM = new Relation(
            "consent-form", "The current consent form in the language of the request.", HttpMethod.GET,
            "/swagger-ui.html#/Participation");
    public static final Relation CONSENT_TEXT = new Relation(
            "consent-text", "One language of one version of the consent form (used for the pt-BR reference).",
            HttpMethod.GET, "/swagger-ui.html#/Participation");
    public static final Relation CONSENT = new Relation(
            "consent", "The participant's decision on the consent form; PUT records it.", HttpMethod.GET,
            "/swagger-ui.html#/Participation");
    public static final Relation SKILL = new Relation(
            "skill", "The participant's professional skill; PUT records it once.", HttpMethod.GET,
            "/swagger-ui.html#/Participation");
    public static final Relation SCREENING = new Relation(
            "screening", "The participant's screening answer; PUT records it once.", HttpMethod.GET,
            "/swagger-ui.html#/Participation");
    public static final Relation SCREENING_EXAMPLE = new Relation(
            "screening-example", "The fixed English use case and test case shown before the screening question.",
            HttpMethod.GET, "/swagger-ui.html#/Participation");

    private static final Map<String, Relation> BY_NAME = new LinkedHashMap<>();

    static {
        for (Relation relation : new Relation[] {
            V1, REQUEST_CODE, SIGN_IN, SESSION, SIGN_OUT, ME, BATCHES, REQUEST_PREVIEW, STUDY_PHASE, CONFIRM,
            NEXT_TASK, RESERVE, SAVE_RESPONSE, SUBMIT, ITEMS, REVIEW, ONBOARDING, CONSENT_FORM, CONSENT_TEXT, CONSENT, SKILL,
            SCREENING, SCREENING_EXAMPLE
        }) {
            BY_NAME.put(relation.name(), relation);
        }
    }

    private LinkRelations() {}

    public static Optional<Relation> find(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    public static Collection<Relation> all() {
        return BY_NAME.values();
    }

    /** A relation by its name without the prefix; {@link #curied()} gives {@code vd:<name>}. */
    public record Relation(String name, String description, HttpMethod method, String documentation) {

        public String curied() {
            return HypermediaConfig.CURIE + ":" + name;
        }
    }
}
