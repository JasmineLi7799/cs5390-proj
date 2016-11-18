package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.util.concurrent.ConcurrentHashMap;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.net.SocketException;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

/* The WelcomeSocket is a thin wrapper around DatagramSocket.
 *
 * It maintains the thread map that maps a source (client)
 * SocketAddress to a ClientThread. This is used to statefully
 * dispatch UDP packets during the handshake process.
 *
 * It doesn't directly extend DatagramSocket because it implements
 * send(), receive(), and the constructor in a slightly different,
 * more convenient way.
 */
public final class WelcomeSocket {
    // These could be smaller, but the important thing is that no
    // valid handshake-related protocol message will ever be larger
    // than these buffers.
    private final static int RECV_BUF_SIZE = 1024;
    private final static int SEND_BUF_SIZE = 1024;

    private DatagramSocket _socket;

    private Server _server;

    // Maps source (client) SocketAddresses to ClientThread listeners
    // for stateful UDP dispatching.
    private ConcurrentHashMap<SocketAddress, ClientThread> _threadMap;

    // =========================================================================
    // Constructor
    // =========================================================================

    /* Default constructor */
    public WelcomeSocket() {
        _server = Server.instance();
        _threadMap = new ConcurrentHashMap<SocketAddress, ClientThread>();
    }

    // =========================================================================
    // Socket management
    // =========================================================================

    /* Opens and binds the WelcomeSocket.
     *
     * Obtains the bind address and port from Server.config
     *
     * @return True on success, false on failure.
     */
    public boolean open() {
        InetAddress addr = _server.config.bindAddr();
        int port = _server.config.bindPort();

        try {
            _socket = new DatagramSocket(port, addr);
        } catch (SocketException e) {
            Console.fatal("Exception while opening welcome port "
                          + addr.getHostAddress() + ":" + port
                          + ": " + e);
            return false;
        }
        Console.info("Listening for connections via UDP on "
                     + addr.getHostAddress() + ":" + port + ".");
        return true;
    }

    /* Closes the WelcomeSocket. */
    public void close() {
        if (!_socket.isClosed()) {
            String addr = _socket.getLocalAddress().getHostAddress();
            int port = _socket.getLocalPort();
            Console.info("Closed UDP port " + addr + ":" + port + ".");
            _socket.close();
        }
    }

    /* Is the socket closed?
     *
     * @return True if the socket is closed, false if it is open.
     */
    public boolean isClosed() {
        return _socket.isClosed();
    }

    // =========================================================================
    // Socket IO
    // =========================================================================

    /* Pulls a datagram off the wire.
     *
     * @return The packet retrieved from the socket, or null if an
     * error occurred.
     */
    public DatagramPacket receive() {
        byte[] buf = new byte[RECV_BUF_SIZE];
        DatagramPacket dgram = new DatagramPacket(buf, RECV_BUF_SIZE);
        try {
            _socket.receive(dgram);
        } catch (IOException e) {
            Console.error("Welcome port: caught IOExcpetion: " + e);
            return null;
        }
        return dgram;
    }

    /* Sends a String as a UDP datagram.
     *
     * @param data A String containing the payload.
     * @param addr Destination address.
     * @param port Destination port.
     */
    public void send(String data, InetAddress addr, int port)
        throws IOException {

        send(data.getBytes(StandardCharsets.UTF_8), addr, port);
    }

    /* Sends arbitrary data as a UDP datagram.
     *
     * @param data A byte array containing the payload.
     * @param addr Destination address.
     * @param port Destination port.
     */
    public void send(byte[] data, InetAddress addr, int port)
        throws IOException {

        DatagramPacket dgram
            = new DatagramPacket(data, data.length, addr, port);
        _socket.send(dgram);
    }

    // =========================================================================
    // ClientThread mapping service
    // =========================================================================
    //
    // These functions provide the client SocketAddress ->
    // ClientThread mapping used by WelcomeThread and ClientThread to
    // maintain a connection-oriented layer on top of UDP. I.e., this allows
    // the WelcomeThread to dispatch UDP datagrams to the relevant
    // ClientThread during the handshake process.

    /* Locates a ClientThread listener by the client's SocketAddress.
     *
     * Used to dispatch datagrams associated with an existing
     * connection to its associated ClientThread.
     *
     * @param id The SocketAddress to search for.
     *
     * @return Matching ClientThread object, if one was found.
     *
     * @throws NullPointerException Thrown if no match was found.
     */
    public ClientThread findThread(SocketAddress sockAddr) {
        ClientThread thread;
        try {
            thread = _threadMap.get(sockAddr);
        } catch (NullPointerException e) {
            return null;
        }
        return thread;
    }

    /* Maps a ClientThread listener to a client connection's
     * SocketAddress.
     *
     * Any future datagrams from this source socket will be forwarded
     * to the work queue of the specified ClientThread.
     *
     * @param sockAddr The socket address of the client connection.
     * @param thread The ClientThread listener.
     */
    public void mapThread(SocketAddress sockAddr, ClientThread thread) {
        _threadMap.put(sockAddr, thread);
    }

    /* Unmaps the ClientThread listener for a client connection's
     * SocketAddress.
     *
     * Removes the ClientThread mapping for the specified source
     * SocketAddress. Essentially, tells the WelcomeThread to "forget"
     * the connection.
     *
     * @param sockAddr The socket address of the client connection.
     */
    public ClientThread unmapThread(SocketAddress sockAddr) {
        return _threadMap.remove(sockAddr);
    }
}
