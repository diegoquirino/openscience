package br.edu.ufcg.splab.claret.model;

import java.util.ArrayList;
import java.util.List;

public class Step {
    private final int number;
    private final String actor;
    private final String action;
    private final List<Integer> alternativeFlowIds = new ArrayList<>();
    private final List<Integer> exceptionFlowIds = new ArrayList<>();
    private Integer basicFlowStepReturn = null;

    public Step(int number, String actor, String action) {
        this.number = number;
        this.actor = actor;
        this.action = action;
    }

    public int getNumber() {
        return number;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public List<Integer> getAlternativeFlowIds() {
        return alternativeFlowIds;
    }

    public List<Integer> getExceptionFlowIds() {
        return exceptionFlowIds;
    }

    public Integer getBasicFlowStepReturn() {
        return basicFlowStepReturn;
    }

    public void setBasicFlowStepReturn(Integer basicFlowStepReturn) {
        this.basicFlowStepReturn = basicFlowStepReturn;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("step ").append(number).append(" ").append(actor).append(" \"").append(action).append("\"");
        if (!alternativeFlowIds.isEmpty()) {
            sb.append(" af").append(alternativeFlowIds);
        }
        if (!exceptionFlowIds.isEmpty()) {
            sb.append(" ef").append(exceptionFlowIds);
        }
        if (basicFlowStepReturn != null) {
            sb.append(" bs ").append(basicFlowStepReturn);
        }
        return sb.toString();
    }
}
