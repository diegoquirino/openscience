package io.github.diegoquirino.veredito.domain.withdrawal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WithdrawalOptionTest {

    @Test
    void parsesBothCodesAndRejectsUnknown() {
        assertThat(WithdrawalOption.parse("keep_responses")).contains(WithdrawalOption.KEEP_RESPONSES);
        assertThat(WithdrawalOption.parse("DISCARD_RESPONSES")).contains(WithdrawalOption.DISCARD_RESPONSES);
        assertThat(WithdrawalOption.parse("delete")).isEmpty();
        assertThat(WithdrawalOption.DISCARD_RESPONSES.discardsResponses()).isTrue();
        assertThat(WithdrawalOption.KEEP_RESPONSES.discardsResponses()).isFalse();
    }

    @Test
    void impactNeverNegative() {
        assertThat(WithdrawalImpact.of(-1, -2, -3)).isEqualTo(WithdrawalImpact.of(0, 0, 0));
    }
}
