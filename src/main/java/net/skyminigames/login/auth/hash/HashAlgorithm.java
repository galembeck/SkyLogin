package net.skyminigames.login.auth.hash;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum HashAlgorithm {
    BCRYPT(new BCryptHasher()),
    SHA256(new Sha256Hasher());

    private final PasswordHasher hasher;

    public static HashAlgorithm parse(String value) {
        if (value == null) {
            return BCRYPT;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BCRYPT;
        }
    }

    /**
     * Finds the algorithm a stored hash was created with, or {@code null} if unknown.
     */
    public static HashAlgorithm detect(String hash) {
        for (HashAlgorithm algorithm : values()) {
            if (algorithm.hasher.supports(hash)) {
                return algorithm;
            }
        }
        return null;
    }
}
