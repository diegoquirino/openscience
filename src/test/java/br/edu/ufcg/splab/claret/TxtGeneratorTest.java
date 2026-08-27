package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.engine.ClaretProcessor;
import br.edu.ufcg.splab.claret.generator.TxtGenerator;
import br.edu.ufcg.splab.claret.model.ClaretSystem;
import br.edu.ufcg.splab.claret.model.CoverageCriteria;
import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.UseCase;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TxtGeneratorTest {

    @Test
    @DisplayName("Should generate tabulated TXT test specification matching XLSX structure")
    void testGenerateTxtFromLoginMinitest() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);
            List<TestCase> testCases = ClaretProcessor.extractTestCases(system.getName(), uc, CoverageCriteria.GT);

            assertFalse(testCases.isEmpty());

            File tempTxt = File.createTempFile("test_login_suite", ".txt");
            tempTxt.deleteOnExit();

            TxtGenerator.generateTxt(system.getName(), uc.getName(), uc.getVersion(), CoverageCriteria.GT, testCases, tempTxt);

            assertTrue(tempTxt.exists());
            String txtContent = Files.readString(tempTxt.toPath(), StandardCharsets.UTF_8);

            assertTrue(txtContent.contains("System:\tEmail"));
            assertTrue(txtContent.contains("Use Case:\tLogin User\tVersion:\t1.0"));
            assertTrue(txtContent.contains("Suite Type:\tReduced (Greedy Heuristic - Transition Coverage)"));
            assertTrue(txtContent.contains("Test Case ID:\tTC1"));
            assertTrue(txtContent.contains("1\temailUser: launches the login screen\t\tSYSTEM presents a form containing a username text field, a masked password field, and a submit button\t\t"));
        }
    }

    @Test
    @DisplayName("Should generate consolidated tabulated TXT grouped by system with per-usecase headers")
    void testGenerateConsolidatedTxtGroupedBySystem() throws Exception {
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
                "        version \"2.0\", type:\"Creation\", user:\"Everton\", date:\"20/03/2015\"\n" +
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

        File tempConsolidated = File.createTempFile("all_usecases--GT-", ".txt");
        tempConsolidated.deleteOnExit();

        TxtGenerator.generateConsolidatedTxt(allTestCases, CoverageCriteria.GT, tempConsolidated);

        assertTrue(tempConsolidated.exists());
        String txtContent = Files.readString(tempConsolidated.toPath(), StandardCharsets.UTF_8);

        assertTrue(txtContent.contains("System:\tEmail"));
        assertTrue(txtContent.contains("System:\tSAFF"));
        assertTrue(txtContent.contains("Use Case:\tLogin User\tVersion:\t1.0"));
        assertTrue(txtContent.contains("Use Case:\tCRUD Customer\tVersion:\t2.0"));
    }
}
