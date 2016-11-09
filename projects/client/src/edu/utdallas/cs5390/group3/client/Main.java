package edu.utdallas.cs5390.group3.client;

import java.lang.Runtime;
import java.lang.ThreadGroup;

import java.lang.IllegalStateException;
import java.util.NoSuchElementException;

import java.util.Scanner;

public final class Main {
    private static Client _client;

    public static void main(String[] args) throws Exception {
        Main.registerShutdownHook();
        _client = new Client(Integer.parseInt(args[0]));
        Console.info("Chat client initialized.");
        Console.info("Type 'log on' to begin, 'quit' or 'exit' to exit (case-insensitive).");

        Scanner in = new Scanner(System.in);
        while (!Thread.interrupted()) {
            String command;
            try {
                command = in.nextLine();
            } catch (NoSuchElementException |
                     IllegalStateException e) {
                Console.fatal("Console caught input exception: " + e);
                break;
            }

            // QUIT/EXIT command
            if (command.matches("(?i)^(quit|exit)$")) {
                break;
            }

            // LOG ON command
            else if (command.matches("(?i)^log on")) {
                Client.State state;
                try {
                    state = _client.state();
                } catch (InterruptedException e) {
                    break;
                }
                if (state != Client.State.START) {
                    Console.error("You are already logged on, or a "
                                  + "login is already in progress");
                    continue;
                }
                Console.info("Initiated login. Waiting for response...");
                new HandshakeThread(_client).start();
            }

            // Any other input.
            else {
                Console.error("Invalid command: " + command);
                continue;
            }
        }

        in.close();
        // Kill all threads in main()'s ThreadGroup when
        // main() exits.
        Thread.currentThread().getThreadGroup().interrupt();
    }


    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO: state-specific cleanup tasks?

                // Kill all threads in main()'s ThreadGroup.
                Thread.currentThread().
                    getThreadGroup().interrupt();

            }
        }));
    }

}
