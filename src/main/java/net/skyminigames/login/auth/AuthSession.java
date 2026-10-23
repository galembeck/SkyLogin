package net.skyminigames.login.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skyminigames.login.model.Account;

import java.util.UUID;

/**
 * Login state of one online player, from join until quit.
 */
@Getter
@Setter
@RequiredArgsConstructor
public final class AuthSession {

    private final UUID uniqueId;
    private final String name;
    private final long joinTime = System.currentTimeMillis();

    /** {@code null} until loaded, and when the player has no account. */
    private volatile Account account;
    /** Whether the account lookup has finished. */
    private volatile boolean loaded;
    /** Read from async chat threads, hence volatile. */
    private volatile boolean authenticated;
    /** A password check or registration is in progress. */
    private boolean processing;
    private int failedAttempts;

    // Player state saved while restricted, restored after login
    private boolean restricted;
    private float savedWalkSpeed;
    private float savedFlySpeed;
    private int savedLevel;
    private float savedExp;

    public boolean isRegistered() {
        return account != null;
    }

    public int getElapsedSeconds() {
        return (int) ((System.currentTimeMillis() - joinTime) / 1000L);
    }
}
