package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.lang.Runnable;
import java.lang.Runtime;
import java.lang.Thread;

import java.lang.IllegalStateException;
import java.util.NoSuchElementException;

import java.util.Scanner;

public final class Main {
    // TODO: roll System.in into Console object? Only a single thread
    // reads from System.in, so we don't need a locking semaphore as
    // with System.out...
    public static void main(String[] args) throws Exception {
        Main.registerShutdownHook();

        String configFileName = "server.cfg";
        if(args.length > 0)
            configFileName = args[0];
        Config cfg = Config.instance();
        if (!cfg.init(configFileName)) {
            Console.fatal("Server initialization failed.");
            return;
        }

        Server server = Server.instance(cfg);

        server.spinWelcomeThread();
        Console.info("Type 'quit' or 'exit' to terminate (case-insensitive).");
        Scanner in = new Scanner(System.in);
        while (!Thread.interrupted()) {
            if (!server.welcomeIsAlive()) {
                Console.fatal("Welcome thread terminated unexpectedly.");
                System.exit(-1);
            }
            if (Thread.interrupted()) {
                Console.fatal("Console thread interrupted.");
                System.exit(-1);
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
                System.exit(-1);
            }
        }
        // Reap all other threads when the main (interactive console)
        // thread exits. Otherwise the server keeps running, just
        // without the main thread. Alternatively, we could call
        // System.exit() since the shutdown hook also calls
        // server.shutDown().
        server.shutDown();
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Server.instance().shutDown();
                    }
                }
            )
        );
    }
}
