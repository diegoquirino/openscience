package io.github.diegoquirino.veredito.exporting;

import io.github.diegoquirino.veredito.api.ProblemException;
import io.github.diegoquirino.veredito.api.ProblemType;
import io.github.diegoquirino.veredito.domain.exporting.CsvWriter;
import io.github.diegoquirino.veredito.domain.exporting.ExportColumns;
import io.github.diegoquirino.veredito.domain.exporting.ExportFileNames;
import io.github.diegoquirino.veredito.domain.exporting.ExportRows;
import io.github.diegoquirino.veredito.exporting.ExportQuery.ItemRow;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the five CSV downloads of spec 009 from the final responses; read-only, regenerated on each call. */
@Service
public class ExportService {

    private final ExportQuery query;

    public ExportService(ExportQuery query) {
        this.query = query;
    }

    /** Systems with items, in the order of their first item in queue order. */
    @Transactional(readOnly = true)
    public List<String> systems() {
        return query.systems();
    }

    @Transactional(readOnly = true)
    public CsvDownload wide() {
        CsvWriter csv = new CsvWriter(ExportColumns.WIDE);
        query.itemRows().forEach(row -> csv.record(ExportRows.wide(row.item(), row.role1(), row.role2())));
        return new CsvDownload(ExportFileNames.WIDE, csv.bytes());
    }

    @Transactional(readOnly = true)
    public CsvDownload longTable() {
        CsvWriter csv = new CsvWriter(ExportColumns.LONG);
        query.longResponses().forEach(response -> csv.record(ExportRows.longRow(response)));
        return new CsvDownload(ExportFileNames.LONG, csv.bytes());
    }

    @Transactional(readOnly = true)
    public CsvDownload completo(String system) {
        List<ItemRow> rows = rowsOf(system);
        CsvWriter csv = new CsvWriter(ExportColumns.COMPLETO);
        rows.forEach(row -> csv.record(ExportRows.completo(row.item(), row.role1(), row.role2())));
        return new CsvDownload(ExportFileNames.completo(system), csv.bytes());
    }

    /** The pipeline file of role 1 or 2 for one system. */
    @Transactional(readOnly = true)
    public CsvDownload rater(String system, int role) {
        List<ItemRow> rows = rowsOf(system);
        CsvWriter csv = new CsvWriter(ExportColumns.RATER);
        rows.forEach(row -> csv.record(ExportRows.rater(row.item(), role == 1 ? row.role1() : row.role2())));
        return new CsvDownload(ExportFileNames.rater(system, role), csv.bytes());
    }

    private List<ItemRow> rowsOf(String system) {
        if (system == null || system.isBlank()) {
            throw ProblemException.invalidFields(Map.of("system", "required"));
        }
        if (!query.systemExists(system)) {
            throw new ProblemException(ProblemType.SYSTEM_NOT_FOUND, system).with("system", system);
        }
        return query.itemRows(system);
    }
}