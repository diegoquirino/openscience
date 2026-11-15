package io.github.diegoquirino.veredito.domain.onboarding;

import java.util.List;

/**
 * The fixed English use case and test case shown before the screening question (Appendix B.3). Study data,
 * never translated; {@link #requireValid()} fails with a clear message when a required part is missing.
 */
public record ScreeningExample(String version, UseCase useCase, TestCase testCase) {

    public record UseCase(String id, String title, List<String> basicFlow, ExceptionFlow exceptionFlow) {}

    public record ExceptionFlow(String id, String condition, List<String> steps) {}

    public record TestCase(String id, String title, List<String> derivedFrom, List<String> steps, String expected) {}

    /** Required: titles, a basic flow, an exception flow with condition and steps, test case steps and result. */
    public ScreeningExample requireValid() {
        require(version, "version");
        require(useCase != null, "useCase");
        require(useCase.title(), "useCase.title");
        require(useCase.basicFlow(), "useCase.basicFlow");
        require(useCase.exceptionFlow() != null, "useCase.exceptionFlow");
        require(useCase.exceptionFlow().condition(), "useCase.exceptionFlow.condition");
        require(useCase.exceptionFlow().steps(), "useCase.exceptionFlow.steps");
        require(testCase != null, "testCase");
        require(testCase.title(), "testCase.title");
        require(testCase.steps(), "testCase.steps");
        require(testCase.expected(), "testCase.expected");
        return this;
    }

    private static void require(String text, String part) {
        require(text != null && !text.isBlank(), part);
    }

    private static void require(List<String> steps, String part) {
        require(steps != null && !steps.isEmpty() && steps.stream().noneMatch(step -> step == null || step.isBlank()),
                part + " (at least one non-blank step)");
    }

    private static void require(boolean present, String part) {
        if (!present) {
            throw new IllegalStateException("The screening example is missing or has an empty part: " + part);
        }
    }
}
