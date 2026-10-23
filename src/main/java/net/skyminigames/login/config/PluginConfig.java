package net.skyminigames.login.config;

import lombok.Getter;
import net.skyminigames.login.auth.hash.HashAlgorithm;
import net.skyminigames.login.storage.StorageType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Typed, read-only view of config.yml.
 */
@Getter
public final class PluginConfig {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]{1,64}$");
    private static final String DEFAULT_TABLE = "skylogin_accounts";

    // storage
    private final StorageType storageType;
    private final String table;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final int mysqlPoolSize;

    // security
    private final HashAlgorithm hashAlgorithm;
    private final int passwordMinLength;
    private final int passwordMaxLength;
    private final Set<String> blockedPasswords;
    private final int maxLoginAttempts;
    private final int maxAccountsPerIp;
    private final Pattern namePattern;
    private final boolean hideCommandsInConsole;

    // login
    private final boolean clearChatOnJoin;
    private final int loginTimeoutSeconds;
    private final int messageIntervalSeconds;
    private final boolean applyBlindness;
    private final boolean countdownInXpBar;
    private final boolean showScoreboard;
    private final Set<String> allowedCommands;

    // sounds (null = disabled)
    private final SoundEffect welcomeSound;
    private final SoundEffect registerSound;
    private final SoundEffect loginSound;
    private final SoundEffect wrongPasswordSound;
    private final SoundEffect errorSound;

    // session
    private final long sessionDurationMillis;

    // antibot
    private final boolean antiBotEnabled;
    private final int antiBotMaxNewPlayers;
    private final int antiBotWindowSeconds;
    private final int antiBotAttackDurationSeconds;
    private final int antiBotRejoinDelaySeconds;

    // bungeecord (network mode)
    /** {@code null} = "auto": follow spigot.yml's settings.bungeecord. */
    private final Boolean bungeeEnabled;
    private final String bungeeSendTo;
    private final int bungeeDelayTicks;

    public PluginConfig(FileConfiguration config) {
        this(config, Logger.getLogger("SkyLogin"));
    }

    public PluginConfig(FileConfiguration config, Logger logger) {
        storageType = StorageType.parse(config.getString("storage.type"));
        String configuredTable = config.getString("storage.table", DEFAULT_TABLE);
        table = SAFE_TABLE_NAME.matcher(configuredTable).matches() ? configuredTable : DEFAULT_TABLE;
        mysqlHost = config.getString("storage.mysql.host", "localhost");
        mysqlPort = config.getInt("storage.mysql.port", 3306);
        mysqlDatabase = config.getString("storage.mysql.database", "skylogin");
        mysqlUsername = config.getString("storage.mysql.username", "skylogin");
        mysqlPassword = config.getString("storage.mysql.password", "");
        mysqlPoolSize = Math.max(1, config.getInt("storage.mysql.pool-size", 10));

        hashAlgorithm = HashAlgorithm.parse(config.getString("security.hash-algorithm"));
        passwordMinLength = Math.max(1, config.getInt("security.password.min-length", 4));
        passwordMaxLength = Math.max(passwordMinLength, config.getInt("security.password.max-length", 32));
        blockedPasswords = lowerCaseSet(config.getStringList("security.password.blocked"));
        maxLoginAttempts = Math.max(1, config.getInt("security.max-login-attempts", 3));
        maxAccountsPerIp = Math.max(0, config.getInt("security.max-accounts-per-ip", 3));
        namePattern = Pattern.compile(config.getString("security.name-pattern", "^[a-zA-Z0-9_]{3,16}$"));
        hideCommandsInConsole = config.getBoolean("security.hide-commands-in-console", true);

        clearChatOnJoin = config.getBoolean("login.clear-chat-on-join", true);
        loginTimeoutSeconds = Math.max(10, config.getInt("login.timeout", 60));
        messageIntervalSeconds = Math.max(0, config.getInt("login.message-interval", 0));
        applyBlindness = config.getBoolean("login.apply-blindness", false);
        countdownInXpBar = config.getBoolean("login.show-countdown-in-xp-bar", true);
        showScoreboard = config.getBoolean("login.show-scoreboard", true);
        allowedCommands = lowerCaseSet(config.getStringList("login.allowed-commands"));

        welcomeSound = SoundEffect.parse(config.getConfigurationSection("sounds.welcome"), logger);
        registerSound = SoundEffect.parse(config.getConfigurationSection("sounds.register"), logger);
        loginSound = SoundEffect.parse(config.getConfigurationSection("sounds.login"), logger);
        wrongPasswordSound = SoundEffect.parse(config.getConfigurationSection("sounds.wrong-password"), logger);
        errorSound = SoundEffect.parse(config.getConfigurationSection("sounds.error"), logger);

        sessionDurationMillis =TimeUnit.MINUTES.toMillis(Math.max(0, config.getInt("session.duration-minutes", 0)));

        antiBotEnabled = config.getBoolean("antibot.enabled", true);
        antiBotMaxNewPlayers = Math.max(1, config.getInt("antibot.max-new-players", 6));
        antiBotWindowSeconds = Math.max(1, config.getInt("antibot.window-seconds", 10));
        antiBotAttackDurationSeconds = Math.max(1, config.getInt("antibot.attack-duration-seconds", 60));
        antiBotRejoinDelaySeconds = Math.max(1, config.getInt("antibot.rejoin-delay-seconds", 5));

        bungeeEnabled = config.isBoolean("bungeecord.enabled") ? config.getBoolean("bungeecord.enabled") : null;
        bungeeSendTo = config.getString("bungeecord.send-to", "lobby");
        bungeeDelayTicks = Math.max(0, config.getInt("bungeecord.delay-ticks", 20));
    }

    private static Set<String> lowerCaseSet(List<String> values) {
        Set<String> set = new HashSet<>();
        for (String value : values) {
            set.add(value.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(set);
    }
}
