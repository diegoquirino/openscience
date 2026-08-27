package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.engine.ClaretProcessor;
import br.edu.ufcg.splab.claret.generator.XlsxGenerator;
import br.edu.ufcg.splab.claret.model.ClaretSystem;
import br.edu.ufcg.splab.claret.model.CoverageCriteria;
import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.UseCase;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XlsxGeneratorTest {

    @Test
    @DisplayName("Should generate standard XLSX spreadsheet with single use case header layout")
    void testGenerateXlsxFromLoginMinitest() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);
            List<TestCase> testCases = ClaretProcessor.extractTestCases(system.getName(), uc, CoverageCriteria.GT);

            assertFalse(testCases.isEmpty());

            File tempXlsx = File.createTempFile("test_login_suite", ".xlsx");
            tempXlsx.deleteOnExit();

            XlsxGenerator.generateXlsx(system.getName(), uc.getName(), uc.getVersion(), CoverageCriteria.GT, testCases, tempXlsx);

            assertTrue(tempXlsx.exists());
            assertTrue(tempXlsx.length() > 0);

            try (FileInputStream fis = new FileInputStream(tempXlsx);
                 Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheet("Test Cases");
                assertNotNull(sheet);

                Row r0 = sheet.getRow(0);
                assertEquals("System: ", r0.getCell(0).getStringCellValue());
                assertEquals("Email", r0.getCell(1).getStringCellValue());

                Row r1 = sheet.getRow(1);
                assertEquals("Use Case: ", r1.getCell(0).getStringCellValue());
                assertEquals("Login User", r1.getCell(1).getStringCellValue());
                assertEquals("Version: ", r1.getCell(2).getStringCellValue());
                assertEquals("1.0", r1.getCell(3).getStringCellValue());

                Row r2 = sheet.getRow(2);
                assertEquals("Suite Type:", r2.getCell(0).getStringCellValue());
                assertTrue(r2.getCell(1).getStringCellValue().contains("Greedy Heuristic"));

                // In single usecase sheet, Use Case is only in header; test case block starts with Test Case ID at row 5
                Row r5 = sheet.getRow(5);
                assertEquals("Test Case ID: ", r5.getCell(0).getStringCellValue());
                assertEquals("TC1", r5.getCell(1).getStringCellValue());
            }
        }
    }

    @Test
    @DisplayName("Should generate consolidated XLSX grouped by System Name into separate worksheets with per-usecase headers")
    void testGenerateConsolidatedXlsxGroupedBySystem() throws Exception {
        List<TestCase> allTestCases = new ArrayList<>();

        // System 1: Email
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            ClaretSystem sys1 = ClaretParser.parseString(content);
            allTestCases.addAll(ClaretProcessor.extractTestCases(sys1.getName(), sys1.getUseCases().get(0), CoverageCriteria.GT));
        }

        // System 2: SAFF
        String saffClaret = "system \"SAFF\", {\n" +
                "    usecase \"CRUD Customer\", {\n" +
                "        version \"2.1\", type:\"Creation\", user:\"Everton\", date:\"20/03/2015\"\n" +
                "        actor superAdmin, \"Super Admin\"\n" +
                "        preCondition \"Logged in\"\n" +
                "        basicFlow {\n" +
                "            step 1, superAdmin, \"accesses customer menu\"\n" +
                "            step 2, system, \"shows customer list\"\n" +
                "        }\n" +
                "        postCondition \"Done\"\n" +
                "    }\n" +
                "}";
        ClaretSystem sys2 = ClaretParser.parseString(saffClaret);
        allTestCases.addAll(ClaretProcessor.extractTestCases(sys2.getName(), sys2.getUseCases().get(0), CoverageCriteria.GT));

        File tempConsolidated = File.createTempFile("all_usecases--GT-", ".xlsx");
        tempConsolidated.deleteOnExit();

        XlsxGenerator.generateConsolidatedXlsx(allTestCases, CoverageCriteria.GT, tempConsolidated);

        assertTrue(tempConsolidated.exists());
        assertTrue(tempConsolidated.length() > 0);

        try (FileInputStream fis = new FileInputStream(tempConsolidated);
             Workbook workbook = new XSSFWorkbook(fis)) {
            assertEquals(2, workbook.getNumberOfSheets(), "Should have created 2 sheets for 2 systems");

            Sheet emailSheet = workbook.getSheet("Email");
            assertNotNull(emailSheet, "Sheet 'Email' must exist");
            assertEquals("Email", emailSheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("All Use Cases", emailSheet.getRow(1).getCell(1).getStringCellValue());
            assertNull(emailSheet.getRow(1).getCell(2), "Version must not be in header of consolidated sheet");

            // In consolidated sheet, row 5 has Use Case name and Version
            Row r5 = emailSheet.getRow(5);
            assertEquals("Use Case: ", r5.getCell(0).getStringCellValue());
            assertEquals("Login User", r5.getCell(1).getStringCellValue());
            assertEquals("Version: ", r5.getCell(2).getStringCellValue());
            assertEquals("1.0", r5.getCell(3).getStringCellValue());

            // Row 6 has Test Case ID
            Row r6 = emailSheet.getRow(6);
            assertEquals("Test Case ID: ", r6.getCell(0).getStringCellValue());
            assertEquals("TC1", r6.getCell(1).getStringCellValue());

            Sheet saffSheet = workbook.getSheet("SAFF");
            assertNotNull(saffSheet, "Sheet 'SAFF' must exist");
            assertEquals("SAFF", saffSheet.getRow(0).getCell(1).getStringCellValue());

            Row saffR5 = saffSheet.getRow(5);
            assertEquals("Use Case: ", saffR5.getCell(0).getStringCellValue());
            assertEquals("CRUD Customer", saffR5.getCell(1).getStringCellValue());
            assertEquals("Version: ", saffR5.getCell(2).getStringCellValue());
            assertEquals("2.1", saffR5.getCell(3).getStringCellValue());
        }
    }
}
