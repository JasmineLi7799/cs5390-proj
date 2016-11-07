package edu.utdallas.cs5390.group3.server;

import java.lang.Runnable;
import java.lang.Runtime;
import java.lang.Thread;

import java.lang.IllegalStateException;
import java.util.NoSuchElementException;

import java.util.Scanner;


public final class Main {
    private static Thread shutdownHandler() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Server.instance().shutDown();
            }
        });
    }

    // TODO: roll System.in into Console object? Only a single thread
    // reads from System.in, so we don't need a locking semaphore as
    // with System.out...
    public static void main(String[] args) throws Exception {
        Runtime.getRuntime().addShutdownHook(shutdownHandler());
        Server server = Server.instance();
        server.spinWelcomeThread();
        Console.info("Server welcome thread running.");
        Console.info("Type 'quit' to terminate.");
        Scanner in = new Scanner(System.in);
        boolean keepRunning = true;
        while (keepRunning) {
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
                if (command.equals("quit")) {
                    keepRunning = false;
                } else {
                    Console.error("Unknown command: '" + command + "'");
                }
            } catch (NoSuchElementException |
                     IllegalStateException e) {
                Console.fatal("Console caught input exception: " + e);
                System.exit(-1);
            }
        }
        // Note: this call is necessary. Java programs do not
        // terminate until all non-daemon threads have
        // exited. Reaching the end of main() merely kills the main
        // thread (interactive console). We have to tell the various
        // other threads to die in order to terminate the server
        // process.
        server.shutDown();
    }
}
