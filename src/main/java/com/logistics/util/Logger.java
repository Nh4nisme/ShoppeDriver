package com.logistics.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized logging utility with Vietnamese messages
 */
public class Logger {
    public enum Level {
        INFO, ERROR, DEBUG
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log message with timestamp and module
     * @param module the module name
     * @param message the message to log
     */
    public static void log(String module, String message) {
        log(Level.INFO, module, message);
    }

    /**
     * Log message with level, timestamp and module
     * @param level the log level
     * @param module the module name
     * @param message the message to log
     */
    public static void log(Level level, String module, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formattedMessage = String.format("[%s] [%s] [%s] - %s",
                timestamp, module, level.name(), message);

        System.out.println(formattedMessage);

        // TODO: Also send to UI log panel when available
    }

    /**
     * Log error message
     * @param module the module name
     * @param message the error message
     */
    public static void error(String module, String message) {
        log(Level.ERROR, module, message);
    }

    /**
     * Log debug message
     * @param module the module name
     * @param message the debug message
     */
    public static void debug(String module, String message) {
        log(Level.DEBUG, module, message);
    }
}
