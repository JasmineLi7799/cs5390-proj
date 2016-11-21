package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.InterruptedException;
import java.io.IOException;
import java.net.UnknownHostException;

import java.lang.Thread;

import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.util.Scanner;
import java.lang.StringBuilder;
// There's no XML here. We just need this to convert hex strings to byte arrays
// without reinventing the wheel.
import javax.xml.bind.DatatypeConverter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/* HandshakeThread implements a listener thread that accepts protocol
 * messages from the client over UDP during the handshake. It is a
 * simple state machine model that transitions on inputs from its
 * work queue of UDP datagrams (furnished by the WelcomeThread).
 */
public final class HandshakeThread extends Thread {
    // Associated client info
    private Client _client;
    private InetAddress _clientAddr;
    private int _clientPort;
    private SocketAddress _clientSockAddr;
    private boolean _isComplete;

    // The HandshakeThread needs to write to the WelcomeSocket to
    // generate responses during the handshake.
    private WelcomeSocket _welcomeSock;
    // Work queue for handshake process
    private LinkedBlockingQueue<DatagramPacket> _workQueue;

    // Needed for client ID lookups.
    private Server _server;

    // The expected client response value. Populated in sendChallenge() and
    // referenced in handleResponse()
    private byte[] _xres;


    // =========================================================================
    // Constructor
    // =========================================================================

    /* Creates a new HandshakeThread associated with a particular source
     * (client) socket.
     *
     * @param sockAddr The UDP socket address this HandshakeThread will
     * service during handshake
     * @param sock The WelcomeSocket the HandshakeThread will send
     * responses over.
     */
    public HandshakeThread(SocketAddress sockAddr, WelcomeSocket sock) {
        // Creates a named thread. The name is in the format:
        // "handshake 1.2.3.4:5"
        super(Server.instance().threadGroup(),
              "handshake "
              + ((InetSocketAddress)sockAddr).getAddress().getHostAddress()
              + ":"
              + ((InetSocketAddress)sockAddr).getPort());

        _server = Server.instance();
        _workQueue = new LinkedBlockingQueue<DatagramPacket>();

        // Note: this is only a safe cast because we are certain that
        // the underlying subtype for this SocketAddress (abstract
        // class) is, in fact, an InetSocketAddress (concrete
        // subclass).
        InetSocketAddress inetSockAddr = (InetSocketAddress)sockAddr;
        _clientAddr = inetSockAddr.getAddress();
        _clientPort = inetSockAddr.getPort();

        _clientSockAddr = sockAddr;
        _welcomeSock = sock;
    }

    // =========================================================================
    // Thread runtime management
    // =========================================================================

    /* Handshake state machine loop.
     *
     * Parses handshake protocol messages from the network and updates the
     * Client's state accordingly.
     */
    public void run() {
        _isComplete = false;
        Console.debug(tag("Spun new handshake thread."));

        while (!Thread.interrupted()) {
            try {
                // Fetch datagram
                DatagramPacket dgram;
                try {
                    dgram = this.fetchDatagram();
                } catch (NullPointerException e) {
                    _client.setState(Client.State.OFFLINE);
                    break;
                }

                // dispatch datagram
                if (!this.dispatch(dgram)) {
                    // either the data was invalid, or the handshake is done.
                    break;
                }

            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                Console.error(tag("IOException in HandshakeThread."));
                break;
            }
        }

        this.exitCleanup();
    }

    /* Performs cleanup tasks whenever the thread exits.
     *
     * Primarily, this unmaps the thread from the WelcomeSocket's
     * thread map so that no future UDP datagrams will be routed here.
     */
    private void exitCleanup() {
        if (!_isComplete && _client != null) {
            try {
                _client.setState(Client.State.OFFLINE);
                Console.debug(tag("Aborted with partial handshake. Resetting "
                                + "client state."));
            } catch (InterruptedException e) {
                // Nothing to do about this; we're about to exit anyway.
            }
        }
        Console.debug(tag("Handshake thread terminating."));
        if (_welcomeSock.unmapThread(_clientSockAddr) == null) {
            Console.debug(tag("Tried to unmap handshake thread, but it was not "
                             + "mapped."));
        }
    }

    // =========================================================================
    // Output helper
    // =========================================================================

    /* This function safely appends as much client info as possible to a base
     * message.
     *
     * Essentially, this omits the _client.id() from the message if _client
     * is presently null (we have not received a valid HELLO yet).
     *
     * @param msg The base message to append client info to.
     */
    private String tag(String msg) {
        StringBuilder out = new StringBuilder();
        out.append("[Handshake ");
        if (_client != null) {
            out.append("client=");
            out.append(_client.id());
        } else {
            out.append("src=");
            out.append(_clientAddr.getHostAddress());
            out.append(':');
            out.append(_clientPort);
        }
        out.append("] ");
        out.append(msg);
        return out.toString();
    }

