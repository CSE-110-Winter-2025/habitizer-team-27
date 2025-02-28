package edu.ucsd.cse110.habitizer.lib.util;

/**
 * Simple logger utility that works in non-Android modules
 */
public class Logger {
    public static void d(String tag, String message) {
        System.out.println("[DEBUG] " + tag + ": " + message);
    }

    public static void i(String tag, String message) {
        System.out.println("[INFO] " + tag + ": " + message);
    }

    public static void w(String tag, String message) {
        System.out.println("[WARN] " + tag + ": " + message);
    }

    public static void e(String tag, String message) {
        System.err.println("[ERROR] " + tag + ": " + message);
    }

    public static void e(String tag, String message, Throwable e) {
        System.err.println("[ERROR] " + tag + ": " + message);
        e.printStackTrace();
    }
} 