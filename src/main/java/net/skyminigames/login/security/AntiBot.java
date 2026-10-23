package net.skyminigames.login.security;

import net.skyminigames.login.config.PluginConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Detects bot floods by counting new (unregistered) players per time window.
 * <p>
 * During an attack, every new player is kicked once and has to reconnect after a short delay to be verified.
 * Bots almost never reconnect, real players do. Registered players are never checked.
 */
public final class AntiBot {

    /** How long a pending verification stays valid after the rejoin delay. */
    private static final long VERIFICATION_WINDOW_MILLIS = TimeUnit.SECONDS.toMillis(60);

    private final boolean enabled;
    private final int maxNewPlayers;
    private final long windowMillis;
    private final long attackDurationMillis;
    private final long rejoinDelayMillis;
    private final Logger logger;

    private final Deque<Long> recentNewPlayers = new ArrayDeque<>();
    private final Map<String, Long> pendingVerification = new HashMap<>();
    private final Set<String> verified = new HashSet<>();
    private long attackUntil;

    public AntiBot(PluginConfig config, Logger logger) {
        this.enabled = config.isAntiBotEnabled();
        this.maxNewPlayers = config.getAntiBotMaxNewPlayers();
        this.windowMillis = TimeUnit.SECONDS.toMillis(config.getAntiBotWindowSeconds());
        this.attackDurationMillis = TimeUnit.SECONDS.toMillis(config.getAntiBotAttackDurationSeconds());
        this.rejoinDelayMillis = TimeUnit.SECONDS.toMillis(config.getAntiBotRejoinDelaySeconds());
        this.logger = logger;
    }

    /**
     * Called (from the async pre-login thread) for every connection of a player without an account.
     *
     * @return {@code true} if the player may join, {@code false} if they must reconnect to be verified
     */
    public synchronized boolean allowNewPlayer(String ip) {
        if (!enabled) {
            return true;
        }
        long now = System.currentTimeMillis();

        recentNewPlayers.addLast(now);
        while (!recentNewPlayers.isEmpty() && now - recentNewPlayers.peekFirst() > windowMillis) {
            recentNewPlayers.pollFirst();
        }
        if (recentNewPlayers.size() > maxNewPlayers) {
            if (now >= attackUntil) {
                logger.warning("[AntiBot] " + recentNewPlayers.size() + " new players in " + (windowMillis / 1000)
                        + "s - attack mode enabled. New players must reconnect to be verified.");
            }
            attackUntil = now + attackDurationMillis;
        }

        if (now >= attackUntil) {
            if (!pendingVerification.isEmpty() || !verified.isEmpty()) {
                logger.info("[AntiBot] Attack mode disabled.");
                pendingVerification.clear();
                verified.clear();
            }
            return true;
        }

        if (verified.contains(ip)) {
            return true;
        }
        Long firstAttempt = pendingVerification.get(ip);
        if (firstAttempt != null) {
            long elapsed = now - firstAttempt;
            if (elapsed < rejoinDelayMillis) {
                // Reconnected too fast: keep the original timestamp so they just wait the rest of the delay
                return false;
            }
            if (elapsed <= rejoinDelayMillis + VERIFICATION_WINDOW_MILLIS) {
                pendingVerification.remove(ip);
                verified.add(ip);
                return true;
            }
        }
        pendingVerification.put(ip, now);
        return false;
    }

    public synchronized boolean isUnderAttack() {
        return System.currentTimeMillis() < attackUntil;
    }
}
