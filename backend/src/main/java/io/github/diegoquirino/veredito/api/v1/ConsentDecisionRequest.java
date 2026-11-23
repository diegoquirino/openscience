package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Body of {@code PUT} on {@code vd:consent}; {@code @NotNull} marks the HAL-FORMS properties as required. */
@Schema(name = "ConsentDecisionRequest", description = "The decision on the consent form shown on screen.")
public record ConsentDecisionRequest(
        @NotNull
        @Schema(
                description = "accepted or refused.",
                allowableValues = {"accepted", "refused"},
                example = "accepted",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String decision,
        @NotNull
        @Schema(
                description = "Version of the consent form the participant read; must be the current one.",
                example = "v1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String consentFormVersion,
        @Schema(
                description =
                        "When true (consent UI choice \"No\" / remain anonymous), paper/thesis acknowledgments "
                                + "omit the name and count this person under \"Rotuladores Anonimizados por pedido [N]\". "
                                + "Default false (choice \"Yes\" / list first+last name). "
                                + "Ignored when decision is refused.",
                example = "false",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean acknowledgmentAnonymous) {}