package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;

import java.util.HashMap;

public final class WelcomeThread extends Thread {

    private DatagramSocket _socket;
    private int _socketPortNo;

    // Map from port number to Client for routing UDP datagrams
    // received by the welcome port to the appropriate client
    // thread.
    private HashMap<Integer, Client> _clientPortMap;

    @Override
    public void run() {
        _socketPortNo = 9876;
        try {
            _socket = new DatagramSocket(_socketPortNo);
        } catch (SocketException e) {
            Console.fatal("Exception while opening welcome port "
                          + _socketPortNo
                          + ": "
                          + e);
            return;
        }
        Console.info("Opened UDP welcome socket on: " + _socketPortNo);
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        _socket.close();
        Console.info("Closed UDP welcome socket: " + _socketPortNo);
    }

    private Client findClientByPort(int port) {
        return _clientPortMap.get(port);
    }

    /*
    public void send(int dport, String datagram) {}
    */
}
