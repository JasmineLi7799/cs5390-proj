package edu.utdallas.cs5390.group3.server;

import java.lang.Runnable;
import java.lang.String;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public final class ClientThread implements Runnable {

    private Client _client;
    private Server _server;
    private Queue<String> _udpRcvBuffer;
    private int _udpPort;
    private Semaphore _hasUdpData;

    public ClientThread(Client client, Server server, int udpPort) {
        _client = client;
        _server = server;
        _udpPort = udpPort;
    }

    public void run() {}

    /*
    public udpEnqueue(String datagram) {}
    */
}
