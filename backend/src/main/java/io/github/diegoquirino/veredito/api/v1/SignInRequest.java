package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignInRequest", description = "The code received by e-mail.")
public record SignInRequest(
        @Schema(description = "The e-mail the code was sent to.", example = "ana@example.com") String email,
        @Schema(description = "The 6-digit code; spaces and hyphens are ignored.", example = "123 456")
                String code) {}
