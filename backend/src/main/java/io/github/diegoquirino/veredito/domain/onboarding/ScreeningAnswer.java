package io.github.diegoquirino.veredito.domain.onboarding;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** The three answers of Appendix B.3; {@code yes_*} make the participant eligible, {@code no} is permanent. */
public enum ScreeningAnswer {
    YES_PROFESSIONAL("yes_professional", true),
    YES_ACADEMIC("yes_academic", true),
    NO("no", false);

    private final String code;
    private final boolean eligible;

    ScreeningAnswer(String code, boolean eligible) {
        this.code = code;
        this.eligible = eligible;
    }

    public String code() {
        return code;
    }

    public boolean eligible() {
        return eligible;
    }

    public static Optional<ScreeningAnswer> fromCode(String code) {
        return Arrays.stream(values()).filter(answer -> answer.code.equals(code)).findFirst();
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(ScreeningAnswer::code).toList();
    }
}
