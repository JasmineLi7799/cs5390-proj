package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.InterruptedException;
import java.io.IOException;

import java.lang.Thread;

import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.lang.StringBuilder;

import java.util.concurrent.LinkedBlockingQueue;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

// There's no XML here. We just need this to convert hex strings to byte arrays
// without reinventing the wheel.
import javax.xml.bind.DatatypeConverter;

/* ClientThread implements a listener thread that accepts protocol
 * messages from the client over UDP during the handshake, and subsequently
 * TCP for a registered connection.
 *
 * More abstractly, ClientThread generates (most of) the inputs that drive
 * the Client's state.
 */
public final class ClientThread extends Thread {
    // Associated client info
    private Client _client;
    private InetAddress _clientAddr;
    private SocketAddress _clientSockAddr;
    private int _clientPort;
    private byte[] _xres;

    private Server _server;

    // Work queue for handshake process
    private LinkedBlockingQueue<DatagramPacket> _udpRcvBuffer;
    // The ClientThread needs to write to the WelcomeSocket to
    // generate responses during the handshake.
    private WelcomeSocket _welcomeSock;

    // =========================================================================
    // Constructor
    // =========================================================================

    /* Creates a new ClientThread associated with a particular source
     * (client) socket.
     *
     * @param sockAddr The UDP socket address this ClientThread will
     * service during handshake
     * @param sock The WelcomeSocket the ClienThread will send
     * responses over.
     */
    public ClientThread(SocketAddress sockAddr, WelcomeSocket sock) {
        // Creates a named thread. The name is in the format:
        // "client listener 1.2.3.4:5"
        super(Server.instance().threadGroup(),
              "client listener "
              + ((InetSocketAddress)sockAddr).getAddress().getHostAddress()
              + ":"
              + ((InetSocketAddress)sockAddr).getPort());

        _server = Server.instance();
        _udpRcvBuffer = new LinkedBlockingQueue<DatagramPacket>();

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

    /* ClientThread loop.
     *
     * Parses protocol messages from the network and updates the Client's
     * state accordingly.
     */
    public void run() {
        Console.debug(tag("Spun new client listener thread"));

        while (!Thread.interrupted()) {
            try {
                if (_client != null
                    && _client.state() == Client.State.AUTHENTICATED) {
                    break;
                }

                DatagramPacket dgram = this.udpTake();
                if (dgram.getLength() == 0) {
                    Console.warn(tag("Received unknown message"));
                    continue;
                }
                Scanner msg = new Scanner(new ByteArrayInputStream(
                    dgram.getData(), 0, dgram.getLength()));
                if (!msg.hasNext()) {
                    Console.warn(tag("Received unknown message"));
                    continue;
                }

                switch (msg.next()) {
                case "HELLO":
                    this.handleHello(dgram);
                    break;
                case "RESPONSE":
                    this.handleResponse(dgram);
                }
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                Console.error(tag("IOException in ClientThread"));
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
        Console.debug(tag("Listener thread is exiting"));
        if (_welcomeSock.unmapThread(_clientSockAddr) == null) {
            Console.warn(tag("Tried to unmap listener thread, but it was not "
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
        out.append(msg);
        out.append(" (");
        if (_client != null) {
            out.append("client=");
            out.append(_client.id());
            out.append(", ");
        }
        out.append("src=");
        out.append(_clientAddr.getHostAddress());
        out.append(':');
        out.append(_clientPort);
        out.append(").");
        return out.toString();
    }

    // =========================================================================
    // Protocol message parsing
    // =========================================================================

    /* Validates and processes HELLO messages.
     *
     * @param dgram The datagram containing the HELLO.
     */
    private void handleHello(DatagramPacket dgram)
        throws InterruptedException, IOException {

        // Are we in the right state for this?
        if (_client != null
            && _client.state() != Client.State.OFFLINE) {
            Console.warn(tag("Received HELLO in invalid state"));
            return;
        }

        // Get id from message.
        Scanner helloMsg = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        helloMsg.next();
        if (!helloMsg.hasNextInt()) {
            Console.warn(tag("Received malformed HELLO"));
            return;
        }
        int id = helloMsg.nextInt();
        helloMsg.close();

        // Check if id is valid.
        Client client = _server.findClient(id);
        if (_server.findClient(id) == null) {
            Console.warn(tag("Received HELLO for unknown client: " + id));
            return;
        }

        // Set client. Send response.
        _client = client;
        Console.info(tag("Received HELLO"));
        this.sendChallenge();

        _client.setState(Client.State.CHALLENGE_SENT);
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
        Console.debug(tag("Sending CHALLENGE"));
        Console.debug(tag("Content = " + payload));

        _xres = Cryptor.hash1(_client.privateKey(), randBytes);
        _welcomeSock.send(payload, _clientAddr, _clientPort);
    }

    /* Validates and processes RESPONSE messages.
     *
     * @param dgram The datagram containing the RESPONSE.
     */
    private void handleResponse(DatagramPacket dgram)
        throws InterruptedException, IOException {

        if (_client != null
            && _client.state() != Client.State.CHALLENGE_SENT) {
            Console.warn(tag("Received RESPONSE in invalid state"));
            return;
        }

        Scanner response = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        response.next();
        // No client ID
        if (!response.hasNextInt()) {
            Console.warn(tag("Receieved truncated RESPONSE (expected id)"));
            return;
        }
        // Client spoofing? Sure, why not.
        int id = response.nextInt();
        if (id != _client.id()) {
            Console.warn(tag("Receieved RESPONSE from wrong client: "
                             + id));
            return;
        }

        if (!response.hasNext()) {
            Console.warn(tag("Receieved truncated RESPONSE (expected res)"));
            return;
        }
        String resString = response.next();
        if (response.hasNext()) {
            Console.warn(tag("Extra bytes in RESPONSE"));
        }

        Console.debug(tag("Received RESPONSE"));
        Console.debug(tag("res = " + resString));

        byte[] res = DatatypeConverter.parseHexBinary(resString);
        if (Arrays.equals(res, _xres)) {
            _welcomeSock.send("AUTH_SUCCESS", _clientAddr, _clientPort);
            _client.setState(Client.State.AUTHENTICATED);
            Console.info(tag("AUTH_SUCCESS"));
        } else {
            _welcomeSock.send("AUTH_FAIL", _clientAddr, _clientPort);
            _client.setState(Client.State.OFFLINE);
            Console.info(tag("AUTH_FAIL"));
        }
    }

    // =========================================================================
    // WelcomeSocket IO
    // =========================================================================

    /* Takes the next UDP datagram from the work queue.
     *
     * @return The next UDP datagram in the queue.
     */
    private DatagramPacket udpTake() throws InterruptedException {
        return _udpRcvBuffer.take();
    }

    /* Enqueues a UDP datagram to the work queue.
     *
     * @param dgram The datagram to enqueue.
     */
    public void udpPut(DatagramPacket dgram) throws InterruptedException {
        _udpRcvBuffer.put(dgram);
    }

}
