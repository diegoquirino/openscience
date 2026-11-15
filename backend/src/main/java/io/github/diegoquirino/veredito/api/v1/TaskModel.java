package io.github.diegoquirino.veredito.api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.diegoquirino.veredito.domain.queue.QueueOutcome;
import io.github.diegoquirino.veredito.persistence.TaskRepository.TaskRow;
import io.github.diegoquirino.veredito.queue.TaskReservationService.TaskView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Schema(name = "Task", description = "A block-role in the labeling queue.")
@Relation(collectionRelation = "vd:tasks", itemRelation = "vd:task")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskModel extends RepresentationModel<TaskModel> {

    private final long id;
    private final int blockNumber;
    private final int role;
    private final String status;
    private final int size;
    private final long answeredCount;
    private final Instant reservationExpiresAt;
    private final Integer reservationMinutes;
    private final String outcome;
    private final Instant serverNow;

    public TaskModel(TaskView view) {
        TaskRow task = view.task();
        this.id = task.id();
        this.blockNumber = view.blockNumber();
        this.role = task.role();
        this.status = task.status();
        this.size = view.size();
        this.answeredCount = view.answeredCount();
        this.reservationExpiresAt = "reserved".equals(task.status()) ? task.expiresAt() : null;
        this.reservationMinutes = "reserved".equals(task.status()) ? view.reservationMinutes() : null;
        QueueOutcome outcome = view.outcome();
        this.outcome = outcome == null ? null : outcome.slug();
        this.serverNow = view.serverNow();
    }

    public long getId() {
        return id;
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public int getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public int getSize() {
        return size;
    }

    public long getAnsweredCount() {
        return answeredCount;
    }

    public Instant getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public Integer getReservationMinutes() {
        return reservationMinutes;
    }

    public String getOutcome() {
        return outcome;
    }

    public Instant getServerNow() {
        return serverNow;
    }
}
