package io.github.diegoquirino.veredito.importing;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.config.VereditoProperties;
import io.github.diegoquirino.veredito.config.VereditoProperties.Environment;
import io.github.diegoquirino.veredito.domain.importing.BlockPlan;
import io.github.diegoquirino.veredito.domain.importing.ImportClassifier;
import io.github.diegoquirino.veredito.domain.importing.ImportClassifier.ClassifiedRow;
import io.github.diegoquirino.veredito.domain.importing.ImportClassifier.Result;
import io.github.diegoquirino.veredito.domain.importing.StudyPhaseGate;
import io.github.diegoquirino.veredito.importing.CsvImportReader.ParsedCsv;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import io.github.diegoquirino.veredito.persistence.ImportPreviewRepository;
import io.github.diegoquirino.veredito.persistence.ImportPreviewRepository.Preview;
import io.github.diegoquirino.veredito.persistence.ItemRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Builds and reads import previews without writing study data (spec 005 US1). */
@Service
public class PreviewService {

    private static final int PREVIEW_MINUTES = 30;
    private static final int FILE_NAME_MAX = 255;

    private final CsvImportReader reader;
    private final ImportPreviewRepository previews;
    private final ItemRepository items;
    private final ConfigRepository config;
    private final VereditoProperties properties;
    private final JsonMapper json;
    private final MessageSource messages;
    private final Clock clock;

    public PreviewService(
            CsvImportReader reader,
            ImportPreviewRepository previews,
            ItemRepository items,
            ConfigRepository config,
            VereditoProperties properties,
            JsonMapper json,
            MessageSource messages,
            Clock clock) {
        this.reader = reader;
        this.previews = previews;
        this.items = items;
        this.config = config;
        this.properties = properties;
        this.json = json;
        this.messages = messages;
        this.clock = clock;
    }

    public record RowIssue(int line, String code, String reason, String system, String editKey) {}

    public record PreviewView(
            long id,
            String fileName,
            Instant expiresAt,
            int includedCount,
            int ignoredCount,
            int errorCount,
            int blockCount,
            List<Integer> blockSizes,
            List<String> extraColumns,
            List<RowIssue> ignored,
            List<RowIssue> errors) {}

    @Transactional
    public PreviewView create(MultipartFile file, Locale locale) {
        if (file == null || file.isEmpty()) {
            throw ProblemException.invalidFields(Map.of("file", "required"));
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        previews.deleteExpired(now);

        ParsedCsv parsed;
        try (InputStream input = file.getInputStream()) {
            parsed = reader.read(input);
        } catch (IOException ex) {
            throw new ProblemException(ProblemType.CSV_INVALID);
        }

        boolean staging = properties.environment() == Environment.STAGING;
        String phase = config.studyPhase();
        if (StudyPhaseGate.minitestOnly(staging, phase)) {
            List<String> foreign = StudyPhaseGate.foreignSystems(parsed.dataRows());
            if (!foreign.isEmpty()) {
                throw new ProblemException(ProblemType.MINITEST_ONLY, String.join(", ", foreign))
                        .with("systems", foreign);
            }
        }

        int blockSize = config.blockSize();
        Result classified = ImportClassifier.classify(parsed.dataRows(), items.existingKeys());
        List<Integer> blockSizes = BlockPlan.sizes(classified.included().size(), blockSize);
        Instant expiresAt = now.plus(PREVIEW_MINUTES, ChronoUnit.MINUTES);
        String fileName = truncate(originalName(file), FILE_NAME_MAX);

        long id = previews.insert(
                fileName,
                now,
                expiresAt,
                classified.included().size(),
                classified.ignored().size(),
                classified.errors().size(),
                blockSizes.size(),
                blockSize,
                writeJson(parsed.extraColumns()),
                writeJson(classified.included()),
                writeJson(storedIssues(classified.ignored())),
                writeJson(storedIssues(classified.errors())),
                writeJson(blockSizes));

        return view(
                id,
                fileName,
                expiresAt,
                classified.included().size(),
                classified.ignored().size(),
                classified.errors().size(),
                blockSizes,
                parsed.extraColumns(),
                classified.ignored(),
                classified.errors(),
                locale);
    }

    @Transactional(readOnly = true)
    public PreviewView get(long id, Locale locale) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Preview row = previews.findValid(id, now)
                .orElseThrow(() -> new ProblemException(ProblemType.PREVIEW_NOT_FOUND));
        List<Integer> blockSizes = readList(row.blockSizesJson(), Integer.class);
        List<String> extra = readList(row.extraColumnsJson(), String.class);
        List<StoredIssue> ignored = readList(row.ignoredJson(), StoredIssue.class);
        List<StoredIssue> errors = readList(row.errorsJson(), StoredIssue.class);
        return view(
                row.id(),
                row.fileName(),
                row.expiresAt(),
                row.includedCount(),
                row.ignoredCount(),
                row.errorCount(),
                blockSizes,
                extra,
                toClassified(ignored),
                toClassified(errors),
                locale);
    }

    private PreviewView view(
            long id,
            String fileName,
            Instant expiresAt,
            int included,
            int ignored,
            int errors,
            List<Integer> blockSizes,
            List<String> extra,
            List<ClassifiedRow> ignoredRows,
            List<ClassifiedRow> errorRows,
            Locale locale) {
        return new PreviewView(
                id,
                fileName,
                expiresAt,
                included,
                ignored,
                errors,
                blockSizes.size(),
                blockSizes,
                extra,
                localize(ignoredRows, locale),
                localize(errorRows, locale));
    }

    private List<RowIssue> localize(List<ClassifiedRow> rows, Locale locale) {
        List<RowIssue> issues = new ArrayList<>(rows.size());
        for (ClassifiedRow row : rows) {
            issues.add(new RowIssue(
                    row.line(),
                    row.code(),
                    messages.getMessage("import.reason." + row.code(), null, locale),
                    value(row.fields(), "system"),
                    value(row.fields(), "edit_key")));
        }
        return List.copyOf(issues);
    }

    private static List<ClassifiedRow> toClassified(List<StoredIssue> stored) {
        List<ClassifiedRow> rows = new ArrayList<>(stored.size());
        for (StoredIssue issue : stored) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("system", issue.system() == null ? "" : issue.system());
            fields.put("edit_key", issue.editKey() == null ? "" : issue.editKey());
            rows.add(new ClassifiedRow(issue.line(), fields, issue.code()));
        }
        return rows;
    }

    private static List<Map<String, Object>> storedIssues(List<ClassifiedRow> rows) {
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (ClassifiedRow row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("line", row.line());
            map.put("code", row.code());
            map.put("system", value(row.fields(), "system"));
            map.put("editKey", value(row.fields(), "edit_key"));
            out.add(map);
        }
        return out;
    }

    private record StoredIssue(int line, String code, String system, String editKey) {}

    private String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private <T> List<T> readList(String payload, Class<T> elementType) {
        try {
            return json.readerForListOf(elementType).readValue(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? "upload.csv" : name;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String value(Map<String, String> fields, String column) {
        String raw = fields.get(column);
        return raw == null ? "" : raw;
    }
}
