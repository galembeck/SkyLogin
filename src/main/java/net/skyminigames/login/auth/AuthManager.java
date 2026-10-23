package net.skyminigames.login.auth;

import lombok.Getter;
import net.skyminigames.login.SkyLogin;
import net.skyminigames.login.config.PluginConfig;
import net.skyminigames.login.config.SoundEffect;
import net.skyminigames.login.message.Messages;
import net.skyminigames.login.model.Account;
import net.skyminigames.login.util.LoginScoreboard;
import net.skyminigames.login.util.NamedThreadFactory;
import net.skyminigames.login.util.TitleSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Tracks every online player's login state and applies / removes the "not logged in" restrictions.
 * All methods except {@link #isAuthenticated} and {@link #preload} must be called on the main thread.
 */
public final class AuthManager {

    private static final float DEFAULT_WALK_SPEED = 0.2F;
    private static final float DEFAULT_FLY_SPEED = 0.1F;
    private static final int CHAT_HISTORY_LINES = 100;

    private final SkyLogin plugin;
    private final PluginConfig config;
    private final Messages messages;

    private final Map<UUID, AuthSession> sessions = new ConcurrentHashMap<>();
    /** Accounts looked up during async pre-login, picked up on join. Key: lower-case name. */
    private final Map<String, Optional<Account>> preloaded = new ConcurrentHashMap<>();
    /** BCrypt is slow on purpose; it runs here instead of the main or storage threads. */
    @Getter
    private final ExecutorService cryptoExecutor = Executors.newFixedThreadPool(2, new NamedThreadFactory("SkyLogin-Crypto"));

    public AuthManager(SkyLogin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.messages = plugin.getMessages();
    }

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static String ipOf(Player player) {
        InetSocketAddress address = player.getAddress();
        return address == null ? "unknown" : address.getAddress().getHostAddress();
    }

    // ------------------------------------------------------------------ lookup

    public void preload(String name, Optional<Account> account) {
        preloaded.put(key(name), account);
    }

    public void discardPreload(String name) {
        preloaded.remove(key(name));
    }

    public AuthSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public Collection<AuthSession> getSessions() {
        return new ArrayList<>(sessions.values());
    }

    /** Thread-safe. Players without a session are treated as not logged in. */
    public boolean isAuthenticated(UUID uniqueId) {
        AuthSession session = sessions.get(uniqueId);
        return session != null && session.isAuthenticated();
    }

    public boolean isAuthenticated(Player player) {
        return isAuthenticated(player.getUniqueId());
    }

    // ------------------------------------------------------------------ join / quit

    public void handleJoin(Player player) {
        AuthSession session = new AuthSession(player.getUniqueId(), player.getName());
        sessions.put(player.getUniqueId(), session);
        applyRestrictions(player, session);

        Optional<Account> account = preloaded.remove(key(player.getName()));
        // One tick later, so the server's "joined the game" line (and other plugins' join messages)
        // are already sent: they get cleared too and the welcome message ends up last
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || sessions.get(player.getUniqueId()) != session) {
                return;
            }
            if (config.isClearChatOnJoin()) {
                clearChat(player);
            }
            loadAccount(player, session, account);
        });
    }

    private void loadAccount(Player player, AuthSession session, Optional<Account> account) {
        if (account != null) {
            onAccountLoaded(player, session, account.orElse(null));
            return;
        }

        // Not pre-loaded (e.g. after /reload): look it up now
        plugin.getStorage().find(key(player.getName())).whenComplete((result, error) -> plugin.sync(() -> {
            if (!player.isOnline() || sessions.get(player.getUniqueId()) != session) {
                return;
            }
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not load the account of " + player.getName(), error);
                player.kickPlayer(messages.raw("kick.database-error"));
                return;
            }
            onAccountLoaded(player, session, result.orElse(null));
        }));
    }

    private void onAccountLoaded(Player player, AuthSession session, Account account) {
        session.setAccount(account);
        session.setLoaded(true);

        if (account != null && hasValidSession(player, account)) {
            recordLogin(player, account);
            completeAuthentication(player, session, "login.session-restored", "after-login", config.getLoginSound());
            return;
        }
        sendPrompt(player, session);
    }

    public void handleQuit(Player player) {
        AuthSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.isAuthenticated()) {
            saveLastSeen(session);
        } else {
            // Restore speed / XP before the server saves the player's data
            removeRestrictions(player, session);
            LoginScoreboard.hide(player);
            TitleSender.reset(player);
        }
    }

    // ------------------------------------------------------------------ authentication

    /** First message + title on join: the register or login welcome block. */
    public void sendPrompt(Player player, AuthSession session) {
        String path = session.isRegistered() ? "login.welcome" : "register.welcome";
        for (String line : messages.lines(path, "player", player.getName())) {
            player.sendMessage(line);
        }
        sendPromptTitle(player, session);
        if (config.isShowScoreboard()) {
            LoginScoreboard.show(player, messages.raw("scoreboard.title"),
                    messages.lines(session.isRegistered() ? "scoreboard.login" : "scoreboard.register", "player", player.getName()));
        }
        playSound(player, config.getWelcomeSound());
    }

    /** Plays a configured sound; {@code null} (disabled in config.yml) does nothing. */
    public static void playSound(Player player, SoundEffect sound) {
        if (sound != null) {
            sound.play(player);
        }
    }

    public void sendReminder(Player player, AuthSession session) {
        player.sendMessage(messages.get(session.isRegistered() ? "login.request" : "register.request"));
    }

    public void sendPromptTitle(Player player, AuthSession session) {
        TitleSender.send(player, messages.title(session.isRegistered() ? "before-login" : "before-register"));
    }

    public void completeAuthentication(Player player, AuthSession session, String chatPath, String titleKey, SoundEffect sound) {
        session.setAuthenticated(true);
        removeRestrictions(player, session);
        LoginScoreboard.hide(player);
        TitleSender.send(player, messages.title(titleKey));
        player.sendMessage(messages.get(chatPath));
        playSound(player, sound);
        if (plugin.getBungeeBridge() != null) {
            plugin.getBungeeBridge().onAuthenticated(player);
        }
    }

    /** Saves the IP and time of a successful login (used for sessions). */
    public void recordLogin(Player player, Account account) {
        String ip = ipOf(player);
        long now = System.currentTimeMillis();
        account.setLastIp(ip);
        account.setLastLogin(now);
        plugin.getStorage().updateLogin(account.getName(), ip, now).exceptionally(error -> {
            plugin.getLogger().log(Level.WARNING, "Could not save the login of " + account.getRealName(), error);
            return null;
        });
    }

    private boolean hasValidSession(Player player, Account account) {
        long duration = config.getSessionDurationMillis();
        return duration > 0
                && account.getLastSeen() > 0
                && System.currentTimeMillis() - account.getLastSeen() <= duration
                && ipOf(player).equals(account.getLastIp());
    }

    private void saveLastSeen(AuthSession session) {
        Account account = session.getAccount();
        if (account == null) {
            return;
        }
        long now = System.currentTimeMillis();
        account.setLastSeen(now);
        plugin.getStorage().updateLastSeen(account.getName(), now).exceptionally(error -> {
            plugin.getLogger().log(Level.WARNING, "Could not save the session of " + account.getRealName(), error);
            return null;
        });
    }

    /** The client keeps the last 100 chat lines, so 100 empty lines push everything older out of view. */
    private static void clearChat(Player player) {
        for (int i = 0; i < CHAT_HISTORY_LINES; i++) {
            player.sendMessage("");
        }
    }

    // ------------------------------------------------------------------ restrictions

    private void applyRestrictions(Player player, AuthSession session) {
        session.setSavedWalkSpeed(player.getWalkSpeed());
        session.setSavedFlySpeed(player.getFlySpeed());
        session.setSavedLevel(player.getLevel());
        session.setSavedExp(player.getExp());
        session.setRestricted(true);

        player.setWalkSpeed(0F);
        player.setFlySpeed(0F);
        if (config.isApplyBlindness()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false), true);
        }
    }

    private void removeRestrictions(Player player, AuthSession session) {
        if (!session.isRestricted()) {
            return;
        }
        session.setRestricted(false);

        // A saved speed of 0 means the server stopped while the player was frozen last time
        player.setWalkSpeed(session.getSavedWalkSpeed() > 0 ? session.getSavedWalkSpeed() : DEFAULT_WALK_SPEED);
        player.setFlySpeed(session.getSavedFlySpeed() > 0 ? session.getSavedFlySpeed() : DEFAULT_FLY_SPEED);
        if (config.isCountdownInXpBar()) {
            player.setLevel(session.getSavedLevel());
            player.setExp(session.getSavedExp());
        }
        if (config.isApplyBlindness()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    /** Called on plugin disable: restores frozen players and saves sessions of logged-in ones. */
    public void shutdown() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            AuthSession session = sessions.get(player.getUniqueId());
            if (session == null) {
                continue;
            }
            if (session.isAuthenticated()) {
                saveLastSeen(session);
            } else {
                removeRestrictions(player, session);
                LoginScoreboard.hide(player);
                TitleSender.reset(player);
            }
        }
        sessions.clear();
        preloaded.clear();
        cryptoExecutor.shutdown();
    }
}
