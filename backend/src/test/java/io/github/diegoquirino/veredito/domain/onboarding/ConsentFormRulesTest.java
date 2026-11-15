package io.github.diegoquirino.veredito.domain.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.domain.onboarding.ConsentFormRules.FormSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Which consent form is current (spec 006 FR-008, FR-012), without Spring. */
class ConsentFormRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final Set<String> ALL = Set.of("pt-BR", "en-US", "es-ES");

    private static FormSnapshot form(long id, String publishedAt, Set<String> languages) {
        return new FormSnapshot(id, "v" + id, publishedAt == null ? null : Instant.parse(publishedAt), languages);
    }

    @Test
    void noFormsMeansNoCurrentForm() {
        assertThat(ConsentFormRules.current(List.of(), NOW)).isEmpty();
    }

    @Test
    void latestCompletePublishedWins() {
        FormSnapshot old = form(1, "2026-01-01T00:00:00Z", ALL);
        FormSnapshot newer = form(2, "2026-06-01T00:00:00Z", ALL);
        assertThat(ConsentFormRules.current(List.of(old, newer), NOW)).contains(newer);
        assertThat(ConsentFormRules.current(List.of(newer, old), NOW)).contains(newer);
    }

    @Test
    void tiesAreBrokenById() {
        FormSnapshot a = form(1, "2026-01-01T00:00:00Z", ALL);
        FormSnapshot b = form(2, "2026-01-01T00:00:00Z", ALL);
        assertThat(ConsentFormRules.current(List.of(a, b), NOW)).contains(b);
    }

    @Test
    void incompleteVersionIsSkippedAndItsMissingLanguagesReported() {
        FormSnapshot complete = form(1, "2026-01-01T00:00:00Z", ALL);
        FormSnapshot incomplete = form(2, "2026-06-01T00:00:00Z", Set.of("pt-BR", "en-US"));
        assertThat(ConsentFormRules.current(List.of(complete, incomplete), NOW)).contains(complete);
        assertThat(ConsentFormRules.missingLanguages(incomplete)).containsExactly("es-ES");
        assertThat(ConsentFormRules.incompletePublished(List.of(complete, incomplete), NOW))
                .containsExactly(incomplete);
    }

    @Test
    void futureOrUnpublishedFormsAreIgnored() {
        FormSnapshot future = form(1, "2027-01-01T00:00:00Z", ALL);
        FormSnapshot draft = form(2, null, ALL);
        assertThat(ConsentFormRules.current(List.of(future, draft), NOW)).isEmpty();
        assertThat(ConsentFormRules.incompletePublished(
                        List.of(form(3, "2027-01-01T00:00:00Z", Set.of("pt-BR")), form(4, null, Set.of())), NOW))
                .isEmpty();
    }

    @Test
    void publishedExactlyNowIsCurrent() {
        FormSnapshot now = form(1, "2026-11-15T12:00:00Z", ALL);
        assertThat(ConsentFormRules.current(List.of(now), NOW)).contains(now);
    }

    @Test
    void missingLanguagesFollowTheFixedOrder() {
        assertThat(ConsentFormRules.missingLanguages(form(1, null, Set.of()))).containsExactly("pt-BR", "en-US", "es-ES");
    }
}
