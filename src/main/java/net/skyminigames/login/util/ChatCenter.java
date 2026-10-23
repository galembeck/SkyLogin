package net.skyminigames.login.util;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

/**
 * Centers a line in the default chat window, using the pixel widths of Minecraft's default font.
 */
@UtilityClass
public class ChatCenter {

    /** Half the width of the default chat window, in pixels. */
    private static final int CENTER_PX = 154;
    private static final int SPACE_WIDTH = 4;

    public String center(String message) {
        int width = 0;
        boolean afterColorChar = false;
        boolean bold = false;

        for (char c : message.toCharArray()) {
            if (c == ChatColor.COLOR_CHAR) {
                afterColorChar = true;
                continue;
            }
            if (afterColorChar) {
                afterColorChar = false;
                bold = c == 'l' || c == 'L';
                continue;
            }
            width += charWidth(c) + (bold ? 1 : 0) + 1;
        }

        int padding = CENTER_PX - width / 2;
        StringBuilder builder = new StringBuilder();
        for (int added = 0; added < padding; added += SPACE_WIDTH) {
            builder.append(' ');
        }
        return builder.append(message).toString();
    }

    private static int charWidth(char c) {
        switch (c) {
            case 'i': case '!': case ',': case '.': case ':': case ';': case '|': case '\'':
                return 1;
            case 'l': case '`':
                return 2;
            case 'I': case 't': case ' ': case '[': case ']': case '"':
                return 3;
            case 'f': case 'k': case '<': case '>': case '(': case ')': case '{': case '}': case '*':
                return 4;
            case '@': case '~':
                return 6;
            default:
                return 5;
        }
    }
}
