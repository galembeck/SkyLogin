package net.skyminigames.login.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The sidebar shown while a player is not logged in. Each player gets their own scoreboard,
 * replaced by the server's main one after login.
 */
@UtilityClass
public class LoginScoreboard {

    private static final String OBJECTIVE = "skylogin";
    /** 1.8 limits: objective display name 32 characters, score entries 40. */
    private static final int MAX_TITLE_LENGTH = 32;
    private static final int MAX_LINE_LENGTH = 40;
    /** The sidebar shows at most 15 lines. */
    private static final int MAX_LINES = 15;

    /** Shows {@code lines} top to bottom under {@code title}. Lines are already colored. */
    public void show(Player player, String title, List<String> lines) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE, "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(truncate(title, MAX_TITLE_LENGTH));

        int count = Math.min(lines.size(), MAX_LINES);
        Set<String> used = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String entry = unique(truncate(lines.get(i), MAX_LINE_LENGTH), used);
            // Higher scores are drawn higher, so the first line gets the highest score
            objective.getScore(entry).setScore(count - i);
        }
        player.setScoreboard(board);
    }

    /** Puts the player back on the main scoreboard, unless another plugin has already replaced ours. */
    public void hide(Player player) {
        if (player.getScoreboard().getObjective(OBJECTIVE) != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /** Scoreboard entries must be unique (blank lines especially): invisible color codes tell them apart. */
    private static String unique(String line, Set<String> used) {
        String entry = line;
        for (int i = 0; !used.add(entry); i++) {
            entry = truncate(line, MAX_LINE_LENGTH - 2 * (i / 16 + 1)) + suffix(i);
        }
        return entry;
    }

    private static String suffix(int index) {
        StringBuilder suffix = new StringBuilder();
        ChatColor[] colors = ChatColor.values();
        do {
            suffix.append(colors[index % 16]);
            index /= 16;
        } while (index > 0);
        return suffix.toString();
    }

    private static String truncate(String text, int max) {
        String result = text.length() <= max ? text : text.substring(0, max);
        // A color code cut in half would show as a stray "§"
        return result.endsWith(String.valueOf(ChatColor.COLOR_CHAR)) ? result.substring(0, result.length() - 1) : result;
    }
}
