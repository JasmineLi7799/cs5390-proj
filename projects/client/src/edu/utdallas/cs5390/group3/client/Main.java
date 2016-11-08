package edu.utdallas.cs5390.group3.client;

import java.lang.Runtime;

import java.lang.RuntimeException;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.DatagramPacket;

public final class Main {

    private static BufferedReader _in;
    private static Client _client;
    private static WelcomeSocket _welcome;

    public static void main(String[] args) {
        Main.registerShutdownHook();
        _client = new Client();
        Console.info("Chat client initialized.");
        Console.info("Type 'log on' to begin, or 'quit' to exit (case-insensitive).");

        _in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String input;
            try {
                input = _in.readLine();
            } catch (IOException e) {
                Console.error("Caught: " + e);
                System.exit(-1);
                // Superfluous, but needed to satisfy the compiler that
                // we aren't using String without initialziaton;
                return;
            }
            if (input.matches("(?i)^quit$")) {
                return;
            }
            if (input.matches("(?i)^log on")) {
                try {
                    _in.close();
                } catch (IOException e) {
                    Console.error("While closing input stream: "
                                  + "Caught: " + e);
                    break;
                }
                break;
            } else {
                Console.error("Invalid command: " + input);
            }
        }

        try {
            _welcome = new WelcomeSocket();
        } catch (RuntimeException e) {
            Console.fatal("Caught excpetion: " + e);
            System.exit(-1);
            // Superfluous, but needed to satisfy the compiler that
            // we aren't using WelcomeSocket without initialziaton;
            return;
        }

        // send HELLO
        _welcome.send(_client.hello());

        // receive CHALLENGE
        DatagramPacket challenge = _welcome.receive();
        if (challenge == null) {
            System.exit(-1);
        }
        Console.debug("Got CHALLENGE from server.");

        // TODO: complete handshake
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (_in != null) {
                    try {
                        _in.close();
                    } catch (IOException e) {
                        Console.error("While closing input stream: "
                                  + "Caught: " + e);
                    }
                }
                if (_welcome != null) {
                    _welcome.close();
                }
            }
        }));
    }
}
