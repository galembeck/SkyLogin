package net.skyminigames.login;

import lombok.Getter;
import net.skyminigames.login.auth.AuthManager;
import net.skyminigames.login.auth.PasswordService;
import net.skyminigames.login.bungee.BungeeBridge;
import net.skyminigames.login.command.LoginCommand;
import net.skyminigames.login.command.RegisterCommand;
import net.skyminigames.login.config.PluginConfig;
import net.skyminigames.login.listener.ConnectionListener;
import net.skyminigames.login.listener.RestrictionListener;
import net.skyminigames.login.message.Messages;
import net.skyminigames.login.security.AntiBot;
import net.skyminigames.login.security.CommandLogFilter;
import net.skyminigames.login.storage.AccountStorage;
import net.skyminigames.login.task.AuthTask;
import net.skyminigames.login.util.StartupBanner;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

@Getter
public final class SkyLogin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private Messages messages;
    private PasswordService passwordService;
    private AccountStorage storage;
    private AntiBot antiBot;
    private AuthManager authManager;
    private CommandLogFilter commandLogFilter;
    /** {@code null} unless bungeecord.enabled (network mode). */
    private BungeeBridge bungeeBridge;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        saveDefaultConfig();
        pluginConfig = new PluginConfig(getConfig(), getLogger());
        messages = new Messages(this);
        messages.reload();
        passwordService = new PasswordService(pluginConfig.getHashAlgorithm());
        antiBot = new AntiBot(pluginConfig, getLogger());

        storage = new AccountStorage(getDataFolder(), pluginConfig);
        try {
            storage.init();
        } catch (SQLException e) {
            // The plugin stays enabled: while the database is down nobody can join without logging in
            getLogger().log(Level.SEVERE, "Could not connect to the " + pluginConfig.getStorageType()
                    + " database. Players will be refused until this is fixed.", e);
        }

        Boolean bungee = pluginConfig.getBungeeEnabled();
        if (bungee != null ? bungee : getServer().spigot().getConfig().getBoolean("settings.bungeecord")) {
            bungeeBridge = new BungeeBridge(this);
        }
        authManager = new AuthManager(this);

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new ConnectionListener(this), this);
        plugins.registerEvents(new RestrictionListener(this), this);
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));

        if (pluginConfig.isHideCommandsInConsole()) {
            commandLogFilter = CommandLogFilter.install(pluginConfig.getAllowedCommands());
        }

        new AuthTask(this).runTaskTimer(this, 20L, 20L);

        // After /reload, players already online must log in again
        for (Player player : getServer().getOnlinePlayers()) {
            authManager.handleJoin(player);
        }

        StartupBanner.printEnable(this, System.currentTimeMillis() - start);
    }

    @Override
    public void onDisable() {
        if (authManager != null) {
            authManager.shutdown();
        }
        if (storage != null) {
            storage.close();
        }
        if (commandLogFilter != null) {
            commandLogFilter.deactivate();
        }
        if (bungeeBridge != null) {
            bungeeBridge.unregister();
        }
        StartupBanner.printDisable(this);
    }

    /**
     * Runs {@code task} on the main thread (immediately if already on it).
     */
    public void sync(Runnable task) {
        if (getServer().isPrimaryThread()) {
            task.run();
        } else if (isEnabled()) {
            getServer().getScheduler().runTask(this, task);
        }
    }
}
