package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.lang.Runtime;
import java.lang.ThreadGroup;

import java.io.IOException;
import java.lang.IllegalStateException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;

import java.util.Properties;

import java.io.FileInputStream;
import java.io.InputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class Main {
    private static Client _client = null;
    private static InetSocketAddress _serverSockAddr = null;

    public static void main(String[] args) throws Exception {
        Main.registerShutdownHook();

        String configFileName = "client.cfg";
        if(args.length > 0) {
            configFileName = args[0];
        }
        try {
            _client = new Client(new Config(configFileName));
        } catch (NullPointerException e) {
            Console.fatal("Client configuration failed.");
            return;
        }

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
                // valid only from OFFLINE state
                Client.State state;
                try {
                    state = _client.state();
                } catch (InterruptedException e) {
                    break;
                }
                if (state != Client.State.OFFLINE) {
                    Console.error("You are already logged on, or a "
                                  + "login is already in progress");
                    continue;
                }
                try {
                    HandshakeThread handshake =
                        new HandshakeThread(_client);
                    handshake.start();
                } catch (SocketException e) {
                    Console.error("Could not initiate login: " + e);
                }
            }

            // Any other input.
            else {
                Console.error("Invalid command: " + command);
                continue;
            }
        }

        // Kill all threads in the "client" group.
        _client.threadGroup().interrupt();
        Console.close();
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                // Kill all threads in the "client" group.
                _client.threadGroup().interrupt();
            }
        }));
    }

}
