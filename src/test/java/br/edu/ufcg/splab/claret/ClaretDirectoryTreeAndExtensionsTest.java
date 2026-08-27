package br.edu.ufcg.splab.claret;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClaretDirectoryTreeAndExtensionsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should collect and process both .claret and .dsl extensions in source directory")
    void testCollectClaretAndDslFiles() throws Exception {
        File workDir = tempDir.resolve("work").toFile();
        workDir.mkdirs();

        // 1. Create a .claret file
        File claretFile = new File(workDir, "login.claret");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret");
             FileOutputStream fos = new FileOutputStream(claretFile)) {
            assertNotNull(in);
            fos.write(in.readAllBytes());
        }

        // 2. Create a .dsl file
        File dslFile = new File(workDir, "customer.dsl");
        String dslContent = "system \"SAFF\", {\n" +
                "    usecase \"CRUD Customer\", {\n" +
                "        version \"1.0\", type:\"Creation\", user:\"Everton\", date:\"20/03/2015\"\n" +
                "        actor superAdmin, \"Super Admin\"\n" +
                "        preCondition \"Logged in as super admin\"\n" +
                "        basicFlow {\n" +
                "            step 1, superAdmin, \"clicks customer menu\", af:[1]\n" +
                "            step 2, system, \"shows customer list\"\n" +
                "        }\n" +
                "        alternative 1, \"Cancel action\", {\n" +
                "            step 1, superAdmin, \"clicks cancel\", bfs:2\n" +
                "        }\n" +
                "        postCondition \"Completed\"\n" +
                "    }\n" +
                "}";
        try (FileOutputStream fos = new FileOutputStream(dslFile)) {
            fos.write(dslContent.getBytes(StandardCharsets.UTF_8));
        }

        // Validate collector
        List<File> collected = new ArrayList<>();
        Main.collectFiles(workDir, collected);

        assertEquals(2, collected.size(), "Should have collected both .claret and .dsl files");
        assertTrue(collected.stream().anyMatch(f -> f.getName().equals("login.claret")));
        assertTrue(collected.stream().anyMatch(f -> f.getName().equals("customer.dsl")));
    }

    @Test
    @DisplayName("Should move processed files into sibling src/ directory and generate output/ (tgf/, xlsx/, docx/, odt/, txt/, alts/, xml/)")
    void testGenerateDirectoryTreeAndMoveSources() throws Exception {
        File workDir = tempDir.resolve("project_date_dir").toFile();
        workDir.mkdirs();

        // Write sample files in workDir root (.claret and .dsl)
        File claretFile = new File(workDir, "RF001 - Autenticar Usuario.claret");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("login-minitest.claret");
             FileOutputStream fos = new FileOutputStream(claretFile)) {
            assertNotNull(in);
            fos.write(in.readAllBytes());
        }

        File dslFile = new File(workDir, "RF002 - Realizar logout.dsl");
        String dslContent = "system \"Portal\", {\n" +
                "    usecase \"Logout\", {\n" +
                "        version \"1.0\", type:\"Creation\", user:\"Dalton\", date:\"01/01/2018\"\n" +
                "        actor user, \"Logged User\"\n" +
                "        preCondition \"User is authenticated\"\n" +
                "        basicFlow {\n" +
                "            step 1, user, \"clicks logout button\"\n" +
                "            step 2, system, \"invalidates session and presents login screen\"\n" +
                "        }\n" +
                "        postCondition \"Session ended\"\n" +
                "    }\n" +
                "}";
        try (FileOutputStream fos = new FileOutputStream(dslFile)) {
            fos.write(dslContent.getBytes(StandardCharsets.UTF_8));
        }

        File outDir = new File(workDir, "output");

        // Execute Main with coverage gt
        String[] args = {
                "-i", workDir.getAbsolutePath(),
                "-o", outDir.getAbsolutePath(),
                "-f", "all",
                "-c", "gt"
        };
        Main.main(args);

        // Verify src/ and output/ exist as siblings inside workDir
        File srcFolder = new File(workDir, "src");
        assertTrue(srcFolder.exists() && srcFolder.isDirectory(), "src/ directory must exist at same level as output/");
        assertTrue(outDir.exists() && outDir.isDirectory(), "output/ directory must exist at same level as src/");

        // Verify source files were MOVED into workDir/src/
        assertTrue(new File(srcFolder, "RF001 - Autenticar Usuario.claret").exists(), "src/ must contain moved .claret file");
        assertTrue(new File(srcFolder, "RF002 - Realizar logout.dsl").exists(), "src/ must contain moved .dsl file");
        assertFalse(claretFile.exists(), "Original .claret file must have been moved from root");
        assertFalse(dslFile.exists(), "Original .dsl file must have been moved from root");

        // Verify output subdirectories
        File tgfFolder = new File(outDir, "tgf");
        File xlsxFolder = new File(outDir, "xlsx");
        File docxFolder = new File(outDir, "docx");
        File odtFolder = new File(outDir, "odt");
        File txtFolder = new File(outDir, "txt");
        File altsFolder = new File(outDir, "alts");
        File xmlFolder = new File(outDir, "xml");

        assertTrue(tgfFolder.exists() && tgfFolder.isDirectory());
        assertTrue(xlsxFolder.exists() && xlsxFolder.isDirectory());
        assertTrue(docxFolder.exists() && docxFolder.isDirectory());
        assertTrue(odtFolder.exists() && odtFolder.isDirectory());
        assertTrue(txtFolder.exists() && txtFolder.isDirectory());
        assertTrue(altsFolder.exists() && altsFolder.isDirectory());
        assertTrue(xmlFolder.exists() && xmlFolder.isDirectory());

        // Verify generated files in each format directory
        assertTrue(new File(tgfFolder, "RF001 - Autenticar Usuario.tgf").exists());
        assertTrue(new File(tgfFolder, "RF001 - Autenticar Usuario-annotated.tgf").exists());
        assertTrue(new File(tgfFolder, "RF002 - Realizar logout.tgf").exists());

        assertTrue(new File(xlsxFolder, "RF001 - Autenticar Usuario--GT-.xlsx").exists());
        assertTrue(new File(xlsxFolder, "all_usecases--GT-.xlsx").exists());

        assertTrue(new File(txtFolder, "RF001 - Autenticar Usuario--GT-.txt").exists());
        assertTrue(new File(txtFolder, "all_usecases--GT-.txt").exists());

        assertTrue(new File(docxFolder, "RF001 - Autenticar Usuario.docx").exists());
        assertTrue(new File(docxFolder, "all_usecases.docx").exists());

        assertTrue(new File(odtFolder, "RF001 - Autenticar Usuario.odt").exists());
        assertTrue(new File(odtFolder, "all_usecases.odt").exists());

        assertTrue(new File(altsFolder, "RF001 - Autenticar Usuario.alts").exists());
        assertTrue(new File(xmlFolder, "RF001 - Autenticar Usuario_testlink.xml").exists());
        assertTrue(new File(xmlFolder, "all_usecases_testlink.xml").exists());
    }
}
