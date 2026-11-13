package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.api.v1.LinkRelations.Relation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "Relation", description = "Documentation of one vd: link relation.")
public class RelationModel extends RepresentationModel<RelationModel> {

    private final Relation relation;

    RelationModel(Relation relation) {
        this.relation = relation;
    }

    @Schema(example = "vd:request-code")
    public String getRel() {
        return relation.curied();
    }

    @Schema(example = "Request a sign-in code by e-mail.")
    public String getDescription() {
        return relation.description();
    }

    @Schema(description = "HTTP method of the operation behind the link.", example = "POST")
    public String getMethod() {
        return relation.method().name();
    }

    @Schema(description = "Swagger UI address of the operation.", example = "/swagger-ui.html#/Access")
    public String getDocumentation() {
        return relation.documentation();
    }
}
