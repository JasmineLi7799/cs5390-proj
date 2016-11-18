package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.Thread;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import java.io.IOException;
import java.net.SocketException;
import java.lang.InterruptedException;
import java.lang.NullPointerException;

import java.util.Arrays;
import java.util.Scanner;
import java.io.ByteArrayInputStream;

public final class HandshakeThread extends Thread {
    private static HandshakeSocket _handshakeSock;

    private Client _client;

    public HandshakeThread(Client client)
        throws SocketException {

        super();
        _client = client;
        InetSocketAddress serverSockAddr =
            new InetSocketAddress(_client.config.serverAddr(),
                                  _client.config.serverPort());
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
        } catch (IOException e) {
            Console.fatal("While sending HELLO caught: " + e);
            return;
        } catch (InterruptedException e) {
            return;
        }

        byte[] rand;
        try {
            rand = this.getChallenge();
        } catch (IOException e) {
            Console.fatal("While waiting for CHALLENGE caught: " + e);
            return;
        } catch (InterruptedException e) {
            return;
        }

        // TODO: finish handshake.
        Console.debug("Handshake thread is terminating.");
    }

    private void sendHello() throws InterruptedException, IOException {
        _handshakeSock.send("HELLO " + _client.id());
        _client.setState(Client.State.HELLO_SENT);
    }

    private byte[] getChallenge() throws InterruptedException, IOException {
        while (!Thread.interrupted()) {
            DatagramPacket dgram = _handshakeSock.receive();

            if (dgram.getLength() == 0) {
                Console.warn("Received empty datagram from server.");
                continue;
            }

            Scanner msg = new Scanner(new ByteArrayInputStream(
                dgram.getData(), 0, dgram.getLength()));
            if (!msg.hasNext("CHALLENGE")) {
                // Since this could be arbitrary binary data, we can't
                // really print the unexpected reply to the console...
                Console.warn("Received unexpected reply from server.");
                continue;
            }

            if (dgram.getLength() < 14) {
                Console.warn("Received truncated CHALLENGE from server.");
                continue;
            } else if (dgram.getLength() > 14) {
                Console.warn("Ignoring unexpected bytes at end of CHALLENGE "
                             + "from server.");
            }

            // 14 is not a typo; the range of copyOfRange is inclusive on the
            // start and exclusive on the end. Don't ask me why.
            byte[] rand = Arrays.copyOfRange(dgram.getData(), 10, 14);
            _client.setState(Client.State.CHALLENGE_RECV);
            Console.info("Got CHALLENGE from server (rand = "
                          + String.format("0x%02X%02X%02X%02X",
                                          rand[0], rand[1],
                                          rand[2], rand[3])
                          + ")");
            return rand;
        }
        throw new InterruptedException();
    }
}
