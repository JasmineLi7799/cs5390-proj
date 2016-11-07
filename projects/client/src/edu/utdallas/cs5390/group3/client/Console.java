package edu.utdallas.cs5390.group3.client;

public final class Console {
    private static void write(String prefix, String message) {
        System.out.println(prefix + " " + message);
    }

    // Normal server messages.
    public static void info(String message) {
        write("[INFO]", message);
    }

    // Debug output.
    // TODO: debug mode toggle to globally squelch debug output.
    public static void debug(String message) {
        write("[DEBUG]", message);
    }

    // For things that are not quit an error, but may indicate weird behavior.
    public static void warn(String message) {
        write("[WARN]", message);
    }

    // Recoverable errors
    public static void error(String message) {
        write("[ERROR]", message);
    }

    // Non-recoverable errors
    public static void fatal(String message) {
        write("[FATAL]", message);
    }
}
