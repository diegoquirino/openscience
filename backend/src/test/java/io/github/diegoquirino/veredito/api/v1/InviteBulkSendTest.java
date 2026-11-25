package io.github.diegoquirino.veredito.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.invite.InviteBulkSendJob;
import io.github.diegoquirino.veredito.support.ApiTestBase;
import io.github.diegoquirino.veredito.support.MailpitClient.Message;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Spec 033: bulk lembrete / agradecimento when the clock reaches configured moments. */
class InviteBulkSendTest extends ApiTestBase {

    @Autowired
    InviteBulkSendJob bulkSend;

    @BeforeEach
    void clearMailbox() {
        mailpit.clear();
    }

    @Test
    void lembreteAndAgradecimentoGoOutOnceWhenDue() {
        String emailA = "bulk-a-" + System.nanoTime() + "@example.com";
        String emailB = "bulk-b-" + System.nanoTime() + "@example.com";
        signIn("ignored", emailA, "pt-BR");
        signIn("ignored", emailB, "pt-BR");
        // Sign-in is e-mail only (013); names come from the profile step.
        jdbc.sql("UPDATE participant SET name = ? WHERE email = ?")
                .params("Ana Bulk", normalized(emailA))
                .update();
        jdbc.sql("UPDATE participant SET name = ? WHERE email = ?")
                .params("Bruno Bulk", normalized(emailB))
                .update();
        mailpit.clear();

        Instant reminder = clock.instant().plusSeconds(60);
        Instant end = clock.instant().plusSeconds(120);
        String admin = administratorSession();
        String href = href(as(admin).get("/api/v1"), "email-settings");
        as(admin).contentType("application/json")
                .body(Map.of(
                        "caae", "CAAE bulk",
                        "platformUrl", "https://diegoquirino.cloud",
                        "collectionStartAt", clock.instant().minusSeconds(3600).toString(),
                        "collectionEndAt", end.toString(),
                        "reminderAt", reminder.toString()))
                .put(href)
                .then()
                .statusCode(200);

        assertThat(bulkSend.run().lembrete()).isZero();
        assertThat(mailpit.countTo(emailA)).isZero();

        clock.advance(java.time.Duration.ofSeconds(90));
        InviteBulkSendJob.Sent afterReminder = bulkSend.run();
        // Staging seed participants are also eligible ("everyone with e-mail").
        assertThat(afterReminder.lembrete()).isGreaterThanOrEqualTo(2);
        assertThat(afterReminder.agradecimento()).isZero();

        Message lembreteA = mailpit.latestTo(emailA);
        assertThat(lembreteA.subject()).containsIgnoringCase("Lembrete");
        assertThat(lembreteA.text()).contains("Ana Bulk");
        assertThat(mailpit.latestTo(emailB).text()).contains("Bruno Bulk");

        assertThat(bulkSend.run().lembrete()).isZero();
        assertThat(mailpit.countTo(emailA)).isEqualTo(1);

        clock.advance(java.time.Duration.ofSeconds(60));
        InviteBulkSendJob.Sent afterEnd = bulkSend.run();
        assertThat(afterEnd.agradecimento()).isGreaterThanOrEqualTo(2);
        Message thanks = mailpit.latestTo(emailA);
        assertThat(thanks.subject()).containsIgnoringCase("obrigado");
        assertThat(bulkSend.run().agradecimento()).isZero();
        assertThat(mailpit.countTo(emailA)).isEqualTo(2);
    }
}
