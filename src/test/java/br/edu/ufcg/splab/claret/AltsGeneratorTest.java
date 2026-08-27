package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.generator.AltsGenerator;
import br.edu.ufcg.splab.claret.model.ClaretSystem;
import br.edu.ufcg.splab.claret.model.UseCase;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class AltsGeneratorTest {

    @Test
    @DisplayName("Should generate ALTS specification with states, transitions, guards and actors")
    void testGenerateAltsFromLoginMinitest() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);

            String altsOutput = AltsGenerator.generateAltsString(uc);

            assertNotNull(altsOutput);
            assertTrue(altsOutput.contains("ALTS_SPECIFICATION \"Login User\""));
            assertTrue(altsOutput.contains("PRE_CONDITION: \"There is an active network connection.\""));
            assertTrue(altsOutput.contains("POST_CONDITION: \"User successfully logged\""));
            assertTrue(altsOutput.contains("ACTORS {"));
            assertTrue(altsOutput.contains("emailUser: \"Email User\""));
            assertTrue(altsOutput.contains("STATES {"));
            assertTrue(altsOutput.contains("S_INIT"));
            assertTrue(altsOutput.contains("S_BF_1"));
            assertTrue(altsOutput.contains("S_AF1_1"));
            assertTrue(altsOutput.contains("S_EF1_1"));
            assertTrue(altsOutput.contains("S_EF2_1"));
            assertTrue(altsOutput.contains("S_END"));
            assertTrue(altsOutput.contains("TRANSITIONS {"));

            File tempAlts = File.createTempFile("test_login", ".alts");
            tempAlts.deleteOnExit();
            AltsGenerator.generateAlts(uc, tempAlts);

            assertTrue(tempAlts.exists());
            assertTrue(Files.size(tempAlts.toPath()) > 0);
        }
    }
}
