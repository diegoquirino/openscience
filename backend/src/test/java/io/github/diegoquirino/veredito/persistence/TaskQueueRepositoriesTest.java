package io.github.diegoquirino.veredito.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import io.github.diegoquirino.veredito.domain.access.PersonName;
import io.github.diegoquirino.veredito.domain.queue.NextTaskChooser;
import io.github.diegoquirino.veredito.persistence.ItemRepository.NewItem;
import io.github.diegoquirino.veredito.persistence.ParticipantRepository.ParticipantProfile;
import io.github.diegoquirino.veredito.support.MySqlContainerConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** Repositories of spec 007 against MySQL 9.7 (reserve, draft, finalize). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MySqlContainerConfig.class)
class TaskQueueRepositoriesTest {

    @Autowired
    ImportBatchRepository batches;

    @Autowired
    BlockRepository blocks;

    @Autowired
    ItemRepository items;

    @Autowired
    TaskRepository tasks;

    @Autowired
    ResponseRepository responses;

    @Autowired
    ParticipantRepository participants;

    @Autowired
    ConfigRepository config;

    @Autowired
    TransactionTemplate transaction;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void clearStudyData() {
        // The MySQL container is shared by the whole run: other tests leave free tasks behind.
        jdbc.sql("DELETE FROM response").update();
        jdbc.sql("DELETE FROM task").update();
        jdbc.sql("DELETE FROM item").update();
        jdbc.sql("DELETE FROM block").update();
        jdbc.sql("DELETE FROM import_batch").update();
        jdbc.sql("DELETE FROM import_preview").update();
    }

    @Test
    void reserveWithSkipLockedUpsertsDraftAndFinalizes() {
        transaction.executeWithoutResult(status -> {
            config.lockForUpdate();
            int minutes = config.reservationMinutes();
            assertThat(minutes).isEqualTo(20);

            long batchId = batches.insert("queue-repos.csv", now, 1, 0, 0);
            long blockId = blocks.insert(batchId, blocks.maxNumber() + 1, 1);
            int queue = tasks.maxQueuePosition() + 1;
            long taskId = tasks.insertFree(blockId, 1, queue);
            tasks.insertFree(blockId, 2, queue + 1);
            items.insert(List.of(new NewItem(
                    "minitest",
                    "repo-edit-1",
                    "UC01",
                    "MOD",
                    "step",
                    "before",
                    "after",
                    "1.0",
                    "1.1",
                    "uc before",
                    "uc after",
                    batchId,
                    blockId,
                    1)));

            ParticipantProfile person = participants.upsertOnSignIn(
                    EmailAddress.parse("queue-repos@example.com").orElseThrow(),
                    PersonName.parse("Queue Repos").orElseThrow(),
                    "en-US",
                    now);

            List<NextTaskChooser.Candidate> candidates = tasks.lockCandidatesForUpdateSkipLocked(now);
            assertThat(candidates).isNotEmpty();
            Optional<NextTaskChooser.Candidate> chosen =
                    NextTaskChooser.choose(candidates, tasks.heldBlockIds(person.id()), now);
            assertThat(chosen).isPresent();
            assertThat(chosen.get().taskId()).isEqualTo(taskId);

            Instant expires = now.plus(minutes, ChronoUnit.MINUTES);
            tasks.reserve(taskId, person.id(), now, expires);

            long itemId = items.listByBlockOrdered(blockId).getFirst().id();
            responses.upsertDraft(taskId, itemId, person.id(), "LOW", "KEEP", null, "en-US", now);
            assertThat(responses.countByTaskId(taskId)).isEqualTo(1);
            responses.upsertDraft(taskId, itemId, person.id(), "HIGH", "UPDATE", "note", "pt-BR", now);
            assertThat(responses.countByTaskId(taskId)).isEqualTo(1);
            assertThat(responses.findByTaskAndItem(taskId, itemId).orElseThrow().changeImpact())
                    .isEqualTo("HIGH");

            responses.finalizeAll(taskId);
            tasks.complete(taskId, now);
            assertThat(tasks.findById(taskId).orElseThrow().status()).isEqualTo("completed");
            assertThat(responses.findByTaskAndItem(taskId, itemId).orElseThrow().state()).isEqualTo("final");
        });
    }
}
