package br.edu.ufcg.splab.claret.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UseCase {
    private final String name;
    private String version = "1.0";
    private String type = "Creation";
    private String user = "";
    private String date = "";
    private String preCondition = "";
    private String postCondition = "";
    private final Map<String, Actor> actors = new LinkedHashMap<>();
    private final List<Step> basicFlow = new ArrayList<>();
    private final List<AlternativeFlow> alternatives = new ArrayList<>();
    private final List<ExceptionFlow> exceptions = new ArrayList<>();

    public UseCase(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPreCondition() {
        return preCondition;
    }

    public void setPreCondition(String preCondition) {
        this.preCondition = preCondition;
    }

    public String getPostCondition() {
        return postCondition;
    }

    public void setPostCondition(String postCondition) {
        this.postCondition = postCondition;
    }

    public Map<String, Actor> getActors() {
        return actors;
    }

    public List<Step> getBasicFlow() {
        return basicFlow;
    }

    public List<AlternativeFlow> getAlternatives() {
        return alternatives;
    }

    public List<ExceptionFlow> getExceptions() {
        return exceptions;
    }

    public AlternativeFlow getAlternativeById(int id) {
        for (AlternativeFlow af : alternatives) {
            if (af.getId() == id) return af;
        }
        return null;
    }

    public ExceptionFlow getExceptionById(int id) {
        for (ExceptionFlow ef : exceptions) {
            if (ef.getId() == id) return ef;
        }
        return null;
    }

    @Override
    public String toString() {
        return "UseCase \"" + name + "\" (v" + version + ", steps: " + basicFlow.size() +
               ", alts: " + alternatives.size() + ", excs: " + exceptions.size() + ")";
    }
}
