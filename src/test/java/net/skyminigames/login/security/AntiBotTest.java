package net.skyminigames.login.security;

import net.skyminigames.login.config.PluginConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiBotTest {

    private static AntiBot antiBot(int maxNewPlayers) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("antibot.enabled", true);
        yaml.set("antibot.max-new-players", maxNewPlayers);
        yaml.set("antibot.window-seconds", 10);
        yaml.set("antibot.attack-duration-seconds", 60);
        yaml.set("antibot.rejoin-delay-seconds", 1);
        return new AntiBot(new PluginConfig(yaml), Logger.getLogger("test"));
    }

    @Test
    void allowsNormalTraffic() {
        AntiBot antiBot = antiBot(3);
        assertTrue(antiBot.allowNewPlayer("1.1.1.1"));
        assertTrue(antiBot.allowNewPlayer("1.1.1.2"));
        assertTrue(antiBot.allowNewPlayer("1.1.1.3"));
        assertFalse(antiBot.isUnderAttack());
    }

    @Test
    void floodEnablesAttackModeAndRejoinVerifies() throws InterruptedException {
        AntiBot antiBot = antiBot(3);
        for (int i = 0; i < 3; i++) {
            antiBot.allowNewPlayer("10.0.0." + i);
        }
        // 4th new player in the window: attack mode, must reconnect
        assertFalse(antiBot.allowNewPlayer("10.0.0.99"));
        assertTrue(antiBot.isUnderAttack());

        // Reconnecting too fast is still refused
        assertFalse(antiBot.allowNewPlayer("10.0.0.99"));

        Thread.sleep(1100);
        assertTrue(antiBot.allowNewPlayer("10.0.0.99"), "a player who waited and reconnected is verified");
        assertTrue(antiBot.allowNewPlayer("10.0.0.99"), "verified IPs stay allowed during the attack");
        assertFalse(antiBot.allowNewPlayer("10.0.0.50"), "other new IPs must still verify");
    }
}
