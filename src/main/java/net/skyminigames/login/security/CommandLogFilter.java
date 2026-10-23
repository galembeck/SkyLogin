package net.skyminigames.login.security;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.Locale;
import java.util.Set;

/**
 * Hides "Player issued server command: /login password" lines from the console and log files.
 */
public final class CommandLogFilter extends AbstractFilter {

    private static final String COMMAND_MARKER = " issued server command: /";

    private final Set<String> hiddenCommands;
    /** Log4j filters can't be removed, so the filter switches itself off when the plugin is disabled. */
    private volatile boolean active = true;

    private CommandLogFilter(Set<String> hiddenCommands) {
        this.hiddenCommands = hiddenCommands;
    }

    public static CommandLogFilter install(Set<String> hiddenCommands) {
        CommandLogFilter filter = new CommandLogFilter(hiddenCommands);
        ((Logger) LogManager.getRootLogger()).addFilter(filter);
        return filter;
    }

    public void deactivate() {
        active = false;
    }

    private Result check(String message) {
        if (!active || message == null) {
            return Result.NEUTRAL;
        }
        int index = message.indexOf(COMMAND_MARKER);
        if (index < 0) {
            return Result.NEUTRAL;
        }
        String command = message.substring(index + COMMAND_MARKER.length()).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int namespace = command.indexOf(':');
        if (namespace >= 0) {
            command = command.substring(namespace + 1);
        }
        return hiddenCommands.contains(command) ? Result.DENY : Result.NEUTRAL;
    }

    @Override
    public Result filter(LogEvent event) {
        return event == null || event.getMessage() == null ? Result.NEUTRAL : check(event.getMessage().getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message message, Throwable throwable) {
        return message == null ? Result.NEUTRAL : check(message.getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object... params) {
        return check(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object message, Throwable throwable) {
        return message == null ? Result.NEUTRAL : check(message.toString());
    }
}
