package net.skyminigames.login.command;

import lombok.RequiredArgsConstructor;
import net.skyminigames.login.SkyLogin;
import net.skyminigames.login.auth.AuthManager;
import net.skyminigames.login.auth.AuthSession;
import net.skyminigames.login.message.Messages;
import net.skyminigames.login.model.Account;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * /login &lt;password&gt;
 */
@RequiredArgsConstructor
public final class LoginCommand implements CommandExecutor {

    private final SkyLogin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return true;
        }
        Player player = (Player) sender;
        AuthManager auth = plugin.getAuthManager();
        AuthSession session = auth.getSession(player);
        if (session == null) {
            return true;
        }
        if (session.isAuthenticated()) {
            player.sendMessage(messages.get("login.already-logged"));
            return true;
        }
        if (!session.isLoaded() || session.isProcessing()) {
            player.sendMessage(messages.get("general.processing"));
            return true;
        }
        if (!session.isRegistered()) {
            player.sendMessage(messages.get("login.not-registered"));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(messages.get("login.usage"));
            AuthManager.playSound(player, plugin.getPluginConfig().getErrorSound());
            return true;
        }

        Account account = session.getAccount();
        String password = args[0];
        String hash = account.getPasswordHash();

        session.setProcessing(true);
        CompletableFuture
                .supplyAsync(() -> plugin.getPasswordService().matches(password, hash), auth.getCryptoExecutor())
                .whenComplete((matches, error) -> plugin.sync(() -> {
                    session.setProcessing(false);
                    if (!player.isOnline() || auth.getSession(player) != session) {
                        return;
                    }
                    if (error != null) {
                        plugin.getLogger().log(Level.SEVERE, "Could not check the password of " + player.getName(), error);
                        player.sendMessage(messages.get("general.database-error"));
                        return;
                    }
                    if (matches) {
                        auth.recordLogin(player, account);
                        auth.completeAuthentication(player, session, "login.success", "after-login", plugin.getPluginConfig().getLoginSound());
                        return;
                    }
                    failedAttempt(player, session);
                }));
        return true;
    }

    private void failedAttempt(Player player, AuthSession session) {
        int max = plugin.getPluginConfig().getMaxLoginAttempts();
        session.setFailedAttempts(session.getFailedAttempts() + 1);
        int remaining = max - session.getFailedAttempts();
        if (remaining <= 0) {
            player.kickPlayer(plugin.getMessages().raw("kick.too-many-attempts"));
            plugin.getLogger().info(player.getName() + " was kicked after " + max + " wrong passwords (" + AuthManager.ipOf(player) + ").");
            return;
        }
        player.sendMessage(plugin.getMessages().get("login.wrong-password", "attempts", remaining));
        AuthManager.playSound(player, plugin.getPluginConfig().getWrongPasswordSound());
    }
}
