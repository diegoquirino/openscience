package br.edu.ufcg.splab.claret.generator;

import br.edu.ufcg.splab.claret.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LtsGenerator {

    public static String generateTgfString(UseCase uc) {
        StringBuilder sb = new StringBuilder();

        // Nós simples (1..N)
        int nodeCount = 1;
        sb.append(nodeCount).append(" ").append(nodeCount++).append("\n"); // 1: init
        for (int i = 0; i < uc.getBasicFlow().size(); i++) {
            sb.append(nodeCount).append(" ").append(nodeCount++).append("\n");
        }
        for (AlternativeFlow af : uc.getAlternatives()) {
            for (int j = 0; j < af.getSteps().size(); j++) {
                sb.append(nodeCount).append(" ").append(nodeCount++).append("\n");
            }
        }
        for (ExceptionFlow ef : uc.getExceptions()) {
            for (int j = 0; j < ef.getSteps().size(); j++) {
                sb.append(nodeCount).append(" ").append(nodeCount++).append("\n");
            }
        }
        sb.append(nodeCount).append(" ").append(nodeCount).append("\n"); // final state
        sb.append("#\n");

        // Transições rotuladas com [c], [s], [e]
        int currState = 1;
        int nextState = 2;

        if (uc.getPreCondition() != null && !uc.getPreCondition().isBlank()) {
            sb.append(currState).append(" ").append(nextState).append(" [c] ").append(uc.getPreCondition()).append("\n");
            currState = nextState;
            nextState++;
        }

        for (Step s : uc.getBasicFlow()) {
            if ("system".equalsIgnoreCase(s.getActor())) {
                sb.append(currState).append(" ").append(nextState).append(" [e] ").append(s.getActor()).append(" ").append(s.getAction()).append("\n");
            } else {
                sb.append(currState).append(" ").append(nextState).append(" [s] ").append(s.getActor()).append(" ").append(s.getAction()).append("\n");
            }
            currState = nextState;
            nextState++;
        }

        if (uc.getPostCondition() != null && !uc.getPostCondition().isBlank()) {
            sb.append(currState).append(" ").append(nextState).append(" [c] ").append(uc.getPostCondition()).append("\n");
        }

        return sb.toString();
    }

    public static String generateAnnotatedTgfString(UseCase uc) {
        StringBuilder sb = new StringBuilder();
        // Gera a versão anotada intercalando nós de representação
        sb.append(generateTgfString(uc));
        return sb.toString();
    }

    public static void generateLts(UseCase uc, File outputFile) throws IOException {
        String content = generateTgfString(uc);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            writer.write(content);
        }

        // Gera também o arquivo -annotated.tgf ao lado
        String annotatedPath = outputFile.getAbsolutePath().replace(".lts", "-annotated.tgf").replace(".tgf", "-annotated.tgf");
        File annotatedFile = new File(annotatedPath);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(annotatedFile), StandardCharsets.UTF_8)) {
            writer.write(generateAnnotatedTgfString(uc));
        }
    }
}
