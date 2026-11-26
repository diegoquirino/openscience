package io.github.diegoquirino.veredito.api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.diegoquirino.veredito.persistence.ItemRepository.Item;
import io.github.diegoquirino.veredito.persistence.ResponseRepository.ResponseRow;
import io.github.diegoquirino.veredito.queue.DraftService.ItemView;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/** An item of a held task and the caller's own draft; null sides and a null draft are explicit. */
@Schema(name = "TaskItem", description = "One item of a task's block with the caller's own response, if any.")
@Relation(collectionRelation = "vd:items", itemRelation = "vd:item")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class TaskItemModel extends RepresentationModel<TaskItemModel> {

    private final int position;
    private final long itemId;
    private final String system;
    private final String uc;
    private final String kind;
    private final String field;
    private final String originVersion;
    private final String targetVersion;
    private final String originUsecase;
    private final String targetUsecase;
    private final String originTests;
    private final String targetTests;
    private final String origin;
    private final String target;
    private final Draft draft;

    public TaskItemModel(ItemView view) {
        Item item = view.item();
        this.position = view.position();
        this.itemId = item.id();
        this.system = item.system();
        this.uc = item.uc();
        this.kind = item.kind();
        this.field = item.field();
        this.originVersion = item.originVersion();
        this.targetVersion = item.targetVersion();
        this.originUsecase = item.originUsecase();
        this.targetUsecase = item.targetUsecase();
        this.originTests = item.originTests();
        this.targetTests = item.targetTests();
        this.origin = item.origin();
        this.target = item.target();
        this.draft = view.draft() == null ? null : Draft.of(view.draft());
    }

    @Schema(name = "TaskItemDraft")
    public record Draft(String changeImpact, String maintenanceOperation, String comment, String language) {

        static Draft of(ResponseRow row) {
            return new Draft(row.changeImpact(), row.maintenanceOperation(), row.comment(), row.language());
        }
    }

    public int getPosition() {
        return position;
    }

    public long getItemId() {
        return itemId;
    }

    public String getSystem() {
        return system;
    }

    public String getUc() {
        return uc;
    }

    public String getKind() {
        return kind;
    }

    public String getField() {
        return field;
    }

    public String getOriginVersion() {
        return originVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public String getOriginUsecase() {
        return originUsecase;
    }

    public String getTargetUsecase() {
        return targetUsecase;
    }

    public String getOriginTests() {
        return originTests;
    }

    public String getTargetTests() {
        return targetTests;
    }

    public String getOrigin() {
        return origin;
    }

    public String getTarget() {
        return target;
    }

    public Draft getDraft() {
        return draft;
    }
}
