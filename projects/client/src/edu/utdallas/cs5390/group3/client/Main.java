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

import java.net.URLDecoder;

public final class Main {
    private static Client _client = null;
    private static InetSocketAddress _serverSockAddr = null;
    private static String cmd;
    private static int sessionID=0;
    public Scanner input = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        if (!Main.createClient(args)) {
            Console.fatal("Client configuration failed.");
            return;
        }

        Console.info("Client started.");
        Console.info("Type \"log on\" to start."
                     + " Type \"log off\" or \"quit/exit\" to quit.");

        while (!Thread.interrupted()) {
            try {
                String command;
                try {
                    command = Console.nextLine();
                } catch (NoSuchElementException |
                        IllegalStateException e) {
                    Console.fatal("Console caught input exception: " + e);
                    break;
                }

                // "quit/exit" command
                if (command.matches("(?i)^(quit|exit|log off|logoff) *$")) {
                    // valid from any state.
                    break;
                }

                //  "log on" command
                else if (command.matches("(?i)^(log on|logon) *$")) {
                    Main.handleLogOn();
                }

                // "chat" command
                else if (command.matches("(?i)^(chat$|chat .*$)")) {
                    handleConnect(command);
                }

                // "end chat" command
                else if (command.matches("(?i)^end chat *$")) {
                    handleEndChat();
                }

                else if (command.matches("(?i)^(history$|history .*$)")) {
                    handleHistory(command);
                }

                else if (_client.state() == Client.State.ACTIVE_CHAT
                         && command.matches(".*")) {
                    handleChat(command);
                }

                // Any other input.
                else {
                    Console.error("Invalid command: " + command);
                    continue;
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        // Kill all threads in the "client" group when Main exits.
        // (Otherwise the process doesn't quit; you just lose the
        // interactive client console).
        _client.threadGroup().interrupt();
        Console.close();
    }

    public static boolean createClient(String[] args) {
        // This bit of ugliness obtains the directory that the .jar file
        // is located info.
        String path = Main.class.getProtectionDomain().getCodeSource()
            .getLocation().getPath();
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(path, "UTF-8");
        } catch (Exception e) {
            Console.fatal("In createClient: " + e);
            return false;
        }
        String basePath = "";
        int endIndex = decodedPath.lastIndexOf("/");
        if (endIndex != -1)
        {
            basePath = decodedPath.substring(0, endIndex + 1);
        }

        // By default, look for client.cfg in the .jar file's directory
        String configFileName = basePath + "client.cfg";
        // But use the first command line argument to locate the config file
        // if it has been specified.
        if(args.length > 0) {
            configFileName = args[0];
        }
        Console.info("Using config file: " + configFileName);

        // Create the client
        _client = Client.instance();
        // Configure it.
        try {
            _client.configure(configFileName);
        } catch (IllegalStateException | NullPointerException e) {
            Console.fatal("Client configuration failed.");
            return false;
        }
        return true;
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

    private static void handleConnect(String cmd) {
        // Check syntax
        if (!cmd.matches("(?i)^chat [1-9][0-9]*$")) {
            Console.error("Bad \"chat\" syntax."
                          + " Usage: chat <client id>");
            return;
        }

        // Check for valid state (requires an active chat session)
        try {
            if (_client.state() != Client.State.ONLINE) {
                Console.error("You are not online. Try \"log on\".");
                return;
            }
        } catch (InterruptedException e) {
            return;
        }

        // Parse the command
        Scanner cmdScan = new Scanner(cmd).useDelimiter(" ");
        // Skip over "chat" to get to the target client id.
        cmdScan.next();
        // Get the target client id.
        final int clientBId = cmdScan.nextInt();
        if (_client.id() == clientBId) {
            Console.warn("Talking to yourself?");
            return;
        }

        // Spin worker thread to send CONNECT message.
        Thread worker = new Thread(
            _client.threadGroup(),
            new Runnable() {
                public void run() {
                    try {
                        _client.setState(Client.State.WAIT_FOR_CHAT);
                        _client.sessionSock().writeMessage(
                            "CONNECT " + clientBId);
                    } catch (Exception e) {
                        Console.debug("While sending CONNECT: " + e);
                    }
                }
            },
            "CONNECT worker");
        worker.start();
    }

    private static void handleEndChat() {
        // Check for valid state (requires an active chat session)
        try {
            if (_client.state() != Client.State.ACTIVE_CHAT) {
                Console.error("You are not in a chat session.");
                return;
            }
        } catch (InterruptedException e) {
            return;
        }

        // Spin worker thread to send END_REQUEST message.
        Thread worker = new Thread(
            _client.threadGroup(),
            new Runnable() {
                public void run() {
                    try {
                        _client.sessionSock().writeMessage(
                            "END_REQUEST " + _client.chatSessionId());
                    } catch (Exception e) {
                        Console.debug("While sending END_REQUEST: " + e);
                    }
                }
            },
            "END_REQUEST worker");
        worker.start();
    }

    private static void handleChat(String message) {
        // Spin worker thread to send CHAT message.
        Thread worker = new Thread(
            _client.threadGroup(),
            new Runnable() {
                public void run() {
                    try {
                        Console.info("[-> to client " + _client.chatPartnerId() + "] "
                                     + message);
                        _client.sessionSock().writeMessage(
                            "CHAT " + _client.chatSessionId()
                            + " " + message);
                    } catch (Exception e) {
                        Console.debug("While sending CHAT: " + e);
                    }
                }
            },
            "CHAT worker");
        worker.start();
    }

    private static void handleHistory(String command) {
        // Check syntax
        if (!command.matches("(?i)^history [1-9][0-9]*$")) {
            Console.error("Bad \"history\" syntax."
                          + " Usage: history <client id>");
            return;
        }

        // Parse the command
        Scanner scan = new Scanner(command);
        // Skip over "history" to get to the target client id.
        scan.next();
        // Get the target client id.
        final int partnerId = scan.nextInt();

        // Spin worker thread to send HISTORY message.
        Thread worker = new Thread(
            _client.threadGroup(),
            new Runnable() {
                public void run() {
                    try {
                        _client.sessionSock().writeMessage(
                            "HISTORY_REQ " + partnerId);
                    } catch (Exception e) {
                        Console.debug("While sending HISTORY_REQ: " + e);
                    }
                }
            },
            "HISTORY_REQ worker");
        worker.start();
    }

}
