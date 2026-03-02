package com.notification.consumer.logging;

import org.slf4j.LoggerFactory;

public final class Logger {

    private Logger() {
    }

    private static org.slf4j.Logger get(Class<?> source) {
        return LoggerFactory.getLogger(source);
    }

    public static void logInfo(Class<?> source, String message, Object... args) {
        get(source).info(message, args);
    }

    public static void logWarn(Class<?> source, String message, Object... args) {
        get(source).warn(message, args);
    }

    public static void logDebug(Class<?> source, String message, Object... args) {
        get(source).debug(message, args);
    }

    public static void logError(Class<?> source, String message, Object... args) {
        get(source).error(message, args);
    }

    public static void logError(Class<?> source, String message, Throwable throwable) {
        get(source).error(message, throwable);
    }
}
