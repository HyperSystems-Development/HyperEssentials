package com.hyperessentials.util;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Wrapped logger with HyperEssentials prefix and formatting.
 */
public final class Logger {

    private static final String PREFIX = "[HyperEssentials] ";
    private static java.util.logging.Logger logger;

    private Logger() {}

    public static void init(@NotNull java.util.logging.Logger parentLogger) {
        logger = parentLogger;
    }

    public static void info(@NotNull String message) {
        if (logger != null) {
            logger.info(PREFIX + message);
        } else {
            System.out.println(PREFIX + "[INFO] " + message);
        }
    }

    public static void info(@NotNull String message, Object... args) {
        info(String.format(message, args));
    }

    public static void warn(@NotNull String message) {
        if (logger != null) {
            logger.warning(PREFIX + message);
        } else {
            System.out.println(PREFIX + "[WARN] " + message);
        }
    }

    public static void warn(@NotNull String message, Object... args) {
        warn(String.format(message, args));
    }

    public static void severe(@NotNull String message) {
        if (logger != null) {
            logger.severe(PREFIX + message);
        } else {
            System.err.println(PREFIX + "[SEVERE] " + message);
        }
    }

    public static void severe(@NotNull String message, Object... args) {
        severe(String.format(message, args));
    }

    public static void severe(@NotNull String message, @NotNull Throwable throwable, Object... args) {
        String formatted = String.format(message, args);
        if (logger != null) {
            logger.log(Level.SEVERE, PREFIX + formatted, throwable);
        } else {
            System.err.println(PREFIX + "[SEVERE] " + formatted);
            throwable.printStackTrace();
        }
    }

    public static void debug(@NotNull String message) {
        if (logger != null) {
            logger.fine(PREFIX + "[DEBUG] " + message);
        }
    }

    public static void debug(@NotNull String message, Object... args) {
        debug(String.format(message, args));
    }
}
