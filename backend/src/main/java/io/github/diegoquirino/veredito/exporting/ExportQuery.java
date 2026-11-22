package io.github.diegoquirino.veredito.exporting;

import io.github.diegoquirino.veredito.domain.exporting.ExportItem;
import io.github.diegoquirino.veredito.domain.exporting.ExportLongResponse;
import io.github.diegoquirino.veredito.domain.exporting.ExportRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The read-only JDBC queries of spec 009. A role contributes only when its task is {@code completed} and
 * the response of that item is {@code final}; every query orders by {@code block.number, item.position}.
 */
@Repository
public class ExportQuery {

    /** One item with the (possibly absent) final response of each role. */
    public record ItemRow(ExportItem item, ExportRole role1, ExportRole role2) {}

    private static final String ITEM_COLUMNS = """
            i.`system` AS i_system, i.edit_key AS i_edit_key, i.uc AS i_uc, i.kind AS i_kind, i.field AS i_field,
            i.origin AS i_origin, i.target AS i_target, i.origin_version AS i_origin_version,
            i.target_version AS i_target_version, i.origin_usecase AS i_origin_usecase,
            i.target_usecase AS i_target_usecase""";

    private final JdbcClient jdbc;

    public ExportQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Distinct systems in the order of their first item in queue order. */
    public List<String> systems() {
        return jdbc.sql("""
                        SELECT i.`system` FROM item i JOIN block b ON b.id = i.block_id
                        GROUP BY i.`system` ORDER BY MIN(b.number * 256 + i.position)""")
                .query(String.class)
                .list();
    }

    public boolean systemExists(String system) {
        return jdbc.sql("SELECT COUNT(*) FROM item WHERE `system` = ?")
                        .param(system)
                        .query(Long.class)
                        .single()
                > 0;
    }

    /** Every item of every system (wide table). */
    public List<ItemRow> itemRows() {
        return jdbc.sql(itemSql("")).query(ExportQuery::mapItemRow).list();
    }

    /** Every item of one system (completo and rater files). */
    public List<ItemRow> itemRows(String system) {
        return jdbc.sql(itemSql("WHERE i.`system` = ?")).param(system).query(ExportQuery::mapItemRow).list();
    }

    /** One entry per final response of a completed task (long table). */
    public List<ExportLongResponse> longResponses() {
        return jdbc.sql("""
                        SELECT i.`system` AS i_system, i.edit_key, b.number AS block_number, t.role, p.pseudonym,
                               p.professional_skill, p.organization, p.organization_other,
                               p.preferred_language, p.consent_language,
                               r.change_impact, r.maintenance_operation, r.comment, r.language,
                               t.reserved_at, t.submitted_at
                        FROM response r
                        JOIN task t ON t.id = r.task_id AND t.status = 'completed'
                        JOIN item i ON i.id = r.item_id
                        JOIN block b ON b.id = i.block_id
                        JOIN participant p ON p.id = t.participant_id
                        WHERE r.state = 'final'
                        ORDER BY b.number, i.position, t.role""")
                .query((rs, row) -> new ExportLongResponse(
                        rs.getString("i_system"),
                        rs.getString("edit_key"),
                        rs.getInt("block_number"),
                        rs.getInt("role"),
                        rs.getString("pseudonym"),
                        rs.getString("professional_skill"),
                        rs.getString("organization"),
                        rs.getString("organization_other"),
                        rs.getString("preferred_language"),
                        rs.getString("consent_language"),
                        rs.getString("change_impact"),
                        rs.getString("maintenance_operation"),
                        rs.getString("comment"),
                        rs.getString("language"),
                        instant(rs, "reserved_at"),
                        instant(rs, "submitted_at")))
                .list();
    }

    private static String itemSql(String where) {
        return "SELECT " + ITEM_COLUMNS + ",\n" + roleColumns(1) + ",\n" + roleColumns(2) + """

                FROM item i
                JOIN block b ON b.id = i.block_id
                """ + roleJoins(1) + roleJoins(2) + where + """

                ORDER BY b.number, i.position""";
    }

    private static String roleColumns(int n) {
        String r = "r" + n;
        String p = "p" + n;
        String c = "c" + n;
        return """
                %1$s.change_impact AS r%4$d_change_impact, %1$s.maintenance_operation AS r%4$d_operation,
                %1$s.comment AS r%4$d_comment, %1$s.language AS r%4$d_language,
                %2$s.pseudonym AS r%4$d_pseudonym, %2$s.name AS r%4$d_name, %2$s.email AS r%4$d_email,
                %2$s.anonymized_at AS r%4$d_anonymized_at, %2$s.professional_skill AS r%4$d_skill,
                %2$s.organization AS r%4$d_organization, %2$s.organization_other AS r%4$d_organization_other,
                %2$s.consent_decision AS r%4$d_consent_decision, %2$s.consent_language AS r%4$d_consent_language,
                %2$s.consent_decided_at AS r%4$d_consent_decided_at, %3$s.version AS r%4$d_consent_version"""
                .formatted(r, p, c, n);
    }

    private static String roleJoins(int n) {
        return """
                LEFT JOIN task t%1$d ON t%1$d.block_id = i.block_id AND t%1$d.role = %1$d AND t%1$d.status = 'completed'
                LEFT JOIN response r%1$d ON r%1$d.task_id = t%1$d.id AND r%1$d.item_id = i.id AND r%1$d.state = 'final'
                LEFT JOIN participant p%1$d ON p%1$d.id = t%1$d.participant_id
                LEFT JOIN consent_form c%1$d ON c%1$d.id = p%1$d.consent_form_id
                """.formatted(n);
    }

    private static ItemRow mapItemRow(ResultSet rs, int row) throws SQLException {
        ExportItem item = new ExportItem(
                rs.getString("i_system"),
                rs.getString("i_edit_key"),
                rs.getString("i_uc"),
                rs.getString("i_kind"),
                rs.getString("i_field"),
                rs.getString("i_origin"),
                rs.getString("i_target"),
                rs.getString("i_origin_version"),
                rs.getString("i_target_version"),
                rs.getString("i_origin_usecase"),
                rs.getString("i_target_usecase"));
        return new ItemRow(item, role(rs, 1), role(rs, 2));
    }

    private static ExportRole role(ResultSet rs, int n) throws SQLException {
        String prefix = "r" + n + "_";
        String impact = rs.getString(prefix + "change_impact");
        if (impact == null) {
            return null;
        }
        return new ExportRole(
                rs.getString(prefix + "pseudonym"),
                rs.getString(prefix + "name"),
                rs.getString(prefix + "email"),
                rs.getTimestamp(prefix + "anonymized_at") != null,
                "accepted".equals(rs.getString(prefix + "consent_decision")),
                rs.getString(prefix + "consent_version"),
                rs.getString(prefix + "consent_language"),
                instant(rs, prefix + "consent_decided_at"),
                rs.getString(prefix + "skill"),
                rs.getString(prefix + "organization"),
                rs.getString(prefix + "organization_other"),
                impact,
                rs.getString(prefix + "operation"),
                rs.getString(prefix + "comment"),
                rs.getString(prefix + "language"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
