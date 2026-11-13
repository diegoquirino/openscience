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

    private static final Map<String, Relation> BY_NAME = new LinkedHashMap<>();

    static {
        for (Relation relation : new Relation[] {V1, REQUEST_CODE, SIGN_IN, SESSION, SIGN_OUT, ME}) {
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
