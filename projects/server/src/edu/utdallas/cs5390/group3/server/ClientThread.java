package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;

import java.util.concurrent.LinkedBlockingQueue;

import java.net.DatagramPacket;

import java.lang.InterruptedException;

public final class ClientThread extends Thread {

    private Client _client;
    private int _clientPort;
    private Server _server;
    private LinkedBlockingQueue<DatagramPacket> _udpRcvBuffer;
    private WelcomeSocket _welcomeSock;

    public ClientThread(Client client, int clientPort, WelcomeSocket sock) {
        _udpRcvBuffer = new LinkedBlockingQueue<DatagramPacket>();
        _server = Server.instance();
        _client = client;
        _clientPort = clientPort;
        _welcomeSock = sock;
    }

    public Client client() { return _client; }

    public void run() {
        Console.debug("ClientThread for client "
                      + _client.id()
                      + " is now running.");

        // TODO: handshake
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // break;
        }

        Console.debug("ClientThread for client "
                      + _client.id()
                      + " is exiting.");

        if (_server.unMapThread(_clientPort) == null) {
            Console.warn("Tried to unmap listener thread for client "
                         + _client.id()
                         + ", but it was not found in the thread map.");
        }
    }

    public void udpPut(DatagramPacket dgram) throws InterruptedException {
        _udpRcvBuffer.put(dgram);
    }

}
