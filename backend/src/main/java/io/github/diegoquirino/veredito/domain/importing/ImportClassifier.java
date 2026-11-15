package io.github.diegoquirino.veredito.domain.importing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Classifies CSV data rows as included, ignored or error (FR-003 / FR-053). */
public final class ImportClassifier {

    public record ClassifiedRow(int line, Map<String, String> fields, String code) {}

    public record Result(
            List<Map<String, String>> included,
            List<ClassifiedRow> ignored,
            List<ClassifiedRow> errors) {}

    private ImportClassifier() {}

    /**
     * @param dataRows rows in file order; line numbers start at 2 (header is line 1)
     * @param existingKeys {@code (system, edit_key)} already in the database
     */
    public static Result classify(List<Map<String, String>> dataRows, Set<String> existingKeys) {
        Objects.requireNonNull(dataRows, "dataRows");
        Objects.requireNonNull(existingKeys, "existingKeys");
        List<Map<String, String>> included = new ArrayList<>();
        List<ClassifiedRow> ignored = new ArrayList<>();
        List<ClassifiedRow> errors = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int line = 2;
        for (Map<String, String> row : dataRows) {
            var validation = ItemRowRules.firstError(row);
            if (validation.isPresent()) {
                errors.add(new ClassifiedRow(line, row, validation.get()));
                line++;
                continue;
            }
            String key = key(row);
            if (existingKeys.contains(key)) {
                ignored.add(new ClassifiedRow(line, row, "already-imported"));
            } else if (!seenInFile.add(key)) {
                errors.add(new ClassifiedRow(line, row, "duplicate-in-file"));
            } else {
                included.add(row);
            }
            line++;
        }
        return new Result(List.copyOf(included), List.copyOf(ignored), List.copyOf(errors));
    }

    public static String key(Map<String, String> row) {
        return value(row, "system") + "\0" + value(row, "edit_key");
    }

    private static String value(Map<String, String> row, String column) {
        String raw = row.get(column);
        return raw == null ? "" : raw;
    }
}