    // =========================================================================
    // Protocol message parsing
    // =========================================================================

    /* Dispatch the packet to the appropriate handler.
     *
     * @param dgram The datagram to dispatch.
     *
     * @return True if the handshake should continue. False if the handshake
     * is complete or an unrecoverable error occured (ex. invalid data from
     * client).
     */
    private boolean dispatch(DatagramPacket dgram)
        throws InterruptedException, IOException {

        // Peek at the protocol message type
        Scanner msg = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        if (!msg.hasNext()) {
            Console.debug(tag("Received empty message."));
            return false;
        }

        // Dispatch accordingly.
        switch (msg.next()) {
        case "HELLO":
            if (!this.handleHello(dgram)) {
                return false;
            }
            break;
        case "RESPONSE":
            if (!this.handleResponse(dgram)) {
                return false;
            }
            break;
        case "REGISTER":
            if (!this.handleRegister(dgram)) {
                // Partial handshake; reset client state
                return false;
            }
            // Handshake complete; SessionThread will take it from here.
            return false;
        }

        return true;
    }

    /* Validates and processes HELLO messages.
     *
     * @param dgram The datagram containing the HELLO.
     *
     * @return False if validation failed or we are in an inappropriate state
     * for this message type.
     */
    private boolean handleHello(DatagramPacket dgram)
        throws InterruptedException, IOException {

        // Are we in the right state for this?
        if (_client != null
            && _client.state() != Client.State.OFFLINE) {
            Console.debug(tag("Received HELLO in invalid state."));
            return false;
        }

        // Get id from message.
        Scanner helloMsg = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        helloMsg.next();
        if (!helloMsg.hasNextInt()) {
            Console.debug(tag("Received malformed HELLO."));
            return false;
        }
        int id = helloMsg.nextInt();
        helloMsg.close();

        // Check if id is valid.
        Client client = _server.findClient(id);
        if (_server.findClient(id) == null) {
            Console.warn(tag("Received HELLO for unknown client: " + id));
            return false;
        }

        // Set client. Send response.
        _client = client;
        Console.info(tag("Received HELLO."));
        this.sendChallenge();

        _client.setState(Client.State.CHALLENGE_SENT);
        return true;
    }

    /* Generates a CHALLENGE message.
     */
    private void sendChallenge()
        throws InterruptedException, IOException {
        // Generate secure random 32-bit value.
        byte[] randBytes = new byte[4];
        Cryptor.nextBytes(randBytes);
        String rand = DatatypeConverter.printHexBinary(randBytes);

        // Construct the challenge packet payload.
        String payload = "CHALLENGE " + rand;

        // Send the challenge
        Console.debug(tag("Sending " + payload));

        // Set the encryption key for post-auth communication.
        byte[] ckey = Cryptor.hash2(_client.privateKey(), randBytes);
        String ckeyString = DatatypeConverter.printHexBinary(ckey);
        Console.debug(tag("Setting client cryptkey: " + ckeyString));
        _client.setCryptKey(ckey);

        _xres = Cryptor.hash1(_client.privateKey(), randBytes);
        _welcomeSock.send(payload, _clientAddr, _clientPort);
    }

    /* Validates and processes RESPONSE messages.
     *
     * @param dgram The datagram containing the RESPONSE.
     *
     * @return False, if validation failed or we are in an invalid state for
     * this message type.
     */
    private boolean handleResponse(DatagramPacket dgram)
        throws InterruptedException, IOException {

        if (_client == null
            || (_client != null
                && _client.state() != Client.State.CHALLENGE_SENT)) {

            Console.debug(tag("Received RESPONSE in invalid state."));
            return false;
        }

        Scanner response = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        response.next();
        // No client ID
        if (!response.hasNextInt()) {
            Console.debug(tag("Receieved truncated RESPONSE (expected id)."));
            return false;
        }
        // Client spoofing? Sure, why not.
        int id = response.nextInt();
        if (id != _client.id()) {
            Console.warn(tag("Receieved RESPONSE from wrong client: "
                             + id + " (spoofing attack?)"));
            return false;
        }

        if (!response.hasNext()) {
            Console.debug(tag("Receieved truncated RESPONSE (expected res)."));
            return false;
        }
        String resString = response.next();
        if (response.hasNext()) {
            Console.debug(tag("Extra bytes in RESPONSE."));
        }

        Console.debug(tag("Received RESPONSE " + resString));

        byte[] res = DatatypeConverter.parseHexBinary(resString);
        if (Arrays.equals(res, _xres)) {
            try {
                byte[] authMsg = Cryptor.encrypt(
                    _client.cryptKey(),
                    "AUTH_SUCCESS");
                _welcomeSock.send(authMsg, _clientAddr, _clientPort);
            } catch (Exception e) {
                Console.error(tag("Encryption failure: " + e));
                return false;
            }
            _client.setState(Client.State.AUTHENTICATED);
            Console.info(tag("AUTH_SUCCESS for client " + _client.id()));
        } else {
            _welcomeSock.send("AUTH_FAIL", _clientAddr, _clientPort);
            _client.setState(Client.State.OFFLINE);
            Console.info(tag("AUTH_FAIL for client " + _client.id()));
        }

        return true;
    }

