package io.github.diegoquirino.veredito.domain.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.domain.dashboard.DashboardProgress.BlockFact;
import io.github.diegoquirino.veredito.domain.dashboard.DashboardProgress.Result;
import io.github.diegoquirino.veredito.domain.dashboard.DashboardProgress.TaskFact;
import io.github.diegoquirino.veredito.domain.dashboard.ParticipantTotals.Contributor;
import io.github.diegoquirino.veredito.domain.dashboard.ParticipantTotals.Fact;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Domain rules of spec 010 without Spring. */
class DashboardRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    @Nested
    class TaskBuckets {

        @Test
        void freeTasksAreFree() {
            assertThat(TaskBucket.of("free", null, NOW)).isEqualTo(TaskBucket.FREE);
        }

        @Test
        void aReservationInTheFutureIsInProgress() {
            assertThat(TaskBucket.of("reserved", NOW.plusSeconds(1), NOW)).isEqualTo(TaskBucket.IN_PROGRESS);
        }

        @Test
        void aReservationExpiringExactlyNowIsFreeAgain() {
            assertThat(TaskBucket.of("reserved", NOW, NOW)).isEqualTo(TaskBucket.FREE);
            assertThat(TaskBucket.of("reserved", NOW.minusSeconds(1), NOW)).isEqualTo(TaskBucket.FREE);
        }

        @Test
        void completedTasksAreCompleted() {
            assertThat(TaskBucket.of("completed", null, NOW)).isEqualTo(TaskBucket.COMPLETED);
        }
    }

    @Nested
    class Counts {

        @ParameterizedTest
        @CsvSource({"0,0,0,0.0", "3,0,1,25.0", "1,1,1,33.3", "2,0,1,33.3", "0,0,7,100.0", "5,5,0,0.0", "1,0,2,66.7"})
        void completedPercentHasOneDecimalAndIsZeroWithoutTasks(long free, long inProgress, long completed, double expected) {
            assertThat(TaskCounts.of(free, inProgress, completed).completedPercent()).isEqualTo(expected);
        }
    }

    @Nested
    class Progress {

        private final List<BlockFact> blocks = List.of(
                new BlockFact("minitest", 1, "a.csv", 10, 10),
                new BlockFact("minitest", 1, "a.csv", 11, 10),
                new BlockFact("other", 2, "b.csv", 12, 4));

        private TaskFact task(long block, int role, int position, String status, Instant expiresAt) {
            return new TaskFact(block, role, position, status, expiresAt);
        }

        @Test
        void overallRowFirstThenOneRowPerSystemAndBatchInQueueOrder() {
            List<TaskFact> tasks = List.of(
                    task(10, 1, 1, "completed", null), task(10, 2, 2, "free", null),
                    task(11, 1, 3, "reserved", NOW.plusSeconds(60)), task(11, 2, 4, "reserved", NOW.minusSeconds(60)),
                    task(12, 1, 5, "free", null), task(12, 2, 6, "free", null));

            Result result = DashboardProgress.build(blocks, tasks, NOW);

            assertThat(result.rows()).hasSize(3);
            DashboardProgress.Row overall = result.rows().get(0);
            assertThat(overall.system()).isNull();
            assertThat(overall.batchId()).isNull();
            assertThat(overall.items()).isEqualTo(24);
            assertThat(overall.blocks()).isEqualTo(3);
            assertThat(overall.role1()).isEqualTo(TaskCounts.of(1, 1, 1));
            assertThat(overall.role2()).isEqualTo(TaskCounts.of(3, 0, 0));
            DashboardProgress.Row minitest = result.rows().get(1);
            assertThat(minitest.system()).isEqualTo("minitest");
            assertThat(minitest.batchFileName()).isEqualTo("a.csv");
            assertThat(minitest.items()).isEqualTo(20);
            assertThat(minitest.blocks()).isEqualTo(2);
            assertThat(minitest.role1()).isEqualTo(TaskCounts.of(0, 1, 1));
            assertThat(minitest.role2()).isEqualTo(TaskCounts.of(2, 0, 0));
            assertThat(result.rows().get(2).system()).isEqualTo("other");
            assertThat(result.rows().get(2).role1()).isEqualTo(TaskCounts.of(1, 0, 0));
        }

        @Test
        void firstFreeQueuePositionIncludesExpiredReservationsAndIsNullWhenNoneLeft() {
            List<TaskFact> tasks = List.of(
                    task(10, 1, 1, "completed", null), task(10, 2, 2, "reserved", NOW.plusSeconds(60)),
                    task(11, 1, 3, "reserved", NOW.minusSeconds(1)));
            assertThat(DashboardProgress.build(blocks, tasks, NOW).firstFreeQueuePosition()).isEqualTo(3);

            List<TaskFact> none = List.of(task(10, 1, 1, "completed", null), task(10, 2, 2, "reserved", NOW.plusSeconds(60)));
            assertThat(DashboardProgress.build(blocks, none, NOW).firstFreeQueuePosition()).isNull();
        }

        @Test
        void emptyStudyHasOnlyAnOverallRowOfZeros() {
            Result result = DashboardProgress.build(List.of(), List.of(), NOW);

            assertThat(result.rows()).hasSize(1);
            assertThat(result.rows().get(0).items()).isZero();
            assertThat(result.rows().get(0).role1()).isEqualTo(TaskCounts.ZERO);
            assertThat(result.firstFreeQueuePosition()).isNull();
        }
    }

    @Nested
    class Participants {

        @ParameterizedTest
        @CsvSource(nullValues = "null", value = {
            "refused,null,null,DECLINED_CONSENT",
            "refused,yes_professional,true,DECLINED_CONSENT",
            "accepted,no,false,INELIGIBLE",
            "accepted,no,null,INELIGIBLE",
            "accepted,yes_academic,false,INELIGIBLE",
            "accepted,yes_professional,true,ELIGIBLE",
            "null,null,null,INCOMPLETE",
            "accepted,null,null,INCOMPLETE",
            "accepted,yes_professional,null,INCOMPLETE"})
        void everyParticipantFallsInExactlyOneBucket(String consent, String screening, Boolean eligible, ParticipantBucket expected) {
            assertThat(ParticipantBucket.of(consent, screening, eligible)).isEqualTo(expected);
        }

        @Test
        void totalsCountEachFactOnceAndSkillsOnlyWhenPresent() {
            List<Fact> facts = List.of(
                    new Fact("R-0001", "pt-BR", "accepted", "yes_professional", true, "tester"),
                    new Fact("R-0002", "pt-BR", "accepted", "yes_academic", true, "analyst"),
                    new Fact("R-0003", "en-US", "accepted", "no", false, "tester"),
                    new Fact("R-0004", "es-ES", "refused", null, null, null),
                    new Fact("R-0005", null, null, null, null, null));

            ParticipantTotals totals = ParticipantTotals.of(facts);

            assertThat(totals.eligible()).isEqualTo(2);
            assertThat(totals.ineligible()).isEqualTo(1);
            assertThat(totals.declinedConsent()).isEqualTo(1);
            assertThat(totals.incomplete()).isEqualTo(1);
            assertThat(totals.bySkill()).containsExactly(Map.entry("tester", 2L), Map.entry("analyst", 1L));
        }

        @Test
        void contributorsAreOrderedByCountThenPseudonymWithSharesOfTheTotal() {
            Map<String, Long> completed = new LinkedHashMap<>();
            completed.put("R-0003", 1L);
            completed.put("R-0002", 2L);
            completed.put("R-0001", 1L);

            List<Contributor> contributors = ParticipantTotals.contributors(completed);

            assertThat(contributors).extracting(Contributor::pseudonym).containsExactly("R-0002", "R-0001", "R-0003");
            assertThat(contributors.get(0).shareOfCompleted()).isEqualTo(0.5);
            assertThat(contributors.get(1).shareOfCompleted()).isEqualTo(0.25);
            assertThat(ParticipantTotals.contributors(Map.of())).isEmpty();
        }
    }

    @Nested
    class Languages {

        @Test
        void alwaysThreeLocalesInFixedOrderWithZeros() {
            List<LanguageTally.Row> rows = LanguageTally.rows(Map.of("pt-BR", 4L), Map.of("es-ES", 2L));

            assertThat(rows).extracting(LanguageTally.Row::language).containsExactly("pt-BR", "en-US", "es-ES");
            assertThat(rows).extracting(LanguageTally.Row::participantsPreferred).containsExactly(4L, 0L, 0L);
            assertThat(rows).extracting(LanguageTally.Row::finalResponses).containsExactly(0L, 0L, 2L);
        }

        @Test
        void emptyTalliesStillListEveryLocale() {
            assertThat(LanguageTally.rows(Map.of(), Map.of())).hasSize(3);
        }
    }

    @Nested
    class Quota {

        @ParameterizedTest
        @CsvSource({"12,450,438,false", "441,450,9,true", "440,450,10,false", "450,450,0,true", "500,450,0,true", "0,0,0,true"})
        void remainingIsNeverNegativeAndLowBelowTen(long sent, int limit, long remaining, boolean low) {
            EmailQuota quota = EmailQuota.of(sent, limit);

            assertThat(quota.emailsRemaining()).isEqualTo(remaining);
            assertThat(quota.lowRemaining()).isEqualTo(low);
            assertThat(quota.emailsSentLast24h()).isEqualTo(sent);
            assertThat(quota.dailyEmailLimit()).isEqualTo(limit);
        }
    }
}