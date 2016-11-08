package edu.utdallas.cs5390.group3.client;

import java.lang.Runtime;

import java.lang.RuntimeException;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.DatagramPacket;

public final class Main {

    private static enum State {
        START,
        HELLO_SENT,
        CHALLENGE_RECV,
        RESPONSE_SENT,
        AUTHENTICATED,
        REGISTERED
        // ...
    }

    private static BufferedReader _in;
    private static Client _client;
    private static WelcomeSocket _welcome;
    private static State _state = State.START;

    public static void main(String[] args) {
        Main.registerShutdownHook();
        _client = new Client();
        Console.info("Chat client initialized.");
        Console.info("Type 'log on' to begin, 'quit' or 'exit' to exit (case-insensitive).");

        _in = new BufferedReader(new InputStreamReader(System.in));
        while (!Thread.interrupted()) {
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

            // QUIT/EXIT command
            if (input.matches("(?i)^(quit|exit)$")) {
                return;
            }

            // LOG ON command
            else if (input.matches("(?i)^log on")) {
                if (_state != State.START) {
                    Console.error("You are already logged on, or a login is in progress");
                    continue;
                }
                Main.login();
            }

            // Any other inputStreamReader.
            else {
                Console.error("Invalid command: " + input);
                continue;
            }
        }
    }

    private static void login() {
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
        _state = State.HELLO_SENT;

        // receive CHALLENGE
        DatagramPacket challenge = _welcome.receive();
        if (challenge == null) {
            System.exit(-1);
        }
        Console.debug("Got CHALLENGE from server.");

        _state = State.CHALLENGE_RECV;

        // TODO: complete handshake
    }

    // TODO: state-specific cleanup tasks?
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
