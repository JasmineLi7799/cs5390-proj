package edu.utdallas.cs5390.group3.server;

import java.net.DatagramSocket;
import java.net.DatagramPacket;

import java.net.SocketException;
import java.io.IOException;

public final class WelcomeSocket {
    private final static int RECV_BUF_SIZE = 1024;
    private final static int SEND_BUF_SIZE = 1024;
    // TODO: read this from a config file or something.
    private final static int PORT_NO = 9876;

    private DatagramSocket _socket;

    public boolean open() {
        try {
            _socket = new DatagramSocket(PORT_NO);
        } catch (SocketException e) {
            Console.fatal("Exception while opening welcome port "
                          + PORT_NO + ": " + e);
            return false;
        }
        Console.info("Listening on UDP port " + PORT_NO);
        return true;
    }

    public void close() {
        _socket.close();
        Console.info("Closed UDP port " + PORT_NO);
    }

    public DatagramPacket receive() {
        byte[] buf = new byte[RECV_BUF_SIZE];
        DatagramPacket dgram = new DatagramPacket(buf, RECV_BUF_SIZE);
        try {
            _socket.receive(dgram);
        } catch (IOException e) {
            Console.error("Welcome thread: caught IOExcpetion: " + e);
            return null;
        }
        return dgram;
    }

    public boolean isClosed() {
        return _socket.isClosed();
    }

}
