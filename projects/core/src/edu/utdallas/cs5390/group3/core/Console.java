package edu.utdallas.cs5390.group3.core;

import java.util.concurrent.Semaphore;
import java.util.Scanner;

public final class Console {
    private static boolean debugMode = false;
    private static Semaphore _writeLock;
    private static Scanner _in;

    static {
        _writeLock = new Semaphore(1);
        _in = new Scanner(System.in);
    }

    public static void close() {
        _in.close();
    }

    public static String nextLine() {
        return _in.nextLine();
    }

    private static void write(String prefix, String message) {
        try {
            _writeLock.acquire();
            System.out.println(prefix + " " + message);
            _writeLock.release();
        } catch (InterruptedException e) {
            /* There's really nothing we can reasonably do about this
               but to ignore it. */
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
