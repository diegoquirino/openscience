package br.edu.ufcg.splab.claret.model;

public class Actor {
    private final String id;
    private final String description;

    public Actor(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return id + " (\"" + description + "\")";
    }
}
