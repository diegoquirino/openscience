package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.onboarding.SkillService.SkillView;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "Skill", description = "The participant's professional skill.")
public class SkillModel extends RepresentationModel<SkillModel> {

    private final String skill;
    private final String skillOther;

    public SkillModel(SkillView view) {
        this.skill = view.skill();
        this.skillOther = view.skillOther();
    }

    @Schema(description = "Skill code; null until answered.", example = "test_analyst")
    public String getSkill() {
        return skill;
    }

    @Schema(description = "Description typed with skill = other, as typed.", example = "Technical writer")
    public String getSkillOther() {
        return skillOther;
    }
}