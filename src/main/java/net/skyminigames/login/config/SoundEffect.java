package net.skyminigames.login.config;

import lombok.Value;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * A sound from config.yml, played only to one player.
 */
@Value
public class SoundEffect {

    Sound sound;
    float volume;
    float pitch;

    public void play(Player player) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Reads {@code sound}, {@code volume} and {@code pitch} from a section.
     *
     * @return the sound, or {@code null} if the section is missing, disabled ("NONE") or invalid
     */
    public static SoundEffect parse(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return null;
        }
        String name = section.getString("sound", "NONE").trim().toUpperCase(Locale.ROOT);
        if (name.isEmpty() || name.equals("NONE")) {
            return null;
        }
        try {
            return new SoundEffect(Sound.valueOf(name),
                    (float) section.getDouble("volume", 1.0),
                    (float) section.getDouble("pitch", 1.0));
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown sound '" + name + "' in config.yml (" + section.getCurrentPath() + "). Use a 1.8 sound name, e.g. LEVEL_UP.");
            return null;
        }
    }
}
