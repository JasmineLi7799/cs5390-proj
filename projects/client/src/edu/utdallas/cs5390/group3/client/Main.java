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

import java.util.Scanner;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class Main {
    private static final int DEFAULT_SERVER_PORT = 9876;
    private static final boolean DEFAULT_DEBUG_MODE = false;
    private static Client _client;
    private static InetSocketAddress _serverSockAddr;

    public static void main(String[] args) throws Exception {
        Main.registerShutdownHook();

        String configFileName = "client.cfg";
        if(args.length > 0)
            configFileName = args[0];
        if (!Main.configure(configFileName)) {
            Console.fatal("Client initialization failed.");
            return;
        }

        Console.info("Chat client initialized.");
        Console.info("Type 'log on' to begin, 'quit' or 'exit' "
                     + "to exit (case-insensitive).");

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
                // valid from any state.
                break;
            }

            // LOG ON command
            else if (command.matches("(?i)^log on")) {
                // valid only from START state
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
                try {
                    HandshakeThread handshake =
                        new HandshakeThread(_client, _serverSockAddr);
                    Console.info("Initiated login. Waiting for response...");
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

        in.close();
        // Kill all threads in main()'s ThreadGroup when
        // main() exits.
        Thread.currentThread().getThreadGroup().interrupt();
    }

    private static boolean configure(String configFileName) {
        Properties config = Main.loadConfig(configFileName);
        if (config == null) {
            Console.fatal("Could not load configuration file.");
            return false;
        }
        _client = Main.loadClientConfig(config, configFileName);
        _serverSockAddr = Main.loadServerConfig(config,
                                                configFileName);
        if(loadDebugConfig(config, configFileName))
            Console.enterDebugMode();
        if (_client == null || _serverSockAddr == null) {
            return false;
        }
        return true;
    }

    private static Properties loadConfig(String configFileName) {
        InputStream configFile = null;
        Properties config = new Properties();
        try {
            configFile = new FileInputStream(configFileName);
            config.load(configFile);
        } catch (IOException e) {
            Console.fatal("IOException while parsing "
                          + "'" + configFileName + "': " + e);
            return null;
        }
        if (configFile != null) {
            try {
                configFile.close();
            } catch (IOException e) {
                Console.warn("IOException while closing "
                            + "'" + configFileName + "': " + e);
            }
        }
        return config;
    }

    private static Client loadClientConfig(Properties config,
                                     String configFileName) {
        // validate 'user_id' format
        int id;
        String userIdString = config.getProperty("user_id");
        if (userIdString == null) {
            Console.fatal("Missing required 'user_id' property in "
                          + "'" + configFileName + "'.");
            return null;
        } else if (userIdString.matches("^[1-9][0-9]*$")) {
            id = Integer.parseInt(userIdString);
        } else {
            Console.fatal("Malformed 'user_id' property in "
                          + "'" + configFileName + "': "
                          + userIdString);
            return null;
        }

        // validate 'private_key' format
        String privateKey = config.getProperty("private_key");
        if (privateKey == null || privateKey.equals("")) {
            Console.fatal("Missing required 'private_key' property in "
                          + "'" + configFileName + "'");
            return null;
        }
        return new Client(id, privateKey);
    }

    private static InetSocketAddress loadServerConfig(
        Properties config, String configFileName) {

        // validate 'server' format
        String serverString = config.getProperty("server");
        InetAddress serverAddr;
        if (serverString == null) {
            Console.fatal("Missing required 'server' property in "
                          + "'" + configFileName + "'.");
            return null;
        }
        try {
            serverAddr = InetAddress.getByName(serverString);
        } catch (UnknownHostException e) {
            Console.fatal("Could not resolve 'server' property in "
                          + "'" + configFileName + "' to a valid host: "
                          + serverString);
            return null;
        }

        // validate 'server_port' format
        String serverPortString = config.getProperty("server_port");
        int serverPort;
        if (serverPortString == null) {
            serverPort = Main.DEFAULT_SERVER_PORT;
        } else if (serverPortString.matches("^[0-9]+$")) {
            serverPort = Integer.parseInt(serverPortString);
            if (serverPort < 0 || serverPort > 65535) {
                Console.fatal("Specified 'server_port' property in "
                              + "'" + configFileName + "' is out-of-range: '"
                              + serverPort + "' (must be 0-65535)");
                return null;
            }
        } else {
            Console.fatal("Malformed 'server_port' property in "
                          + "'" + configFileName + "': '"
                          + serverPortString + "' (must be 0-65535)");
            return null;
        }

        return new InetSocketAddress(serverAddr, serverPort);
    }

    private static boolean loadDebugConfig(
        Properties config, String configFileName) {
        
        String debugProp = config.getProperty("debug");

        // Default value if ommitted.
        if (debugProp == null) {
            return DEFAULT_DEBUG_MODE;
        }

        if (debugProp.equalsIgnoreCase("true"))
            return true;
        else if (debugProp.equalsIgnoreCase("false"))
            return false;
        else {
            Console.fatal("Specified 'debug' property in "
                            + "'" + configFileName + "' is invalid: '"
                            + " Must be 'true' or 'false'");
            throw new NullPointerException();
        }
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
