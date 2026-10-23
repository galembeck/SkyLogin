package net.skyminigames.login.listener;

import lombok.RequiredArgsConstructor;
import net.skyminigames.login.SkyLogin;
import net.skyminigames.login.auth.AuthManager;
import net.skyminigames.login.message.Messages;
import net.skyminigames.login.model.Account;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Connection checks (nickname, duplicates, anti-bot) and the join / quit lifecycle.
 */
@RequiredArgsConstructor
public final class ConnectionListener implements Listener {

    private static final String DUPLICATE_LOGIN_REASON = "You logged in from another location";

    private final SkyLogin plugin;

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        Messages messages = plugin.getMessages();
        String name = event.getName();

        if (!plugin.getStorage().isReady()) {
            // Never let anyone in without authentication if the database is down
            disallow(event, messages.raw("kick.database-error"));
            return;
        }
        if (!plugin.getPluginConfig().getNamePattern().matcher(name).matches()) {
            disallow(event, messages.raw("kick.invalid-name"));
            return;
        }
        // In offline mode a second connection with the same nick would kick the real player
        if (Bukkit.getPlayerExact(name) != null) {
            disallow(event, messages.raw("kick.already-online"));
            return;
        }

        Optional<Account> account;
        try {
            account = plugin.getStorage().find(AuthManager.key(name)).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load the account of " + name, e);
            disallow(event, messages.raw("kick.database-error"));
            return;
        }

        if (account.isPresent()) {
            String registeredName = account.get().getRealName();
            if (!registeredName.equals(name)) {
                disallow(event, messages.raw("kick.name-case-mismatch", "name", name, "registered", registeredName));
                return;
            }
        } else if (!plugin.getAntiBot().allowNewPlayer(event.getAddress().getHostAddress())) {
            disallow(event, messages.raw("kick.antibot-verify", "seconds", plugin.getPluginConfig().getAntiBotRejoinDelaySeconds()));
            return;
        }

        plugin.getAuthManager().preload(name, account);
    }

    /** Another plugin may still deny the connection after us; don't keep its account cached. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLoginResult(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.getAuthManager().discardPreload(event.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        Player online = Bukkit.getPlayerExact(event.getPlayer().getName());
        if (online != null && online != event.getPlayer()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.getMessages().raw("kick.already-online"));
        }
    }

    /** The server kicks the online player when someone joins with the same nick; keep the original player instead. */
    @EventHandler(priority = EventPriority.LOW)
    public void onKick(PlayerKickEvent event) {
        if (event.getReason() != null && event.getReason().contains(DUPLICATE_LOGIN_REASON)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getAuthManager().handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getAuthManager().handleQuit(event.getPlayer());
    }

    private static void disallow(AsyncPlayerPreLoginEvent event, String message) {
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message);
    }
}
