package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.net.SocketException;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

public final class HandshakeSocket {
    private static final int RECV_BUF_SIZE = 1024;

    private DatagramSocket _socket;

    private Client _client;

    public HandshakeSocket() throws SocketException {
        _client = Client.instance();
        // port 0 = emphemeral port
        _socket = new DatagramSocket(0, _client.config.clientAddr());
        Console.debug("Opened handshake socket ("
                      + _socket.getLocalPort() + ").");
    }

    public void finalize() {
        this.close();
    }

    public void setSoTimeout(int timeout) throws SocketException {
        _socket.setSoTimeout(timeout);
    }

    public void close() {
        if (!_socket.isClosed()) {
            Console.debug("Handshake socket (" + _socket.getLocalPort() + ")"
                          + " closed.");
            _socket.close();
        }
    }

    public void send(String message) throws IOException {
        byte[] data =  message.getBytes(StandardCharsets.UTF_8);
        this.send(data);
    }

    public void send(byte[] data) throws IOException {
        DatagramPacket dgram = new DatagramPacket(
            data,
            data.length,
            _client.config.serverAddr(),
            _client.config.serverPort()
        );
        _socket.send(dgram);
    }

    public DatagramPacket receive() throws IOException {
        byte[] buf = new byte[RECV_BUF_SIZE];
        DatagramPacket dgram = new DatagramPacket(buf, RECV_BUF_SIZE);
        _socket.receive(dgram);
        return dgram;
    }
}
