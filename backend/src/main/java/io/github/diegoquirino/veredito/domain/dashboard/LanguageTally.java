package io.github.diegoquirino.veredito.domain.dashboard;

import java.util.List;
import java.util.Map;

/** Language rows of the dashboard: always the three locales, in a fixed order, zeros included. */
public final class LanguageTally {

    public static final List<String> LANGUAGES = List.of("pt-BR", "en-US", "es-ES");

    /** Preferred-language participants and final responses of one locale. */
    public record Row(String language, long participantsPreferred, long finalResponses) {}

    private LanguageTally() {}

    public static List<Row> rows(Map<String, Long> preferredByLanguage, Map<String, Long> finalByLanguage) {
        return LANGUAGES.stream()
                .map(language -> new Row(
                        language,
                        preferredByLanguage.getOrDefault(language, 0L),
                        finalByLanguage.getOrDefault(language, 0L)))
                .toList();
    }
}