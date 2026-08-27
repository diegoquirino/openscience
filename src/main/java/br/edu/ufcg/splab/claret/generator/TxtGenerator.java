package br.edu.ufcg.splab.claret.generator;

import br.edu.ufcg.splab.claret.model.CoverageCriteria;
import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.TestStep;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TxtGenerator {

    public static void generateTxt(String systemName, String useCaseName, String version, CoverageCriteria criteria, List<TestCase> testCases, File outputFile) throws IOException {
        String content = generateTxtContent(systemName, useCaseName, version, criteria, testCases, false);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    public static void generateConsolidatedTxt(List<TestCase> allTestCases, CoverageCriteria criteria, File outputFile) throws IOException {
        // Group test cases by system
        Map<String, List<TestCase>> testCasesBySystem = new LinkedHashMap<>();
        for (TestCase tc : allTestCases) {
            String sys = tc.getSystemName() != null && !tc.getSystemName().isBlank() ? tc.getSystemName() : "System";
            testCasesBySystem.computeIfAbsent(sys, k -> new ArrayList<>()).add(tc);
        }

        StringBuilder sb = new StringBuilder();
        int sysCounter = 0;
        for (Map.Entry<String, List<TestCase>> entry : testCasesBySystem.entrySet()) {
            if (sysCounter++ > 0) {
                sb.append("\n================================================================================\n\n");
            }
            sb.append(generateTxtContent(entry.getKey(), "All Use Cases", null, criteria, entry.getValue(), true));
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        }
    }

    private static String generateTxtContent(String systemName, String useCaseName, String version, CoverageCriteria criteria, List<TestCase> testCases, boolean isConsolidated) {
        StringBuilder sb = new StringBuilder();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Header
        sb.append("System:\t").append(systemName != null ? systemName : "System").append("\n");

        if (!isConsolidated) {
            sb.append("Use Case:\t").append(useCaseName != null ? useCaseName : "")
              .append("\tVersion:\t").append(version != null ? version : "1.0").append("\n");
        } else {
            sb.append("Use Case:\t").append(useCaseName != null ? useCaseName : "").append("\n");
        }

        sb.append("Suite Type:\t").append(criteria.getSuiteTypeName())
          .append("\tSize: ").append(testCases.size()).append(" test case(s)")
          .append("\tCreation Date:\t").append(dateStr).append("\n\n");

        // Test Cases
        for (TestCase tc : testCases) {
            if (isConsolidated) {
                sb.append("Use Case:\t").append(tc.getUseCaseName())
                  .append("\tVersion:\t").append(tc.getUseCaseVersion()).append("\n");
            }

            sb.append("Test Case ID:\t").append(tc.getId())
              .append("\tPriority (low,medium,high):\t\tExecuted by:\t\n");

            sb.append("Description:\t").append(tc.getSummary())
              .append("\t\t\tExecution Date:\t\n");

            sb.append("Precondition:\t").append(tc.getPreCondition() != null ? tc.getPreCondition() : "").append("\n");

            // Steps Table Header (tab-separated)
            sb.append("#\tSteps\tTest Data\tExpected Results\tExecution Status (pass/fail/blocked)\tActual Result\n");

            // Steps Rows
            for (TestStep step : tc.getSteps()) {
                sb.append(step.getStepNumber()).append("\t")
                  .append(step.getActor()).append(": ").append(step.getAction()).append("\t\t")
                  .append(step.getExpectedResult()).append("\t\t\n");
            }

            sb.append("Postcondition:\t").append(tc.getPostCondition() != null ? tc.getPostCondition() : "").append("\n\n");
        }

        return sb.toString();
    }
}