    /* Validates and processes REGISTER message.
     *
     * Creates the SessionThread if the REGISTER request is valid.
     *
     * @param dgram The datagram containing the REGISTER.
     *
     * @return False if validation failed or we are in an invalid state for
     * this message type.
     */
    private boolean handleRegister(DatagramPacket dgram)
        throws InterruptedException, IOException {

        if (_client == null
            || (_client != null
                && _client.state() != Client.State.AUTHENTICATED)) {

            Console.debug(tag("Received REGISTER in invalid state."));
            return false;
        }

        Scanner register = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        register.next();

        // No client IP
        if (!register.hasNext()) {
            Console.debug(tag("Receieved truncated REGISTER (expected addr)."));
            return false;
        }
        String regAddrString = register.next();

        // Validate REGISTER IP
        InetAddress regAddr;
        try {
            regAddr = InetAddress.getByName(regAddrString);
        } catch (UnknownHostException e) {
            // bad client IP
            Console.error(tag("Bad REGISTER address: " + regAddrString));
            return false;
        }

        // Not necessarily wrong, just weird and worth logging.
        if (!regAddr.equals(_clientAddr)) {
            Console.warn(tag("REGISTER adddress "
                             + "'" + regAddrString + "' "
                             + "does not match handshake address "
                             + "'" + _clientAddr.getHostAddress() + "' "
                             + "(misconfigured client? spoofing attack?)"));
        }

        // Validate port
        if (!register.hasNextInt()) {
            Console.error(tag("Receieved truncated REGISTER (expected port)."));
            Console.debug(tag("next token: '" + register.next() + "'"));
            return false;
        }
        int regPort = register.nextInt();
        if (regPort < 0 || regPort > 65535) {
            Console.error(tag("Bad REGISTER port number: " + regPort));
            return false;
        }

        if (register.hasNext()) {
            Console.debug(tag("Extra bytes in REGISTER."));
        }

        // Finally good.
        Console.debug(tag("Received REGISTER " + regAddrString
                          + " " + regPort));
        (new SessionThread(_client, regAddr, regPort)).start();
        _isComplete = true;

        return true;
    }

    // =========================================================================
    // WelcomeSocket IO
    // =========================================================================

    /* Takes the next UDP datagram from the work queue.
     *
     * @return The next UDP datagram in the queue.
     */
    private DatagramPacket udpPoll() throws InterruptedException {
        if (_server.config.timeoutInterval() > 0) {
            return _workQueue.poll(_server.config.timeoutInterval(),
                                TimeUnit.MILLISECONDS);
        } else {
            return _workQueue.take();
        }
    }

    /* Enqueues a UDP datagram to the work queue.
     *
     * @param dgram The datagram to enqueue.
     */
    public void udpPut(DatagramPacket dgram) throws InterruptedException {
        _workQueue.put(dgram);
    }

    /* Fetches the next datagram and decrypts it if we are in a state that
     * requires encryption (AUTHENTICATED).
     *
     * @return The datagram packet
     */
    private DatagramPacket fetchDatagram()
        throws InterruptedException, IOException, NullPointerException {
        // Fetch next datagram
        DatagramPacket dgram = this.udpPoll();
        if (dgram == null) {
            if (_client != null) {
                Console.debug(
                    tag("Timed out while waiting for RESPONSE."));
                throw new NullPointerException();
            }
        }
        if (dgram.getLength() == 0) {
            Console.debug(tag("Received empty message."));
            throw new NullPointerException();
        }

        // From the AUTHENTICATED state onward, all client responses
        // will be encrypted.
        if (_client != null
            && _client.state() == Client.State.AUTHENTICATED) {
            try {
                Cryptor.decrypt(_client.cryptKey(), dgram);
            } catch (Exception e) {
                Console.error(tag("Encryption failure: " + e));
                throw new NullPointerException();
            }
        }

        return dgram;
    }

}
