package io.github.diegoquirino.veredito.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** FR-017: the three message bundles are valid UTF-8, have the same keys and no empty value. */
class MessageBundleParityTest {

    private static final List<String> BUNDLES =
            List.of("messages.properties", "messages_pt_BR.properties", "messages_es_ES.properties");

    @Test
    void bundlesHaveTheSameKeysAndNoEmptyValue() throws IOException {
        Map<String, Properties> bundles = new LinkedHashMap<>();
        for (String name : BUNDLES) {
            bundles.put(name, load(name));
        }
        Set<String> allKeys = new TreeSet<>();
        bundles.values().forEach(properties -> allKeys.addAll(properties.stringPropertyNames()));

        List<String> problems = new ArrayList<>();
        for (String key : allKeys) {
            bundles.forEach((name, properties) -> {
                String value = properties.getProperty(key);
                if (value == null) {
                    problems.add("missing key " + key + " in " + name);
                } else if (value.isBlank()) {
                    problems.add("empty value of " + key + " in " + name);
                }
            });
        }
        assertTrue(problems.isEmpty(), () -> String.join(System.lineSeparator(), problems));
    }

    private static Properties load(String name) throws IOException {
        try (InputStream in = MessageBundleParityTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("Bundle not found on the classpath: " + name);
            }
            Properties properties = new Properties();
            CharsetDecoder strictUtf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            try (Reader reader = new InputStreamReader(in, strictUtf8)) {
                properties.load(reader);
            }
            return properties;
        }
    }
}
