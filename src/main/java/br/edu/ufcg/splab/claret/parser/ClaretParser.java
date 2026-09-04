package br.edu.ufcg.splab.claret.parser;

import br.edu.ufcg.splab.claret.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClaretParser {

    public static ClaretSystem parse(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return parseString(content);
    }

    public static ClaretSystem parseString(String content) {
        // Normaliza quebras de linha
        content = content.replace("\r\n", "\n").replace("\r", "\n");

        // 1. Extrai o nome do sistema
        String systemName = "System";
        Matcher sysMatcher1 = Pattern.compile("systemName\\s+\"([^\"]+)\"").matcher(content);
        if (sysMatcher1.find()) {
            systemName = sysMatcher1.group(1);
        } else {
            Matcher sysMatcher2 = Pattern.compile("system\\s+\"([^\"]+)\"").matcher(content);
            if (sysMatcher2.find()) {
                systemName = sysMatcher2.group(1);
            }
        }

        ClaretSystem claretSystem = new ClaretSystem(systemName);

        // 2. Extrai os blocos usecase
        Pattern ucPattern = Pattern.compile("usecase\\s+\"([^\"]+)\"\\s*,?\\s*\\{");
        Matcher ucMatcher = ucPattern.matcher(content);

        while (ucMatcher.find()) {
            String ucName = ucMatcher.group(1);
            int blockStartIndex = ucMatcher.end() - 1;
            int blockEndIndex = findMatchingBrace(content, blockStartIndex);

            if (blockEndIndex > blockStartIndex) {
                String ucBody = content.substring(blockStartIndex + 1, blockEndIndex);
                UseCase uc = parseUseCaseBody(ucName, ucBody);
                claretSystem.getUseCases().add(uc);
            }
        }

        return claretSystem;
    }

    private static UseCase parseUseCaseBody(String ucName, String body) {
        UseCase uc = new UseCase(ucName);

        // Version & Metadata
        Matcher verMatcher = Pattern.compile("version\\s+\"([^\"]+)\"").matcher(body);
        if (verMatcher.find()) {
            uc.setVersion(verMatcher.group(1));
        }

        Matcher typeMatcher = Pattern.compile("type\\s*[:=]?\\s*\"([^\"]+)\"").matcher(body);
        if (typeMatcher.find()) {
            uc.setType(typeMatcher.group(1));
        }

        Matcher userMatcher = Pattern.compile("user\\s*[:=]?\\s*\"([^\"]+)\"").matcher(body);
        if (userMatcher.find()) {
            uc.setUser(userMatcher.group(1));
        }

        Matcher dateMatcher = Pattern.compile("date\\s*[:=]?\\s*\"([^\"]+)\"").matcher(body);
        if (dateMatcher.find()) {
            uc.setDate(dateMatcher.group(1));
        }

        // Actors
        Pattern actorPattern = Pattern.compile("actor\\s+([^,\\s\":]+)\\s*[:=,]?\\s*\"([^\"]+)\"");
        Matcher actorMatcher = actorPattern.matcher(body);
        while (actorMatcher.find()) {
            String actorId = actorMatcher.group(1);
            String actorDesc = actorMatcher.group(2);
            uc.getActors().put(actorId, new Actor(actorId, actorDesc));
        }

        // PreCondition
        Pattern prePattern = Pattern.compile("preCondition\\s+((?:\"[^\"]*\"\\s*,?\\s*)+)");
        Matcher preMatcher = prePattern.matcher(body);
        if (preMatcher.find()) {
            String rawPre = preMatcher.group(1).trim();
            // Extrai todas as strings entre aspas
            List<String> preList = extractQuotedStrings(rawPre);
            uc.setPreCondition(String.join(" ; ", preList));
        }

        // PostCondition
        Pattern postPattern = Pattern.compile("postCondition\\s+\"([^\"]*)\"");
        Matcher postMatcher = postPattern.matcher(body);
        if (postMatcher.find()) {
            uc.setPostCondition(postMatcher.group(1).trim());
        }

        // Basic / BasicFlow block
        Pattern bfPattern = Pattern.compile("(?:basic|basicFlow)\\s*\\{");
        Matcher bfMatcher = bfPattern.matcher(body);
        if (bfMatcher.find()) {
            int start = bfMatcher.end() - 1;
            int end = findMatchingBrace(body, start);
            if (end > start) {
                String bfContent = body.substring(start + 1, end);
                parseSteps(bfContent, uc.getBasicFlow());
            }
        }

        // Alternative Flows
        Pattern altPattern = Pattern.compile("alternative\\s+([0-9]+)\\s*,?\\s*\"([^\"]+)\"\\s*,?\\s*\\{");
        Matcher altMatcher = altPattern.matcher(body);
        while (altMatcher.find()) {
            int altId = Integer.parseInt(altMatcher.group(1));
            String altDesc = altMatcher.group(2);
            int start = altMatcher.end() - 1;
            int end = findMatchingBrace(body, start);
            if (end > start) {
                String altBody = body.substring(start + 1, end);
                AlternativeFlow af = new AlternativeFlow(altId, altDesc);
                parseSteps(altBody, af.getSteps());
                uc.getAlternatives().add(af);
            }
        }

        // Exception Flows
        Pattern efPattern = Pattern.compile("exception\\s+([0-9]+)\\s*,?\\s*\"([^\"]+)\"\\s*,?\\s*\\{");
        Matcher efMatcher = efPattern.matcher(body);
        while (efMatcher.find()) {
            int efId = Integer.parseInt(efMatcher.group(1));
            String efDesc = efMatcher.group(2);
            int start = efMatcher.end() - 1;
            int end = findMatchingBrace(body, start);
            if (end > start) {
                String efBody = body.substring(start + 1, end);
                ExceptionFlow ef = new ExceptionFlow(efId, efDesc);
                parseSteps(efBody, ef.getSteps());
                uc.getExceptions().add(ef);
            }
        }

        return uc;
    }

    private static void parseSteps(String content, List<Step> targetList) {
        String[] lines = content.split("\n");

        // Casos:
        // step 1 emailUser "launches the login screen"
        // step 3 emailUser "fills out the fields and click on the submit button" af[1]
        // step 4 system "displays a successful message" ef[1,2]
        // step 1 emailUser "selects a suggested user name, types password and click on the submit button" bs 4
        // step 5, superAdmin, "preenche os campos", af:[1]
        Pattern stepRegex = Pattern.compile(
            "step\\s+([0-9]+)\\s*[:=,]?\\s*([^,\\s\":]+)\\s*,?\\s*\"([^\"]+)\"(?:\\s*,?\\s*(.*))?"
        );

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("step")) {
                Matcher m = stepRegex.matcher(trimmed);
                if (m.find()) {
                    int num = Integer.parseInt(m.group(1));
                    String actor = m.group(2);
                    String action = m.group(3);
                    Step step = new Step(num, actor, action);

                    String extra = m.group(4);
                    if (extra != null && !extra.isBlank()) {
                        parseStepExtras(extra, step);
                    }
                    targetList.add(step);
                }
            }
        }
    }

    private static void parseStepExtras(String extra, Step step) {
        // Suporta af[1], af[1, 2], af:[1], af:[1, 2]
        Matcher afMatcher = Pattern.compile("af\\s*[:=]?\\s*\\[([0-9,\\s]+)\\]").matcher(extra);
        if (afMatcher.find()) {
            String[] parts = afMatcher.group(1).split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    step.getAlternativeFlowIds().add(Integer.parseInt(p.trim()));
                }
            }
        }

        // Suporta ef[1], ef[1, 2], ef:[1], ef:[1, 2]
        Matcher efMatcher = Pattern.compile("ef\\s*[:=]?\\s*\\[([0-9,\\s]+)\\]").matcher(extra);
        if (efMatcher.find()) {
            String[] parts = efMatcher.group(1).split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    step.getExceptionFlowIds().add(Integer.parseInt(p.trim()));
                }
            }
        }

        // Suporta bs 4, bs: 4, bs:4, bfs: 4, bfs:4
        Matcher bsMatcher = Pattern.compile("(?:bfs|bs)\\s*[:=]?\\s*([0-9]+)").matcher(extra);
        if (bsMatcher.find()) {
            step.setBasicFlowStepReturn(Integer.parseInt(bsMatcher.group(1)));
        }
    }

    private static int findMatchingBrace(String text, int openBracePos) {
        int depth = 0;
        boolean inQuotes = false;

        for (int i = openBracePos; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }

            if (!inQuotes) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static List<String> extractQuotedStrings(String text) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(text);
        while (m.find()) {
            list.add(m.group(1));
        }
        return list;
    }
}
