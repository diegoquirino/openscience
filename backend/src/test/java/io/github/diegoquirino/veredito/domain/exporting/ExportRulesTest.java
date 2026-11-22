package io.github.diegoquirino.veredito.domain.exporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Domain rules of spec 009 without Spring (organization columns from spec 025). */
class ExportRulesTest {

    private static final ExportItem ITEM = new ExportItem(
            "minitest", "LoginTest|1|1", "LoginTest", "MOD", "step", "origin text", "target text",
            "v1.0", "v1.1", "usecase A", "usecase B");

    private static ExportRole role(String pseudonym, String comment) {
        return new ExportRole(
                pseudonym, "Ana Souza", "ana@example.com", false, true, "v1", "pt-BR",
                Instant.parse("2026-11-20T17:03:00Z"), "test_analyst", "ufcg", null, "HIGH", "UPDATE", comment,
                "es-ES");
    }

    @Nested
    class Neutralizer {

        @ParameterizedTest
        @ValueSource(strings = {"=1+1", "+cmd", "-5", "@SUM(A1)"})
        void prefixesAnApostropheWhenTheTextStartsAFormula(String text) {
            assertThat(FormulaNeutralizer.neutralize(text)).isEqualTo("'" + text);
        }

        @ParameterizedTest
        @ValueSource(strings = {"plain", "a=b", "1+1", " =lead space", "'quoted", "\tTab", "ação"})
        void leavesEverythingElseUnchanged(String text) {
            assertThat(FormulaNeutralizer.neutralize(text)).isEqualTo(text);
        }

        @Test
        void nullAndEmptyBecomeEmpty() {
            assertThat(FormulaNeutralizer.neutralize(null)).isEmpty();
            assertThat(FormulaNeutralizer.neutralize("")).isEmpty();
        }
    }

    @Nested
    class Headers {

        @Test
        void longHeaderIsExactlyTheLockedOne() {
            assertThat(String.join(",", ExportColumns.LONG)).isEqualTo(
                    "system,edit_key,block,role,pseudonym,professional_skill,organization,organization_other,"
                            + "participant_preferred_language,consent_language,change_impact,maintenance_operation,"
                            + "comment,response_language,reserved_at,submitted_at");
        }

        @Test
        void raterHeaderHasTheNinePipelineColumns() {
            assertThat(String.join(",", ExportColumns.RATER))
                    .isEqualTo("edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment");
        }

        @Test
        void wideAndCompletoHeadersMatchTheSpec() {
            assertThat(ExportColumns.WIDE).hasSize(27).startsWith("system", "edit_key", "uc");
            assertThat(String.join(",", ExportColumns.WIDE)).endsWith(
                    "rater_2_name,rater_2_email,rater_2_accepted_research_terms_lgpd,rater_2_professional_skill,"
                            + "rater_2_organization,rater_2_organization_other,"
                            + "rater_2_change_impact_choice,rater_2_maintenance_operation_choice,rater_2_comment,"
                            + "rater_2_response_language");
            assertThat(String.join(",", ExportColumns.COMPLETO)).isEqualTo(
                    "system,edit_key,uc,kind,field,origin,target,origin_version,target_version,origin_usecase,"
                            + "target_usecase,rater_1_pseudonym,rater_1_change_impact,rater_1_maintenance_operation,"
                            + "rater_1_comment,rater_1_response_language,rater_2_pseudonym,rater_2_change_impact,"
                            + "rater_2_maintenance_operation,rater_2_comment,rater_2_response_language");
        }
    }

    @Nested
    class Rows {

        @Test
        void everyRowHasAsManyCellsAsItsHeader() {
            ExportRole r = role("R-0001", "ok");
            assertThat(ExportRows.wide(ITEM, r, null)).hasSameSizeAs(ExportColumns.WIDE);
            assertThat(ExportRows.completo(ITEM, r, r)).hasSameSizeAs(ExportColumns.COMPLETO);
            assertThat(ExportRows.rater(ITEM, r)).hasSameSizeAs(ExportColumns.RATER);
            assertThat(ExportRows.longRow(longResponse("c"))).hasSameSizeAs(ExportColumns.LONG);
        }

        @Test
        void incompleteRoleLeavesItsCellsBlank() {
            List<String> wide = ExportRows.wide(ITEM, role("R-0001", "ok"), null);
            assertThat(wide.subList(17, 27)).containsOnly("");
            List<String> completo = ExportRows.completo(ITEM, null, role("R-0002", "ok"));
            assertThat(completo.subList(11, 16)).containsOnly("");
            assertThat(completo.get(16)).isEqualTo("R-0002");
            assertThat(ExportRows.rater(ITEM, null).subList(6, 9)).containsOnly("");
        }

        @Test
        void wideCarriesConsentCellProfessionalSkillCodeAndLanguage() {
            List<String> wide = ExportRows.wide(ITEM, role("R-0001", "Texto"), null);
            assertThat(wide.subList(7, 17)).containsExactly(
                    "Ana Souza", "ana@example.com", "v1 pt-BR 2026-11-20T17:03:00+00:00", "test_analyst", "ufcg", "",
                    "HIGH", "UPDATE", "Texto", "es-ES");
        }

