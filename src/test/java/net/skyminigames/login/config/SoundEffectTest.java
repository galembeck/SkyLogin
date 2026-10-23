package net.skyminigames.login.config;

import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SoundEffectTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    private static SoundEffect parse(String sound) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("s.sound", sound);
        yaml.set("s.volume", 0.5);
        yaml.set("s.pitch", 1.5);
        return SoundEffect.parse(yaml.getConfigurationSection("s"), LOGGER);
    }

    @Test
    void readsSoundVolumeAndPitch() {
        SoundEffect effect = parse("level_up");
        assertEquals(Sound.LEVEL_UP, effect.getSound());
        assertEquals(0.5F, effect.getVolume());
        assertEquals(1.5F, effect.getPitch());
    }

    @Test
    void noneMissingOrUnknownDisablesTheSound() {
        assertNull(parse("NONE"));
        assertNull(parse("NOT_A_SOUND"));
        assertNull(SoundEffect.parse(null, LOGGER));
    }
}
