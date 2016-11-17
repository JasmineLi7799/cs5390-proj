package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.lang.Runnable;
import java.lang.Runtime;
import java.lang.Thread;

import java.lang.IllegalStateException;
import java.util.NoSuchElementException;

import java.util.Scanner;

/* The Main class implements the server's interactive console and
 * creates the Server object.
 */
public final class Main {

    // TODO: roll System.in into Console object? Only a single thread
    // reads from System.in, so we don't need a locking semaphore as
    // with System.out...
    public static void main(String[] args) throws Exception {
        Main.registerShutdownHook();

        // Initialize Config singleton so that various objects in the
        // system can obtain these parameters without excessive
        // parameter passing.
        String configFileName = "server.cfg";
        if(args.length > 0)
            configFileName = args[0];
        Config cfg = Config.instance();
        if (!cfg.init(configFileName)) {
            Console.fatal("Server initialization failed.");
            return;
        }

        // Create server singleton
        Server server = Server.instance();
        server.start();

        // Console input loop
        Console.info("Type 'quit' or 'exit' to terminate (case-insensitive).");
        Scanner in = new Scanner(System.in);
        while (!Thread.interrupted()) {
            // If the WelcomeThread dies, the server is toast.
            if (!server.welcomeIsAlive()) {
                Console.fatal("Welcome thread terminated unexpectedly.");
                break;
            }

            try {
                String command = in.nextLine();
                if (command.matches("(?i:)^(quit|exit)$")) {
                    break;
                } else {
                    Console.error("Unknown command: '"
                                  + command + "'");
                }
            } catch (NoSuchElementException |
                     IllegalStateException e) {
                Console.fatal("Console caught input exception: " + e);
                break;
            }
        }
        // If we exited the console input loop due to an interrupt,
        // let the user know what happened.
        if (Thread.interrupted()) {
            Console.fatal("Console thread interrupted.");
        }

        // If we don't make this call, the other threads of the server
        // will happily continue running without the main thread
        // (interactive console).
        server.stop();
    }

    /* Whenever and wherever the program exits, we should try to call
     * stop() on the server for a clean exit.
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Server.instance().stop();
                    }
                }
            )
        );
    }
}
