package io.github.diegoquirino.veredito.access;

import io.github.diegoquirino.veredito.config.AccessConfig.CodeHashKey;
import io.github.diegoquirino.veredito.domain.access.EmailAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** {@code HMAC-SHA256(key, email + ":" + code)} in lowercase hexadecimal (research section 4). */
@Component
public class CodeHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    @Autowired
    public CodeHasher(CodeHashKey key) {
        this(key.bytes());
    }

    CodeHasher(byte[] key) {
        this.key = new SecretKeySpec(key, ALGORITHM);
    }

    public String hash(EmailAddress email, String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal((email.value() + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is part of every JDK", e);
        }
    }

    /** Compares in constant time, so the answer time says nothing about the stored hash. */
    public boolean matches(String storedHash, EmailAddress email, String code) {
        return MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.US_ASCII),
                hash(email, code).getBytes(StandardCharsets.US_ASCII));
    }
}
