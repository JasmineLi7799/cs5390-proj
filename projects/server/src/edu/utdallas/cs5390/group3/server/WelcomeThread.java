package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.InetAddress;

import java.io.IOException;
import java.lang.InterruptedException;
import java.net.SocketException;

/* The WelcomeThread serves as a dispatcher for UDP datagrams received
 * on the WelcomeSocket.
 */
public final class WelcomeThread extends Thread {
    private WelcomeSocket _welcomeSock;

    // =========================================================================
    // Constructor
    // =========================================================================

    /* Default constructor */
    public WelcomeThread() {
        super(Server.instance().threadGroup(), "welcome");
        _welcomeSock = new WelcomeSocket();
    }

    // =========================================================================
    // WelcomeThread runtime maintenance
    // =========================================================================

    /* Interrupts the Welcome Thread.
     *
     * DatagramSocket.receive() does not throw InterruptedException.
     * So, to interrupt the WelcomeThread when it is blocking on this call,
     * we close the underlying socket, causing an immediate SocketException
     */
    @Override
    public void interrupt() {
        super.interrupt();
        _welcomeSock.close();
    }

    /* Main loop for the WelcomeSocket.
     *
     * UDP is stateless, but the handshake process is not. Thus, we need
     * to implement our own connection-oriented layer on top of UDP. This
     * is accomplished by maintaining a map of existing connections keyed
     * to the source (client) SocketAddress.
     *
     * For each incoming UDP datagram, the WelcomeThread checks whether
     * there is an associated HandshakeThread in the thread map. If so,
     * the datagram is forwarded to the HandshakeThread's work queue. If not,
     * The WelcomeThread spins a new HandshakeThread and maps it to the
     * client's SocketAddress, before forwarding it on.
     */
    @Override
    public void run() {
        // It may not be possible to open the requested port.
        // For instance: a port conflict or a non-local
        // bind_address in the server config file.
        if (!_welcomeSock.open()) {
            return;
        }

        // Keep pulling datagrams out of the socket until something
        // interrupts the thread.
        while (!Thread.interrupted()
               && !_welcomeSock.isClosed()) {

            DatagramPacket dgram = _welcomeSock.receive();
            if (dgram == null) {
                // IOException occurred. Abort this iteration.
                continue;
            }

            // Check if the datagram is associated with an existing
            // HandshakeThread.
            SocketAddress sockAddr = dgram.getSocketAddress();
            HandshakeThread hsThread =
                _welcomeSock.findThread(sockAddr);

            if (hsThread == null) {
                // If not, create one.
                hsThread = new HandshakeThread(sockAddr, _welcomeSock);
                // And add the thread to the map so we know where to
                // forward future datagrams.
                _welcomeSock.mapThread(sockAddr, hsThread);
                hsThread.start();
            }

            // Forward the datagram to its HandshakeThread.
            try {
                hsThread.udpPut(dgram);
            } catch (InterruptedException e) {
                // Unecessary since we're at the bottom of the loop,
                // anyway, but this "futureproofs" against changes to
                // the WelcomeThread loop.
                break;
            }
        }

        // Close up shop cleanly.
        if (!_welcomeSock.isClosed()) {
            _welcomeSock.close();
        }
    }
}
