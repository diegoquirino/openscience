package io.github.diegoquirino.veredito.api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.diegoquirino.veredito.importing.BatchImportService.ConfirmResult;
import io.github.diegoquirino.veredito.importing.BatchImportService.QueuePositions;
import io.github.diegoquirino.veredito.importing.PreviewService.RowIssue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "ImportBatch", description = "Result of confirming a CSV import preview.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportBatchModel extends RepresentationModel<ImportBatchModel> {

    private final Long id;
    private final String fileName;
    private final Instant importedAt;
    private final int includedCount;
    private final int ignoredCount;
    private final int errorCount;
    private final int blockCount;
    private final List<Integer> blockSizes;
    private final QueuePositions queuePositions;
    private final List<String> systems;
    private final List<ImportPreviewModel.RowIssueModel> ignored;

    public ImportBatchModel(ConfirmResult result) {
        this.id = result.id();
        this.fileName = result.fileName();
        this.importedAt = result.importedAt();
        this.includedCount = result.includedCount();
        this.ignoredCount = result.ignoredCount();
        this.errorCount = result.errorCount();
        this.blockCount = result.blockCount();
        this.blockSizes = result.blockSizes();
        this.queuePositions = result.queuePositions();
        this.systems = result.systems();
        this.ignored = result.ignored().stream().map(ImportPreviewModel.RowIssueModel::new).toList();
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public int getIncludedCount() {
        return includedCount;
    }

    public int getIgnoredCount() {
        return ignoredCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public List<Integer> getBlockSizes() {
        return blockSizes;
    }

    public QueuePositions getQueuePositions() {
        return queuePositions;
    }

    public List<String> getSystems() {
        return systems;
    }

    public List<ImportPreviewModel.RowIssueModel> getIgnored() {
        return ignored;
    }
}
