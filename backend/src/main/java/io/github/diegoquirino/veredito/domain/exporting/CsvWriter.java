package io.github.diegoquirino.veredito.domain.exporting;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RFC 4180 writer of the exports: a field is quoted when it holds a comma, a double quote, a carriage
 * return or a line feed (quotes doubled); records end with {@code \n}; the bytes are UTF-8 without BOM.
 */
public final class CsvWriter {

    private final StringBuilder out = new StringBuilder();

    public CsvWriter(List<String> header) {
        record(header);
    }

    /** Appends one record. */
    public CsvWriter record(List<String> fields) {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(escape(fields.get(i)));
        }
        out.append('\n');
        return this;
    }

    public String text() {
        return out.toString();
    }

    public byte[] bytes() {
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String escape(String field) {
        if (field == null || field.isEmpty()) {
            return "";
        }
        boolean quote = false;
        for (int i = 0; i < field.length() && !quote; i++) {
            char c = field.charAt(i);
            quote = c == ',' || c == '"' || c == '\n' || c == '\r';
        }
        return quote ? '"' + field.replace("\"", "\"\"") + '"' : field;
    }
}