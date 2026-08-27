package br.edu.ufcg.splab.claret.generator;

import br.edu.ufcg.splab.claret.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class AltsGenerator {

    public static String generateAltsString(UseCase uc) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTS_SPECIFICATION \"").append(uc.getName()).append("\" {\n");
        sb.append("  VERSION: \"").append(uc.getVersion()).append("\"\n");
        sb.append("  PRE_CONDITION: \"").append(uc.getPreCondition()).append("\"\n");
        sb.append("  POST_CONDITION: \"").append(uc.getPostCondition()).append("\"\n\n");

        sb.append("  ACTORS {\n");
        for (Actor a : uc.getActors().values()) {
            sb.append("    ").append(a.getId()).append(": \"").append(a.getDescription()).append("\"\n");
        }
        sb.append("  }\n\n");

        sb.append("  STATES {\n");
        sb.append("    S_INIT [initial, guard: \"").append(uc.getPreCondition()).append("\"]\n");

        for (Step s : uc.getBasicFlow()) {
            sb.append("    S_BF_").append(s.getNumber())
              .append(" [flow: \"basic\", actor: \"").append(s.getActor())
              .append("\", action: \"").append(escapeStr(s.getAction())).append("\"");
            if (!s.getAlternativeFlowIds().isEmpty()) {
                sb.append(", branches_af: ").append(s.getAlternativeFlowIds());
            }
            if (!s.getExceptionFlowIds().isEmpty()) {
                sb.append(", branches_ef: ").append(s.getExceptionFlowIds());
            }
            sb.append("]\n");
        }

        for (AlternativeFlow af : uc.getAlternatives()) {
            for (Step s : af.getSteps()) {
                sb.append("    S_AF").append(af.getId()).append("_").append(s.getNumber())
                  .append(" [flow: \"alternative_").append(af.getId())
                  .append("\", description: \"").append(escapeStr(af.getDescription()))
                  .append("\", actor: \"").append(s.getActor())
                  .append("\", action: \"").append(escapeStr(s.getAction())).append("\"");
                if (s.getBasicFlowStepReturn() != null) {
                    sb.append(", returns_to_bf: ").append(s.getBasicFlowStepReturn());
                }
                sb.append("]\n");
            }
        }

        for (ExceptionFlow ef : uc.getExceptions()) {
            for (Step s : ef.getSteps()) {
                sb.append("    S_EF").append(ef.getId()).append("_").append(s.getNumber())
                  .append(" [flow: \"exception_").append(ef.getId())
                  .append("\", description: \"").append(escapeStr(ef.getDescription()))
                  .append("\", actor: \"").append(s.getActor())
                  .append("\", action: \"").append(escapeStr(s.getAction())).append("\"]\n");
            }
        }

        sb.append("    S_END [terminal, guard: \"").append(uc.getPostCondition()).append("\"]\n");
        sb.append("  }\n\n");

        sb.append("  TRANSITIONS {\n");

        if (!uc.getBasicFlow().isEmpty()) {
            sb.append("    S_INIT -> S_BF_").append(uc.getBasicFlow().get(0).getNumber())
              .append(" [trigger: \"Start\"]\n");
        }

        for (int i = 0; i < uc.getBasicFlow().size(); i++) {
            Step current = uc.getBasicFlow().get(i);
            String fromState = "S_BF_" + current.getNumber();

            if (i + 1 < uc.getBasicFlow().size()) {
                Step next = uc.getBasicFlow().get(i + 1);
                String toState = "S_BF_" + next.getNumber();
                sb.append("    ").append(fromState).append(" -> ").append(toState)
                  .append(" [event: \"").append(next.getActor()).append(": ").append(escapeStr(next.getAction())).append("\"]\n");
            } else {
                sb.append("    ").append(fromState).append(" -> S_END [event: \"Complete\"]\n");
            }

            for (int afId : current.getAlternativeFlowIds()) {
                AlternativeFlow af = uc.getAlternativeById(afId);
                if (af != null && !af.getSteps().isEmpty()) {
                    sb.append("    ").append(fromState).append(" -> S_AF").append(afId).append("_").append(af.getSteps().get(0).getNumber())
                      .append(" [branch: \"AF_").append(afId).append(" - ").append(escapeStr(af.getDescription())).append("\"]\n");
                }
            }

            for (int efId : current.getExceptionFlowIds()) {
                ExceptionFlow ef = uc.getExceptionById(efId);
                if (ef != null && !ef.getSteps().isEmpty()) {
                    sb.append("    ").append(fromState).append(" -> S_EF").append(efId).append("_").append(ef.getSteps().get(0).getNumber())
                      .append(" [branch: \"EF_").append(efId).append(" - ").append(escapeStr(ef.getDescription())).append("\"]\n");
                }
            }
        }

        for (AlternativeFlow af : uc.getAlternatives()) {
            for (int j = 0; j < af.getSteps().size(); j++) {
                Step current = af.getSteps().get(j);
                String fromState = "S_AF" + af.getId() + "_" + current.getNumber();

                if (j + 1 < af.getSteps().size()) {
                    Step next = af.getSteps().get(j + 1);
                    String toState = "S_AF" + af.getId() + "_" + next.getNumber();
                    sb.append("    ").append(fromState).append(" -> ").append(toState)
                      .append(" [event: \"").append(next.getActor()).append(": ").append(escapeStr(next.getAction())).append("\"]\n");
                } else {
                    if (current.getBasicFlowStepReturn() != null) {
                        sb.append("    ").append(fromState).append(" -> S_BF_").append(current.getBasicFlowStepReturn())
                          .append(" [resume_basic_flow: ").append(current.getBasicFlowStepReturn()).append("]\n");
                    } else {
                        sb.append("    ").append(fromState).append(" -> S_END [event: \"Complete\"]\n");
                    }
                }
            }
        }

        for (ExceptionFlow ef : uc.getExceptions()) {
            for (int j = 0; j < ef.getSteps().size(); j++) {
                Step current = ef.getSteps().get(j);
                String fromState = "S_EF" + ef.getId() + "_" + current.getNumber();

                if (j + 1 < ef.getSteps().size()) {
                    Step next = ef.getSteps().get(j + 1);
                    String toState = "S_EF" + ef.getId() + "_" + next.getNumber();
                    sb.append("    ").append(fromState).append(" -> ").append(toState)
                      .append(" [event: \"").append(next.getActor()).append(": ").append(escapeStr(next.getAction())).append("\"]\n");
                } else {
                    sb.append("    ").append(fromState).append(" -> S_END [event: \"ErrorExit\"]\n");
                }
            }
        }

        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }

    public static void generateAlts(UseCase uc, File outputFile) throws IOException {
        String content = generateAltsString(uc);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static String escapeStr(String text) {
        if (text == null) return "";
        return text.replace("\"", "'").replace("\n", " ").trim();
    }
}
