package br.edu.ufcg.splab.claret.model;

import java.util.ArrayList;
import java.util.List;

public class ExceptionFlow {
    private final int id;
    private final String description;
    private final List<Step> steps = new ArrayList<>();

    public ExceptionFlow(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<Step> getSteps() {
        return steps;
    }

    @Override
    public String toString() {
        return "ExceptionFlow " + id + " (\"" + description + "\", steps: " + steps.size() + ")";
    }
}
