package io.github.diegoquirino.veredito.domain.onboarding;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Which consent form is current and which published forms lack a language (research section 3). */
public final class ConsentFormRules {

    /** The three languages every published form must have; pt-BR is the reference. */
    public static final List<String> REQUIRED_LANGUAGES = List.of("pt-BR", "en-US", "es-ES");

    private ConsentFormRules() {}

    /** One row of {@code consent_form} with the languages of its texts. */
    public record FormSnapshot(long id, String version, Instant publishedAt, Set<String> languages) {}

    public static boolean isPublished(FormSnapshot form, Instant now) {
        return form.publishedAt() != null && !form.publishedAt().isAfter(now);
    }

    public static boolean isComplete(FormSnapshot form) {
        return missingLanguages(form).isEmpty();
    }

    /** The required languages without a text, in the order pt-BR, en-US, es-ES. */
    public static List<String> missingLanguages(FormSnapshot form) {
        return REQUIRED_LANGUAGES.stream().filter(language -> !form.languages().contains(language)).toList();
    }

    /**
     * The published, complete form with the greatest {@code publishedAt} (ties broken by id); a future
     * publication date or a missing text never makes a form current.
     */
    public static Optional<FormSnapshot> current(List<FormSnapshot> forms, Instant now) {
        return forms.stream()
                .filter(form -> isPublished(form, now) && isComplete(form))
                .max(Comparator.comparing(FormSnapshot::publishedAt).thenComparingLong(FormSnapshot::id));
    }

    /** Published forms that lack at least one language; they are logged at startup and never current. */
    public static List<FormSnapshot> incompletePublished(List<FormSnapshot> forms, Instant now) {
        return forms.stream().filter(form -> isPublished(form, now) && !isComplete(form)).toList();
    }
}
