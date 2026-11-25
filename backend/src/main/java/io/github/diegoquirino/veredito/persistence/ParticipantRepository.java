package io.github.diegoquirino.veredito.persistence;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.access.Pseudonym;
import io.github.diegoquirino.veredito.domain.onboarding.ConsentDecision;
import io.github.diegoquirino.veredito.domain.onboarding.OnboardingState;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningAnswer;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The participant data that sign-in reads and writes (research section 8). */
@Repository
public class ParticipantRepository {

    /** Placeholder pseudonyms start with a character that {@link Pseudonym} never uses. */
    private static final String PLACEHOLDER_PREFIX = "~";

    private static final String SELECT =
            "SELECT id, pseudonym, name, registered_at, preferred_language FROM participant";

    private final JdbcClient jdbc;
    private final SecureRandom random = new SecureRandom();

    public ParticipantRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record ParticipantProfile(
            long id, String pseudonym, String name, Instant registeredAt, String preferredLanguage) {}

    /**
     * Creates the participant at the first sign-in or refreshes language (and name when provided);
     * must run in a transaction. A null name leaves an existing name untouched and inserts null for a
     * first visit (profile step of spec 013).
     */
    public ParticipantProfile upsertOnSignIn(EmailAddress email, PersonName name, String language, Instant now) {
        // InnoDB spends an auto-increment value on every INSERT ... ON DUPLICATE KEY UPDATE, even when it
        // updates; a returning person must not leave a gap in the pseudonyms (FR-006). The plain read takes
        // no gap lock, so two concurrent first sign-ins still meet in the upsert below.
        Optional<Long> existing = jdbc.sql("SELECT id FROM participant WHERE email = ?")
                .param(email.value())
                .query(Long.class)
                .optional();
        if (existing.isPresent()) {
            if (name != null) {
                jdbc.sql("""
                                UPDATE participant SET name = ?, preferred_language = COALESCE(preferred_language, ?)
                                WHERE id = ?""")
                        .params(name.value(), language, existing.get())
                        .update();
            } else {
                jdbc.sql("""
                                UPDATE participant SET preferred_language = COALESCE(preferred_language, ?)
                                WHERE id = ?""")
                        .params(language, existing.get())
                        .update();
            }
            return jdbc.sql(SELECT + " WHERE id = ?")
                    .param(existing.get())
                    .query(ParticipantRepository::profile)
                    .single();
        }
        String placeholder = placeholder();
        jdbc.sql("""
                        INSERT INTO participant (pseudonym, name, email, registered_at, preferred_language)
                        VALUES (?, ?, ?, ?, ?) AS typed
                        ON DUPLICATE KEY UPDATE
                            name = COALESCE(typed.name, participant.name),
                            preferred_language = COALESCE(participant.preferred_language, typed.preferred_language)""")
                .params(
                        placeholder,
                        name == null ? null : name.value(),
                        email.value(),
                        Timestamp.from(now),
                        language)
                .update();
        ParticipantProfile row = jdbc.sql(SELECT + " WHERE email = ? FOR UPDATE")
                .param(email.value())
                .query(ParticipantRepository::profile)
                .single();
        if (!placeholder.equals(row.pseudonym())) {
            return row;
        }
        String pseudonym = Pseudonym.of(row.id());
        jdbc.sql("UPDATE participant SET pseudonym = ? WHERE id = ?").params(pseudonym, row.id()).update();
        return new ParticipantProfile(row.id(), pseudonym, row.name(), row.registeredAt(), row.preferredLanguage());
    }

    public Optional<ParticipantProfile> findById(long id) {
        return jdbc.sql(SELECT + " WHERE id = ?").param(id).query(ParticipantRepository::profile).optional();
    }

    public void updatePreferredLanguage(long id, String language) {
        jdbc.sql("UPDATE participant SET preferred_language = ? WHERE id = ?").params(language, id).update();
    }

    private static final String ONBOARDING_SELECT = """
            SELECT consent_form_id, consent_decision, consent_language, consent_decided_at,
                   professional_skill, professional_skill_other, organization, organization_other,
                   screening_answer
            FROM participant WHERE id = ?""";

