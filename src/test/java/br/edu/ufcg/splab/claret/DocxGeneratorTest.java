package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.engine.ClaretProcessor;
import br.edu.ufcg.splab.claret.generator.DocxGenerator;
import br.edu.ufcg.splab.claret.model.ClaretSystem;
import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.UseCase;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocxGeneratorTest {

    @Test
    @DisplayName("Should generate valid .docx (Microsoft Word) containing test case headings, metadata and steps table")
    void testGenerateDocxFromLoginMinitest() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);
            List<TestCase> testCases = ClaretProcessor.extractTestCases(uc);

            File tempDocx = File.createTempFile("test_login", ".docx");
            tempDocx.deleteOnExit();

            DocxGenerator.generateDocx("Test Cases: " + uc.getName(), testCases, tempDocx);

            assertTrue(tempDocx.exists());
            assertTrue(tempDocx.length() > 0);

            try (FileInputStream fis = new FileInputStream(tempDocx);
                 XWPFDocument document = new XWPFDocument(fis)) {
                assertFalse(document.getParagraphs().isEmpty());
                assertTrue(document.getParagraphs().get(0).getText().contains("Test Cases: Login User"));

                List<XWPFTable> tables = document.getTables();
                assertFalse(tables.isEmpty(), "Document should contain test step tables");

                boolean foundSystemMeta = document.getParagraphs().stream().anyMatch(p -> p.getText().contains("System:"));
                assertTrue(foundSystemMeta, "Document should display System name");

                XWPFTable table1 = tables.get(0);
                assertEquals("Step #", table1.getRow(0).getCell(0).getText());
                assertEquals("Actor Action", table1.getRow(0).getCell(1).getText());
                assertEquals("Expected Result (System)", table1.getRow(0).getCell(2).getText());
            }
        }
    }
}
