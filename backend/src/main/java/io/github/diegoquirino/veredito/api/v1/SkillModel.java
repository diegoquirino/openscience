package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.onboarding.SkillService.SkillView;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "Skill", description = "The participant's name and professional skill (profile).")
public class SkillModel extends RepresentationModel<SkillModel> {

    private final String name;
    private final String skill;
    private final String skillOther;

    public SkillModel(SkillView view) {
        this.name = view.name();
        this.skill = view.skill();
        this.skillOther = view.skillOther();
    }

    @Schema(description = "Display name; null until the profile step is completed.", example = "Ana Souza")
    public String getName() {
        return name;
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
