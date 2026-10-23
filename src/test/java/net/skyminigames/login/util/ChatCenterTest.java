package net.skyminigames.login.util;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatCenterTest {

    @Test
    void padsShortTextTowardsTheMiddle() {
        String text = ChatColor.GOLD + "" + ChatColor.BOLD + "SKY MINIGAMES";
        String centered = ChatCenter.center(text);

        assertTrue(centered.endsWith(text));
        int padding = centered.length() - text.length();
        // "SKY MINIGAMES" in bold is ~90px wide: (154 - 45) / 4 = ~27 spaces
        assertTrue(padding > 20 && padding < 32, "padding was " + padding);
    }

    @Test
    void leavesFullWidthTextAlone() {
        StringBuilder wide = new StringBuilder();
        for (int i = 0; i < 70; i++) {
            wide.append('W');
        }
        assertEquals(wide.toString(), ChatCenter.center(wide.toString()));
    }
}
