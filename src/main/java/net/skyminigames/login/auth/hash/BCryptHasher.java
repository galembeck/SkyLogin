package net.skyminigames.login.auth.hash;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt hashes ({@code $2a$10$...}), the same format used by nLogin and OpeNLogin.
 */
public final class BCryptHasher implements PasswordHasher {

    private static final int LOG_ROUNDS = 10;

    @Override
    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(LOG_ROUNDS));
    }

    @Override
    public boolean matches(String password, String hash) {
        try {
            // $2y$ and $2b$ (PHP / other plugins) are the same algorithm; jBCrypt only reads $2a$
            return BCrypt.checkpw(password, "$2a$" + hash.substring(4));
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    @Override
    public boolean supports(String hash) {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}