    /** The consent, skill and screening columns of one participant (spec 006). */
    public Optional<OnboardingState> findOnboardingState(long id) {
        return jdbc.sql(ONBOARDING_SELECT).param(id).query(ParticipantRepository::onboarding).optional();
    }

    /** Same as {@link #findOnboardingState} but locks the row; the caller must be in a transaction. */
    public Optional<OnboardingState> lockOnboardingState(long id) {
        return jdbc.sql(ONBOARDING_SELECT + " FOR UPDATE")
                .param(id)
                .query(ParticipantRepository::onboarding)
                .optional();
    }

    /** Writes the consent columns together ({@code ck_participant_consent_complete}) plus acknowledgment preference. */
    public void recordConsent(
            long id,
            long formId,
            ConsentDecision decision,
            String language,
            Instant decidedAt,
            boolean acknowledgmentAnonymous) {
        jdbc.sql("""
                        UPDATE participant
                        SET consent_form_id = ?, consent_decision = ?, consent_language = ?, consent_decided_at = ?,
                            acknowledgment_anonymous = ?
                        WHERE id = ?""")
                .params(
                        formId,
                        decision.code(),
                        language,
                        Timestamp.from(decidedAt),
                        acknowledgmentAnonymous,
                        id)
                .update();
    }

    /** Whether paper acknowledgments should omit this participant's name. */
    public boolean acknowledgmentAnonymous(long id) {
        Boolean value = jdbc.sql("SELECT acknowledgment_anonymous FROM participant WHERE id = ?")
                .param(id)
                .query(Boolean.class)
                .optional()
                .orElse(Boolean.FALSE);
        return Boolean.TRUE.equals(value);
    }

    /** Stores the skill once; returns 0 when it was already answered. */
    public int recordSkill(long id, String skill, String other) {
        return jdbc.sql("""
                        UPDATE participant SET professional_skill = ?, professional_skill_other = ?
                        WHERE id = ? AND professional_skill IS NULL""")
                .params(skill, other, id)
                .update();
    }

    /** Sets name, skill and organization together (profile step and later edits). */
    public void updateProfile(
            long id, String name, String skill, String skillOther, String organization, String organizationOther) {
        jdbc.sql("""
                        UPDATE participant
                        SET name = ?, professional_skill = ?, professional_skill_other = ?,
                            organization = ?, organization_other = ?
                        WHERE id = ?""")
                .params(name, skill, skillOther, organization, organizationOther, id)
                .update();
    }

    /** Name, e-mail and inicial-invite marker for the post-profile send (spec 031). */
    public record InviteRecipient(String name, String email, Instant inicialSentAt) {}

    public Optional<InviteRecipient> findInviteRecipient(long id) {
        return jdbc.sql("""
                        SELECT name, email, invite_inicial_sent_at FROM participant WHERE id = ?""")
                .param(id)
                .query((rs, row) -> new InviteRecipient(
                        rs.getString("name"),
                        rs.getString("email"),
                        instant(rs.getTimestamp("invite_inicial_sent_at"))))
                .optional();
    }

    /** Claims the inicial send slot; returns false when already sent or the row is missing. */
    public boolean claimInviteInicial(long id, Instant now) {
        return jdbc.sql("""
                        UPDATE participant SET invite_inicial_sent_at = ?
                        WHERE id = ? AND invite_inicial_sent_at IS NULL""")
                .params(Timestamp.from(now), id)
                .update()
                == 1;
    }

    /** Clears the claim so a later profile touch can retry after SMTP failure. */
    public void clearInviteInicial(long id) {
        jdbc.sql("UPDATE participant SET invite_inicial_sent_at = NULL WHERE id = ?")
                .param(id)
                .update();
    }

    /** Eligible bulk invite targets (spec 033): e-mail present, not anonymized. */
    public record BulkInviteRecipient(long id, String name, String email) {}

