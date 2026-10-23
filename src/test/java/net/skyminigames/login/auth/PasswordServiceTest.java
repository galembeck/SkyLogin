package net.skyminigames.login.auth;

import net.skyminigames.login.auth.hash.HashAlgorithm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    @Test
    void bcryptHashesAndVerifies() {
        PasswordService service = new PasswordService(HashAlgorithm.BCRYPT);
        String hash = service.hash("minhaSenha123");

        assertTrue(hash.startsWith("$2a$10$"));
        assertTrue(service.matches("minhaSenha123", hash));
        assertFalse(service.matches("minhasenha123", hash));
    }

    @Test
    void sha256HashesAndVerifies() {
        PasswordService service = new PasswordService(HashAlgorithm.SHA256);
        String hash = service.hash("minhaSenha123");

        assertTrue(hash.startsWith("$SHA$"));
        assertTrue(service.matches("minhaSenha123", hash));
        assertFalse(service.matches("outraSenha", hash));
    }

    @Test
    void samePasswordGetsDifferentSalts() {
        PasswordService service = new PasswordService(HashAlgorithm.SHA256);
        assertNotEquals(service.hash("abc12345"), service.hash("abc12345"));
    }

    @Test
    void verifiesHashesOfTheOtherAlgorithm() {
        // Switching security.hash-algorithm must not lock out existing players
        String bcrypt = new PasswordService(HashAlgorithm.BCRYPT).hash("senhaAntiga");
        String sha = new PasswordService(HashAlgorithm.SHA256).hash("senhaAntiga");

        PasswordService service = new PasswordService(HashAlgorithm.SHA256);
        assertTrue(service.matches("senhaAntiga", bcrypt));
        assertTrue(service.matches("senhaAntiga", sha));
    }

    @Test
    void acceptsBcrypt2yAnd2bPrefixes() {
        String hash = new PasswordService(HashAlgorithm.BCRYPT).hash("senha2y");
        PasswordService service = new PasswordService(HashAlgorithm.BCRYPT);

        assertTrue(service.matches("senha2y", "$2y$" + hash.substring(4)));
        assertTrue(service.matches("senha2y", "$2b$" + hash.substring(4)));
    }

    @Test
    void verifiesKnownAuthMeShaFormat() {
        // $SHA$salt$sha256(sha256(password) + salt), as written by nLogin / AuthMe
        String salt = "1234567890abcdef";
        String hash = "$SHA$" + salt + "$" + sha256(sha256("teste") + salt);
        assertTrue(new PasswordService(HashAlgorithm.BCRYPT).matches("teste", hash));
    }

    @Test
    void rejectsUnknownOrBrokenHashes() {
        PasswordService service = new PasswordService(HashAlgorithm.BCRYPT);
        assertFalse(service.matches("x", null));
        assertFalse(service.matches("x", "plaintext"));
        assertFalse(service.matches("x", "$SHA$broken"));
        assertFalse(service.matches("x", "$2a$10$tooShort"));
        assertEquals(HashAlgorithm.BCRYPT, HashAlgorithm.parse("nonsense"));
    }

    private static String sha256(String text) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
