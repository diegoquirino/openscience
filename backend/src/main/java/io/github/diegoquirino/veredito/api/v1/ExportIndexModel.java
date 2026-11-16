package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "ExportIndex", description = "The systems with items and the links to every CSV download.")
public class ExportIndexModel extends RepresentationModel<ExportIndexModel> {

    private final List<String> systems;

    public ExportIndexModel(List<String> systems) {
        this.systems = List.copyOf(systems);
    }

    @Schema(description = "Each distinct item system, in the queue order of its first item.", example = "[\"minitest\"]")
    public List<String> getSystems() {
        return systems;
    }
}