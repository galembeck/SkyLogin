package net.skyminigames.login.message;

import lombok.Value;

/**
 * A title + subtitle shown on the player's screen. Times are in ticks.
 */
@Value
public class TitleMessage {
    String title;
    String subtitle;
    int fadeIn;
    int stay;
    int fadeOut;
}
