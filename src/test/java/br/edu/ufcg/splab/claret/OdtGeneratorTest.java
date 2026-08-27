package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.engine.ClaretProcessor;
import br.edu.ufcg.splab.claret.generator.OdtGenerator;
import br.edu.ufcg.splab.claret.model.ClaretSystem;
import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.UseCase;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class OdtGeneratorTest {

    @Test
    @DisplayName("Should generate valid .odt (OpenDocument Text) containing mimetype, manifest, styles and content.xml")
    void testGenerateOdtFromLoginMinitest() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);
            List<TestCase> testCases = ClaretProcessor.extractTestCases(uc);

            File tempOdt = File.createTempFile("test_login", ".odt");
            tempOdt.deleteOnExit();

            OdtGenerator.generateOdt("Test Cases: " + uc.getName(), testCases, tempOdt);

            assertTrue(tempOdt.exists());
            assertTrue(tempOdt.length() > 0);

            try (ZipFile zip = new ZipFile(tempOdt)) {
                ZipEntry mimeEntry = zip.getEntry("mimetype");
                assertNotNull(mimeEntry, "Must contain mimetype entry");

                ZipEntry manifestEntry = zip.getEntry("META-INF/manifest.xml");
                assertNotNull(manifestEntry, "Must contain META-INF/manifest.xml");

                ZipEntry stylesEntry = zip.getEntry("styles.xml");
                assertNotNull(stylesEntry, "Must contain styles.xml");

                ZipEntry contentEntry = zip.getEntry("content.xml");
                assertNotNull(contentEntry, "Must contain content.xml");

                try (InputStream contentStream = zip.getInputStream(contentEntry)) {
                    String contentXml = new String(contentStream.readAllBytes(), StandardCharsets.UTF_8);
                    assertTrue(contentXml.contains("Test Cases: Login User"));
                    assertTrue(contentXml.contains("System:"));
                    assertTrue(contentXml.contains("TC1"));
                    assertTrue(contentXml.contains("emailUser: launches the login screen"));
                }
            }
        }
    }
}