        @Test
        void anonymizedParticipantExportsTheMarker() {
            ExportRole anonymized = new ExportRole(
                    "R-0003", null, null, true, true, "v1", "en-US", Instant.parse("2026-11-20T17:03:00Z"),
                    "other", "other", "Acme", "LOW", "KEEP", null, "en-US");
            List<String> wide = ExportRows.wide(ITEM, anonymized, null);
            assertThat(wide.get(7)).isEqualTo("anonymized");
            assertThat(wide.get(8)).isEqualTo("anonymized");
            assertThat(wide.get(15)).isEmpty();
        }

        @Test
        void consentCellIsBlankWhenConsentWasNotAccepted() {
            ExportRole refused = new ExportRole(
                    "R-0004", "N", "n@example.com", false, false, "v1", "pt-BR", null, "other", "tjpb", null, "LOW",
                    "KEEP", "", "pt-BR");
            assertThat(ExportRows.consentCell(refused)).isEmpty();
            assertThat(ExportRows.consentCell(null)).isEmpty();
        }

        @Test
        void wideAndLongNeutralizeButCompletoAndRaterKeepTheBytes() {
            ExportItem formulaItem = new ExportItem(
                    "minitest", "k", "uc", "MOD", "step", "=origin", "-target", "v1", "v2", "=uc1", "+uc2");
            ExportRole r = role("R-0001", "@comment");
            assertThat(ExportRows.wide(formulaItem, r, null).subList(5, 7)).containsExactly("'=origin", "'-target");
            assertThat(ExportRows.wide(formulaItem, r, null).get(15)).isEqualTo("'@comment");
            assertThat(ExportRows.longRow(longResponse("=boom")).get(12)).isEqualTo("'=boom");
            assertThat(ExportRows.completo(formulaItem, r, null).subList(5, 11))
                    .containsExactly("=origin", "-target", "v1", "v2", "=uc1", "+uc2");
            assertThat(ExportRows.completo(formulaItem, r, null).get(14)).isEqualTo("@comment");
            assertThat(ExportRows.rater(formulaItem, r).subList(4, 9))
                    .containsExactly("=origin", "-target", "HIGH", "UPDATE", "@comment");
        }

        @Test
        void nullOriginAndCommentExportAsEmpty() {
            ExportItem add = new ExportItem("minitest", "k", "uc", "ADD", "step", null, "t", "v1", "v2", "a", "b");
            ExportRole r = role("R-0001", null);
            assertThat(ExportRows.rater(add, r)).containsExactly("k", "uc", "ADD", "step", "", "t", "HIGH", "UPDATE", "");
        }

        @Test
        void longRowHasNoNameOrEmailAndFormatsTimestamps() {
            List<String> row = ExportRows.longRow(longResponse("note"));
            assertThat(row).containsExactly(
                    "minitest", "k", "2", "1", "R-0001", "developer", "tjpb", "", "pt-BR", "pt-BR", "LOW", "KEEP",
                    "note", "en-US", "2026-11-20T16:00:00+00:00", "2026-11-20T16:30:00+00:00");
        }

        private ExportLongResponse longResponse(String comment) {
            return new ExportLongResponse(
                    "minitest", "k", 2, 1, "R-0001", "developer", "tjpb", null, "pt-BR", "pt-BR", "LOW", "KEEP", comment,
                    "en-US", Instant.parse("2026-11-20T16:00:00Z"), Instant.parse("2026-11-20T16:30:00Z"));
        }
    }

    @Nested
    class Writer {

        @Test
        void quotesCommasQuotesAndLineBreaks() {
            CsvWriter writer = new CsvWriter(List.of("a", "b"));
            writer.record(Arrays.asList("x,y", "say \"hi\""));
            writer.record(Arrays.asList("line\nbreak", null));
            writer.record(List.of("cr\rhere", "plain"));
            assertThat(writer.text()).isEqualTo(
                    "a,b\n" + "\"x,y\",\"say \"\"hi\"\"\"\n" + "\"line\nbreak\",\n" + "\"cr\rhere\",plain\n");
        }

        @Test
        void bytesAreUtf8WithoutBom() {
            byte[] bytes = new CsvWriter(List.of("comment")).record(List.of("ação")).bytes();
            assertThat(bytes[0]).isEqualTo((byte) 'c');
            assertThat(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("comment\nação\n");
        }

        @Test
        void headerOnlyDocumentEndsWithOneLineFeed() {
            assertThat(new CsvWriter(ExportColumns.RATER).text()).isEqualTo(String.join(",", ExportColumns.RATER) + "\n");
        }
    }

    @Nested
    class FileNames {

        @Test
        void followsTheResearchTable() {
            assertThat(ExportFileNames.WIDE).isEqualTo("veredito_wide.csv");
            assertThat(ExportFileNames.LONG).isEqualTo("veredito_long.csv");
            assertThat(ExportFileNames.completo("minitest")).isEqualTo("veredito_minitest_completo.csv");
            assertThat(ExportFileNames.rater("minitest", 1)).isEqualTo("t1_minitest_rater1.csv");
            assertThat(ExportFileNames.rater("minitest", 2)).isEqualTo("t1_minitest_rater2.csv");
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {"my sys\"tem|my_sys_tem", "a/b\\c|a_b_c", "sistema-ç|sistema-_"})
        void unsafeCharactersBecomeUnderscores(String system, String safe) {
            assertThat(ExportFileNames.safe(system)).isEqualTo(safe);
        }
    }
}
