package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.InterruptedException;
import java.io.IOException;

import java.lang.Thread;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import java.util.concurrent.LinkedBlockingQueue;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.io.ByteArrayOutputStream;


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

        // Wait for a good HELLO packet.
        while (!Thread.interrupted()) {
            try {
                if (this.getHello()) break;
            } catch (InterruptedException e) {
                this.exitCleanup();
                return;
            }
        }

        // Try to send CHALLENGE
        if (!this.sendChallenge()) {
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

    /* Validates HELLO messages.
     *
     * @return True if a valid HELLO message was received.
     */
    private boolean getHello() throws InterruptedException {
        // Fetch the next datagram from the work queue.
        DatagramPacket dgram = this.udpTake();

        // Set up a scanner to parse the datagram.
        Scanner scan = new Scanner(new ByteArrayInputStream(
            dgram.getData(), 0, dgram.getLength()));
        if (scan == null) {
            return false;
        }

        // Is it a HELLO packet for a new client connection?
        if (scan.hasNext("HELLO")) {
            scan.next();
        } else {
            return false;
        }

        // Validate the format of the clientId...
        int clientId;
        if (scan.hasNextInt()) {
            clientId = scan.nextInt();
        } else {
            Console.warn("Received malformed HELLO (src="
                         + _clientAddr.getHostAddress()
                         + ":" + _clientPort + ")");
            return false;
        }

        // Then validate the content of the clientId...
        Client client = _server.findClientById(clientId);
        if (client == null) {
            Console.warn( "Received HELLO from unknown client: " + clientId
                         + " (src=" + _clientAddr.getHostAddress()
                         + ":" + _clientPort + ")");
            return false;
        }

        // All is well. Set the _client for this thread.
        _client = client;
        Console.info("Received HELLO from client: " + clientId
                     + " (src=" + _clientAddr.getHostAddress()
                     + ":" + _clientPort + ")");

        return true;
    }

    /* Generates a CHALLENGE message.
     *
     * @return True if CHALLENGE was succesfully sent.
     */
    private boolean sendChallenge() {
        // Generate secure random 32-bit value.
        byte[] rand = new byte[4];
        Cryptor.nextBytes(rand);

        // Construct the challenge packet payload.
        ByteArrayOutputStream challengeStream
            = new ByteArrayOutputStream();
        try {
            challengeStream.write("CHALLENGE".getBytes("UTF-8"));
            challengeStream.write(rand);
        } catch (IOException e) {
            // This exception shouldn't actually be possible.
            return false;
        }
        byte[] challenge = challengeStream.toByteArray();

        // Send the challenge
        try {
            Console.debug("Sending CHALLENGE to client "
                          + _client.id() + ".");
            _welcomeSock.send(challenge, _clientAddr, _clientPort);
        } catch (IOException e) {
            Console.error("In listener thread for client "
                          + _client.id()
                          + ", while sending CHALLENGE: "
                          + e);
            return false;
        }
        return true;
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
