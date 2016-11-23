package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.lang.Runtime;
import java.lang.ThreadGroup;

import java.lang.IllegalStateException;
import java.net.SocketException;
import java.util.NoSuchElementException;

import java.util.Scanner;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class Main {
    private static Client _client = null;
    private static InetSocketAddress _serverSockAddr = null;

    public static void main(String[] args) throws Exception {

        // Create and configure Client
        String configFileName = "client.cfg";
        if(args.length > 0) {
            configFileName = args[0];
        }
        _client = Client.instance();
        try {
            _client.configure(configFileName);
        } catch (IllegalStateException | NullPointerException e) {
            Console.fatal("Client configuration failed.");
            return;
        }

        Main.registerShutdownHook();

        Console.info("Chat client initialized.");
        Console.info("Type 'log on' to begin, 'quit' or 'exit' "
                     + "to exit (case-insensitive).");

        while (!Thread.interrupted()) {
            String command;
            try {
                command = Console.nextLine();
            } catch (NoSuchElementException |
                     IllegalStateException e) {
                Console.fatal("Console caught input exception: " + e);
                break;
            }

            // QUIT/EXIT command
            if (command.matches("(?i)^(quit|exit)$")) {
                // valid from any state.
                break;
            }

            // LOG ON command
            else if (command.matches("(?i)^log on")) {
                handleLogOn();
            }

            // CHAT command
            else if (command.matches("(?i)^(chat$|chat .*$)")) {
                handleChat(command);
            }

            // Any other input.
            else {
                Console.error("Invalid command: " + command);
                continue;
            }
        }

        // Kill all threads in the "client" group when Main exits.
        // (Otherwise the process doesn't quit; you just lose the
        // interactive client console).
        _client.threadGroup().interrupt();
        Console.close();
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                // Kill all threads in the "client" group.
                Client.instance().threadGroup().interrupt();
            }
        }));
    }

    /* Handles "log on" commands from the user.*/
    private static void handleLogOn() {
        // valid only from OFFLINE state
        Client.State state;
        try {
            state = _client.state();
        } catch (InterruptedException e) {
            return;
        }
        if (state != Client.State.OFFLINE) {
            Console.error("You are already logged on, or a "
                            + "login is already in progress");
            return;
        }
        try {
            HandshakeThread handshake =
                new HandshakeThread();
            handshake.start();
        } catch (SocketException e) {
            Console.error("Could not initiate login: " + e);
        }
    }

    private static void handleChat(String cmd) {
        // Check syntax
        if (!cmd.matches("(?i)^chat [1-9][0-9]* \\S.*$")) {
            Console.error("Bad \"chat\" syntax."
                          + " Usage: CHAT <session id> <message>");
            return;
        }

        // Check for valid state (requires an active chat session)
        Client.State state;
        try {
            state = _client.state();
        } catch (InterruptedException e) {
            return;
        }
        if (state != Client.State.ACTIVE_CHAT) {
            Console.error("You do not have a chat session yet."
                          + " Try \"CONNECT <user id>\".");
            return;
        }

        // Parse the command
        Scanner cmdScan = new Scanner(cmd);
        // Skip over "chat" to get to the chat session id.
        cmdScan.next();
        // Get the chat session id
        int chatId = cmdScan.nextInt();
        // Get the chat text.
        String chatText = cmdScan.next();

        // Generate and send the CHAT message in a separate thread so
        // that the console can immediately accept more input from
        // the user without waiting for the network IO to complete.
        //
        // The thread belongs to _client.threadGroup() and is named
        // "CHAT worker". All threads need to belong to this ThreadGroup
        // so that we can kill any outstanding threads when the console
        // exits.
        Thread worker = new Thread(
            _client.threadGroup(),
            new Runnable() {
                public void run() {
                    try {
                        _client.sessionSock().writeMessage(
                            "CHAT " + chatId + " " + chatText);
                    } catch (Exception e) {
                        Console.debug("While sending CHAT: " + e);
                    }
                }
            },
            "CHAT worker");
        worker.start();
    }

}
