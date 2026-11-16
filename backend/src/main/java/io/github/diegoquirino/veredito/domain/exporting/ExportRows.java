package io.github.diegoquirino.veredito.domain.exporting;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Builds the cells of one CSV record per format; the order follows {@link ExportColumns}. */
public final class ExportRows {

    /** Marker for the name and e-mail of an anonymized participant (glossary term). */
    public static final String ANONYMIZED = "anonymized";

    private static final DateTimeFormatter OFFSET_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx").withZone(ZoneOffset.UTC);

    private ExportRows() {}

    /** Wide table (Appendix C): neutralized, name and e-mail of each completed role. */
    public static List<String> wide(ExportItem item, ExportRole role1, ExportRole role2) {
        List<String> cells = new ArrayList<>(ExportColumns.WIDE.size());
        cells.add(item.system());
        cells.add(item.editKey());
        cells.add(item.uc());
        cells.add(item.kind());
        cells.add(item.field());
        cells.add(item.origin());
        cells.add(item.target());
        wideRole(cells, role1);
        wideRole(cells, role2);
        return neutralized(cells);
    }

    /** Long table: one final response, no name or e-mail, neutralized. */
    public static List<String> longRow(ExportLongResponse response) {
        List<String> cells = java.util.Arrays.asList(
                response.system(),
                response.editKey(),
                Integer.toString(response.blockNumber()),
                Integer.toString(response.role()),
                response.pseudonym(),
                response.professionalSkill(),
                response.participantPreferredLanguage(),
                response.consentLanguage(),
                response.changeImpact(),
                response.maintenanceOperation(),
                response.comment(),
                response.responseLanguage(),
                timestamp(response.reservedAt()),
                timestamp(response.submittedAt()));
        return neutralized(cells);
    }

    /** Completo file: Appendix D cells as stored plus five cells per role; never neutralized, no PII. */
    public static List<String> completo(ExportItem item, ExportRole role1, ExportRole role2) {
        List<String> cells = new ArrayList<>(ExportColumns.COMPLETO.size());
        cells.add(item.system());
        cells.add(item.editKey());
        cells.add(item.uc());
        cells.add(item.kind());
        cells.add(item.field());
        cells.add(item.origin());
        cells.add(item.target());
        cells.add(item.originVersion());
        cells.add(item.targetVersion());
        cells.add(item.originUsecase());
        cells.add(item.targetUsecase());
        completoRole(cells, role1);
        completoRole(cells, role2);
        return plain(cells);
    }

    /** Pipeline file of one role (nine columns); label cells are blank when the role is not completed. */
    public static List<String> rater(ExportItem item, ExportRole role) {
        List<String> cells = new ArrayList<>(ExportColumns.RATER.size());
        cells.add(item.editKey());
        cells.add(item.uc());
        cells.add(item.kind());
        cells.add(item.field());
        cells.add(item.origin());
        cells.add(item.target());
        cells.add(role == null ? "" : role.changeImpact());
        cells.add(role == null ? "" : role.maintenanceOperation());
        cells.add(role == null ? "" : role.comment());
        return plain(cells);
    }

    /** {@code <version> <language> <timestamp>}, e.g. {@code v1 pt-BR 2026-11-20T17:03:00+00:00}; blank if not accepted. */
    static String consentCell(ExportRole role) {
        if (role == null
                || !role.consentAccepted()
                || role.consentVersion() == null
                || role.consentLanguage() == null
                || role.consentDecidedAt() == null) {
            return "";
        }
        return role.consentVersion() + " " + role.consentLanguage() + " " + timestamp(role.consentDecidedAt());
    }

    /** ISO-8601 with offset (UTC), whole seconds; empty for {@code null}. */
    static String timestamp(Instant instant) {
        return instant == null ? "" : OFFSET_SECONDS.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }

    private static void wideRole(List<String> cells, ExportRole role) {
        if (role == null) {
            for (int i = 0; i < 8; i++) {
                cells.add("");
            }
            return;
        }
        cells.add(role.anonymized() ? ANONYMIZED : role.name());
        cells.add(role.anonymized() ? ANONYMIZED : role.email());
        cells.add(consentCell(role));
        cells.add(role.professionalSkill());
        cells.add(role.changeImpact());
        cells.add(role.maintenanceOperation());
        cells.add(role.comment());
        cells.add(role.responseLanguage());
    }

    private static void completoRole(List<String> cells, ExportRole role) {
        if (role == null) {
            for (int i = 0; i < 5; i++) {
                cells.add("");
            }
            return;
        }
        cells.add(role.pseudonym());
        cells.add(role.changeImpact());
        cells.add(role.maintenanceOperation());
        cells.add(role.comment());
        cells.add(role.responseLanguage());
    }

    private static List<String> neutralized(List<String> cells) {
        return cells.stream().map(FormulaNeutralizer::neutralize).toList();
    }

    private static List<String> plain(List<String> cells) {
        return cells.stream().map(cell -> cell == null ? "" : cell).toList();
    }
}