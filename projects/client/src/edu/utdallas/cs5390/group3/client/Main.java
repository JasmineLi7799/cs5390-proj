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

        // This bit of ugliness obtains the directory that the .jar file
        // is located info.
        String path = Main.class.getProtectionDomain().getCodeSource()
            .getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
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
            return;
        }

        Main.registerShutdownHook();

        Console.info("Chat client initialized.");
        Console.info("Type 'log on' to begin, 'quit' or 'exit' to exit.");

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

            else if (command.matches("(?i)^end chat")){
            	handleEndChat(command);
            }

            // CHAT command
            else if (command.matches("(?i)^(chat$|chat .*$)")) {
                handleChat(command);
            }

            else if (command.matches("[\\s\\S]*")){
            	handleChatSession(command);
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
        if (!cmd.matches("(?i)^chat [1-9][0-9]*$")) {
            Console.error("Bad \"chat\" syntax."
                          + " Usage: CHAT <client id>");
            return;
        }

        // Check for valid state (requires an active chat session)
        Client.State state;
        try {
            state = _client.state();
            System.out.println("==================");
            _client.getState();
        } catch (InterruptedException e) {
            return;
        }
        if (state != Client.State.REGISTERED) {
            Console.error("You are not online. Try \"log on \".");
            return;
        }

        // Parse the command
        Scanner cmdScan = new Scanner(cmd).useDelimiter(" ");
        // Skip over "chat" to get to the chat session id.
        cmdScan.next();
        // Get the chat session id
        final int clientBId = cmdScan.nextInt();
        System.out.println("clientBId is " + clientBId);

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
                            "CONNECT " + clientBId);
                        System.out.println("The CONNECT message has sent");

                     // read Start message

                        byte[] revStart = _client.sessionSock().readMessage();
//                        System.out.println(revStart);
                        String startMsg = new String(revStart);
                        String[] msg = startMsg.split("\\s+");
                        sessionID = Integer.parseInt(msg[1]);
                        System.out.println("The session Id is "+ sessionID);
                        if(msg[0].equals(new String("START"))){
                        	System.out.println("The start msg is " + startMsg);
                            System.out.println("------------------------");
                            System.out.println("Chat Started");
                        }else if(msg[0].equals(new String("UNREACHABLE"))){
                        	System.out.println("Correspondent unreachable");
                        }

                    } catch (Exception e) {
                        Console.debug("While sending CHAT: " + e);
                    }
                }
            },
            "CHAT worker");
        worker.start();
    }


    private static void handleChatSession(final String cmd){

    	 // Check for valid state (requires an active chat session)
        Client.State state;
        try {
            state = _client.state();
            System.out.println("==================");
            _client.getState();
        } catch (InterruptedException e) {
            return;
        }
        if (state != Client.State.REGISTERED) {
            Console.error("You are not online. Try \"log on \".");
            return;
        }

        // Parse the command
//        Scanner cmdScan = new Scanner(cmd).useDelimiter(" ");
        // Skip over "chat" to get to the chat session id.
//        cmdScan.next();
        // Get the chat session id
//        final int clientBId = cmdScan.nextInt();
//        System.out.println("clientBId is " + clientBId);

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
                    	String chatCotent = "CHAT " + sessionID + " " + cmd;
                        _client.sessionSock().writeMessage(chatCotent);
                        System.out.println("Chat msg is "+ chatCotent);
                        System.out.println("The Chat content is "+ cmd);

                    } catch (Exception e) {
                        Console.debug("While sending CHAT: " + e);
                    }
                }
            },
            "CHAT worker");
        worker.start();
    }


    private static void handleEndChat(String cmd){
//    	Client.State state;
//        try {
//            state = _client.state();
//            System.out.println("==================");
//            _client.getState();
//        } catch (InterruptedException e) {
//            return;
//        }
//        if (state != Client.State.REGISTERED) {
//            Console.error("You are not online. Try \"log on \".");
//            return;
//        }
    	System.out.println("Client want to end chat session");
        // Parse the command
//        Scanner cmdScan = new Scanner(cmd).useDelimiter(" ");
        // Skip over "chat" to get to the chat session id.
//        cmdScan.next();
        // Get the chat session id
//        final int clientBId = cmdScan.nextInt();
//        System.out.println("clientBId is " + clientBId);

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
                    	String chatCotent = "END_REQUEST " + sessionID;
                        _client.sessionSock().writeMessage(chatCotent);
                        System.out.println("END_REQUEST has sent to server");

                        byte[] endNotif = _client.sessionSock().readMessage();
                        String notif = new String(endNotif);
                        System.out.println("the end notification is "+ notif);
                        System.out.println("Chat ended");
                    } catch (Exception e) {
                        Console.debug("While sending CHAT: " + e);
                    }
                }
            },
            "CHAT worker");
        worker.start();
    }

    public String getCommand() throws Exception{
        String command = input.nextLine();
    	return command;
    }

}
