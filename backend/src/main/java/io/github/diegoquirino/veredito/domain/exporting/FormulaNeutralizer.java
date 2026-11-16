package io.github.diegoquirino.veredito.domain.exporting;

/**
 * Spreadsheet formula injection guard of the wide and long tables (spec 009, FR-006): a text that starts
 * with {@code =}, {@code +}, {@code -} or {@code @} gets one leading apostrophe. The pipeline files and
 * the Appendix D columns of the completo file never go through here.
 */
public final class FormulaNeutralizer {

    private static final String TRIGGERS = "=+-@";

    private FormulaNeutralizer() {}

    /** The text with a leading {@code '} when it would start a formula; {@code null} becomes empty. */
    public static String neutralize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return TRIGGERS.indexOf(text.charAt(0)) >= 0 ? "'" + text : text;
    }
}