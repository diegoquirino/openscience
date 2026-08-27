package br.edu.ufcg.splab.claret.model;

import java.util.ArrayList;
import java.util.List;

public class TestCase {
    private final String id;
    private final String systemName;
    private final String useCaseName;
    private final String useCaseVersion;
    private final String summary;
    private final String preCondition;
    private final String postCondition;
    private final List<TestStep> steps = new ArrayList<>();

    public TestCase(String id, String systemName, String useCaseName, String useCaseVersion, String summary, String preCondition, String postCondition) {
        this.id = id;
        this.systemName = systemName != null && !systemName.isBlank() ? systemName : "System";
        this.useCaseName = useCaseName;
        this.useCaseVersion = useCaseVersion != null && !useCaseVersion.isBlank() ? useCaseVersion : "1.0";
        this.summary = summary;
        this.preCondition = preCondition;
        this.postCondition = postCondition;
    }

    public TestCase(String id, String systemName, String useCaseName, String summary, String preCondition, String postCondition) {
        this(id, systemName, useCaseName, "1.0", summary, preCondition, postCondition);
    }

    public TestCase(String id, String useCaseName, String summary, String preCondition, String postCondition) {
        this(id, "System", useCaseName, "1.0", summary, preCondition, postCondition);
    }

    public String getId() {
        return id;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getUseCaseName() {
        return useCaseName;
    }

    public String getUseCaseVersion() {
        return useCaseVersion;
    }

    public String getSummary() {
        return summary;
    }

    public String getPreCondition() {
        return preCondition;
    }

    public String getPostCondition() {
        return postCondition;
    }

    public List<TestStep> getSteps() {
        return steps;
    }

    @Override
    public String toString() {
        return "TestCase [" + id + "] " + summary + " (" + steps.size() + " steps)";
    }
}
