package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.domain.onboarding.ScreeningExample;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningExample.TestCase;
import io.github.diegoquirino.veredito.domain.onboarding.ScreeningExample.UseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "ScreeningExample", description = "The fixed English use case and test case of the screening.")
public class ScreeningExampleModel extends RepresentationModel<ScreeningExampleModel> {

    private final String version;
    private final UseCase useCase;
    private final TestCase testCase;

    public ScreeningExampleModel(ScreeningExample example) {
        this.version = example.version();
        this.useCase = example.useCase();
        this.testCase = example.testCase();
    }

    @Schema(description = "Version of the example file.", example = "1")
    public String getVersion() {
        return version;
    }

    @Schema(description = "A use case with a basic flow and an exception flow; English in every language.")
    public UseCase getUseCase() {
        return useCase;
    }

    @Schema(description = "A test case derived from the exception flow; English in every language.")
    public TestCase getTestCase() {
        return testCase;
    }
}