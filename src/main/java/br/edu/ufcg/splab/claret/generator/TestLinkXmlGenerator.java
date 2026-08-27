package br.edu.ufcg.splab.claret.generator;

import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.TestStep;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestLinkXmlGenerator {

    public static String generateTestLinkXmlString(String suiteName, List<TestCase> testCases) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuite name=\"").append(escapeXml(suiteName)).append("\">\n");
        xml.append("  <node_order><![CDATA[0]]></node_order>\n");
        xml.append("  <details><![CDATA[Test suite automatically generated from CLARET specifications (UFCG)]]></details>\n");

        // Group test cases by use case
        Map<String, List<TestCase>> byUseCase = new LinkedHashMap<>();
        for (TestCase tc : testCases) {
            String uc = tc.getUseCaseName() != null && !tc.getUseCaseName().isBlank() ? tc.getUseCaseName() : "Use Case";
            byUseCase.computeIfAbsent(uc, k -> new ArrayList<>()).add(tc);
        }

        if (byUseCase.size() <= 1 && byUseCase.containsKey(suiteName)) {
            // Single usecase matching suite name - render test cases directly
            appendTestCases(xml, testCases, "  ");
        } else {
            // Multiple use cases or consolidated suite - render nested testsuites per use case
            int suiteOrder = 1;
            for (Map.Entry<String, List<TestCase>> entry : byUseCase.entrySet()) {
                xml.append("  <testsuite name=\"").append(escapeXml(entry.getKey())).append("\">\n");
                xml.append("    <node_order><![CDATA[").append(suiteOrder++).append("]]></node_order>\n");
                xml.append("    <details><![CDATA[Use case: ").append(escapeCdata(entry.getKey())).append("]]></details>\n");
                appendTestCases(xml, entry.getValue(), "    ");
                xml.append("  </testsuite>\n");
            }
        }

        xml.append("</testsuite>\n");
        return xml.toString();
    }

    private static void appendTestCases(StringBuilder xml, List<TestCase> testCases, String indent) {
        int nodeOrder = 1;
        for (TestCase tc : testCases) {
            xml.append(indent).append("<testcase name=\"").append(escapeXml(tc.getId() + " - " + tc.getSummary())).append("\">\n");
            xml.append(indent).append("  <node_order><![CDATA[").append(nodeOrder++).append("]]></node_order>\n");
            xml.append(indent).append("  <externalid><![CDATA[").append(tc.getId()).append("]]></externalid>\n");
            xml.append(indent).append("  <version><![CDATA[1]]></version>\n");
            xml.append(indent).append("  <summary><![CDATA[").append(escapeCdata(tc.getSummary())).append("]]></summary>\n");
            xml.append(indent).append("  <preconditions><![CDATA[").append(escapeCdata(tc.getPreCondition())).append("]]></preconditions>\n");
            xml.append(indent).append("  <execution_type><![CDATA[1]]></execution_type>\n"); // 1 = Manual, 2 = Automated
            xml.append(indent).append("  <importance><![CDATA[2]]></importance>\n");       // 2 = Medium, 3 = High
            xml.append(indent).append("  <steps>\n");

            for (TestStep step : tc.getSteps()) {
                xml.append(indent).append("    <step>\n");
                xml.append(indent).append("      <step_number><![CDATA[").append(step.getStepNumber()).append("]]></step_number>\n");
                xml.append(indent).append("      <actions><![CDATA[").append(escapeCdata(step.getActor() + ": " + step.getAction())).append("]]></actions>\n");
                xml.append(indent).append("      <expectedresults><![CDATA[").append(escapeCdata(step.getExpectedResult())).append("]]></expectedresults>\n");
                xml.append(indent).append("      <execution_type><![CDATA[1]]></execution_type>\n");
                xml.append(indent).append("    </step>\n");
            }

            xml.append(indent).append("  </steps>\n");
            xml.append(indent).append("</testcase>\n");
        }
    }

    public static void generateTestLinkXml(String suiteName, List<TestCase> testCases, File outputFile) throws IOException {
        String content = generateTestLinkXmlString(suiteName, testCases);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    private static String escapeCdata(String text) {
        if (text == null) return "";
        return text.replace("]]>", "]]]]><![CDATA[>");
    }
}
