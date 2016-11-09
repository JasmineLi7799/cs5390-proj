package edu.utdallas.cs5390.group3.client;

import java.lang.Runtime;
import java.lang.ThreadGroup;

import java.lang.IllegalStateException;
import java.util.NoSuchElementException;
import java.io.IOException;

import java.util.Properties;

import java.util.Scanner;
import java.io.FileInputStream;
import java.io.InputStream;

public final class Main {
    private static Client _client;

    public static void main(String[] args) throws Exception {
        Main.registerShutdownHook();

        Properties config = Main.loadConfig();
        _client = Main.initClient(config);
        if (_client == null) {
            Console.fatal("Could not configure client.");
            return;
        }

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

    private static Properties loadConfig() {
        InputStream configFile = null;
        Properties config = new Properties();
        try {
            configFile = new FileInputStream("client-config.properties");
            config.load(configFile);
        } catch (IOException e) {
            Console.fatal("IOException while parsing config file: " + e);
            return null;
        }
        if (configFile != null) {
            try {
                configFile.close();
            } catch (IOException e) {
                Console.warn("IOException while closing config file: " + e);
            }
        }
        return config;
    }

    private static Client initClient(Properties config) {
        // user id parsing
        int id;
        String userIdString = config.getProperty("user_id");
        if (userIdString == null) {
            Console.fatal("Missing required 'user_id' property "
                          + "in 'client-config.properties'");
            return null;
        } else if (userIdString.matches("^[1-9][0-9]*$")) {
            id = Integer.parseInt(userIdString);
        } else {
            Console.fatal("Malformed 'user_id' property "
                          + "in 'client-config.properties': "
                          + userIdString);
            return null;
        }

        // privateKey parsing (it just can't be an empty string/null)
        String privateKey = config.getProperty("private_key");
        if (privateKey == null || privateKey.equals("")) {
            Console.fatal("Missing required 'private_key' property "
                          + "in 'client-config.properties'");
            return null;
        }
        return new Client(id, privateKey);
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
