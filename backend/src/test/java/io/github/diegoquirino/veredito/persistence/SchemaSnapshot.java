package io.github.diegoquirino.veredito.persistence;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Columns, indexes and constraints of the current schema, without the schema name (SC-003). */
record SchemaSnapshot(List<String> columns, List<String> indexes, List<String> constraints, List<String> checks) {

    static SchemaSnapshot of(JdbcClient jdbc) {
        return new SchemaSnapshot(
                jdbc.sql("""
                        SELECT CONCAT_WS('|', table_name, column_name, ordinal_position, column_type, is_nullable,
                                         IFNULL(column_default, '<null>'), IFNULL(collation_name, '<none>'), extra)
                        FROM information_schema.columns WHERE table_schema = DATABASE()
                        ORDER BY table_name, ordinal_position""").query(String.class).list(),
                jdbc.sql("""
                        SELECT CONCAT_WS('|', table_name, index_name, seq_in_index, column_name, non_unique)
                        FROM information_schema.statistics WHERE table_schema = DATABASE()
                        ORDER BY table_name, index_name, seq_in_index""").query(String.class).list(),
                jdbc.sql("""
                        SELECT CONCAT_WS('|', table_name, constraint_name, constraint_type)
                        FROM information_schema.table_constraints WHERE table_schema = DATABASE()
                        ORDER BY table_name, constraint_name""").query(String.class).list(),
                jdbc.sql("""
                        SELECT CONCAT_WS('|', constraint_name, check_clause)
                        FROM information_schema.check_constraints WHERE constraint_schema = DATABASE()
                        ORDER BY constraint_name""").query(String.class).list());
    }
}
