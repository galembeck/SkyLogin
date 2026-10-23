package net.skyminigames.login.bungee;

import net.skyminigames.login.SkyLogin;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Network mode (BungeeCord). After a successful login / register:
 * <ol>
 *     <li>tells the proxy (SkyProxy) the player logged in, which unlocks the other servers;</li>
 *     <li>after a short delay, asks BungeeCord to move the player to the lobby.</li>
 * </ol>
 */
public final class BungeeBridge {

    /** Must match SkyProxy's AuthMessage. */
    private static final String AUTH_CHANNEL = "skylogin:auth";
    private static final String AUTHENTICATED = "authenticated";
    /** BungeeCord's built-in channel ("Connect" sub-channel moves a player). */
    private static final String BUNGEE_CHANNEL = "BungeeCord";

    private final SkyLogin plugin;

    public BungeeBridge(SkyLogin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, AUTH_CHANNEL);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
    }

    public void onAuthenticated(Player player) {
        player.sendPluginMessage(plugin, AUTH_CHANNEL, write(AUTHENTICATED));

        String target = plugin.getPluginConfig().getBungeeSendTo();
        if (target == null || target.isEmpty()) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendPluginMessage(plugin, BUNGEE_CHANNEL, write("Connect", target));
            }
        }, plugin.getPluginConfig().getBungeeDelayTicks());
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
    }

    private static byte[] write(String... values) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            for (String value : values) {
                out.writeUTF(value);
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
