package io.github.diegoquirino.veredito.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.onboarding.ConsentDecision;
import io.github.diegoquirino.veredito.domain.onboarding.ConsentFormRules;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingState;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import io.github.diegoquirino.veredito.persistence.ConsentFormRepository.ConsentFormRow;
import io.github.diegoquirino.veredito.support.MySqlContainerConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** Spec 006 T004 and T007: the staging fixture, the current-form query and the onboarding columns (MySQL 9.7). */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.flyway.locations=classpath:db/migration,classpath:db/staging")
@Import(MySqlContainerConfig.class)
@Transactional
class ConsentFormRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ConsentFormRepository forms;

    @Autowired
    ParticipantRepository participants;

    @Test
    void stagingLocationAddsV6AndTheFixtureForm() {
        List<String> versions = jdbc.sql("""
                        SELECT version FROM flyway_schema_history
                        WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank""")
                .query(String.class).list();
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8");

        ConsentFormRow current = forms.findCurrent(NOW).orElseThrow();
        assertThat(current.version()).isEqualTo("fixture-1");
        assertThat(current.referenceLanguage()).isEqualTo("pt-BR");
        assertThat(current.publishedAt()).isEqualTo(Instant.parse("2000-01-01T00:00:00Z"));

        for (String language : ConsentFormRules.REQUIRED_LANGUAGES) {
            String text = forms.findText("fixture-1", language, NOW).orElseThrow().text();
            assertThat(text).contains("FIXTURE").contains("CEP");
        }
        assertThat(forms.findText("fixture-1", "pt-BR", NOW).orElseThrow().text()).contains("NÃO APROVADO PELO CEP");
        assertThat(forms.findText("fixture-1", "es-ES", NOW).orElseThrow().text()).contains("traducción");
    }

    @Test
    void laterCompleteVersionSupersedesTheFixture() {
        insertForm("v1", "2026-06-01T00:00:00Z", "pt-BR", "en-US", "es-ES");
        assertThat(forms.findCurrent(NOW).orElseThrow().version()).isEqualTo("v1");
    }

    @Test
    void incompletePublishedVersionIsSkippedAndListed() {
        insertForm("v2", "2026-06-01T00:00:00Z", "pt-BR", "en-US");
        assertThat(forms.findCurrent(NOW).orElseThrow().version()).isEqualTo("fixture-1");
        assertThat(forms.listIncompletePublished(NOW)).extracting(ConsentFormRules.FormSnapshot::version)
                .containsExactly("v2");
        assertThat(forms.findText("v2", "pt-BR", NOW)).isEmpty();
    }

    @Test
    void futureOrUnpublishedVersionsAreNotCurrent() {
        insertForm("v3", "2027-01-01T00:00:00Z", "pt-BR", "en-US", "es-ES");
        insertForm("v4", null, "pt-BR", "en-US", "es-ES");
        assertThat(forms.findCurrent(NOW).orElseThrow().version()).isEqualTo("fixture-1");
        assertThat(forms.findCurrent(Instant.parse("2027-02-01T00:00:00Z")).orElseThrow().version()).isEqualTo("v3");
        assertThat(forms.findText("v3", "en-US", NOW)).isEmpty();
        assertThat(forms.findText("v4", "en-US", NOW)).isEmpty();
    }

    @Test
    void noPublishedFormMeansNoCurrentForm() {
        jdbc.sql("UPDATE consent_form SET published_at = NULL").update();
        assertThat(forms.findCurrent(NOW)).isEmpty();
    }

    @Test
    void sqlAndJavaRulesAgreeOnTheCurrentForm() {
        insertForm("v1", "2026-06-01T00:00:00Z", "pt-BR", "en-US", "es-ES");
        insertForm("v2", "2026-09-01T00:00:00Z", "pt-BR");
        insertForm("v3", "2026-10-01T00:00:00Z", "pt-BR", "en-US", "es-ES");
        insertForm("v4", "2027-03-01T00:00:00Z", "pt-BR", "en-US", "es-ES");

        String bySql = forms.findCurrent(NOW).orElseThrow().version();
        String byRules = ConsentFormRules.current(forms.listSnapshots(), NOW).orElseThrow().version();
        assertThat(bySql).isEqualTo("v3").isEqualTo(byRules);
    }

    @Test
    void unknownLanguageOrVersionHasNoText() {
        assertThat(forms.findText("fixture-1", "fr-FR", NOW)).isEmpty();
        assertThat(forms.findText("nope", "pt-BR", NOW)).isEmpty();
    }

    @Test
    void onboardingColumnsAreWrittenTogetherAndOnlyOnceForSkillAndScreening() {
        long id = newParticipant();
        assertThat(participants.findOnboardingState(id)).contains(OnboardingState.empty());

        long formId = forms.findCurrent(NOW).orElseThrow().id();
        participants.recordConsent(id, formId, ConsentDecision.REFUSED, "en-US", NOW);
        OnboardingState refused = participants.findOnboardingState(id).orElseThrow();
        assertThat(refused.consentDecision()).isEqualTo(ConsentDecision.REFUSED);
        assertThat(refused.consentFormId()).isEqualTo(formId);
        assertThat(refused.consentLanguage()).isEqualTo("en-US");
        assertThat(refused.consentDecidedAt()).isEqualTo(NOW);

        participants.recordConsent(id, formId, ConsentDecision.ACCEPTED, "es-ES", NOW.plusSeconds(60));
        assertThat(participants.lockOnboardingState(id).orElseThrow().consentDecision())
                .isEqualTo(ConsentDecision.ACCEPTED);

        assertThat(participants.recordSkill(id, "other", "  Writer  ")).isEqualTo(1);
        assertThat(participants.recordSkill(id, "developer", null)).isZero();
        OnboardingState skilled = participants.findOnboardingState(id).orElseThrow();
        assertThat(skilled.professionalSkill()).isEqualTo("other");
        assertThat(skilled.professionalSkillOther()).isEqualTo("  Writer  ");

        assertThat(participants.recordScreening(id, ScreeningAnswer.YES_ACADEMIC)).isEqualTo(1);
        assertThat(participants.recordScreening(id, ScreeningAnswer.NO)).isZero();
        assertThat(participants.findOnboardingState(id).orElseThrow().screeningAnswer())
                .isEqualTo(ScreeningAnswer.YES_ACADEMIC);
        assertThat(jdbc.sql("SELECT eligible FROM participant WHERE id = ?").param(id).query(Boolean.class).single())
                .isTrue();
    }

    private long newParticipant() {
        EmailAddress email = EmailAddress.parse("repo-" + System.nanoTime() + "@example.com").orElseThrow();
        return participants.upsertOnSignIn(email, new PersonName("Repo Test"), "pt-BR", NOW).id();
    }

    private void insertForm(String version, String publishedAt, String... languages) {
        jdbc.sql("""
                        INSERT INTO consent_form (version, reference_language, created_at, published_at)
                        VALUES (?, 'pt-BR', ?, ?)""")
                .params(version, Timestamp.from(NOW), publishedAt == null ? null : Timestamp.from(Instant.parse(publishedAt)))
                .update();
        long id = jdbc.sql("SELECT id FROM consent_form WHERE version = ?").param(version).query(Long.class).single();
        for (String language : Set.of(languages)) {
            jdbc.sql("INSERT INTO consent_form_text (consent_form_id, language, text) VALUES (?, ?, ?)")
                    .params(id, language, "Text of " + version + " in " + language)
                    .update();
        }
    }
}
