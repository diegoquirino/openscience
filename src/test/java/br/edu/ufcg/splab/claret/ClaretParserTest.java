package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.model.*;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ClaretParserTest {

    @Test
    @DisplayName("Should fully parse login-minitest.claret")
    void testParseLoginMinitest() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret")) {
            assertNotNull(in, "File login-minitest.claret must exist in src/test/resources");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);

            assertNotNull(system);
            assertEquals("Email", system.getName());
            assertEquals(1, system.getUseCases().size());

            UseCase uc = system.getUseCases().get(0);
            assertEquals("Login User", uc.getName());
            assertEquals("1.0", uc.getVersion());
            assertEquals("Creation", uc.getType());
            assertEquals("Dalton", uc.getUser());
            assertEquals("01/01/2018", uc.getDate());
            assertEquals("There is an active network connection.", uc.getPreCondition());
            assertEquals("User successfully logged", uc.getPostCondition());

            // Actors
            assertEquals(1, uc.getActors().size());
            assertTrue(uc.getActors().containsKey("emailUser"));
            assertEquals("Email User", uc.getActors().get("emailUser").getDescription());

            // Basic Flow
            assertEquals(4, uc.getBasicFlow().size());
            Step s1 = uc.getBasicFlow().get(0);
            assertEquals(1, s1.getNumber());
            assertEquals("emailUser", s1.getActor());
            assertEquals("launches the login screen", s1.getAction());

            Step s2 = uc.getBasicFlow().get(1);
            assertEquals(2, s2.getNumber());
            assertEquals("system", s2.getActor());
            assertEquals("presents a form containing a username text field, a masked password field, and a submit button", s2.getAction());

            Step s3 = uc.getBasicFlow().get(2);
            assertEquals(3, s3.getNumber());
            assertEquals("emailUser", s3.getActor());
            assertEquals("fills out the fields and click on the submit button", s3.getAction());
            assertEquals(1, s3.getAlternativeFlowIds().size());
            assertEquals(1, s3.getAlternativeFlowIds().get(0));

            Step s4 = uc.getBasicFlow().get(3);
            assertEquals(4, s4.getNumber());
            assertEquals("system", s4.getActor());
            assertEquals("displays a successful message", s4.getAction());
            assertEquals(2, s4.getExceptionFlowIds().size());
            assertEquals(1, s4.getExceptionFlowIds().get(0));
            assertEquals(2, s4.getExceptionFlowIds().get(1));

            // Alternative Flow 1
            assertEquals(1, uc.getAlternatives().size());
            AlternativeFlow af1 = uc.getAlternatives().get(0);
            assertEquals(1, af1.getId());
            assertEquals("Username is predicted", af1.getDescription());
            assertEquals(1, af1.getSteps().size());
            Step af1Step = af1.getSteps().get(0);
            assertEquals(1, af1Step.getNumber());
            assertEquals("emailUser", af1Step.getActor());
            assertEquals("selects a suggested user name, types password and click on the submit button", af1Step.getAction());
            assertEquals(4, af1Step.getBasicFlowStepReturn());

            // Exception Flows
            assertEquals(2, uc.getExceptions().size());
            ExceptionFlow ef1 = uc.getExceptions().get(0);
            assertEquals(1, ef1.getId());
            assertEquals("User does not exist in database", ef1.getDescription());
            assertEquals(1, ef1.getSteps().size());
            assertEquals("alerts that user does not exist", ef1.getSteps().get(0).getAction());

            ExceptionFlow ef2 = uc.getExceptions().get(1);
            assertEquals(2, ef2.getId());
            assertEquals("Incorrect username/password combination", ef2.getDescription());
            assertEquals(1, ef2.getSteps().size());
            assertEquals("alerts that username and/or password are incorrect", ef2.getSteps().get(0).getAction());
        }
    }

    @Test
    @DisplayName("Should fully parse login-minitest-alternative-format-dsl.claret")
    void testParseLoginManifestAlternativeFormat() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest-alternative-format-dsl.claret")) {
            assertNotNull(in, "File login-minitest-alternative-format-dsl.claret must exist in src/test/resources");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ClaretSystem system = ClaretParser.parseString(content);

            assertNotNull(system);
            assertEquals("Email", system.getName());
            assertEquals(1, system.getUseCases().size());

            UseCase uc = system.getUseCases().get(0);
            assertEquals("Login User Alternative Format", uc.getName());
            assertEquals("1.0", uc.getVersion());
            assertEquals("Creation", uc.getType());
            assertEquals("Dalton", uc.getUser());
            assertEquals("01/01/2018", uc.getDate());
            assertEquals("There is an active network connection.", uc.getPreCondition());
            assertEquals("User successfully logged", uc.getPostCondition());

            // Actors
            assertEquals(1, uc.getActors().size());
            assertTrue(uc.getActors().containsKey("emailUser"));
            assertEquals("Email User", uc.getActors().get("emailUser").getDescription());

            // Basic Flow
            assertEquals(4, uc.getBasicFlow().size());
            assertEquals(1, uc.getBasicFlow().get(2).getAlternativeFlowIds().get(0));
            assertEquals(2, uc.getBasicFlow().get(3).getExceptionFlowIds().size());

            // Alternatives & Exceptions
            assertEquals(1, uc.getAlternatives().size());
            assertEquals(4, uc.getAlternatives().get(0).getSteps().get(0).getBasicFlowStepReturn());
            assertEquals(2, uc.getExceptions().size());
        }
    }

    @Test
    @DisplayName("Should parse Groovy legacy format with commas and af:[...]")
    void testParseGroovyFormat() {
        String groovyClaret = "system \"SAFF\", {\n" +
                "    usecase \"CRUD Cliente\", {\n" +
                "        version \"1.0\", type:\"Creation\", user:\"Everton\", date:\"20/03/2015\"\n" +
                "        actor superAdmin, \"Super administrador\"\n" +
                "        preCondition \"Estar logado como super administrador\"\n" +
                "        basicFlow {\n" +
                "            step 1, superAdmin, \"seleciona opção Customers\", af:[1]\n" +
                "            step 2, system, \"exibe lista de clientes\"\n" +
                "        }\n" +
                "        alternative 1, \"Cancela\", {\n" +
                "            step 1, superAdmin, \"clica cancelar\", bfs:2\n" +
                "        }\n" +
                "        postCondition \"Concluido\"\n" +
                "    }\n" +
                "}";

        ClaretSystem system = ClaretParser.parseString(groovyClaret);
        assertNotNull(system);
        assertEquals("SAFF", system.getName());
        assertEquals(1, system.getUseCases().size());

        UseCase uc = system.getUseCases().get(0);
        assertEquals("CRUD Cliente", uc.getName());
        assertEquals(1, uc.getActors().size());
        assertEquals("Super administrador", uc.getActors().get("superAdmin").getDescription());
        assertEquals(2, uc.getBasicFlow().size());
        assertEquals(1, uc.getBasicFlow().get(0).getAlternativeFlowIds().get(0));
        assertEquals(1, uc.getAlternatives().size());
        assertEquals(2, uc.getAlternatives().get(0).getSteps().get(0).getBasicFlowStepReturn());
    }

    @Test
    @DisplayName("Should parse accented actors (e.g. usuário) and step numbers with colons")
    void testParseAccentedActorAndColonFormat() {
        String claretWithAccent = "system \"SAFF\", {\n" +
                "    usecase \"CRUD Diagnosticos\", {\n" +
                "        version \"1.0\", type:\"Creation\", user:\"Everton\", date:\"20/03/2015\"\n" +
                "        actor usuário, \"Usuario super administrador\"\n" +
                "        preCondition \" \"\n" +
                "        basicFlow {\n" +
                "            step 1, usuário, \"loga no sistema com perfil de super administrador\", af:[1]\n" +
                "            step 2, system, \"exibe tela principal\"\n" +
                "            step 3: usuário, \"seleciona opcao no menu\"\n" +
                "            step 4: system, \"exibe lista\"\n" +
                "        }\n" +
                "        alternative 1, \"Cancela\", {\n" +
                "            step 1, usuário, \"Cancela criacao\", bfs:4\n" +
                "        }\n" +
                "        postCondition \" \"\n" +
                "    }\n" +
                "}";

        ClaretSystem system = ClaretParser.parseString(claretWithAccent);
        assertNotNull(system);
        UseCase uc = system.getUseCases().get(0);
        assertEquals("CRUD Diagnosticos", uc.getName());
        assertEquals(1, uc.getActors().size());
        assertTrue(uc.getActors().containsKey("usuário"));
        assertEquals("Usuario super administrador", uc.getActors().get("usuário").getDescription());

        assertEquals(4, uc.getBasicFlow().size());
        assertEquals("usuário", uc.getBasicFlow().get(0).getActor());
        assertEquals("system", uc.getBasicFlow().get(1).getActor());
        assertEquals("usuário", uc.getBasicFlow().get(2).getActor());
        assertEquals("system", uc.getBasicFlow().get(3).getActor());

        assertEquals(1, uc.getAlternatives().size());
        assertEquals(1, uc.getAlternatives().get(0).getSteps().size());
        assertEquals("usuário", uc.getAlternatives().get(0).getSteps().get(0).getActor());
        assertEquals(4, uc.getAlternatives().get(0).getSteps().get(0).getBasicFlowStepReturn());
    }
}
