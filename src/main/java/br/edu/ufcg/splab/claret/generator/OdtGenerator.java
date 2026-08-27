package br.edu.ufcg.splab.claret.generator;

import br.edu.ufcg.splab.claret.model.TestCase;
import br.edu.ufcg.splab.claret.model.TestStep;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class OdtGenerator {

    public static void generateOdt(String docTitle, List<TestCase> testCases, File outputFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            // 1. mimetype (must be uncompressed STORED)
            byte[] mimeBytes = "application/vnd.oasis.opendocument.text".getBytes(StandardCharsets.US_ASCII);
            ZipEntry mimeEntry = new ZipEntry("mimetype");
            mimeEntry.setMethod(ZipEntry.STORED);
            mimeEntry.setSize(mimeBytes.length);
            mimeEntry.setCrc(calculateCrc(mimeBytes));
            zos.putNextEntry(mimeEntry);
            zos.write(mimeBytes);
            zos.closeEntry();

            // 2. META-INF/manifest.xml
            ZipEntry manifestEntry = new ZipEntry("META-INF/manifest.xml");
            zos.putNextEntry(manifestEntry);
            zos.write(getManifestXml().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. styles.xml
            ZipEntry stylesEntry = new ZipEntry("styles.xml");
            zos.putNextEntry(stylesEntry);
            zos.write(getStylesXml().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 4. content.xml
            ZipEntry contentEntry = new ZipEntry("content.xml");
            zos.putNextEntry(contentEntry);
            zos.write(getContentXml(docTitle, testCases).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private static long calculateCrc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    private static String getManifestXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\">\n" +
               "  <manifest:file-entry manifest:full-path=\"/\" manifest:version=\"1.2\" manifest:media-type=\"application/vnd.oasis.opendocument.text\"/>\n" +
               "  <manifest:file-entry manifest:full-path=\"content.xml\" manifest:media-type=\"text/xml\"/>\n" +
               "  <manifest:file-entry manifest:full-path=\"styles.xml\" manifest:media-type=\"text/xml\"/>\n" +
               "</manifest:manifest>";
    }

    private static String getStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<office:document-styles xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\"\n" +
               "                        xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\"\n" +
               "                        xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\"\n" +
               "                        xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\"\n" +
               "                        office:version=\"1.2\">\n" +
               "  <office:styles>\n" +
               "    <style:style style:name=\"Title\" style:family=\"paragraph\">\n" +
               "      <style:text-properties fo:font-size=\"20pt\" fo:font-weight=\"bold\" fo:color=\"#1a365d\"/>\n" +
               "    </style:style>\n" +
               "    <style:style style:name=\"Subtitle\" style:family=\"paragraph\">\n" +
               "      <style:text-properties fo:font-size=\"10pt\" fo:font-style=\"italic\" fo:color=\"#555555\"/>\n" +
               "    </style:style>\n" +
               "    <style:style style:name=\"Heading_1\" style:family=\"paragraph\">\n" +
               "      <style:text-properties fo:font-size=\"15pt\" fo:font-weight=\"bold\" fo:color=\"#1a365d\"/>\n" +
               "    </style:style>\n" +
               "    <style:style style:name=\"Heading_2\" style:family=\"paragraph\">\n" +
               "      <style:text-properties fo:font-size=\"12pt\" fo:font-weight=\"bold\" fo:color=\"#2b6cb0\"/>\n" +
               "    </style:style>\n" +
               "  </office:styles>\n" +
               "</office:document-styles>";
    }

    private static String getContentXml(String title, List<TestCase> testCases) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\"\n");
        sb.append("                         xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\"\n");
        sb.append("                         xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\"\n");
        sb.append("                         xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\"\n");
        sb.append("                         xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\"\n");
        sb.append("                         office:version=\"1.2\">\n");
        sb.append("  <office:automatic-styles>\n");
        sb.append("    <style:style style:name=\"TableHeader\" style:family=\"table-cell\">\n");
        sb.append("      <style:table-cell-properties fo:background-color=\"#2b6cb0\" fo:padding=\"4pt\" fo:border=\"0.5pt solid #000000\"/>\n");
        sb.append("      <style:text-properties fo:color=\"#ffffff\" fo:font-weight=\"bold\" fo:font-size=\"10pt\"/>\n");
        sb.append("    </style:style>\n");
        sb.append("    <style:style style:name=\"TableCell\" style:family=\"table-cell\">\n");
        sb.append("      <style:table-cell-properties fo:padding=\"4pt\" fo:border=\"0.5pt solid #cccccc\"/>\n");
        sb.append("      <style:text-properties fo:font-size=\"9pt\"/>\n");
        sb.append("    </style:style>\n");
        sb.append("    <style:style style:name=\"BoldText\" style:family=\"text\">\n");
        sb.append("      <style:text-properties fo:font-weight=\"bold\" fo:font-size=\"10pt\"/>\n");
        sb.append("    </style:style>\n");
        sb.append("  </office:automatic-styles>\n");
        sb.append("  <office:body>\n");
        sb.append("    <office:text>\n");

        sb.append("      <text:h text:style-name=\"Title\">").append(escapeXml(title)).append("</text:h>\n");
        sb.append("      <text:p text:style-name=\"Subtitle\">Test Case Specification automatically generated from CLARET specifications (UFCG).</text:p>\n");
        sb.append("      <text:p></text:p>\n");

        String currentUseCase = null;

        for (TestCase tc : testCases) {
            // If Use Case changed or starting new use case block, print Heading 1
            if (currentUseCase == null || !currentUseCase.equals(tc.getUseCaseName())) {
                currentUseCase = tc.getUseCaseName();
                sb.append("      <text:h text:style-name=\"Heading_1\">Use Case: ")
                  .append(escapeXml(currentUseCase))
                  .append(" (v").append(escapeXml(tc.getUseCaseVersion())).append(")</text:h>\n");

                sb.append("      <text:p><text:span text:style-name=\"BoldText\">System: </text:span>")
                  .append(escapeXml(tc.getSystemName())).append("</text:p>\n");
            }

            // Test Case Heading (Heading 2)
            sb.append("      <text:h text:style-name=\"Heading_2\">[").append(escapeXml(tc.getId())).append("] ")
              .append(escapeXml(tc.getSummary())).append("</text:h>\n");

            sb.append("      <text:p><text:span text:style-name=\"BoldText\">Preconditions: </text:span>")
              .append(escapeXml(tc.getPreCondition())).append("</text:p>\n");

            sb.append("      <text:p><text:span text:style-name=\"BoldText\">Postconditions: </text:span>")
              .append(escapeXml(tc.getPostCondition())).append("</text:p>\n");

            // Steps Table
            sb.append("      <table:table table:name=\"Table_").append(escapeXml(tc.getId())).append("\">\n");
            sb.append("        <table:table-column table:number-columns-repeated=\"3\"/>\n");

            // Table Header
            sb.append("        <table:table-row>\n");
            sb.append("          <table:table-cell table:style-name=\"TableHeader\"><text:p>Step #</text:p></table:table-cell>\n");
            sb.append("          <table:table-cell table:style-name=\"TableHeader\"><text:p>Actor Action</text:p></table:table-cell>\n");
            sb.append("          <table:table-cell table:style-name=\"TableHeader\"><text:p>Expected Result (System)</text:p></table:table-cell>\n");
            sb.append("        </table:table-row>\n");

            // Step Rows
            for (TestStep step : tc.getSteps()) {
                sb.append("        <table:table-row>\n");
                sb.append("          <table:table-cell table:style-name=\"TableCell\"><text:p>")
                  .append(step.getStepNumber()).append("</text:p></table:table-cell>\n");

                sb.append("          <table:table-cell table:style-name=\"TableCell\"><text:p>")
                  .append(escapeXml(step.getActor() + ": " + step.getAction())).append("</text:p></table:table-cell>\n");

                sb.append("          <table:table-cell table:style-name=\"TableCell\"><text:p>")
                  .append(escapeXml(step.getExpectedResult())).append("</text:p></table:table-cell>\n");
                sb.append("        </table:table-row>\n");
            }

            sb.append("      </table:table>\n");
            sb.append("      <text:p></text:p>\n");
        }

        sb.append("    </office:text>\n");
        sb.append("  </office:body>\n");
        sb.append("</office:document-content>");
        return sb.toString();
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
