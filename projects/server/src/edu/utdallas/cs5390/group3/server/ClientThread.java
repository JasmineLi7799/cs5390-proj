package edu.utdallas.cs5390.group3.server;

import java.lang.Runnable;

import java.lang.String;

import java.util.Queue;

import java.util.concurrent.Semaphore;

import java.net.DatagramPacket;

public final class ClientThread implements Runnable {

    private Client _client;
    private Server _server;
    private Queue<DatagramPacket> _udpRcvBuffer;
    private Semaphore _hasUdpData;
    private WelcomeSocket _welcomeSock;

    public ClientThread(Client client, WelcomeSocket sock) {
        _client = client;
        _server = Server.instance();
        _welcomeSock = sock;
    }

    public void run() {}

    /*
    public udpEnqueue(String datagram) {}
    */
}
