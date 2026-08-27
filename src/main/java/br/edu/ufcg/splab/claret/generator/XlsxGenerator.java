package br.edu.ufcg.splab.claret.generator;

import br.edu.ufcg.splab.claret.model.CoverageCriteria;
import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.TestStep;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class XlsxGenerator {

    public static void generateXlsx(String systemName, String useCaseName, String version, CoverageCriteria criteria, List<TestCase> testCases, File outputFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Casos de Teste");
            populateSheet(workbook, sheet, systemName, useCaseName, version, criteria, testCases, false);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }

    public static void generateConsolidatedXlsx(List<TestCase> allTestCases, CoverageCriteria criteria, File outputFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Group test cases by system
            Map<String, List<TestCase>> testCasesBySystem = new LinkedHashMap<>();
            for (TestCase tc : allTestCases) {
                String sys = tc.getSystemName() != null && !tc.getSystemName().isBlank() ? tc.getSystemName() : "System";
                testCasesBySystem.computeIfAbsent(sys, k -> new ArrayList<>()).add(tc);
            }

            Set<String> usedSheetNames = new HashSet<>();
            for (Map.Entry<String, List<TestCase>> entry : testCasesBySystem.entrySet()) {
                String rawName = entry.getKey();
                String sheetName = sanitizeSheetName(rawName, usedSheetNames);
                usedSheetNames.add(sheetName);

                Sheet sheet = workbook.createSheet(sheetName);
                populateSheet(workbook, sheet, entry.getKey(), "All Use Cases", null, criteria, entry.getValue(), true);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }

    private static void populateSheet(Workbook workbook, Sheet sheet, String systemName, String useCaseName, String version, CoverageCriteria criteria, List<TestCase> testCases, boolean isConsolidated) {
        // Styles
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 10);

        Font tableHeaderFont = workbook.createFont();
        tableHeaderFont.setBold(true);
        tableHeaderFont.setColor(IndexedColors.BLACK.getIndex());

        CellStyle labelBoldStyle = workbook.createCellStyle();
        labelBoldStyle.setFont(boldFont);

        CellStyle tableHeaderStyle = workbook.createCellStyle();
        tableHeaderStyle.setFont(tableHeaderFont);
        tableHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        tableHeaderStyle.setBorderBottom(BorderStyle.THIN);
        tableHeaderStyle.setBorderTop(BorderStyle.THIN);
        tableHeaderStyle.setBorderLeft(BorderStyle.THIN);
        tableHeaderStyle.setBorderRight(BorderStyle.THIN);

        CellStyle borderStyle = workbook.createCellStyle();
        borderStyle.setWrapText(true);
        borderStyle.setVerticalAlignment(VerticalAlignment.TOP);
        borderStyle.setBorderBottom(BorderStyle.THIN);
        borderStyle.setBorderTop(BorderStyle.THIN);
        borderStyle.setBorderLeft(BorderStyle.THIN);
        borderStyle.setBorderRight(BorderStyle.THIN);

        CellStyle centerBorderStyle = workbook.createCellStyle();
        centerBorderStyle.cloneStyleFrom(borderStyle);
        centerBorderStyle.setAlignment(HorizontalAlignment.CENTER);

        // Row 0: System
        Row r0 = sheet.createRow(0);
        Cell c00 = r0.createCell(0);
        c00.setCellValue("System: ");
        c00.setCellStyle(labelBoldStyle);
        r0.createCell(1).setCellValue(systemName != null ? systemName : "System");

        // Row 1: Use Case (& Version if single use case)
        Row r1 = sheet.createRow(1);
        Cell c10 = r1.createCell(0);
        c10.setCellValue("Use Case: ");
        c10.setCellStyle(labelBoldStyle);
        r1.createCell(1).setCellValue(useCaseName != null ? useCaseName : "");
        if (!isConsolidated) {
            Cell c12 = r1.createCell(2);
            c12.setCellValue("Version: ");
            c12.setCellStyle(labelBoldStyle);
            r1.createCell(3).setCellValue(version != null ? version : "1.0");
        }

        // Row 2: Suite Type, Size & Date
        Row r2 = sheet.createRow(2);
        Cell c20 = r2.createCell(0);
        c20.setCellValue("Suite Type:");
        c20.setCellStyle(labelBoldStyle);
        r2.createCell(1).setCellValue(criteria.getSuiteTypeName());
        r2.createCell(3).setCellValue("Size: " + testCases.size() + " test case(s)");
        Cell c24 = r2.createCell(4);
        c24.setCellValue("Creation Date: ");
        c24.setCellStyle(labelBoldStyle);
        r2.createCell(5).setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        int currentRow = 5;

        for (TestCase tc : testCases) {
            if (isConsolidated) {
                // In consolidated sheets: Row above Test Case ID contains Use Case Name and its specific Version
                Row ucRow = sheet.createRow(currentRow++);
                Cell cUcLabel = ucRow.createCell(0);
                cUcLabel.setCellValue("Use Case: ");
                cUcLabel.setCellStyle(labelBoldStyle);
                Cell cUcVal = ucRow.createCell(1);
                cUcVal.setCellValue(tc.getUseCaseName());

                Cell cVerLabel = ucRow.createCell(2);
                cVerLabel.setCellValue("Version: ");
                cVerLabel.setCellStyle(labelBoldStyle);
                Cell cVerVal = ucRow.createCell(3);
                cVerVal.setCellValue(tc.getUseCaseVersion());
            }

            // Test Case Header: ID, Priority, Executed by
            Row tcRow1 = sheet.createRow(currentRow++);
            Cell cTcId = tcRow1.createCell(0);
            cTcId.setCellValue("Test Case ID: ");
            cTcId.setCellStyle(labelBoldStyle);
            tcRow1.createCell(1).setCellValue(tc.getId());
            Cell cPrio = tcRow1.createCell(2);
            cPrio.setCellValue("Priority (low,medium,high): ");
            cPrio.setCellStyle(labelBoldStyle);
            Cell cExec = tcRow1.createCell(4);
            cExec.setCellValue("Executed by:");
            cExec.setCellStyle(labelBoldStyle);

            // Description & Execution Date
            Row tcRow2 = sheet.createRow(currentRow++);
            Cell cDesc = tcRow2.createCell(0);
            cDesc.setCellValue("Description: ");
            cDesc.setCellStyle(labelBoldStyle);
            tcRow2.createCell(1).setCellValue(tc.getSummary());
            Cell cDate = tcRow2.createCell(4);
            cDate.setCellValue("Execution Date: ");
            cDate.setCellStyle(labelBoldStyle);

            // Precondition
            Row tcRow3 = sheet.createRow(currentRow++);
            Cell cPre = tcRow3.createCell(0);
            cPre.setCellValue("Precondition: ");
            cPre.setCellStyle(labelBoldStyle);
            tcRow3.createCell(1).setCellValue(tc.getPreCondition());

            // Steps Table Header
            Row thRow = sheet.createRow(currentRow++);
            String[] tableHeaders = {"#", "Steps", "Test Data", "Expected Results", "Execution Status (pass/fail/blocked)", "Actual Result"};
            for (int i = 0; i < tableHeaders.length; i++) {
                Cell thCell = thRow.createCell(i);
                thCell.setCellValue(tableHeaders[i]);
                thCell.setCellStyle(tableHeaderStyle);
            }

            // Steps
            for (TestStep step : tc.getSteps()) {
                Row sRow = sheet.createRow(currentRow++);
                Cell cNum = sRow.createCell(0);
                cNum.setCellValue(step.getStepNumber());
                cNum.setCellStyle(centerBorderStyle);

                Cell cAct = sRow.createCell(1);
                cAct.setCellValue(step.getActor() + ": " + step.getAction());
                cAct.setCellStyle(borderStyle);

                Cell cData = sRow.createCell(2);
                cData.setCellValue("");
                cData.setCellStyle(borderStyle);

                Cell cExp = sRow.createCell(3);
                cExp.setCellValue(step.getExpectedResult());
                cExp.setCellStyle(borderStyle);

                Cell cStat = sRow.createCell(4);
                cStat.setCellValue("");
                cStat.setCellStyle(borderStyle);

                Cell cActual = sRow.createCell(5);
                cActual.setCellValue("");
                cActual.setCellStyle(borderStyle);
            }

            // Postcondition
            Row postRow = sheet.createRow(currentRow++);
            Cell cPost = postRow.createCell(0);
            cPost.setCellValue("Postcondition: ");
            cPost.setCellStyle(labelBoldStyle);
            postRow.createCell(1).setCellValue(tc.getPostCondition());

            // Blank line separation
            currentRow += 2;
        }

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 4500) {
                sheet.setColumnWidth(i, 4500);
            }
        }
    }

    private static String sanitizeSheetName(String rawName, Set<String> existingNames) {
        if (rawName == null || rawName.isBlank()) {
            rawName = "System";
        }
        String clean = rawName.replaceAll("[\\\\/*?\\[\\]:]", "_");
        if (clean.length() > 28) {
            clean = clean.substring(0, 28);
        }
        String candidate = clean;
        int counter = 1;
        while (existingNames.contains(candidate)) {
            candidate = clean + "_" + (counter++);
        }
        return candidate;
    }

    public static void generateXlsx(List<TestCase> testCases, File outputFile) throws IOException {
        String sysName = testCases.isEmpty() ? "System" : testCases.get(0).getSystemName();
        String ucName = testCases.isEmpty() ? "UseCase" : testCases.get(0).getUseCaseName();
        String version = testCases.isEmpty() ? "1.0" : testCases.get(0).getUseCaseVersion();
        generateXlsx(sysName, ucName, version, CoverageCriteria.GT, testCases, outputFile);
    }
}
