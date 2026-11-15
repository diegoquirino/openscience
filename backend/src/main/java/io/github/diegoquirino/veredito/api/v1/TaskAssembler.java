package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.domain.queue.QueueOutcome;
import io.github.diegoquirino.veredito.queue.TaskReservationService.TaskView;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/** HAL links for a task representation (spec 007). */
@Component
public class TaskAssembler {

    public TaskModel toModel(TaskView view) {
        TaskModel model = new TaskModel(view);
        long id = view.task().id();
        String self = "/api/v1/tasks/" + id;
        model.add(Link.of(self));
        if ("reserved".equals(view.task().status())) {
            model.add(Link.of(self + "/items/{position}", LinkRelations.ITEMS.curied()));
            model.add(Link.of(self, LinkRelations.REVIEW.curied()));
            model.add(Link.of(self + "/responses/{itemId}", LinkRelations.SAVE_RESPONSE.curied()));
            if (view.answeredCount() == view.size()) {
                model.add(Affordances.of(Link.of(self + "/submission", LinkRelations.SUBMIT.curied()))
                        .afford(HttpMethod.POST)
                        .withName("default")
                        .toLink());
            }
        } else if ("completed".equals(view.task().status()) && view.outcome() == QueueOutcome.MORE_AVAILABLE) {
            model.add(Link.of(EntryPointController.TASK_RESERVATIONS, LinkRelations.NEXT_TASK.curied()));
        }
        return model;
    }
}
