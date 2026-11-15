package io.github.diegoquirino.veredito.importing;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.importing.ImportColumns;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/** Reads a multipart CSV as UTF-8 RFC 4180 with Appendix D headers (spec 005). */
@Component
public class CsvImportReader {

    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public record ParsedCsv(List<String> headers, List<String> extraColumns, List<Map<String, String>> dataRows) {}

    public ParsedCsv read(InputStream input) {
        byte[] bytes;
        try {
            bytes = input.readAllBytes();
        } catch (IOException ex) {
            throw new ProblemException(ProblemType.CSV_INVALID);
        }
        String text = decodeUtf8(stripBom(bytes));
        try (CSVParser parser = CSVParser.parse(
                new StringReader(text),
                CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).setAllowMissingColumnNames(false).get())) {
            List<String> headers = List.copyOf(parser.getHeaderNames());
            Set<String> headerSet = new LinkedHashSet<>(headers);
            ImportColumns.missingRequired(headerSet).ifPresent(column -> {
                throw new ProblemException(ProblemType.CSV_MISSING_COLUMN, column).with("column", column);
            });
            List<String> extra = headers.stream().filter(name -> !ImportColumns.ALL.contains(name)).toList();
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = new HashMap<>();
                for (String name : ImportColumns.ALL) {
                    row.put(name, record.isMapped(name) ? nullToEmpty(record.get(name)) : "");
                }
                rows.add(Map.copyOf(row));
            }
            return new ParsedCsv(headers, List.copyOf(extra), List.copyOf(rows));
        } catch (ProblemException problem) {
            throw problem;
        } catch (IOException | IllegalArgumentException ex) {
            throw new ProblemException(ProblemType.CSV_INVALID);
        }
    }

    private static byte[] stripBom(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == UTF8_BOM[0]
                && bytes[1] == UTF8_BOM[1]
                && bytes[2] == UTF8_BOM[2]) {
            byte[] without = new byte[bytes.length - 3];
            System.arraycopy(bytes, 3, without, 0, without.length);
            return without;
        }
        return bytes;
    }

    private static String decodeUtf8(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            throw new ProblemException(ProblemType.CSV_NOT_UTF8);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
