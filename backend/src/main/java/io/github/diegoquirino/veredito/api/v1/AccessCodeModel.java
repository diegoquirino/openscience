package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.access.AccessCodeService.RequestedCode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "AccessCode", description = "A code was sent; the client offers the code field and the resend timer.")
public class AccessCodeModel extends RepresentationModel<AccessCodeModel> {

    private final String email;
    private final long validityMinutes;
    private final long resendAfterSeconds;

    public AccessCodeModel(RequestedCode requested) {
        this.email = requested.email().value();
        this.validityMinutes = requested.validityMinutes();
        this.resendAfterSeconds = requested.resendAfterSeconds();
    }

    @Schema(description = "The normalized e-mail the code was sent to.", example = "ana@example.com")
    public String getEmail() {
        return email;
    }

    @Schema(description = "Minutes the code stays valid.", example = "10")
    public long getValidityMinutes() {
        return validityMinutes;
    }

    @Schema(description = "Seconds before a new code can be requested for this e-mail.", example = "60")
    public long getResendAfterSeconds() {
        return resendAfterSeconds;
    }
}
