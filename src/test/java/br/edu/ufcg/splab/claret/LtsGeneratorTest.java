package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.generator.LtsGenerator;
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

class LtsGeneratorTest {

    @Test
    @DisplayName("Should generate TGF model containing nodes, separator '#' and [c], [s], [e] labels")
    void testGenerateLtsFromLoginMinitest() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);
            UseCase uc = system.getUseCases().get(0);

            String tgfOutput = LtsGenerator.generateTgfString(uc);

            assertNotNull(tgfOutput);
            assertTrue(tgfOutput.contains("#"));
            assertTrue(tgfOutput.contains("[c]"));
            assertTrue(tgfOutput.contains("[s]"));
            assertTrue(tgfOutput.contains("launches the login screen"));

            File tempTgf = File.createTempFile("test_login", ".tgf");
            tempTgf.deleteOnExit();
            LtsGenerator.generateLts(uc, tempTgf);

            assertTrue(tempTgf.exists());
            assertTrue(Files.size(tempTgf.toPath()) > 0);
        }
    }
}
