package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetAddress;

import java.io.IOException;
import java.net.SocketTimeoutException;

public final class SessionThread extends Thread {
    SessionSocket _socket;
    Client _client;
    InetAddress _clientAddr;
    int _clientPort;

    // =========================================================================
    // Constructor
    // =========================================================================

    public SessionThread(Client client, InetAddress addr, int port) {
        _client = client;
        _clientAddr = addr;
        _clientPort = port;
    }

    // =========================================================================
    // Thread Runtime Management
    // =========================================================================

    public void run() {
        if (!this.connectToClient()) {
            this.exitCleanup();
            return;
        }

        try {
            this.sendRegister();
        } catch (IOException e) {
            Console.error(tag("While sending REGISTER: " + e));
            this.exitCleanup();
            return;
        } catch (InterruptedException ie) {
            this.exitCleanup();
            return;
        }

        try {
            this.requestLoop();
        } catch (InterruptedException e) {
            this.exitCleanup();
            return;
        }

        this.exitCleanup();
    }

    /* Thread cleanup tasks */
    private void exitCleanup() {
        _client.setSocket(null);
        try {
            _client.setState(Client.State.OFFLINE);
        } catch (InterruptedException e) {
            // Nothing to do; we were just about to exit anyway.
        }
        Console.debug(tag("Session thread terminating."));
    }

    // =========================================================================
    // Output helper
    // =========================================================================

    /* Tags output messages with a session thread identifier.
     *
     * @param msg The message to tag.
     */
    private String tag(String msg) {
        return "[Session client=" + _client.id() + "] " + msg;
    }

    // =========================================================================
    // Client Interactions
    // =========================================================================

    /* Establishes the TCP session to the client
     *
     * @return True on success, false on failure.
     */
    private boolean connectToClient() {
        Console.debug(tag("Session thread started."));
        try {
            _socket = new SessionSocket(_client, _clientAddr, _clientPort);
        } catch (SocketTimeoutException e) {
            Console.error(tag("Timeout while establishing session with "
                              + "client."));
            try {
                _client.setState(Client.State.OFFLINE);
            } catch (InterruptedException ie) {
            }
            exitCleanup();
            return false;
        } catch (Exception e) {
            Console.error(tag("While creating socket: " + e));
            exitCleanup();
            return false;
        }

        _client.setSocket(_socket);

        Console.debug(tag("Connected to client "
                          + _client.id() + " on "
                          + _clientAddr.getHostAddress()
                          + ":" + _clientPort));

        return true;
    }

    /* Sends REGISTERED message to client.
     *
     * @return True on success, False on failure.
     */
    private boolean sendRegister() throws InterruptedException, IOException {
        // TODO Send the registration message.

        // _client.setState(Client.State.REGISTERED);
        return true;
    }

    /* Fetches and processes protocol messages from the client until they
     * disconnect.
     */
    void requestLoop() throws InterruptedException {
        // TODO: everything
        // while (!Thread.interrupted()) {
        // // handle all the things.
        // }
    };
}
