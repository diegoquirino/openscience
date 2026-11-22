package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "OrganizationSuggestions", description = "Autocomplete values for organization other.")
public class OrganizationSuggestionsModel extends RepresentationModel<OrganizationSuggestionsModel> {

    private final List<String> suggestions;

    public OrganizationSuggestionsModel(List<String> suggestions) {
        this.suggestions = List.copyOf(suggestions);
    }

    @Schema(description = "Distinct organizationOther strings from other participants.")
    public List<String> getSuggestions() {
        return suggestions;
    }
}
