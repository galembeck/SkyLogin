package net.skyminigames.login.auth.hash;

public interface PasswordHasher {

    String hash(String password);

    boolean matches(String password, String hash);

    /**
     * Whether this hasher understands the format of {@code hash}.
     */
    boolean supports(String hash);
}
