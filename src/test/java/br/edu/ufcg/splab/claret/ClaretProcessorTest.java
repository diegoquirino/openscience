package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.engine.ClaretProcessor;
import br.edu.ufcg.splab.claret.model.*;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClaretProcessorTest {

    @Test
    @DisplayName("Should extract MBT test cases using GT algorithm (Greedy Transition Coverage)")
    void testExtractTestCasesGT() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);

            List<TestCase> testCases = ClaretProcessor.extractTestCases(uc, CoverageCriteria.GT);

            assertEquals(4, testCases.size());

            TestCase tcBasic = testCases.get(0);
            assertEquals("TC1", tcBasic.getId());
            assertEquals("Login User", tcBasic.getUseCaseName());
            assertEquals("There is an active network connection.", tcBasic.getPreCondition());
            assertEquals("User successfully logged", tcBasic.getPostCondition());
            assertFalse(tcBasic.getSteps().isEmpty());

            TestStep step1 = tcBasic.getSteps().get(0);
            assertEquals("emailUser", step1.getActor());
            assertEquals("launches the login screen", step1.getAction());
            assertEquals("SYSTEM presents a form containing a username text field, a masked password field, and a submit button", step1.getExpectedResult());

            TestCase tcAlt = testCases.get(1);
            assertEquals("TC2", tcAlt.getId());
            assertTrue(tcAlt.getSummary().contains("Username is predicted"));
            assertFalse(tcAlt.getSteps().isEmpty());

            TestCase tcExc1 = testCases.get(2);
            assertEquals("TC3", tcExc1.getId());
            assertTrue(tcExc1.getSummary().contains("User does not exist in database"));

            TestCase tcExc2 = testCases.get(3);
            assertEquals("TC4", tcExc2.getId());
            assertTrue(tcExc2.getSummary().contains("Incorrect username/password combination"));
        }
    }

    @Test
    @DisplayName("Should extract basic flow only when criteria is BASIC_ONLY")
    void testExtractTestCasesBasicOnly() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);

            List<TestCase> testCases = ClaretProcessor.extractTestCases(uc, CoverageCriteria.BASIC_ONLY);

            assertEquals(1, testCases.size());
            assertEquals("TC1", testCases.get(0).getId());
        }
    }

    @Test
    @DisplayName("Should extract alternative and exception branches when criteria is ALL_BRANCHES")
    void testExtractTestCasesAllBranches() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);

            List<TestCase> testCases = ClaretProcessor.extractTestCases(uc, CoverageCriteria.ALL_BRANCHES);

            assertEquals(3, testCases.size());
            assertEquals("TC2", testCases.get(0).getId());
            assertEquals("TC3", testCases.get(1).getId());
            assertEquals("TC4", testCases.get(2).getId());
        }
    }

    @Test
    @DisplayName("Should convert string to CoverageCriteria enum with coverage codes (gt, gtp, art, complete)")
    void testCoverageCriteriaFromString() {
        assertEquals(CoverageCriteria.GT, CoverageCriteria.fromString("gt"));
        assertEquals(CoverageCriteria.GTP, CoverageCriteria.fromString("gtp"));
        assertEquals(CoverageCriteria.ART, CoverageCriteria.fromString("art"));
        assertEquals(CoverageCriteria.COMPLETE, CoverageCriteria.fromString("complete"));
        assertEquals(CoverageCriteria.BASIC_ONLY, CoverageCriteria.fromString("basic-only"));
        assertEquals(CoverageCriteria.ALL_BRANCHES, CoverageCriteria.fromString("all-branches"));
        assertEquals(CoverageCriteria.GT, CoverageCriteria.fromString("unknown-criteria"));
        assertEquals(CoverageCriteria.GT, CoverageCriteria.fromString(null));
    }
}
