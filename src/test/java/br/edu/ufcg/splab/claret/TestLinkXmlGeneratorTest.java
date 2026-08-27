package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.engine.ClaretProcessor;
import br.edu.ufcg.splab.claret.generator.TestLinkXmlGenerator;
import br.edu.ufcg.splab.claret.model.ClaretSystem;
import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.UseCase;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestLinkXmlGeneratorTest {

    @Test
    @DisplayName("Should generate XML matching TestLink DTD structure and tags")
    void testGenerateTestLinkXmlFromLoginMinitest() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);
            List<TestCase> testCases = ClaretProcessor.extractTestCases(uc);

            String xmlOutput = TestLinkXmlGenerator.generateTestLinkXmlString("Login Suite", testCases);

            assertNotNull(xmlOutput);
            assertTrue(xmlOutput.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
            assertTrue(xmlOutput.contains("<testsuite name=\"Login Suite\">"));
            assertTrue(xmlOutput.contains("<testcase name=\"TC1 - Basic Flow (Happy Path)\">"));
            assertTrue(xmlOutput.contains("<externalid><![CDATA[TC1]]></externalid>"));
            assertTrue(xmlOutput.contains("<preconditions><![CDATA[There is an active network connection.]]></preconditions>"));
            assertTrue(xmlOutput.contains("<steps>"));
            assertTrue(xmlOutput.contains("<step_number><![CDATA[1]]></step_number>"));
            assertTrue(xmlOutput.contains("<actions><![CDATA[emailUser: launches the login screen]]></actions>"));
            assertTrue(xmlOutput.contains("<execution_type><![CDATA[1]]></execution_type>"));
            assertTrue(xmlOutput.contains("</testcase>"));
            assertTrue(xmlOutput.contains("</testsuite>"));

            File tempXml = File.createTempFile("test_login_testlink", ".xml");
            tempXml.deleteOnExit();
            TestLinkXmlGenerator.generateTestLinkXml("Login Suite", testCases, tempXml);

            assertTrue(tempXml.exists());
            assertTrue(Files.size(tempXml.toPath()) > 0);
        }
    }
}
