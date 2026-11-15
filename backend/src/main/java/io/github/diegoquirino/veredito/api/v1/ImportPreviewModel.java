package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.importing.PreviewService.PreviewView;
import io.github.diegoquirino.veredito.importing.PreviewService.RowIssue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "ImportPreview", description = "Classification of a CSV before anything is stored in the study.")
public class ImportPreviewModel extends RepresentationModel<ImportPreviewModel> {

    @Schema(example = "12")
    private final long id;

    @Schema(example = "veredito_minitest_vazio.csv")
    private final String fileName;

    private final Instant expiresAt;
    private final int includedCount;
    private final int ignoredCount;
    private final int errorCount;
    private final int blockCount;
    private final List<Integer> blockSizes;
    private final List<String> extraColumns;
    private final List<RowIssueModel> ignored;
    private final List<RowIssueModel> errors;

    public ImportPreviewModel(PreviewView view) {
        this.id = view.id();
        this.fileName = view.fileName();
        this.expiresAt = view.expiresAt();
        this.includedCount = view.includedCount();
        this.ignoredCount = view.ignoredCount();
        this.errorCount = view.errorCount();
        this.blockCount = view.blockCount();
        this.blockSizes = view.blockSizes();
        this.extraColumns = view.extraColumns();
        this.ignored = view.ignored().stream().map(RowIssueModel::new).toList();
        this.errors = view.errors().stream().map(RowIssueModel::new).toList();
    }

    public long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public Instant getExpiresAt() {
        return expiresAt;
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

    public List<String> getExtraColumns() {
        return extraColumns;
    }

    public List<RowIssueModel> getIgnored() {
        return ignored;
    }

    public List<RowIssueModel> getErrors() {
        return errors;
    }

    @Schema(name = "ImportRowIssue")
    public static final class RowIssueModel {
        private final int line;
        private final String code;
        private final String reason;
        private final String system;
        private final String editKey;

        public RowIssueModel(RowIssue issue) {
            this.line = issue.line();
            this.code = issue.code();
            this.reason = issue.reason();
            this.system = issue.system();
            this.editKey = issue.editKey();
        }

        public int getLine() {
            return line;
        }

        public String getCode() {
            return code;
        }

        public String getReason() {
            return reason;
        }

        public String getSystem() {
            return system;
        }

        public String getEditKey() {
            return editKey;
        }
    }
}
