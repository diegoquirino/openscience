package io.github.diegoquirino.veredito.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

/** The message bundles of the application, read as the tests expect them to be served. */
public final class Bundles {

    private Bundles() {}

    /** {@code messages.properties}, {@code messages_pt_BR.properties} or {@code messages_es_ES.properties}. */
    public static Properties load(String name) {
        try (InputStream in = Bundles.class.getClassLoader().getResourceAsStream(name);
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The bundle file of a supported locale; anything else falls back to pt-BR, as the API does. */
    public static String fileOf(String locale) {
        return switch (locale) {
            case "en-US" -> "messages.properties";
            case "es-ES" -> "messages_es_ES.properties";
            default -> "messages_pt_BR.properties";
        };
    }

    public static String format(Properties texts, String key, String locale, Object... args) {
        return new MessageFormat(texts.getProperty(key), Locale.forLanguageTag(locale)).format(args);
    }
}
