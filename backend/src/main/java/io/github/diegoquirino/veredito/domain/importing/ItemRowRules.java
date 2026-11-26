package io.github.diegoquirino.veredito.domain.importing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * FR-052 plus the column length caps of the 003 data model. Returns stable reason codes from
 * {@code contracts/problem-details.md}.
 */
public final class ItemRowRules {

    public static final Set<String> KINDS = Set.of("ADD", "DEL", "MOD", "META");
    public static final int MAX_TEXT = 5_000;
    public static final int MAX_USECASE = 40_000;
    /** Same cap as use-case texts (spec 040); raise here if regenerated suites exceed it. */
    public static final int MAX_TESTS = MAX_USECASE;
    public static final int MAX_VERSION = 20;
    public static final int MAX_SYSTEM = 100;
    public static final int MAX_EDIT_KEY = 500;
    public static final int MAX_UC = 255;
    public static final int MAX_FIELD = 100;

    private ItemRowRules() {}

    /** Empty optional when the row is valid; otherwise the first reason code. */
    public static Optional<String> firstError(Map<String, String> row) {
        List<String> errors = errors(row);
        return errors.isEmpty() ? Optional.empty() : Optional.of(errors.getFirst());
    }

    public static List<String> errors(Map<String, String> row) {
        String system = value(row, "system");
        String editKey = value(row, "edit_key");
        String uc = value(row, "uc");
        String kind = value(row, "kind");
        String field = value(row, "field");
        String origin = value(row, "origin");
        String target = value(row, "target");
        String originVersion = value(row, "origin_version");
        String targetVersion = value(row, "target_version");
        String originUsecase = value(row, "origin_usecase");
        String targetUsecase = value(row, "target_usecase");
        String originTests = value(row, "origin_tests");
        String targetTests = value(row, "target_tests");

        List<String> reasons = new ArrayList<>();
        if (system.isEmpty()) {
            reasons.add("empty-system");
        } else if (system.length() > MAX_SYSTEM) {
            reasons.add("system-too-long");
        }
        if (editKey.isEmpty()) {
            reasons.add("empty-edit-key");
        } else if (editKey.length() > MAX_EDIT_KEY) {
            reasons.add("edit-key-too-long");
        }
        if (uc.isEmpty()) {
            reasons.add("empty-uc");
        } else if (uc.length() > MAX_UC) {
            reasons.add("uc-too-long");
        }
        if (kind.isEmpty()) {
            reasons.add("empty-kind");
        } else if (!KINDS.contains(kind)) {
            reasons.add("invalid-kind");
        }
        if (field.isEmpty()) {
            reasons.add("empty-field");
        } else if (field.length() > MAX_FIELD) {
            reasons.add("field-too-long");
        }
        if (originVersion.isEmpty()) {
            reasons.add("empty-origin-version");
        } else if (originVersion.length() > MAX_VERSION) {
            reasons.add("version-too-long");
        }
        if (targetVersion.isEmpty()) {
            reasons.add("empty-target-version");
        } else if (targetVersion.length() > MAX_VERSION) {
            reasons.add("version-too-long");
        }
        if (originUsecase.isEmpty()) {
            reasons.add("empty-origin-usecase");
        } else if (originUsecase.length() > MAX_USECASE) {
            reasons.add("usecase-too-long");
        }
        if (targetUsecase.isEmpty()) {
            reasons.add("empty-target-usecase");
        } else if (targetUsecase.length() > MAX_USECASE) {
            reasons.add("usecase-too-long");
        }
        if (originTests.isEmpty()) {
            reasons.add("empty-origin-tests");
        } else if (originTests.length() > MAX_TESTS) {
            reasons.add("tests-too-long");
        }
        if (targetTests.isEmpty()) {
            reasons.add("empty-target-tests");
        } else if (targetTests.length() > MAX_TESTS) {
            reasons.add("tests-too-long");
        }
        if (origin.length() > MAX_TEXT) {
            reasons.add("origin-too-long");
        }
        if (target.length() > MAX_TEXT) {
            reasons.add("target-too-long");
        }
        if ("MOD".equals(kind) || "DEL".equals(kind)) {
            if (origin.isEmpty()) {
                reasons.add("empty-origin");
            }
        }
        if ("MOD".equals(kind) || "ADD".equals(kind)) {
            if (target.isEmpty()) {
                reasons.add("empty-target");
            }
        }
        if ("META".equals(kind) && origin.isEmpty() && target.isEmpty()) {
            reasons.add("empty-meta-sides");
        }
        return reasons;
    }

    private static String value(Map<String, String> row, String key) {
        String raw = row.get(key);
        return raw == null ? "" : raw;
    }
}
