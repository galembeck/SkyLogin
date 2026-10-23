package net.skyminigames.login.auth.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Salted SHA-256 in the {@code $SHA$salt$hash} format used by nLogin and AuthMe,
 * where {@code hash = sha256(sha256(password) + salt)}.
 */
public final class Sha256Hasher implements PasswordHasher {

    private static final String PREFIX = "$SHA$";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String hash(String password) {
        byte[] saltBytes = new byte[8];
        RANDOM.nextBytes(saltBytes);
        String salt = toHex(saltBytes);
        return PREFIX + salt + "$" + digest(password, salt);
    }

    @Override
    public boolean matches(String password, String hash) {
        String[] parts = hash.split("\\$");
        // "", "SHA", salt, hash
        if (parts.length != 4) {
            return false;
        }
        byte[] expected = parts[3].toLowerCase().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = digest(password, parts[2]).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    @Override
    public boolean supports(String hash) {
        return hash.startsWith(PREFIX);
    }

    private static String digest(String password, String salt) {
        return sha256(sha256(password) + salt);
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
