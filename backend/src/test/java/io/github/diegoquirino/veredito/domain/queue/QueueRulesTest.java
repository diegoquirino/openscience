package io.github.diegoquirino.veredito.domain.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Domain rules of spec 007 / 042 without Spring. */
class QueueRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

    @Nested
    class Chooser {

        @Test
        void firstCallerGetsFirstFreeTask() {
            var chosen = NextTaskChooser.choose(queue(), Set.of(), Set.of(), NOW);
            assertThat(chosen).isPresent();
            assertThat(chosen.get().taskId()).isEqualTo(1L);
            assertThat(chosen.get().role()).isEqualTo(1);
        }

        @Test
        void secondCallerGetsOtherRoleOfSameBlock() {
            var chosen = NextTaskChooser.choose(queue(), Set.of(), Set.of(), NOW);
            assertThat(chosen).isPresent();
            var remaining = queue().stream().filter(c -> c.taskId() != chosen.get().taskId()).toList();
            var second = NextTaskChooser.choose(remaining, Set.of(), Set.of(), NOW);
            assertThat(second).isPresent();
            assertThat(second.get().taskId()).isEqualTo(2L);
            assertThat(second.get().role()).isEqualTo(2);
        }

        @Test
        void neverGivesOtherRoleOfAHeldBlock() {
            var chosen = NextTaskChooser.choose(queue(), Set.of(1L), Set.of(), NOW);
            assertThat(chosen).isPresent();
            assertThat(chosen.get().blockId()).isEqualTo(2L);
            assertThat(chosen.get().role()).isEqualTo(1);
        }

        @Test
        void skipsBlocksOfAHeldSystemEvenWhenBlockIdIsNew() {
            var chosen = NextTaskChooser.choose(queue(), Set.of(), Set.of("minitest"), NOW);
            assertThat(chosen).isEmpty();
        }

        @Test
        void allowsAnotherSystemWhenOneIsHeld() {
            List<NextTaskChooser.Candidate> candidates = List.of(
                    free(1L, 1L, 1, 1, "minitest"),
                    free(2L, 2L, 1, 2, "itrust"));
            var chosen = NextTaskChooser.choose(candidates, Set.of(1L), Set.of("minitest"), NOW);
            assertThat(chosen).isPresent();
            assertThat(chosen.get().taskId()).isEqualTo(2L);
            assertThat(chosen.get().systems()).containsExactly("itrust");
        }

        @Test
        void skipsUnexpiredReservationsOfOthers() {
            List<NextTaskChooser.Candidate> candidates = List.of(
                    reserved(1L, 1L, 1, 1, NOW.plusSeconds(60), "minitest"),
                    free(2L, 1L, 2, 2, "minitest"));
            var chosen = NextTaskChooser.choose(candidates, Set.of(), Set.of(), NOW);
            assertThat(chosen).isPresent();
            assertThat(chosen.get().taskId()).isEqualTo(2L);
        }

        @Test
        void takesExpiredReservationAtItsQueuePosition() {
            List<NextTaskChooser.Candidate> candidates = List.of(
                    reserved(1L, 1L, 1, 1, NOW.minusSeconds(1), "minitest"),
                    free(2L, 1L, 2, 2, "minitest"));
            var chosen = NextTaskChooser.choose(candidates, Set.of(), Set.of(), NOW);
            assertThat(chosen).isPresent();
            assertThat(chosen.get().taskId()).isEqualTo(1L);
        }

        @Test
        void returnsEmptyWhenNothingAvailable() {
            assertThat(NextTaskChooser.choose(List.of(), Set.of(), Set.of(), NOW)).isEmpty();
            assertThat(NextTaskChooser.choose(queue(), Set.of(1L, 2L, 3L, 4L), Set.of(), NOW)).isEmpty();
        }

        private static List<NextTaskChooser.Candidate> queue() {
            return List.of(
                    free(1L, 1L, 1, 1, "minitest"),
                    free(2L, 1L, 2, 2, "minitest"),
                    free(3L, 2L, 1, 3, "minitest"),
                    free(4L, 2L, 2, 4, "minitest"));
        }

        private static NextTaskChooser.Candidate free(
                long taskId, long blockId, int role, int position, String system) {
            return new NextTaskChooser.Candidate(
                    taskId, blockId, role, position, "free", null, null, Set.of(system));
        }

        private static NextTaskChooser.Candidate reserved(
                long taskId, long blockId, int role, int position, Instant expiresAt, String system) {
            return new NextTaskChooser.Candidate(
                    taskId, blockId, role, position, "reserved", 99L, expiresAt, Set.of(system));
        }
    }

    @Nested
    class Codes {

        @Test
        void acceptsValidDraft() {
            assertThat(ResponseCodes.firstError("LOW", "KEEP", null)).isEmpty();
            assertThat(ResponseCodes.firstError("HIGH", "FLAG", "ok")).isEmpty();
        }

        @Test
        void refusesInvalidImpactOrOperation() {
            assertThat(ResponseCodes.firstError("MEDIUM", "KEEP", null)).contains("invalid-change-impact");
            assertThat(ResponseCodes.firstError("LOW", "PATCH", null)).contains("invalid-maintenance-operation");
        }

        @Test
        void refusesCommentOverLimit() {
            assertThat(ResponseCodes.firstError("LOW", "KEEP", "x".repeat(1001))).contains("comment-too-long");
            assertThat(ResponseCodes.firstError("LOW", "KEEP", "x".repeat(1000))).isEmpty();
        }
    }
}
