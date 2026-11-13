package io.github.diegoquirino.veredito.access;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/** Session tokens: 32 random bytes, base64url without padding (research section 3). */
@Component
public class TokenGenerator {

    public static final int BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
