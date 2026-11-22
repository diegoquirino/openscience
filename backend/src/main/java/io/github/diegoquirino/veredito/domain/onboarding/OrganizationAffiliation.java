package io.github.diegoquirino.veredito.domain.onboarding;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Fixed organization codes (spec 025); English identifiers, UI translated. */
public enum OrganizationAffiliation {
    TJPB("tjpb"),
    UFCG("ufcg"),
    OTHER("other");

    private final String code;

    OrganizationAffiliation(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<OrganizationAffiliation> fromCode(String code) {
        return Arrays.stream(values()).filter(o -> o.code.equals(code)).findFirst();
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(OrganizationAffiliation::code).toList();
    }
}
