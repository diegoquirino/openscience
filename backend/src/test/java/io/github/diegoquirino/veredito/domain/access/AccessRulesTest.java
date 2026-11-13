package io.github.diegoquirino.veredito.domain.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.diegoquirino.veredito.domain.access.SendLimits.Decision;
import io.github.diegoquirino.veredito.domain.access.SendLimits.Rules;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Validation rules of data-model.md and the limits of research section 5, without Spring. */
class AccessRulesTest {

    @Nested
    class Email {

        @Test
        void isTrimmedAndLowercased() {
            assertThat(EmailAddress.parse("Ana@X.com ")).contains(new EmailAddress("ana@x.com"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ana@", "@x.com", "ana@x", "ana@@x.com", "a b@x.com", "ana@x.com.", "ana@.com", ""})
        void refusesMalformedAddresses(String raw) {
            assertThat(EmailAddress.parse(raw)).isEmpty();
        }

        @Test
        void acceptsUpTo254Characters() {
            String domain = "@example.com";
            assertThat(EmailAddress.parse("a".repeat(254 - domain.length()) + domain)).isPresent();
            assertThat(EmailAddress.parse("a".repeat(255 - domain.length()) + domain)).isEmpty();
        }

        @Test
        void refusesNull() {
            assertThat(EmailAddress.parse(null)).isEmpty();
        }

        @Test
        void constructorRequiresTheNormalizedForm() {
            assertThatThrownBy(() -> new EmailAddress("Ana@x.com")).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Name {

        @Test
        void isTrimmedAndHas2To120Characters() {
            assertThat(PersonName.parse("  Ana Souza ")).contains(new PersonName("Ana Souza"));
            assertThat(PersonName.parse("Al")).isPresent();
            assertThat(PersonName.parse("x".repeat(120))).isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = {"A", " A ", "", "   "})
        void refusesShortNames(String raw) {
            assertThat(PersonName.parse(raw)).isEmpty();
        }

        @Test
        void refusesLongNamesAndNull() {
            assertThat(PersonName.parse("x".repeat(121))).isEmpty();
            assertThat(PersonName.parse(null)).isEmpty();
        }
    }

    @Nested
    class Code {

        @Test
        void normalizationRemovesSpacesAndHyphens() {
            assertThat(AccessCodeFormat.normalize(" 123 456 ")).isEqualTo("123456");
            assertThat(AccessCodeFormat.normalize("123-456")).isEqualTo("123456");
            assertThat(AccessCodeFormat.normalize(null)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345", "1234567", "12a456", "", "??????"})
        void onlySixAsciiDigitsAreWellFormed(String code) {
            assertThat(AccessCodeFormat.isWellFormed(code)).isFalse();
        }

        @Test
        void sixDigitsAreWellFormed() {
            assertThat(AccessCodeFormat.isWellFormed("012345")).isTrue();
        }

        @Test
        void generationCoversTheWholeRangeWithLeadingZeros() {
            assertThat(AccessCodeFormat.generate(fixed(0))).isEqualTo("000000");
            assertThat(AccessCodeFormat.generate(fixed(42))).isEqualTo("000042");
            assertThat(AccessCodeFormat.generate(fixed(999_999))).isEqualTo("999999");
        }

        private RandomGenerator fixed(int value) {
            return new RandomGenerator() {
                @Override
                public long nextLong() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int nextInt(int bound) {
                    assertThat(bound).isEqualTo(1_000_000);
                    return value;
                }
            };
        }
    }

    @Nested
    class Limits {

        private final Instant now = Instant.parse("2026-11-13T12:00:00Z");
        private final Rules rules = new Rules(Duration.ofSeconds(60), 5, 450);

        @Test
        void firstRequestIsAllowed() {
            assertThat(SendLimits.decide(List.of(), 0, now, rules)).isEqualTo(new Decision.Allowed());
        }

        @Test
        void aSendLessThan60SecondsAgoAnswersTheSecondsLeft() {
            Decision decision = SendLimits.decide(List.of(now.minusSeconds(20)), 1, now, rules);
            assertThat(decision).isEqualTo(new Decision.TooSoon(40));
        }

        @Test
        void secondsLeftAreRoundedUp() {
            Decision decision = SendLimits.decide(List.of(now.minusMillis(59_500)), 1, now, rules);
            assertThat(decision).isEqualTo(new Decision.TooSoon(1));
        }

        @Test
        void exactly60SecondsLaterIsAllowed() {
            assertThat(SendLimits.decide(List.of(now.minusSeconds(60)), 1, now, rules))
                    .isEqualTo(new Decision.Allowed());
        }

        @Test
        void fiveSendsInTheLastHourAnswerTheTimeTheOldestLeavesTheWindow() {
            List<Instant> sends = List.of(
                    now.minusSeconds(3000), now.minusSeconds(2000), now.minusSeconds(1000), now.minusSeconds(500),
                    now.minusSeconds(100));
            Decision decision = SendLimits.decide(sends, 5, now, rules);
            assertThat(decision).isEqualTo(new Decision.HourlyLimit(now.minusSeconds(3000).plus(Duration.ofHours(1))));
        }

        @Test
        void tooSoonIsCheckedBeforeTheHourlyLimit() {
            List<Instant> sends = List.of(
                    now.minusSeconds(3000), now.minusSeconds(2000), now.minusSeconds(1000), now.minusSeconds(500),
                    now.minusSeconds(10));
            assertThat(SendLimits.decide(sends, 5, now, rules)).isEqualTo(new Decision.TooSoon(50));
        }

        @Test
        void theDailyLimitCountsEveryEmail() {
            assertThat(SendLimits.decide(List.of(), 449, now, rules)).isEqualTo(new Decision.Allowed());
            assertThat(SendLimits.decide(List.of(), 450, now, rules)).isEqualTo(new Decision.DailyLimit());
        }

        @Test
        void aZeroDailyLimitRefusesEverySend() {
            assertThat(SendLimits.decide(List.of(), 0, now, new Rules(Duration.ofSeconds(60), 5, 0)))
                    .isEqualTo(new Decision.DailyLimit());
        }

        @Test
        void theHourlyLimitIsCheckedBeforeTheDailyLimit() {
            List<Instant> sends = List.of(
                    now.minusSeconds(3000), now.minusSeconds(2000), now.minusSeconds(1000), now.minusSeconds(500),
                    now.minusSeconds(100));
            assertThat(SendLimits.decide(sends, 450, now, rules)).isInstanceOf(Decision.HourlyLimit.class);
        }
    }

    @Nested
    class Pseudonyms {

        @Test
        void areRAndTheIdWithAtLeastFourDigits() {
            assertThat(Pseudonym.of(1)).isEqualTo("R-0001");
            assertThat(Pseudonym.of(42)).isEqualTo("R-0042");
            assertThat(Pseudonym.of(9999)).isEqualTo("R-9999");
            assertThat(Pseudonym.of(10000)).isEqualTo("R-10000");
        }

        @Test
        void requireAPositiveId() {
            assertThatThrownBy(() -> Pseudonym.of(0)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
