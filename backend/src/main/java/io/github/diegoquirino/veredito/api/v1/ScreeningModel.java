package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.onboarding.ScreeningService.ScreeningView;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "Screening", description = "The participant's screening answer.")
public class ScreeningModel extends RepresentationModel<ScreeningModel> {

    private final String answer;
    private final Boolean eligible;

    public ScreeningModel(ScreeningView view) {
        this.answer = view.answer() == null ? null : view.answer().code();
        this.eligible = view.eligible();
    }

    @Schema(description = "yes_professional, yes_academic or no; null until answered.", example = "yes_academic")
    public String getAnswer() {
        return answer;
    }

    @Schema(description = "True for the two yes answers, false for no, null until answered.", example = "true")
    public Boolean getEligible() {
        return eligible;
    }
}