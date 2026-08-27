package br.edu.ufcg.splab.claret.model;

public enum CoverageCriteria {
    GT("gt", "Transition Coverage (Greedy Heuristic)", "Reduced (Greedy Heuristic - Transition Coverage)"),
    GTP("gtp", "Transition Pair Coverage (Greedy Heuristic)", "Reduced (Greedy Heuristic - Transition Pair Coverage)"),
    ART("art", "Adaptive Random Testing (Jaccard Distance)", "Reduced (Adaptive Random Testing by Jaccard Distance)"),
    COMPLETE("complete", "Complete Test Suite (All Paths)", "Complete Test Suite"),
    BASIC_ONLY("basic-only", "Basic Flow Only (Happy Path)", "Basic Flow Only (Happy Path)"),
    ALL_BRANCHES("all-branches", "All Decision Branches (Alternatives & Exceptions)", "Reduced (Decision Branches)");

    private final String code;
    private final String description;
    private final String suiteTypeName;

    CoverageCriteria(String code, String description, String suiteTypeName) {
        this.code = code;
        this.description = description;
        this.suiteTypeName = suiteTypeName;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getSuiteTypeName() {
        return suiteTypeName;
    }

    public String getFileSuffix() {
        switch (this) {
            case GT: return "--GT-";
            case GTP: return "--GTP-";
            case ART: return "--ART-";
            case COMPLETE: return "--Complete-";
            case BASIC_ONLY: return "--Basic-";
            case ALL_BRANCHES: return "--Branches-";
            default: return "";
        }
    }

    public static CoverageCriteria fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return GT;
        }
        String normalized = value.trim().toLowerCase().replace("_", "-");
        for (CoverageCriteria c : values()) {
            if (c.code.equalsIgnoreCase(normalized) || c.name().equalsIgnoreCase(value.trim())) {
                return c;
            }
        }
        if ("all-transitions".equalsIgnoreCase(normalized) || "all_transitions".equalsIgnoreCase(normalized)) {
            return GT;
        }
        if ("all-paths".equalsIgnoreCase(normalized) || "all_paths".equalsIgnoreCase(normalized)) {
            return COMPLETE;
        }
        if ("all-states".equalsIgnoreCase(normalized) || "all_states".equalsIgnoreCase(normalized)) {
            return GT;
        }
        return GT;
    }
}
