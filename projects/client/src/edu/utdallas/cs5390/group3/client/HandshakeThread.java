package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.Thread;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
    public HandshakeThread() throws SocketException {
        super(Client.instance().threadGroup(), "handshake");
        _client = Client.instance();
        _handshakeSock = new HandshakeSocket();
        _handshakeSock.setSoTimeout(_client.config.timeoutInterval());
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
        _handshakeSock.close();
        super.interrupt();
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
        Console.info("Initiated login. Waiting for RESPONSE...");

        while (!Thread.interrupted()) {
            try {
                // AUTH_FAIL -> OFFLINE
                // REGISTER_SENT -> done with handshake, spin SessionThread
                //                  and await REGISTERED response there.
                // Either way, this handshake attempt is done.
                if (_client.state() == Client.State.REGISTER_SENT ||
                    _client.state() == Client.State.OFFLINE) {
                    break;
                }

                DatagramPacket dgram = this.fetchDatagram();
                if (dgram == null) {
                    _client.setState(Client.State.OFFLINE);
                    continue;
                }

                if (!this.dispatch(dgram)) {
                    _client.setState(Client.State.OFFLINE);
                    break;
                }

            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                try {
                    _client.setState(Client.State.OFFLINE);
                } catch (InterruptedException ie) {
                    break;
                }
                Console.fatal("IOException in HandshakeThread: " + e);
                break;
            }
        }

        Console.debug("Handshake thread is terminating.");
    }

    // =========================================================================
    // Message processing
    // =========================================================================

    /* Dispatches a datagram to the appropriate handler
     *
     * @return False if the handshake should be aborted.
     */
    private boolean dispatch(DatagramPacket dgram)
        throws InterruptedException, IOException {

        // Peek at the protocol message type.
        Scanner msg = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        if (!msg.hasNext()) {
            Console.debug("Received empty message.");
            return false;
        }

        // And dispatch accordingly.
        switch (msg.next()) {
        case "CHALLENGE":
            if (!this.handleChallenge(dgram)) {
                return false;
            }
            break;
        case "AUTH_SUCCESS":
            if (!this.handleAuthSuccess()) {
                return false;
            }
            break;
        case "AUTH_FAIL":
            this.handleAuthFail();
            return false;
        default:
            Console.warn("Received unknown message.");
            return false;
        }

        return true;
    }

    /* Generates and sends a HELLO message to the server. */
    private void sendHello() throws InterruptedException, IOException {
        _handshakeSock.send("HELLO " + _client.id());
        _client.setState(Client.State.HELLO_SENT);
    }

    /* Parses and validates CHALLENGE datagram.
     *
     * @param dgram The CHALLENGE datagram to process.
     *
     * @return False if the datagram was invalid, or received in an invalid
     * state.
     */
    private boolean handleChallenge(DatagramPacket dgram)
        throws InterruptedException, IOException {

        if (_client.state() != Client.State.HELLO_SENT) {
            Console.debug("Receieved CHALLENGE in invalid state.");
            return false;
        }

        Scanner msg = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        msg.next();

        if (!msg.hasNext()) {
            Console.debug("Received truncated CHALLENGE.");
            return false;
        }

        String randString = msg.next();
        if (msg.hasNext()) {
            Console.debug("Receieve CHALLENGE with extra bytes.");
            return false;
        }
        byte[] rand = DatatypeConverter.parseHexBinary(randString);
        byte[] ckey = Cryptor.hash2(_client.privateKey(), rand);
        String ckeyString = DatatypeConverter.printHexBinary(ckey);
        Console.debug("Setting cryptkey: " + ckeyString);
        _client.setCryptKey(ckey);

        Console.info("Received CHALLENGE...");
        Console.debug("rand = " + randString);

        sendResponse(rand);
        return true;
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

        Console.info("Sending RESPONSE...");
        Console.debug(payload);
        // Send the response
        _handshakeSock.send(payload);

        _client.setState(Client.State.RESPONSE_SENT);
    }

    /* Parses and validates AUTH_SUCCESS datagram.
     *
     * @param dgram The AUTH_SUCCESS datagram to process.
     *
     * @return False if the datagram was invalid, or received in an invalid
     * state.
     */
    private boolean handleAuthSuccess() throws InterruptedException, IOException {
        if (_client.state() != Client.State.RESPONSE_SENT) {
            Console.warn("Received AUTH_SUCCESS in invalid state.");
            return false;
        }
        Console.info("Received AUTH_SUCCESS...");
        this.sendRegister();
        return true;
    }

    private void handleAuthFail() throws InterruptedException {
        if (_client.state() != Client.State.RESPONSE_SENT) {
            Console.warn("Received AUTH_FAIL in invalid state.");
        }
        Console.info("Authentication failed. Check 'user_id' and "
                     + "'private_key' in the config file and try again.");
        _client.setState(Client.State.OFFLINE);
    }

    private void sendRegister()
        throws InterruptedException, IOException {

        // Generate REGISTER contents.
        String payload = String.format(
            "REGISTER %s %d",
            _client.config.clientExternalAddr().getHostAddress(),
            _client.config.clientPort());

        // Encrypt the REGISTER message.
        byte[] cryptPayload;
        try {
            cryptPayload = Cryptor.encrypt(_client.cryptKey(), payload);
        } catch (Exception e) {
            // This shouldn't be possible.
            Console.error("Encryption failure in "
                          + "HandhsakeThread.sendRegister(): " + e);
            Console.error("Log on aborted.");
            _client.setState(Client.State.OFFLINE);
            return;
        }

        // Start the SessionThread (takes over from here).
        SessionThread st = new SessionThread();
        st.start();

        // Send the REGISTER message.
        Console.info("Sending REGISTER...");
        Console.debug(payload);

        try {
            _client.setState(Client.State.REGISTER_SENT);
            _handshakeSock.send(cryptPayload);
        } catch (IOException e) {
            Console.debug("In SeesionThread.sendRegister(): " + e);
            _client.setState(Client.State.OFFLINE);
            st.interrupt();
        }
    }

    // =========================================================================
    // Socket IO
    // =========================================================================

    /* Fetches the next datagram and decrypts it, if necessary.
     *
     * @return The (decrypted) datagram, or null if an error occurred.
     */
    private DatagramPacket fetchDatagram()
        throws InterruptedException, IOException {

        // Get the next protocol message from the socket.
        DatagramPacket dgram;
        try {
            dgram = _handshakeSock.receive();
        } catch (SocketTimeoutException e) {
            String op = "CHALLENGE";
            if (_client.state() == Client.State.RESPONSE_SENT) {
                op = "AUTH_SUCCESS/AUTH_FAIL";
            }
            _client.setState(Client.State.OFFLINE);
            Console.error("Timeout while waiting for " + op
                            + " from server. Retry your log on.");
            Console.error("This may be a temporary problem. If it "
                            + "persists, check your configuration.");
            return null;
        }

        if (dgram.getLength() == 0) {
            Console.debug("Received empty message.");
            return null;
        }

        // From the RESPONSE_SENT state, we don't know whether the next reply
        // from the server will be encrypted or not since this depends on
        // whether authentication succeeded or failed.
        // Fortunately, AUTH_SUCCESS and AUTH_FAILURE both have distinct,
        // fixed lengths, so we can differentiate without any complicated
        // heuristics.
        if (_client.state() == Client.State.RESPONSE_SENT
            && dgram.getLength() == Cryptor.CRYPT_LENGTH) {
            // replace the datagram data with its decrypted
            // payload.
            try {
                Cryptor.decrypt(_client.cryptKey(), dgram);
            } catch (Exception e) {
                Console.fatal("Decryption failure: " + e);
                return null;
            }
        }

        return dgram;
    }
}
