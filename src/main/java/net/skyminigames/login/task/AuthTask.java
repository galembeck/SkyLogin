package net.skyminigames.login.task;

import lombok.RequiredArgsConstructor;
import net.skyminigames.login.SkyLogin;
import net.skyminigames.login.auth.AuthManager;
import net.skyminigames.login.auth.AuthSession;
import net.skyminigames.login.config.PluginConfig;
import net.skyminigames.login.util.TitleSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runs every second: kicks players who took too long, updates the XP-bar countdown and repeats the reminders.
 */
@RequiredArgsConstructor
public final class AuthTask extends BukkitRunnable {

    /** On plain Spigot titles fade after ~5 seconds, so they are re-sent this often. */
    private static final int SPIGOT_TITLE_REFRESH_SECONDS = 3;

    private final SkyLogin plugin;

    @Override
    public void run() {
        AuthManager auth = plugin.getAuthManager();
        PluginConfig config = plugin.getPluginConfig();
        int timeout = config.getLoginTimeoutSeconds();

        for (AuthSession session : auth.getSessions()) {
            if (session.isAuthenticated()) {
                continue;
            }
            Player player = plugin.getServer().getPlayer(session.getUniqueId());
            if (player == null) {
                continue;
            }

            int elapsed = session.getElapsedSeconds();
            int remaining = timeout - elapsed;
            if (remaining <= 0) {
                player.kickPlayer(plugin.getMessages().raw(session.isRegistered() ? "kick.login-timeout" : "kick.register-timeout"));
                continue;
            }

            if (config.isCountdownInXpBar()) {
                player.setLevel(remaining);
                player.setExp(Math.min(0.99F, (float) remaining / timeout));
            }

            if (!session.isLoaded() || session.isProcessing() || elapsed == 0) {
                continue;
            }
            int interval = config.getMessageIntervalSeconds();
            if (interval > 0 && elapsed % interval == 0) {
                auth.sendReminder(player, session);
            }
            if (!TitleSender.supportsTimings() && elapsed % SPIGOT_TITLE_REFRESH_SECONDS == 0) {
                auth.sendPromptTitle(player, session);
            }
        }
    }
}
