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
        log(Level.INFO, module, removeVietnameseDiacritics(message));
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
                timestamp, module, level.name(), removeVietnameseDiacritics(message));

        System.out.println(formattedMessage);

        // TODO: Also send to UI log panel when available
    }

    /**
     * Log error message
     * @param module the module name
     * @param message the error message
     */
    public static void error(String module, String message) {
        log(Level.ERROR, module, removeVietnameseDiacritics(message));
    }

    /**
     * Log debug message
     * @param module the module name
     * @param message the debug message
     */
    public static void debug(String module, String message) {
        log(Level.DEBUG, module, removeVietnameseDiacritics(message));
    }

    /**
     * Remove Vietnamese diacritics from a string
     */
    private static String removeVietnameseDiacritics(String str) {
        if (str == null) return null;
        String[][] DIACRITICS = {
            {"a", "á", "à", "ả", "ã", "ạ", "ă", "ắ", "ằ", "ẳ", "ẵ", "ặ", "â", "ấ", "ầ", "ẩ", "ẫ", "ậ"},
            {"A", "Á", "À", "Ả", "Ã", "Ạ", "Ă", "Ắ", "Ằ", "Ẳ", "Ẵ", "Ặ", "Â", "Ấ", "Ầ", "Ẩ", "Ẫ", "Ậ"},
            {"d", "đ"},
            {"D", "Đ"},
            {"e", "é", "è", "ẻ", "ẽ", "ẹ", "ê", "ế", "ề", "ể", "ễ", "ệ"},
            {"E", "É", "È", "Ẻ", "Ẽ", "Ẹ", "Ê", "Ế", "Ề", "Ể", "Ễ", "Ệ"},
            {"i", "í", "ì", "ỉ", "ĩ", "ị"},
            {"I", "Í", "Ì", "Ỉ", "Ĩ", "Ị"},
            {"o", "ó", "ò", "ỏ", "õ", "ọ", "ô", "ố", "ồ", "ổ", "ỗ", "ộ", "ơ", "ớ", "ờ", "ở", "ỡ", "ợ"},
            {"O", "Ó", "Ò", "Ỏ", "Õ", "Ọ", "Ô", "Ố", "Ồ", "Ổ", "Ỗ", "Ộ", "Ơ", "Ớ", "Ờ", "Ở", "Ỡ", "Ợ"},
            {"u", "ú", "ù", "ủ", "ũ", "ụ", "ư", "ứ", "ừ", "ử", "ữ", "ự"},
            {"U", "Ú", "Ù", "Ủ", "Ũ", "Ụ", "Ư", "Ứ", "Ừ", "Ử", "Ữ", "Ự"},
            {"y", "ý", "ỳ", "ỷ", "ỹ", "ỵ"},
            {"Y", "Ý", "Ỳ", "Ỷ", "Ỹ", "Ỵ"}
        };
        for (String[] chars : DIACRITICS) {
            String rep = chars[0];
            for (int i = 1; i < chars.length; i++) {
                str = str.replace(chars[i], rep);
            }
        }
        return str;
    }
}
