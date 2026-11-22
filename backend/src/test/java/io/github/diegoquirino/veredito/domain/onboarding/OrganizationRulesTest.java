package io.github.diegoquirino.veredito.domain.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrganizationRulesTest {

    @Test
    void acceptsFixedCodesWithoutOther() {
        for (OrganizationAffiliation org : List.of(OrganizationAffiliation.TJPB, OrganizationAffiliation.UFCG)) {
            OrganizationChoice.Validation result = OrganizationChoice.validate(org.code(), null);
            assertThat(result.valid()).isTrue();
            assertThat(result.choice()).isEqualTo(new OrganizationChoice(org, null));
        }
    }

    @Test
    void rejectsMissingOrUnknown() {
        assertThat(OrganizationChoice.validate(null, null).errors()).isEqualTo(Map.of("organization", "required"));
        assertThat(OrganizationChoice.validate("", null).errors()).isEqualTo(Map.of("organization", "required"));
        assertThat(OrganizationChoice.validate("acme", null).errors()).isEqualTo(Map.of("organization", "option"));
    }

    @Test
    void otherRequiresNonBlankDescription() {
        assertThat(OrganizationChoice.validate("other", null).errors())
                .isEqualTo(Map.of("organizationOther", "required"));
        assertThat(OrganizationChoice.validate("other", "   ").errors())
                .isEqualTo(Map.of("organizationOther", "required"));
    }

    @Test
    void otherKeepsTypedTextAndEnforcesSize() {
        OrganizationChoice.Validation ok = OrganizationChoice.validate("other", "  Acme Ltd  ");
        assertThat(ok.valid()).isTrue();
        assertThat(ok.choice().other()).isEqualTo("  Acme Ltd  ");
        assertThat(OrganizationChoice.validate("other", "a".repeat(100)).valid()).isTrue();
        assertThat(OrganizationChoice.validate("other", "a".repeat(101)).errors())
                .isEqualTo(Map.of("organizationOther", "size"));
    }

    @Test
    void rejectsUnexpectedOtherOnFixedCode() {
        assertThat(OrganizationChoice.validate("tjpb", "x").errors())
                .isEqualTo(Map.of("organizationOther", "unexpected"));
    }
}
