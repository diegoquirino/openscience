package io.github.diegoquirino.veredito.domain.access;

import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/** Shape of the 6-digit access code; a typed code loses its spaces and hyphens first. */
public final class AccessCodeFormat {

    public static final int LENGTH = 6;

    private static final int RANGE = 1_000_000;
    private static final Pattern WELL_FORMED = Pattern.compile("[0-9]{" + LENGTH + "}");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s-]");

    private AccessCodeFormat() {}

    public static String normalize(String typed) {
        return typed == null ? "" : SEPARATORS.matcher(typed).replaceAll("");
    }

    public static boolean isWellFormed(String normalized) {
        return WELL_FORMED.matcher(normalized).matches();
    }

    /** A uniform code from {@code 000000} to {@code 999999}; production passes a {@code SecureRandom}. */
    public static String generate(RandomGenerator random) {
        return String.format(Locale.ROOT, "%06d", random.nextInt(RANGE));
    }
}
