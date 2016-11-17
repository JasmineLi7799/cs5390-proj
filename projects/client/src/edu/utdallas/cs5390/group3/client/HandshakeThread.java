package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.Thread;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import java.net.SocketException;
import java.lang.InterruptedException;
import java.io.UnsupportedEncodingException;

public final class HandshakeThread extends Thread {
    private static HandshakeSocket _handshakeSock;

    private Client _client;

    public HandshakeThread(Client client,
        InetSocketAddress serverSockAddr)
        throws SocketException {

        super();
        _client = client;
        _handshakeSock = new HandshakeSocket(serverSockAddr);
    }

    @Override
    public void interrupt() {
        super.interrupt();
        _handshakeSock.close();
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
        _handshakeSock.send(_client.hello());
        _client.setState(Client.State.HELLO_SENT);
    }

    private void getChallenge() throws InterruptedException {
        DatagramPacket challenge = _handshakeSock.receive();
        if (challenge == null) {
            System.exit(-1);
        }
        Console.debug("Got CHALLENGE from server.");

        _client.setState(Client.State.CHALLENGE_RECV);

        try{
            String challengeContents = new String(challenge.getData(), "UTF-8");
            byte[] challengeResponse = Cryptor.hash1(challengeContents);

            _handshakeSock.send(new String(challengeResponse, "UTF-8"));
        }catch(UnsupportedEncodingException e){
            Console.error("Error - server's challenge was not UTF-8 encodable");
            return;
        }
    }
}
