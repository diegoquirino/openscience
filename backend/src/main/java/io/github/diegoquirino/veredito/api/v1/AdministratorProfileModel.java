package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.persistence.ConfigRepository.AdminProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "AdministratorProfile", description = "The administrator's own profile.")
public class AdministratorProfileModel extends RepresentationModel<AdministratorProfileModel> {

    private final String name;
    private final String email;
    private final String preferredLanguage;

    public AdministratorProfileModel(AdminProfile profile) {
        this.name = profile.name();
        this.email = profile.email();
        this.preferredLanguage = profile.preferredLanguage();
    }

    @Schema(description = "The latest name typed at sign-in; null before the first sign-in.", example = "Diego Quirino")
    public String getName() {
        return name;
    }

    @Schema(description = "The administrator's e-mail, from config.admin_email.", example = "diegoquirino@gmail.com")
    public String getEmail() {
        return email;
    }

    @Schema(description = "pt-BR, en-US or es-ES; set at the first sign-in.", example = "pt-BR")
    public String getPreferredLanguage() {
        return preferredLanguage;
    }
}
