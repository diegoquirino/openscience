package br.edu.ufcg.splab.claret.generator;

import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.TestStep;
import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class DocxGenerator {

    public static void generateDocx(String docTitle, List<TestCase> testCases, File outputFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            // Document Title (Title Style)
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.LEFT);
            titlePara.setSpacingAfter(100);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(docTitle);
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setColor("1A365D");

            // Subtitle
            XWPFParagraph subPara = document.createParagraph();
            subPara.setSpacingAfter(250);
            XWPFRun subRun = subPara.createRun();
            subRun.setText("Test Case Specification automatically generated from CLARET specifications (UFCG).");
            subRun.setItalic(true);
            subRun.setFontSize(10);
            subRun.setColor("555555");

            String currentUseCase = null;

            for (TestCase tc : testCases) {
                // If Use Case changed or starting new use case block, print Heading 1
                if (currentUseCase == null || !currentUseCase.equals(tc.getUseCaseName())) {
                    currentUseCase = tc.getUseCaseName();
                    XWPFParagraph ucHeaderPara = document.createParagraph();
                    ucHeaderPara.setSpacingBefore(300);
                    ucHeaderPara.setSpacingAfter(100);
                    XWPFRun ucHeaderRun = ucHeaderPara.createRun();
                    ucHeaderRun.setText("Use Case: " + currentUseCase + " (v" + tc.getUseCaseVersion() + ")");
                    ucHeaderRun.setBold(true);
                    ucHeaderRun.setFontSize(15);
                    ucHeaderRun.setColor("1A365D");

                    addMetaParagraph(document, "System: ", tc.getSystemName());
                }

                // Test Case Heading (Heading 2)
                XWPFParagraph headingPara = document.createParagraph();
                headingPara.setSpacingBefore(180);
                headingPara.setSpacingAfter(60);
                XWPFRun headingRun = headingPara.createRun();
                headingRun.setText("[" + tc.getId() + "] " + tc.getSummary());
                headingRun.setBold(true);
                headingRun.setFontSize(12);
                headingRun.setColor("2B6CB0");

                // Test Case Preconditions & Postconditions
                addMetaParagraph(document, "Preconditions: ", tc.getPreCondition());
                addMetaParagraph(document, "Postconditions: ", tc.getPostCondition());

                // Steps Table
                List<TestStep> steps = tc.getSteps();
                XWPFTable table = document.createTable(steps.size() + 1, 3);
                table.setWidth("100%");

                // Table Header
                XWPFTableRow headerRow = table.getRow(0);
                setHeaderCell(headerRow.getCell(0), "Step #", 1200);
                setHeaderCell(headerRow.getCell(1), "Actor Action", 4500);
                setHeaderCell(headerRow.getCell(2), "Expected Result (System)", 4500);

                // Step Rows
                for (int i = 0; i < steps.size(); i++) {
                    TestStep step = steps.get(i);
                    XWPFTableRow row = table.getRow(i + 1);

                    setBodyCell(row.getCell(0), String.valueOf(step.getStepNumber()));
                    setBodyCell(row.getCell(1), step.getActor() + ": " + step.getAction());
                    setBodyCell(row.getCell(2), step.getExpectedResult());
                }

                // Spacing after table
                document.createParagraph().createRun().addBreak();
            }

            document.write(fos);
        }
    }

    private static void addMetaParagraph(XWPFDocument doc, String label, String value) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(40);
        XWPFRun labelRun = p.createRun();
        labelRun.setBold(true);
        labelRun.setFontSize(10);
        labelRun.setText(label);

        XWPFRun valueRun = p.createRun();
        valueRun.setFontSize(10);
        valueRun.setText(value != null ? value : "");
    }

    private static void setHeaderCell(XWPFTableCell cell, String text, int width) {
        cell.setWidth(String.valueOf(width));
        cell.setColor("2B6CB0");
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(10);
        r.setColor("FFFFFF");
    }

    private static void setBodyCell(XWPFTableCell cell, String text) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.createRun();
        r.setFontSize(9);
        r.setText(text != null ? text : "");
    }
}
