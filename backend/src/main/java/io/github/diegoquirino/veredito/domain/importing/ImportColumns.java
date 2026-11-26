package io.github.diegoquirino.veredito.domain.importing;

import java.util.List;
import java.util.Set;

/** Appendix D column names required by the Veredito import. */
public final class ImportColumns {

    public static final List<String> ALL = List.of(
            "system",
            "edit_key",
            "uc",
            "kind",
            "field",
            "origin",
            "target",
            "origin_version",
            "target_version",
            "origin_usecase",
            "target_usecase",
            "origin_tests",
            "target_tests");

    public static final Set<String> REQUIRED = Set.of(
            "system",
            "edit_key",
            "uc",
            "kind",
            "field",
            "origin_version",
            "target_version",
            "origin_usecase",
            "target_usecase",
            "origin_tests",
            "target_tests");

    private ImportColumns() {}

    /** First required name missing from the header (Appendix D order), or empty if complete. */
    public static java.util.Optional<String> missingRequired(Set<String> header) {
        return ALL.stream()
                .filter(REQUIRED::contains)
                .filter(name -> !header.contains(name))
                .findFirst();
    }
}
