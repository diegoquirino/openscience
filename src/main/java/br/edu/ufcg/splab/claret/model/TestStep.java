package br.edu.ufcg.splab.claret.model;

public class TestStep {
    private final int stepNumber;
    private final String actor;
    private final String action;
    private final String expectedResult;

    public TestStep(int stepNumber, String actor, String action, String expectedResult) {
        this.stepNumber = stepNumber;
        this.actor = actor;
        this.action = action;
        this.expectedResult = expectedResult;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    @Override
    public String toString() {
        return "Step " + stepNumber + " [" + actor + "]: " + action + " -> Expected: " + expectedResult;
    }
}
