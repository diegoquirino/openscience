package io.github.diegoquirino.veredito.domain.exporting;

/** File names of the downloads (spec 009 research section 9). */
public final class ExportFileNames {

    public static final String WIDE = "veredito_wide.csv";
    public static final String LONG = "veredito_long.csv";

    private ExportFileNames() {}

    public static String completo(String system) {
        return "veredito_" + safe(system) + "_completo.csv";
    }

    public static String rater(String system, int role) {
        return "t1_" + safe(system) + "_rater" + role + ".csv";
    }

    /** Keeps letters, digits, dot, underscore and hyphen; anything else becomes an underscore. */
    static String safe(String system) {
        StringBuilder safe = new StringBuilder();
        system.codePoints().forEach(cp -> safe.append(
                cp < 128 && (Character.isLetterOrDigit(cp) || cp == '.' || cp == '_' || cp == '-')
                        ? (char) cp
                        : '_'));
        return safe.toString();
    }
}