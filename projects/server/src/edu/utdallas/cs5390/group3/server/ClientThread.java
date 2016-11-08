package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.InterruptedException;
import java.io.IOException;

import java.lang.Thread;

import java.util.concurrent.LinkedBlockingQueue;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.io.ByteArrayOutputStream;


public final class ClientThread extends Thread {

    private Client _client;
    private InetAddress _clientAddr;
    private SocketAddress _clientSockAddr;
    private int _clientPort;
    private Server _server;
    private LinkedBlockingQueue<DatagramPacket> _udpRcvBuffer;
    private WelcomeSocket _welcomeSock;

    public ClientThread(Client client, SocketAddress sockAddr, WelcomeSocket sock) {
        _udpRcvBuffer = new LinkedBlockingQueue<DatagramPacket>();
        _server = Server.instance();

        _client = client;
        _clientSockAddr = sockAddr;
        InetSocketAddress inetSockAddr = (InetSocketAddress)sockAddr;
        _clientAddr = inetSockAddr.getAddress();
        _clientPort = inetSockAddr.getPort();
        _welcomeSock = sock;
    }

    public Client client() { return _client; }

    public void run() {
        Console.debug("ClientThread for client "
                      + _client.id()
                      + " is now running.");

        // Generate secure random 32-bit value.
        byte[] rand = new byte[4];
        Cryptor.nextBytes(rand);
        // Construct the challenge packet payload.
        ByteArrayOutputStream challengeStream
            = new ByteArrayOutputStream();
        try {
            challengeStream.write("CHALLENGE\0".getBytes("UTF-8"));
            challengeStream.write(rand);
        } catch (IOException e) {
            // This exception shouldn't actually be possible.
            this.exitCleanup();
            return;
        }
        byte[] challenge = challengeStream.toByteArray();
        try {
            Console.debug("Sending CHALLENGE to client "
                          + _client.id() + ".");
            _welcomeSock.send(challenge, _clientAddr, _clientPort);
        } catch (IOException e) {
            Console.error("In listener thread for client "
                          + _client.id()
                          + ", while sending CHALLENGE: "
                          + e);
            this.exitCleanup();
            return;
        }

        // TODO: rest of handshake
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // break;
        }

        this.exitCleanup();
    }

    public void udpPut(DatagramPacket dgram) throws InterruptedException {
        _udpRcvBuffer.put(dgram);
    }

    private void exitCleanup() {
        Console.debug("ClientThread for client "
                      + _client.id()
                      + " is exiting.");
        if (_server.unMapThread(_clientSockAddr) == null) {
            Console.warn("Tried to unmap listener thread for client "
                         + _client.id()
                         + ", but it was not found in the thread map.");
        }
    }

}
