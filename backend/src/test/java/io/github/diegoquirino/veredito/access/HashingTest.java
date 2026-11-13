package io.github.diegoquirino.veredito.access;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Research sections 3 and 4: what the database keeps of codes and session tokens. */
class HashingTest {

    /** A fixed HMAC input of 32 bytes; the expected digest was computed with Python's hmac module. */
    private static final byte[] FIXED_HMAC_INPUT = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private final EmailAddress ana = EmailAddress.parse("ana@example.com").orElseThrow();
    private final EmailAddress bia = EmailAddress.parse("bia@example.com").orElseThrow();

    @Nested
    class Codes {

        private final CodeHasher hasher = new CodeHasher(FIXED_HMAC_INPUT);

        @Test
        void matchesTheKnownVector() {
            assertThat(hasher.hash(ana, "123456"))
                    .isEqualTo("cf724ef7451db5ba826acfe0ee8347868eefa79c4c416fd8b5687c64d61093a6");
        }

        @Test
        void bindsTheCodeToTheEmail() {
            assertThat(hasher.hash(ana, "123456")).isNotEqualTo(hasher.hash(bia, "123456"));
        }

        @Test
        void dependsOnTheKey() {
            CodeHasher other = new CodeHasher("fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.US_ASCII));
            assertThat(other.hash(ana, "123456")).isNotEqualTo(hasher.hash(ana, "123456"));
        }

        @Test
        void neverContainsTheCode() {
            for (String code : new String[] {"000000", "123456", "999999"}) {
                assertThat(hasher.hash(ana, code)).hasSize(64).matches("[0-9a-f]{64}").doesNotContain(code);
            }
        }

        @Test
        void comparesOnlyTheRightCodeAndEmail() {
            String stored = hasher.hash(ana, "123456");
            assertThat(hasher.matches(stored, ana, "123456")).isTrue();
            assertThat(hasher.matches(stored, ana, "123457")).isFalse();
            assertThat(hasher.matches(stored, bia, "123456")).isFalse();
        }
    }

    @Nested
    class Tokens {

        private final TokenGenerator generator = new TokenGenerator();
        private final TokenHasher hasher = new TokenHasher();

        @Test
        void are32RandomBytesInBase64UrlWithoutPadding() {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                String generated = generator.next();
                assertThat(generated).hasSize(43).matches("[A-Za-z0-9_-]{43}");
                assertThat(Base64.getUrlDecoder().decode(generated)).hasSize(32);
                seen.add(generated);
            }
            assertThat(seen).hasSize(100);
        }

        @Test
        void hashIsSha256InHexadecimal() {
            assertThat(hasher.hash("abc"))
                    .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        }

        @Test
        void hashNeverContainsTheToken() {
            String generated = generator.next();
            assertThat(hasher.hash(generated)).hasSize(64).doesNotContain(generated);
        }
    }
}
