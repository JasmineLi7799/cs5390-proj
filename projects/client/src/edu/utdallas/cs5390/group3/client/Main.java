package edu.utdallas.cs5390.group3.client;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;

public final class Main {
    // TODO: read this info from a config file, or command line arguments, or whatever
    private static final int CLIENT_ID = 1;
    private static final String PRIV_KEY = new String("foo");
    private static final int SERVER_WELCOME_PORT = 9876;

    public static void main(String[] args) {
        InetAddress SERVER_IP;
        try {
            SERVER_IP = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.out.println("Caught UnknownHostException: " + e);
            System.exit(-1);
            // Superfluous, but needed to satisfy the compiler.
            return;
        }
        DatagramSocket _socket;
        try {
            _socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("Caught SocketException: " + e);
            System.exit(-1);
            // Superfluous, but needed to satisfy the compiler.
            return;
        }
        String hello = "HELLO " + CLIENT_ID;
        byte[] data = new byte[hello.length()];
        data = hello.getBytes();
        DatagramPacket dgram = new DatagramPacket(
            data,
            data.length,
            SERVER_IP,
            SERVER_WELCOME_PORT
        );
        try {
            _socket.send(dgram);
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e);
        }
        _socket.close();
    }
}
