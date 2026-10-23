package net.skyminigames.login.auth;

import lombok.RequiredArgsConstructor;
import net.skyminigames.login.auth.hash.HashAlgorithm;

/**
 * Hashes new passwords with the configured algorithm and verifies stored hashes of any supported format.
 * Hashing is slow on purpose (BCrypt) - never call it on the main thread.
 */
@RequiredArgsConstructor
public final class PasswordService {

    private final HashAlgorithm algorithm;

    public String hash(String password) {
        return algorithm.getHasher().hash(password);
    }

    public boolean matches(String password, String hash) {
        if (hash == null) {
            return false;
        }
        HashAlgorithm stored = HashAlgorithm.detect(hash);
        return stored != null && stored.getHasher().matches(password, hash);
    }
}
