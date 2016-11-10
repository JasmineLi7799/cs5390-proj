package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.net.SocketException;
import java.io.IOException;

public final class WelcomeSocket {
    private static final int RECV_BUF_SIZE = 1024;

    private InetAddress _serverIP;
    private int _serverPort;
    private DatagramSocket _socket;

    // TODO: read server paramters from a config file, or whatever.
    public WelcomeSocket(InetSocketAddress serverSockAddr)
        throws SocketException {

        _serverIP = serverSockAddr.getAddress();
        _serverPort = serverSockAddr.getPort();
        _socket = new DatagramSocket();
        Console.debug("Opened welcome socket (" + _socket.getLocalPort() + ").");
    }

    public void finalize() {
        this.close();
    }

    public void close() {
        Console.debug("Welcome socket (" + _socket.getLocalPort() + ")"
                      + " closed.");
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

    public DatagramPacket receive() {
        byte[] buf = new byte[RECV_BUF_SIZE];
        DatagramPacket dgram = new DatagramPacket(buf, RECV_BUF_SIZE);
        try {
            _socket.receive(dgram);
        } catch (IOException e) {
            Console.error("Welcome socket: caught IOExcpetion: " + e);
            return null;
        }
        return dgram;
    }
}
