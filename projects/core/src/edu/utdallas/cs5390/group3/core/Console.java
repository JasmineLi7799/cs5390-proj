package edu.utdallas.cs5390.group3.core;

import java.util.concurrent.Semaphore;
import java.util.Scanner;

public final class Console {
    private static boolean debugMode = false;
    private static Scanner _in;

    static {
        _in = new Scanner(System.in);
    }

    public static void close() {
        _in.close();
    }

    public static String nextLine() {
        return _in.nextLine();
    }

    public static void clientPrompt(){
        Console.info("Type 'log on' to begin, 'quit' or 'exit' "
                     + "to exit, or 'chat <id>' to talk with another client (case-insensitive).");
    }

    private static void write(String prefix, String message) {
        synchronized(System.out) {
            System.out.println(prefix + " " + message);
        }
    }

    // Normal server messages.
    public static void info(String message) {
        write("[INFO]", message);
    }

    // Debug output.
    public static void debug(String message) {
        if(debugMode)
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

    public static void enterDebugMode(){
        debugMode = true;
        debug("Entering Debug Mode");
    }
}
