package br.edu.ufcg.splab.claret.model;

import java.util.ArrayList;
import java.util.List;

public class ClaretSystem {
    private final String name;
    private final List<UseCase> useCases = new ArrayList<>();

    public ClaretSystem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<UseCase> getUseCases() {
        return useCases;
    }

    @Override
    public String toString() {
        return "ClaretSystem \"" + name + "\" (useCases: " + useCases.size() + ")";
    }
}
