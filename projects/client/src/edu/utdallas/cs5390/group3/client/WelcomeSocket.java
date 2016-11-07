package edu.utdallas.cs5390.group3.client;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import java.lang.RuntimeException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;

public final class WelcomeSocket {
    private InetAddress _serverIP;
    private int _serverPort;
    private DatagramSocket _socket;

    // TODO: read server paramters from a config file, or whatever.
    public WelcomeSocket() throws RuntimeException {
        try {
            _serverIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        try {
            _socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        _serverPort = 9876;
    }

    public void finalize() {
        this.close();
    }

    public void close() {
        if (!_socket.isClosed()) {
            _socket.close();
        }
    }

    public boolean send(String message) {
        byte[] data = new byte[message.length()];
        data = message.getBytes();
        DatagramPacket dgram = new DatagramPacket(
            data,
            data.length,
            _serverIP,
            _serverPort
        );
        try {
            _socket.send(dgram);
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e);
            return false;
        }
        return true;
    }
}
