package net.skyminigames.login.util;

import lombok.experimental.UtilityClass;
import net.skyminigames.login.message.TitleMessage;
import org.bukkit.entity.Player;
import org.github.paperspigot.Title;

/**
 * Sends titles with custom timings on PaperSpigot, and falls back to Spigot's fixed-timing title elsewhere.
 */
@UtilityClass
public class TitleSender {

    private static final boolean PAPER_TITLES = classExists("org.github.paperspigot.Title");

    /**
     * Whether a long "stay" time is honoured. If not, the title must be re-sent every few seconds.
     */
    public boolean supportsTimings() {
        return PAPER_TITLES;
    }

    public void send(Player player, TitleMessage title) {
        if (PAPER_TITLES) {
            PaperTitles.send(player, title);
        } else {
            player.sendTitle(title.getTitle(), title.getSubtitle());
        }
    }

    public void reset(Player player) {
        player.resetTitle();
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Kept in its own class so Spigot servers never load the Paper-only {@link Title} class. */
    private static final class PaperTitles {
        static void send(Player player, TitleMessage title) {
            player.sendTitle(new Title(title.getTitle(), title.getSubtitle(), title.getFadeIn(), title.getStay(), title.getFadeOut()));
        }
    }
}
