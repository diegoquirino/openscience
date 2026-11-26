package io.github.diegoquirino.veredito.domain.importing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Domain rules of spec 005 without Spring. */
class ImportRulesTest {

    @Nested
    class Columns {

        @Test
        void reportsTheFirstMissingRequiredColumn() {
            assertThat(ImportColumns.missingRequired(Set.of("system", "edit_key"))).contains("uc");
            assertThat(ImportColumns.missingRequired(Set.copyOf(ImportColumns.ALL))).isEmpty();
        }
    }

    @Nested
    class RowRules {

        @Test
        void acceptsAValidModRow() {
            assertThat(ItemRowRules.firstError(validMod())).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "system, empty-system",
            "edit_key, empty-edit-key",
            "uc, empty-uc",
            "kind, empty-kind",
            "field, empty-field",
            "origin_version, empty-origin-version",
            "target_version, empty-target-version",
            "origin_usecase, empty-origin-usecase",
            "target_usecase, empty-target-usecase",
            "origin_tests, empty-origin-tests",
            "target_tests, empty-target-tests"
        })
        void refusesEmptyRequiredFields(String column, String code) {
            Map<String, String> row = validMod();
            row.put(column, "");
            assertThat(ItemRowRules.firstError(row)).contains(code);
        }

        @Test
        void refusesInvalidKind() {
            Map<String, String> row = validMod();
            row.put("kind", "PATCH");
            assertThat(ItemRowRules.firstError(row)).contains("invalid-kind");
        }

        @Test
        void refusesEmptyOriginInMod() {
            Map<String, String> row = validMod();
            row.put("origin", "");
            assertThat(ItemRowRules.firstError(row)).contains("empty-origin");
        }

        @Test
        void refusesEmptyTargetInAdd() {
            Map<String, String> row = validMod();
            row.put("kind", "ADD");
            row.put("origin", "");
            row.put("target", "");
            assertThat(ItemRowRules.firstError(row)).contains("empty-target");
        }

        @Test
        void allowsOneEmptySideInMeta() {
            Map<String, String> row = validMod();
            row.put("kind", "META");
            row.put("origin", "");
            row.put("target", "REQ-07");
            assertThat(ItemRowRules.firstError(row)).isEmpty();
        }

        @Test
        void refusesBothEmptySidesInMeta() {
            Map<String, String> row = validMod();
            row.put("kind", "META");
            row.put("origin", "");
            row.put("target", "");
            assertThat(ItemRowRules.firstError(row)).contains("empty-meta-sides");
        }

        @Test
        void refusesOverlongOrigin() {
            Map<String, String> row = validMod();
            row.put("origin", "x".repeat(ItemRowRules.MAX_TEXT + 1));
            assertThat(ItemRowRules.firstError(row)).contains("origin-too-long");
        }

        @Test
        void refusesOverlongSystem() {
            Map<String, String> row = validMod();
            row.put("system", "s".repeat(ItemRowRules.MAX_SYSTEM + 1));
            assertThat(ItemRowRules.firstError(row)).contains("system-too-long");
        }
    }

    @Nested
    class Classifier {

        @Test
        void includesValidNewRowsAndIgnoresExistingKeys() {
            Map<String, String> a = validMod();
            Map<String, String> b = validMod();
            b.put("edit_key", "LoginTest|1|2");
            var result = ImportClassifier.classify(List.of(a, b), Set.of(ImportClassifier.key(a)));
            assertThat(result.included()).containsExactly(b);
            assertThat(result.ignored()).singleElement().satisfies(row -> {
                assertThat(row.line()).isEqualTo(2);
                assertThat(row.code()).isEqualTo("already-imported");
            });
            assertThat(result.errors()).isEmpty();
        }

        @Test
        void marksDuplicatesInFileAsErrorsFromTheSecond() {
            Map<String, String> first = validMod();
            Map<String, String> second = validMod();
            second.put("origin", "other text");
            var result = ImportClassifier.classify(List.of(first, second), Set.of());
            assertThat(result.included()).containsExactly(first);
            assertThat(result.errors()).singleElement().satisfies(row -> {
                assertThat(row.line()).isEqualTo(3);
                assertThat(row.code()).isEqualTo("duplicate-in-file");
            });
        }

        @Test
        void validationErrorsWinOverDuplicates() {
            Map<String, String> bad = validMod();
            bad.put("kind", "");
            var result = ImportClassifier.classify(List.of(bad), Set.of());
            assertThat(result.errors()).singleElement().extracting(ImportClassifier.ClassifiedRow::code)
                    .isEqualTo("empty-kind");
        }
    }

    @Nested
    class Blocks {

        @ParameterizedTest
        @CsvSource({
            "1, 10, 1",
            "9, 10, 9",
            "10, 10, 10",
            "11, 10, 10|1",
            "38, 10, 10|10|10|8",
            "205, 10, 10|10|10|10|10|10|10|10|10|10|10|10|10|10|10|10|10|10|10|10|5"
        })
        void plansTheExpectedSizes(int included, int blockSize, String expected) {
            List<Integer> sizes = expected.contains("|")
                    ? java.util.Arrays.stream(expected.split("\\|")).map(Integer::valueOf).toList()
                    : List.of(Integer.valueOf(expected));
            assertThat(BlockPlan.sizes(included, blockSize)).isEqualTo(sizes);
        }

        @Test
        void emptyWhenNoItems() {
            assertThat(BlockPlan.sizes(0, 10)).isEmpty();
        }
    }

    @Nested
    class PhaseGate {

        @Test
        void stagingIsAlwaysMinitestOnly() {
            assertThat(StudyPhaseGate.minitestOnly(true, StudyPhaseGate.PHASE_COLLECTION)).isTrue();
            assertThat(StudyPhaseGate.minitestOnly(true, StudyPhaseGate.PHASE_PILOT)).isTrue();
        }

        @Test
        void productionAllowsOthersOnlyInCollection() {
            assertThat(StudyPhaseGate.minitestOnly(false, StudyPhaseGate.PHASE_PILOT)).isTrue();
            assertThat(StudyPhaseGate.minitestOnly(false, StudyPhaseGate.PHASE_COLLECTION)).isFalse();
        }

        @Test
        void listsForeignSystemsInOrder() {
            Map<String, String> a = validMod();
            a.put("system", "other");
            Map<String, String> b = validMod();
            b.put("system", "third");
            Map<String, String> c = validMod();
            c.put("system", "other");
            assertThat(StudyPhaseGate.foreignSystems(List.of(a, b, c))).containsExactly("other", "third");
        }
    }

    private static Map<String, String> validMod() {
        Map<String, String> row = new HashMap<>();
        row.put("system", "minitest");
        row.put("edit_key", "LoginTest|1|1");
        row.put("uc", "LoginTest");
        row.put("kind", "MOD");
        row.put("field", "step");
        row.put("origin", "before");
        row.put("target", "after");
        row.put("origin_version", "v1.0");
        row.put("target_version", "v1.1");
        row.put("origin_usecase", "origin uc");
        row.put("target_usecase", "target uc");
        row.put("origin_tests", "origin tests");
        row.put("target_tests", "target tests");
        return row;
    }
}
