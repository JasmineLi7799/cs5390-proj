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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
// There's no XML here. We just need this to convert hex strings to byte arrays
// without reinventing the wheel.
import javax.xml.bind.DatatypeConverter;

public final class HandshakeThread extends Thread {
    private static HandshakeSocket _handshakeSock;

    private Client _client;

    // =========================================================================
    // Constructor
    // =========================================================================

     /* Constructs a HandshakeThread for the specified client.
      *
      * @param client The Client object associated with this handshake.
      */
    public HandshakeThread(Client client) throws SocketException {

        super();
        _client = client;
        InetSocketAddress serverSockAddr =
            new InetSocketAddress(_client.config.serverAddr(),
                                  _client.config.serverPort());
        _handshakeSock = new HandshakeSocket(serverSockAddr);
    }

    // =========================================================================
    // Thread runtime management
    // =========================================================================

    /* Interrupts the thread.
     *
     * Socket read/write does not throw InterruptedExceptions, so we interrupt
     * these blocking calls by closing the underlying port. */
    @Override
    public void interrupt() {
        super.interrupt();
        _handshakeSock.close();
    }

    /* This worker thread performs the HELLO, CHALLENGE, RESPONSE,
     * AUTHENTICATED handshake process.
     */
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

        try {
            this.sendResponse(rand);
        } catch (IOException e) {
            Console.fatal("While sending RESPONSE caught: " + e);
            return;
        } catch (InterruptedException e) {
            return;
        }

        // TODO: finish handshake.
        Console.debug("Handshake thread is terminating.");
    }

    // =========================================================================
    // Message processing
    // =========================================================================

    /* Generates and sends a HELLO message to the server. */
    private void sendHello() throws InterruptedException, IOException {
        _handshakeSock.send("HELLO " + _client.id());
        _client.setState(Client.State.HELLO_SENT);
    }

    /* Repeatedly tries to get a CHALLENGE response from the workequeue
     * until successful.
     *
     * @return The "rand" value of the CHALLENGE
     */
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
                Console.warn("Received unexpected reply.");
                continue;
            }
            msg.next();

            if (!msg.hasNext()) {
                Console.warn("Received truncated CHALLENGE.");
                continue;
            }

            String randString = msg.next();
            if (msg.hasNext()) {
                Console.warn("Receieve CHALLENGE with extra bytes.");
            }
            byte[] rand = DatatypeConverter.parseHexBinary(randString);

            _client.setState(Client.State.CHALLENGE_RECV);
            Console.info("Received CHALLENGE from server...");
            Console.debug("rand = " + randString);
            return rand;
        }
        throw new InterruptedException();
    }

    /* Generates and sends the challenge RESPONSE
     *
     * @param rand The rand value from the CHALLENGE.
     */
    private void sendResponse(byte[] rand)
        throws InterruptedException, IOException {

        // Generate the res value.
        ByteArrayOutputStream hashInput
            = new ByteArrayOutputStream();
        hashInput.write(
            _client.privateKey().getBytes(StandardCharsets.UTF_8));
        hashInput.write(rand);
        byte[] resBytes = Cryptor.hash1(hashInput.toByteArray());
        hashInput.close();
        String res = DatatypeConverter.printHexBinary(resBytes);

        String payload = String.format(
            "RESPONSE %d %s",
            _client.id(),
            res);

        Console.info("Sending RESPONSE to server...");
        Console.debug(payload);
        // Send the response
        _handshakeSock.send(payload);

        _client.setState(Client.State.RESPONSE_SENT);
    }
}
