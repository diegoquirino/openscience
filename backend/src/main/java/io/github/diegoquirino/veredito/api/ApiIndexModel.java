package io.github.diegoquirino.veredito.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "ApiIndex", description = "Versions of the API; each one is linked as vd:<version>.")
public class ApiIndexModel extends RepresentationModel<ApiIndexModel> {

    private final List<ApiVersion> versions;

    public ApiIndexModel(List<ApiVersion> versions) {
        this.versions = List.copyOf(versions);
    }

    public List<ApiVersion> getVersions() {
        return versions;
    }

    @Schema(name = "ApiVersion")
    public record ApiVersion(
            @Schema(example = "v1") String version,
            @Schema(description = "A deprecated version also has a sunset date.", example = "false") boolean deprecated) {}
}
