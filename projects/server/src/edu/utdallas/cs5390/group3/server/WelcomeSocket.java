package edu.utdallas.cs5390.group3.server;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.net.SocketException;
import java.io.IOException;

public final class WelcomeSocket {
    private final static int RECV_BUF_SIZE = 1024;
    private final static int SEND_BUF_SIZE = 1024;

    private DatagramSocket _socket;

    public boolean open() {
        Config cfg = Config.instance();
        InetAddress addr = cfg.bindAddr();
        int port = cfg.bindPort();

        try {
            _socket = new DatagramSocket(port, addr);
        } catch (SocketException e) {
            Console.fatal("Exception while opening welcome port "
                          + addr.getHostAddress() + ":" + port
                          + ": " + e);
            return false;
        }
        Console.info("Listening for connections via UDP on "
                     + addr.getHostAddress() + ":" + port + ".");
        return true;
    }

    public void close() {
        if (!_socket.isClosed()) {
            String addr = _socket.getLocalAddress().getHostAddress();
            int port = _socket.getLocalPort();
            Console.info("Closed UDP port " + addr + ":" + port + ".");
            _socket.close();
        }
    }

    public DatagramPacket receive() {
        byte[] buf = new byte[RECV_BUF_SIZE];
        DatagramPacket dgram = new DatagramPacket(buf, RECV_BUF_SIZE);
        try {
            _socket.receive(dgram);
        } catch (IOException e) {
            Console.error("Welcome port: caught IOExcpetion: " + e);
            return null;
        }
        return dgram;
    }

    public void send(byte[] data, InetAddress addr, int port) throws IOException {
        DatagramPacket dgram
            = new DatagramPacket(data, data.length, addr, port);
        _socket.send(dgram);
    }

    public boolean isClosed() {
        return _socket.isClosed();
    }

}
