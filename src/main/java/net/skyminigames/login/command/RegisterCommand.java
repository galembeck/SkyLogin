package net.skyminigames.login.command;

import lombok.RequiredArgsConstructor;
import net.skyminigames.login.SkyLogin;
import net.skyminigames.login.auth.AuthManager;
import net.skyminigames.login.auth.AuthSession;
import net.skyminigames.login.config.PluginConfig;
import net.skyminigames.login.message.Messages;
import net.skyminigames.login.model.Account;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * /register &lt;password&gt; &lt;password&gt;
 */
@RequiredArgsConstructor
public final class RegisterCommand implements CommandExecutor {

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
        if (session.isRegistered()) {
            player.sendMessage(messages.get("register.already-registered"));
            return true;
        }
        if (args.length != 2) {
            error(player, messages.get("register.usage"));
            return true;
        }

        String password = args[0];
        String error = validate(player, password, args[1]);
        if (error != null) {
            error(player, error);
            return true;
        }

        register(player, session, password);
        return true;
    }

    private void error(Player player, String message) {
        player.sendMessage(message);
        AuthManager.playSound(player, plugin.getPluginConfig().getErrorSound());
    }

    private String validate(Player player, String password, String confirmation) {
        Messages messages = plugin.getMessages();
        PluginConfig config = plugin.getPluginConfig();
        if (!password.equals(confirmation)) {
            return messages.get("register.passwords-mismatch");
        }
        if (password.length() < config.getPasswordMinLength()) {
            return messages.get("password.too-short", "min", config.getPasswordMinLength());
        }
        if (password.length() > config.getPasswordMaxLength()) {
            return messages.get("password.too-long", "max", config.getPasswordMaxLength());
        }
        if (password.equalsIgnoreCase(player.getName())) {
            return messages.get("password.same-as-name");
        }
        if (config.getBlockedPasswords().contains(password.toLowerCase(Locale.ROOT))) {
            return messages.get("password.too-weak");
        }
        return null;
    }

    private void register(Player player, AuthSession session, String password) {
        AuthManager auth = plugin.getAuthManager();
        int limit = plugin.getPluginConfig().getMaxAccountsPerIp();
        String ip = AuthManager.ipOf(player);
        boolean checkLimit = limit > 0 && !player.hasPermission("skylogin.bypass");

        session.setProcessing(true);
        CompletableFuture<Integer> accountsOnIp = checkLimit
                ? plugin.getStorage().countByRegistrationIp(ip)
                : CompletableFuture.completedFuture(0);

        accountsOnIp.thenCompose(count -> {
            if (checkLimit && count >= limit) {
                return CompletableFuture.completedFuture((Account) null);
            }
            return CompletableFuture
                    .supplyAsync(() -> plugin.getPasswordService().hash(password), auth.getCryptoExecutor())
                    .thenCompose(hash -> {
                        long now = System.currentTimeMillis();
                        Account account = Account.builder()
                                .name(AuthManager.key(player.getName()))
                                .realName(player.getName())
                                .passwordHash(hash)
                                .registrationIp(ip)
                                .lastIp(ip)
                                .registrationDate(now)
                                .lastLogin(now)
                                .lastSeen(now)
                                .build();
                        return plugin.getStorage().insert(account).thenApply(ignored -> account);
                    });
        }).whenComplete((account, error) -> plugin.sync(() -> {
            session.setProcessing(false);
            if (!player.isOnline() || auth.getSession(player) != session) {
                return;
            }
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not register " + player.getName(), error);
                player.sendMessage(plugin.getMessages().get("general.database-error"));
                return;
            }
            if (account == null) {
                error(player, plugin.getMessages().get("register.too-many-accounts", "max", limit));
                return;
            }
            session.setAccount(account);
            auth.completeAuthentication(player, session, "register.success", "after-register", plugin.getPluginConfig().getRegisterSound());
            plugin.getLogger().info(player.getName() + " registered (" + ip + ").");
        }));
    }
}
