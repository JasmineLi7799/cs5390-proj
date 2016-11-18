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

        while (!Thread.interrupted()) {
            try {
                // At the start of this loop we are HELLO_SENT.
                // we demote to OFFLINE on AUTH_FAIL, or
                // promote to AUTHENTICATED on HELLO_SENT.
                // Either way, we're done. If auth fails, we'll
                // inform the user and they can try logging in again.
                if (_client.state() == Client.State.AUTHENTICATED ||
                    _client.state() == Client.State.OFFLINE) {
                    break;
                }

                DatagramPacket dgram = _handshakeSock.receive();
                if (dgram.getLength() == 0) {
                    Console.warn("Received unknown message.");
                    continue;
                }
                Scanner msg = new Scanner(new ByteArrayInputStream(
                    dgram.getData(), 0, dgram.getLength()));
                if (!msg.hasNext()) {
                    Console.warn("Received unknown message.");
                    continue;
                }

                switch (msg.next()) {
                case "CHALLENGE":
                    this.handleChallenge(dgram);
                    break;
                case "AUTH_SUCCESS":
                    this.handleAuthSuccess();
                    break;
                case "AUTH_FAIL":
                    this.handleAuthFail();
                    break;
                }
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                Console.fatal("IOException in HandshakeThread: " + e);
                break;
            }
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

    /* Parses and validates CHALLENGE datagram.
     *
     * @param dgram The CHALLENGE datagram to process.
     */
    private void handleChallenge(DatagramPacket dgram)
        throws InterruptedException, IOException {

        if (_client.state() != Client.State.HELLO_SENT) {
            Console.warn("Receieved CHALLENGE in invalid state.");
            return;
        }

        Scanner msg = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        msg.next();

        if (!msg.hasNext()) {
            Console.warn("Received truncated CHALLENGE.");
            return;
        }

        String randString = msg.next();
        if (msg.hasNext()) {
            Console.warn("Receieve CHALLENGE with extra bytes.");
        }
        byte[] rand = DatatypeConverter.parseHexBinary(randString);

        Console.info("Received CHALLENGE from server...");
        Console.debug("rand = " + randString);
        sendResponse(rand);
    }

    /* Generates and sends the challenge RESPONSE
     *
     * @param rand The rand value from the CHALLENGE.
     */
    private void sendResponse(byte[] rand)
        throws InterruptedException, IOException {

        // Generate the res value.
        byte[] resBytes = Cryptor.hash1(
            _client.privateKey(),
            rand);
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

    private void handleAuthSuccess() throws InterruptedException {
        if (_client.state() != Client.State.RESPONSE_SENT) {
            Console.warn("Received AUTH_SUCCESS in invalid state.");
        }
        Console.info("Authenticated.");
        _client.setState(Client.State.AUTHENTICATED);
    }

    private void handleAuthFail() throws InterruptedException {
        if (_client.state() != Client.State.RESPONSE_SENT) {
            Console.warn("Received AUTH_FAIL in invalid state.");
        }
        Console.info("Authentication failed. Check 'user_id' and "
                     + "'private_key' in the config file and try again.");
        _client.setState(Client.State.OFFLINE);
    }
}
