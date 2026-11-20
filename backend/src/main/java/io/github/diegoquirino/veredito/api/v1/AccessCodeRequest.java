package io.github.diegoquirino.veredito.api.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccessCodeRequest", description = "Who asks for a sign-in code (e-mail only, spec 013).")
public record AccessCodeRequest(
        @Schema(
                        description = "E-mail that receives the code; trimmed and lowercased, at most 254 characters.",
                        example = "ana@example.com")
                String email) {}
