package io.github.diegoquirino.veredito.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.access.TokenHasher;
import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.access.Pseudonym;
import io.github.diegoquirino.veredito.domain.access.Subject;
import io.github.diegoquirino.veredito.persistence.AccessSessionRepository.ValidSession;
import io.github.diegoquirino.veredito.persistence.ConfigRepository.AdminProfile;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import io.github.diegoquirino.veredito.support.MySqlContainerConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** The repositories of spec 004 against MySQL 9.7 (research sections 4, 5 and 8). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MySqlContainerConfig.class)
class AccessRepositoriesTest {

    private static final PersonName ANA = new PersonName("Ana Souza");

    @Autowired
    AccessCodeRepository codes;

    @Autowired
    AccessSessionRepository sessions;

    @Autowired
    ParticipantRepository participants;

    @Autowired
    ConfigRepository config;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    JdbcClient jdbc;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Test
    void onlyTheLatestCodeOfAnEmailIsReturned() {
        EmailAddress email = unique("codes");
        EmailAddress other = unique("other");
        codes.insert(email, ANA, "a".repeat(64), now.minusSeconds(120), now.plusSeconds(480));
        long latest = codes.insert(email, ANA, "b".repeat(64), now.minusSeconds(30), now.plusSeconds(570));
        codes.insert(other, ANA, "c".repeat(64), now, now.plusSeconds(600));

        var code = codes.latestFor(email).orElseThrow();
        assertThat(code.id()).isEqualTo(latest);
        assertThat(code.codeHash()).isEqualTo("b".repeat(64));
        assertThat(code.name()).isEqualTo("Ana Souza");
        assertThat(code.attempts()).isZero();
        assertThat(code.usedAt()).isNull();

        assertThat(codes.sendTimesSince(email, now.minus(Duration.ofHours(1))))
                .containsExactly(now.minusSeconds(120), now.minusSeconds(30));
        assertThat(codes.countAllSince(now.minusSeconds(200))).isGreaterThanOrEqualTo(3);

        codes.incrementAttempts(latest);
        assertThat(codes.markUsed(latest, now)).isTrue();
        assertThat(codes.markUsed(latest, now)).isFalse();
        code = codes.latestFor(email).orElseThrow();
        assertThat(code.attempts()).isEqualTo(1);
        assertThat(code.usedAt()).isEqualTo(now);
        assertThat(codes.latestFor(unique("nobody"))).isEmpty();
    }

    @Test
    void pseudonymsFollowTheIdsInOrderAndNeverChange() {
        ParticipantProfile first = signIn(unique("first"), ANA, "pt-BR");
        ParticipantProfile second = signIn(unique("second"), ANA, "pt-BR");

        assertThat(first.pseudonym()).isEqualTo(Pseudonym.of(first.id())).startsWith("R-");
        assertThat(second.pseudonym()).isEqualTo(Pseudonym.of(second.id()));
        assertThat(second.id()).isGreaterThan(first.id());
        assertThat(count("SELECT COUNT(*) FROM participant WHERE pseudonym LIKE '~%'")).isZero();
    }

    @Test
    void aReturningPersonKeepsThePseudonymAndGetsTheLatestName() {
        EmailAddress email = unique("again");
        ParticipantProfile first = signIn(email, ANA, "pt-BR");
        ParticipantProfile again = signIn(email, new PersonName("Ana S."), "en-US");

        assertThat(again.id()).isEqualTo(first.id());
        assertThat(again.pseudonym()).isEqualTo(first.pseudonym());
        assertThat(again.name()).isEqualTo("Ana S.");
        assertThat(again.registeredAt()).isEqualTo(first.registeredAt());
    }

    @Test
    void aReturningPersonLeavesNoGapInThePseudonyms() {
        EmailAddress email = unique("returning");
        ParticipantProfile first = signIn(email, ANA, "pt-BR");
        signIn(email, ANA, "pt-BR");
        signIn(email, ANA, "pt-BR");
        ParticipantProfile next = signIn(unique("next"), ANA, "pt-BR");

        assertThat(next.id()).isEqualTo(first.id() + 1);
        assertThat(next.pseudonym()).isEqualTo(Pseudonym.of(first.id() + 1));
    }

    @Test
    void preferredLanguageIsSetOnlyWhenNull() {
        EmailAddress email = unique("language");
        assertThat(signIn(email, ANA, "es-ES").preferredLanguage()).isEqualTo("es-ES");
        assertThat(signIn(email, ANA, "en-US").preferredLanguage()).isEqualTo("es-ES");

        transaction.executeWithoutResult(status -> {
            config.updateAdminSignIn(new PersonName("Diego Quirino"), "es-ES");
            config.updateAdminSignIn(new PersonName("Diego"), "en-US");
            AdminProfile admin = config.adminProfile();
            assertThat(admin.name()).isEqualTo("Diego");
            assertThat(admin.preferredLanguage()).isEqualTo("es-ES");
            assertThat(admin.email()).isEqualTo(config.adminEmail());
            status.setRollbackOnly();
        });
    }

    @Test
    void twoConcurrentFirstSignInsOfTheSameEmailMakeOneParticipant() throws Exception {
        EmailAddress email = unique("race");
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<ParticipantProfile>> racers = List.of(
                CompletableFuture.supplyAsync(() -> afterLatch(start, email)),
                CompletableFuture.supplyAsync(() -> afterLatch(start, email)));
        start.countDown();
        ParticipantProfile one = racers.get(0).get(30, TimeUnit.SECONDS);
        ParticipantProfile two = racers.get(1).get(30, TimeUnit.SECONDS);

        assertThat(one.id()).isEqualTo(two.id());
        assertThat(one.pseudonym()).isEqualTo(two.pseudonym()).isEqualTo(Pseudonym.of(one.id()));
        assertThat(count("SELECT COUNT(*) FROM participant WHERE email = '" + email.value() + "'")).isEqualTo(1);
    }

    @Test
    void sessionsAreFoundUntilTheyExpire() {
        ParticipantProfile participant = signIn(unique("session"), ANA, "pt-BR");
        String participantHash = new TokenHasher().hash("participant-" + System.nanoTime());
        String adminHash = new TokenHasher().hash("administrator-" + System.nanoTime());
        Instant expires = now.plus(Duration.ofHours(24));
        sessions.insert(participantHash, Subject.PARTICIPANT, participant.id(), now, expires);
        sessions.insert(adminHash, Subject.ADMINISTRATOR, null, now, expires);

        assertThat(sessions.findValid(participantHash, now))
                .contains(new ValidSession(Subject.PARTICIPANT, participant.id(), participant.pseudonym(), expires));
        assertThat(sessions.findValid(adminHash, now))
                .contains(new ValidSession(Subject.ADMINISTRATOR, null, null, expires));
        assertThat(sessions.findValid(adminHash, expires)).isEmpty();

        assertThat(sessions.delete(adminHash)).isEqualTo(1);
        assertThat(sessions.findValid(adminHash, now)).isEmpty();
        assertThat(sessions.deleteExpired(expires)).isGreaterThanOrEqualTo(1);
        assertThat(sessions.findValid(participantHash, now)).isEmpty();
    }

    private ParticipantProfile signIn(EmailAddress email, PersonName name, String language) {
        return transaction.execute(status -> participants.upsertOnSignIn(email, name, language, now));
    }

    private ParticipantProfile afterLatch(CountDownLatch start, EmailAddress email) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return signIn(email, ANA, "pt-BR");
    }

    private int count(String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }

    private static EmailAddress unique(String prefix) {
        return EmailAddress.parse(prefix + "-" + System.nanoTime() + "@example.com").orElseThrow();
    }
}