    public java.util.List<BulkInviteRecipient> findPendingLembrete() {
        return findPendingBulk("invite_lembrete_sent_at");
    }

    public java.util.List<BulkInviteRecipient> findPendingAgradecimento() {
        return findPendingBulk("invite_agradecimento_sent_at");
    }

    public boolean claimInviteLembrete(long id, Instant now) {
        return claimInviteColumn("invite_lembrete_sent_at", id, now);
    }

    public boolean claimInviteAgradecimento(long id, Instant now) {
        return claimInviteColumn("invite_agradecimento_sent_at", id, now);
    }

    public void clearInviteLembrete(long id) {
        clearInviteColumn("invite_lembrete_sent_at", id);
    }

    public void clearInviteAgradecimento(long id) {
        clearInviteColumn("invite_agradecimento_sent_at", id);
    }

    private java.util.List<BulkInviteRecipient> findPendingBulk(String sentAtColumn) {
        // Column name is one of two fixed literals from callers above — never user input.
        return jdbc.sql("""
                        SELECT id, name, email FROM participant
                        WHERE email IS NOT NULL AND TRIM(email) <> ''
                          AND anonymized_at IS NULL
                          AND %s IS NULL
                        ORDER BY id""".formatted(sentAtColumn))
                .query((rs, row) -> new BulkInviteRecipient(
                        rs.getLong("id"), rs.getString("name"), rs.getString("email")))
                .list();
    }

    private boolean claimInviteColumn(String column, long id, Instant now) {
        return jdbc.sql("""
                        UPDATE participant SET %s = ?
                        WHERE id = ? AND %s IS NULL""".formatted(column, column))
                .params(Timestamp.from(now), id)
                .update()
                == 1;
    }

    private void clearInviteColumn(String column, long id) {
        jdbc.sql("UPDATE participant SET %s = NULL WHERE id = ?".formatted(column))
                .param(id)
                .update();
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    /**
     * Distinct {@code organization_other} values already saved by other participants with
     * {@code organization = other} (spec 025 suggestions).
     */
    public java.util.List<String> organizationSuggestions(long excludeParticipantId) {
        return jdbc.sql("""
                        SELECT DISTINCT organization_other FROM participant
                        WHERE organization = 'other'
                          AND organization_other IS NOT NULL
                          AND TRIM(organization_other) <> ''
                          AND id <> ?
                        ORDER BY organization_other""")
                .param(excludeParticipantId)
                .query(String.class)
                .list();
    }

    /** Stores the screening answer and the eligibility once; returns 0 when it was already answered. */
    public int recordScreening(long id, ScreeningAnswer answer) {
        return jdbc.sql("""
                        UPDATE participant SET screening_answer = ?, eligible = ?
                        WHERE id = ? AND screening_answer IS NULL""")
                .params(answer.code(), answer.eligible(), id)
                .update();
    }

    private static OnboardingState onboarding(ResultSet rs, int row) throws SQLException {
        long formId = rs.getLong("consent_form_id");
        Long consentFormId = rs.wasNull() ? null : formId;
        Timestamp decided = rs.getTimestamp("consent_decided_at");
        return new OnboardingState(
                consentFormId,
                ConsentDecision.fromCode(rs.getString("consent_decision")).orElse(null),
                rs.getString("consent_language"),
                decided == null ? null : decided.toInstant(),
                rs.getString("professional_skill"),
                rs.getString("professional_skill_other"),
                rs.getString("organization"),
                rs.getString("organization_other"),
                ScreeningAnswer.fromCode(rs.getString("screening_answer")).orElse(null));
    }
    private String placeholder() {
        byte[] bytes = new byte[7];
        random.nextBytes(bytes);
        return PLACEHOLDER_PREFIX + HexFormat.of().formatHex(bytes);
    }

    private static ParticipantProfile profile(ResultSet rs, int row) throws SQLException {
        return new ParticipantProfile(
                rs.getLong("id"),
                rs.getString("pseudonym"),
                rs.getString("name"),
                rs.getTimestamp("registered_at").toInstant(),
                rs.getString("preferred_language"));
    }
}
