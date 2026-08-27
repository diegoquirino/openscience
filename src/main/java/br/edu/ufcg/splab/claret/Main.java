package br.edu.ufcg.splab.claret;

import br.edu.ufcg.splab.claret.engine.ClaretProcessor;
import br.edu.ufcg.splab.claret.generator.*;
import br.edu.ufcg.splab.claret.model.*;
import br.edu.ufcg.splab.claret.parser.ClaretParser;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("i", "input", true, "Path to .claret / .dsl file or source directory containing them (Required)");
        options.addOption("o", "output", true, "Output directory for generated artifacts (default: ./output)");
        options.addOption("f", "formats", true, "Comma-separated output formats (tgf, lts, alts, xlsx, docx, odt, xml, txt, or all) [default: all]");
        options.addOption("c", "coverage", true, "MBT coverage criteria: gt (default), gtp, art, complete, basic-only, all-branches");
        options.addOption(null, "flat", false, "Output all generated files flatly in the root output folder instead of format subdirectories (tgf/, xlsx/, docx/, odt/, txt/, etc.)");
        options.addOption("h", "help", false, "Display help options");

        CommandLineParser cmdParser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = cmdParser.parse(options, args);

            if (cmd.hasOption("h") || !cmd.hasOption("i")) {
                printCustomHelp(formatter, options);
                return;
            }

            String inputPath = cmd.getOptionValue("i");
            String outputPath = cmd.getOptionValue("o", "./output");
            String formatsStr = cmd.getOptionValue("f", "tgf,lts,alts,xlsx,docx,odt,xml,txt");
            String coverageStr = cmd.getOptionValue("c", "gt");
            boolean flat = cmd.hasOption("flat");

            CoverageCriteria criteria = CoverageCriteria.fromString(coverageStr);

            List<String> formats = new ArrayList<>();
            if ("all".equalsIgnoreCase(formatsStr.trim())) {
                formats.addAll(Arrays.asList("tgf", "lts", "alts", "xlsx", "docx", "odt", "xml", "txt"));
            } else {
                for (String fmt : formatsStr.toLowerCase().split(",")) {
                    formats.add(fmt.trim());
                }
            }

            File input = new File(inputPath);
            File outputDir = new File(outputPath);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            List<File> claretFiles = new ArrayList<>();
            if (input.isDirectory()) {
                collectFiles(input, claretFiles);
            } else if (input.isFile() && isSupportedExtension(input.getName())) {
                claretFiles.add(input);
            } else {
                System.err.println("Error: Invalid input file or directory (must be .claret or .dsl): " + inputPath);
                System.exit(1);
            }

            if (claretFiles.isEmpty()) {
                System.err.println("No .claret or .dsl files found in the specified path: " + inputPath);
                return;
            }

            // src/ directory at the same level as output/
            File srcDir;
            if (input.isDirectory()) {
                srcDir = new File(input, "src");
            } else {
                File parent = outputDir.getParentFile();
                srcDir = new File(parent != null ? parent : new File("."), "src");
            }
            srcDir.mkdirs();

            File tgfDir = flat ? outputDir : new File(outputDir, "tgf");
            File xlsxDir = flat ? outputDir : new File(outputDir, "xlsx");
            File docxDir = flat ? outputDir : new File(outputDir, "docx");
            File odtDir = flat ? outputDir : new File(outputDir, "odt");
            File altsDir = flat ? outputDir : new File(outputDir, "alts");
            File xmlDir = flat ? outputDir : new File(outputDir, "xml");
            File txtDir = flat ? outputDir : new File(outputDir, "txt");

            System.out.println("==================================================");
            System.out.println(" CLARET (UFCG / SPLab) - Standalone Test Generator");
            System.out.println("==================================================");
            System.out.println("Files found       : " + claretFiles.size());
            System.out.println("Coverage Criteria : " + criteria.getCode() + " -> " + criteria.getSuiteTypeName());
            System.out.println("Selected Formats  : " + formats);
            System.out.println("Target Directory  : " + outputDir.getAbsolutePath());
            System.out.println("Source Directory  : " + srcDir.getAbsolutePath() + " (same level as output)");
            System.out.println("Output Structure  : " + (flat ? "Flat (single folder)" : "Standard Tree (tgf/, xlsx/, docx/, odt/, txt/, alts/, xml/)"));
            System.out.println("--------------------------------------------------");

            List<TestCase> allGlobalTestCases = new ArrayList<>();
            List<File> successfullyProcessedFiles = new ArrayList<>();

            for (File file : claretFiles) {
                System.out.println("Processing: " + file.getName());
                try {
                    ClaretSystem claretSystem = ClaretParser.parse(file);

                    for (UseCase uc : claretSystem.getUseCases()) {
                        String baseName = getBaseName(file.getName());
                        List<TestCase> testCases = ClaretProcessor.extractTestCases(claretSystem.getName(), uc, criteria);
                        allGlobalTestCases.addAll(testCases);

                        if (formats.contains("lts") || formats.contains("tgf")) {
                            tgfDir.mkdirs();
                            File tgfOut = new File(tgfDir, baseName + ".tgf");
                            LtsGenerator.generateLts(uc, tgfOut);
                            System.out.println("  -> Generated TGF/LTS: " + tgfOut.getName());
                        }

                        if (formats.contains("alts")) {
                            altsDir.mkdirs();
                            File altsOut = new File(altsDir, baseName + ".alts");
                            AltsGenerator.generateAlts(uc, altsOut);
                            System.out.println("  -> Generated ALTS: " + altsOut.getName());
                        }

                        if (formats.contains("xlsx")) {
                            xlsxDir.mkdirs();
                            String suffix = criteria.getFileSuffix();
                            File xlsxOut = new File(xlsxDir, baseName + suffix + ".xlsx");
                            XlsxGenerator.generateXlsx(claretSystem.getName(), uc.getName(), uc.getVersion(), criteria, testCases, xlsxOut);
                            System.out.println("  -> Generated XLSX (" + criteria.getCode() + "): " + xlsxOut.getName());
                        }

                        if (formats.contains("txt")) {
                            txtDir.mkdirs();
                            String suffix = criteria.getFileSuffix();
                            File txtOut = new File(txtDir, baseName + suffix + ".txt");
                            TxtGenerator.generateTxt(claretSystem.getName(), uc.getName(), uc.getVersion(), criteria, testCases, txtOut);
                            System.out.println("  -> Generated TXT (" + criteria.getCode() + "): " + txtOut.getName());
                        }

                        if (formats.contains("docx")) {
                            docxDir.mkdirs();
                            File docxOut = new File(docxDir, baseName + ".docx");
                            DocxGenerator.generateDocx("Test Cases: " + uc.getName(), testCases, docxOut);
                            System.out.println("  -> Generated DOCX (Word): " + docxOut.getName());
                        }

                        if (formats.contains("odt")) {
                            odtDir.mkdirs();
                            File odtOut = new File(odtDir, baseName + ".odt");
                            OdtGenerator.generateOdt("Test Cases: " + uc.getName(), testCases, odtOut);
                            System.out.println("  -> Generated ODT (OpenDocument): " + odtOut.getName());
                        }

                        if (formats.contains("xml")) {
                            xmlDir.mkdirs();
                            File xmlOut = new File(xmlDir, baseName + "_testlink.xml");
                            TestLinkXmlGenerator.generateTestLinkXml(uc.getName(), testCases, xmlOut);
                            System.out.println("  -> Generated XML (TestLink): " + xmlOut.getName());
                        }
                    }

                    successfullyProcessedFiles.add(file);
                } catch (Exception e) {
                    System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Consolidated suites for multiple files
            if (claretFiles.size() > 1 || allGlobalTestCases.size() > 1) {
                if (formats.contains("xml")) {
                    xmlDir.mkdirs();
                    File fullXml = new File(xmlDir, "all_usecases_testlink.xml");
                    TestLinkXmlGenerator.generateTestLinkXml("Consolidated Test Suite", allGlobalTestCases, fullXml);
                    System.out.println("-> Consolidated XML generated: " + fullXml.getName());
                }
                if (formats.contains("xlsx")) {
                    xlsxDir.mkdirs();
                    File fullXlsx = new File(xlsxDir, "all_usecases" + criteria.getFileSuffix() + ".xlsx");
                    XlsxGenerator.generateConsolidatedXlsx(allGlobalTestCases, criteria, fullXlsx);
                    System.out.println("-> Consolidated XLSX generated (grouped by System): " + fullXlsx.getName());
                }
                if (formats.contains("txt")) {
                    txtDir.mkdirs();
                    File fullTxt = new File(txtDir, "all_usecases" + criteria.getFileSuffix() + ".txt");
                    TxtGenerator.generateConsolidatedTxt(allGlobalTestCases, criteria, fullTxt);
                    System.out.println("-> Consolidated TXT generated (grouped by System): " + fullTxt.getName());
                }
                if (formats.contains("docx")) {
                    docxDir.mkdirs();
                    File fullDocx = new File(docxDir, "all_usecases.docx");
                    DocxGenerator.generateDocx("Complete Test Case Suite", allGlobalTestCases, fullDocx);
                    System.out.println("-> Consolidated DOCX generated: " + fullDocx.getName());
                }
                if (formats.contains("odt")) {
                    odtDir.mkdirs();
                    File fullOdt = new File(odtDir, "all_usecases.odt");
                    OdtGenerator.generateOdt("Complete Test Case Suite", allGlobalTestCases, fullOdt);
                    System.out.println("-> Consolidated ODT generated: " + fullOdt.getName());
                }
            }

            // Move all successfully processed source .claret / .dsl files into src/ directory
            for (File processedFile : successfullyProcessedFiles) {
                moveSourceFileToSrc(input, processedFile, srcDir);
            }

            System.out.println("--------------------------------------------------");
            System.out.println("Processing completed successfully!");

        } catch (ParseException e) {
            System.err.println("Parameter error: " + e.getMessage());
            printCustomHelp(formatter, options);
        } catch (Exception e) {
            System.err.println("Error during execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void moveSourceFileToSrc(File inputRoot, File sourceFile, File targetSrcDir) {
        Path inputBasePath = inputRoot.isDirectory() ? inputRoot.toPath() : inputRoot.getParentFile().toPath();
        try {
            // If file is already inside targetSrcDir, do nothing
            if (sourceFile.toPath().toAbsolutePath().startsWith(targetSrcDir.toPath().toAbsolutePath())) {
                return;
            }
            Path relative = inputBasePath.relativize(sourceFile.toPath());
            File dest = targetSrcDir.toPath().resolve(relative).toFile();
            if (dest.getParentFile() != null) {
                dest.getParentFile().mkdirs();
            }
            if (!sourceFile.toPath().toAbsolutePath().equals(dest.toPath().toAbsolutePath())) {
                Files.move(sourceFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("  -> Moved source file " + sourceFile.getName() + " into: " + dest.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to move source file " + sourceFile.getName() + " to src/: " + e.getMessage());
        }
    }

    public static boolean isSupportedExtension(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".claret") || lower.endsWith(".dsl");
    }

    public static String getBaseName(String fileName) {
        if (fileName.toLowerCase().endsWith(".claret")) {
            return fileName.substring(0, fileName.length() - 7);
        }
        if (fileName.toLowerCase().endsWith(".dsl")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    private static void printCustomHelp(HelpFormatter formatter, Options options) {
        System.out.println("CLARET (UFCG / SPLab) - Standalone Test Case Generator");
        System.out.println("Usage: java -jar target/claret-generator.jar -i <input_path> [options]");
        System.out.println();
        System.out.println("Supported Coverage Approaches (-c / --coverage):");
        for (CoverageCriteria c : CoverageCriteria.values()) {
            System.out.println("  * " + c.getCode() + " : " + c.getDescription());
        }
        System.out.println();
        formatter.printHelp("java -jar target/claret-generator.jar", options);
    }

    public static void collectFiles(File dir, List<File> list) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    // Skip output directory
                    if (!"output".equalsIgnoreCase(f.getName())) {
                        collectFiles(f, list);
                    }
                } else if (isSupportedExtension(f.getName())) {
                    list.add(f);
                }
            }
        }
    }
}
