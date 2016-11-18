package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.InterruptedException;
import java.io.IOException;

import java.lang.Thread;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.LinkedBlockingQueue;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;


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
        Console.debug("Spun new client listener thread for "
                      + _clientAddr.getHostAddress() + ":" + _clientPort + ".");

        try {
            if (!this.doHandshake()) {
                this.exitCleanup();
                return;
            }
        } catch (InterruptedException e) {
        }

        this.exitCleanup();
    }

    /* Performs cleanup tasks whenever the thread exits.
     *
     * Primarily, this unmaps the thread from the WelcomeSocket's
     * thread map so that no future UDP datagrams will be routed here.
     */
    private void exitCleanup() {
        Console.debug("Listener thread for " + _clientAddr.getHostAddress()
                      + ":" + _clientPort + " is exiting.");
        if (_welcomeSock.unmapThread(_clientSockAddr) == null) {
            Console.warn("Tried to unmap listener thread for "
                         + _clientAddr.getHostAddress()
                         + ":" + _clientPort
                         + ", but it was not found in the thread map.");
        }
    }

    // =========================================================================
    // Protocol message parsing
    // =========================================================================

    /* Fetches and processess UDP datagrams from the work queue until
     * the handshake is complete.
     *
     * @return False if a non-continuable error has occurred (ex:
     * broken WelcomeSocket).
     */
    private boolean doHandshake() throws InterruptedException {
        while (!Thread.interrupted()) {
            DatagramPacket dgram;
            // Fetch the next datagram from the work queue.
            dgram = this.udpTake();

            // Set up a scanner to parse the datagram.
            Scanner msg = new Scanner(new ByteArrayInputStream(
                dgram.getData(), 0, dgram.getLength()));
            if (!msg.hasNext()) {
                Console.warn("Received empty datagram (src="
                            + _clientAddr.getHostAddress()
                            + ":" + _clientPort + ")");
                continue;
            }

            // Try to handle the message.
            if (!this.handle(msg)) {
                return false;
            }

            // Handshake is done when the client reaches the
            // authenticated state.
            if (_client != null
                && _client.state() == Client.State.AUTHENTICATED) {
                break;
            }
        }
        return true;
    }

    /* Parses the protocol message and takes an appropriate action
     * based on the message and the Client state.
     *
     * @param msg A Scanner object encapsulating the message.
     *
     * @return False if a non-continuable error has occurred (ex:
     * broken WelcomeSocket).
     */
    private boolean handle(Scanner msg) throws InterruptedException {
        switch (msg.next()) {
        case "HELLO":
            if (this.getHello(msg)) {
                try {
                    this.sendChallenge();
                } catch (IOException e) {
                    Console.error("In listener thread for client "
                                    + _client.id()
                                    + ", while sending CHALLENGE: "
                                    + e);
                    return false;
                }

                // TODO: finish handshake.
                _client.setState(Client.State.AUTHENTICATED);
                Thread.sleep(5000);
            }
            break;
        default:
            Console.warn("Received unrecognized message (src="
                        + _clientAddr.getHostAddress()
                        + ":" + _clientPort + ")");
        }
        return true;
    }

    /* Validates and processess HELLO messages.
     *
     * @param msg Scanner object encapsulating the message.
     *
     * @return True if a valid HELLO was received.
     */
    private boolean getHello(Scanner msg) throws InterruptedException {
        // Validate the format of the clientId...
        int clientId;
        if (msg.hasNextInt()) {
            clientId = msg.nextInt();
        } else {
            Console.warn("Received malformed HELLO (src="
                         + _clientAddr.getHostAddress()
                         + ":" + _clientPort + ")");
            return false;
        }

        // Then validate the content of the clientId...
        Client client = _server.findClient(clientId);
        if (client == null) {
            Console.warn( "Received HELLO from unknown client: " + clientId
                         + " (src=" + _clientAddr.getHostAddress()
                         + ":" + _clientPort + ")");
            return false;
        }

        // Client is valid. Set the _client for this thread.
        _client = client;

        // TODO: It's not really clear what we should do in this case
        // since the protocol doesn't define a way to inform a client
        // that the server thinks they are already connected.
        //
        // Resetting the client state seems like a reasonable thing to do
        // at first glance, but the client isn't authenticated yet.
        // Meaning, anyone could spoof a HELLO to blow clients offline.
        //
        // In any case, resetting the client state could get messy.
        // For now, let's defer until we have working chat, etc. Right
        // now we don't even know what we'll have to work with.

        // Ignore duplicate HELLO.
        if (_client.state() != Client.State.START) {
            Console.warn( "Received duplicate HELLO from client: " + clientId
                        + " (src=" + _clientAddr.getHostAddress()
                        + ":" + _clientPort + ")");
            return false;
        }

        Console.info("Received HELLO from client: " + clientId
                     + " (src=" + _clientAddr.getHostAddress()
                     + ":" + _clientPort + ")");

        _client.setState(Client.State.HELLO_RECV);
        return true;
    }

    /* Generates a CHALLENGE message. */
    private void sendChallenge()
        throws InterruptedException, IOException {
        // Generate secure random 32-bit value.
        byte[] rand = new byte[4];
        Cryptor.nextBytes(rand);

        // Construct the challenge packet payload.
        ByteArrayOutputStream challengeStream
            = new ByteArrayOutputStream();
        challengeStream.write("CHALLENGE ".getBytes(
            StandardCharsets.UTF_8));
        challengeStream.write(rand);
        byte[] challenge = challengeStream.toByteArray();

        // Send the challenge
        Console.debug("Sending CHALLENGE to client "
                        + _client.id() + ".");
        _welcomeSock.send(challenge, _clientAddr, _clientPort);

        _client.setState(Client.State.CHALLENGE_SENT);
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
