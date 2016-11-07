package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;

import java.net.DatagramSocket;
import java.net.DatagramPacket;

import java.net.SocketException;
import java.io.IOException;

import java.util.HashMap;

import java.util.Scanner;
import java.io.ByteArrayInputStream;

public final class WelcomeThread extends Thread {
    private final int RECV_BUF_SIZE = 1024;
    private final int SEND_BUF_SIZE = 1024;
    private final int SOCKET_NO = 9876;

    private DatagramSocket _socket;

    // Map from port number to Client for routing UDP datagrams
    // received by the welcome port to the appropriate client
    // thread.
    private HashMap<Integer, Client> _clientPortMap;

    // Fun fact: DatagramSocket.receive() is a blocking call that does
    // not throw InterruptedException. Hence, the usual thread
    // interruption mechanism won't work at this point in the program
    // flow (and this is where WelcomeThread spends 99% of its
    // time...)
    //
    // Closing the socket will do the trick, since this will cause
    // DatagramSocket.receive() to immediately unblock and throw an
    // IOException. And we need to close the socket on our way out,
    // anyway.
    @Override
    public void interrupt() {
        super.interrupt();
        this.closeSocket();
    }

    @Override
    public void run() {
        if (!this.openSocket()) return; // SocketException occurred.
        while (!Thread.interrupted() && !_socket.isClosed()) {
            DatagramPacket dgram = this.receive();
            if (dgram == null) continue; // IOException occurred. Abort this iteration.
            Scanner scan = new Scanner(
                new ByteArrayInputStream(dgram.getData(), 0, dgram.getLength())
            );
            if (scan == null) continue; // IOException occurred.
            if (scan.hasNext("HELLO")) {
                scan.next();
                int clientId;
                if (scan.hasNextInt()) {
                    clientId = scan.nextInt();
                } else {
                    Console.warn("Welcome thread: Received malformed HELLO from: "
                                 + dgram.getAddress());
                    continue;
                }
                // TODO: initiate handshake
                Console.info("Welcome theread: Received HELLO from "
                             + dgram.getAddress().getHostAddress()
                             + " for client " + clientId);
            } else {
                Console.warn("Welcome thread: Received malformed packet from: "
                             + dgram.getAddress());
                continue;
            }
        }
        if (!_socket.isClosed()) {
            this.closeSocket();
        }
    }

    private boolean openSocket() {
        try {
            _socket = new DatagramSocket(SOCKET_NO);
        } catch (SocketException e) {
            Console.fatal("Welcome thread: Exception while opening welcome port "
                          + SOCKET_NO + ": " + e);
            return false;
        }
        Console.info("Welcome thread: Listening on UDP port " + SOCKET_NO);
        return true;
    }

    private void closeSocket() {
        _socket.close();
        Console.info("Welcome thread: Closed UDP port " + SOCKET_NO);
    }

    private DatagramPacket receive() {
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

    private Client findClientByPort(int port) {
        return _clientPortMap.get(port);
    }

    /*
    public void send(int dport, String datagram) {}
    */
}
