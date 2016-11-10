package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.lang.Thread;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import java.net.SocketException;
import java.lang.InterruptedException;

public final class HandshakeThread extends Thread {
    private static WelcomeSocket _welcome;

    private Client _client;

    public HandshakeThread(Client client,
        InetSocketAddress serverSockAddr)
        throws SocketException {

        super();
        _client = client;
        _welcome = new WelcomeSocket(serverSockAddr);
    }

    @Override
    public void interrupt() {
        super.interrupt();
        _welcome.close();
    }

    @Override
    public void run() {
        try {
            this.sendHello();
        } catch (InterruptedException e) {
            return;
        }

        while (!Thread.interrupted()) {
            Client.State state;
            try {
                state = _client.state();
            } catch (InterruptedException e) {
                return;
            }
            if (state == Client.State.CHALLENGE_RECV) break;

            try {
                this.getChallenge();
            } catch (InterruptedException e) {
                return;
            }
        }
        if (Thread.interrupted()) {
            return;
        }

        // TODO: finish handshake.
        Console.debug("Handshake thread is terminating.");
    }

    private void sendHello() throws InterruptedException {
        _welcome.send(_client.hello());
        _client.setState(Client.State.HELLO_SENT);
    }

    private void getChallenge() throws InterruptedException {
        // TODO: parse and validate challenge datagram
        DatagramPacket challenge = _welcome.receive();
        if (challenge == null) {
            System.exit(-1);
        }
        Console.debug("Got CHALLENGE from server.");

        _client.setState(Client.State.CHALLENGE_RECV);
    }
}
