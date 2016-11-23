package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.Semaphore;

public final class SessionThread extends Thread {
    SessionSocket _socket;
    Client _client;
    public Semaphore initLock;

    public SessionThread() {
        _client = Client.instance();
        this.initLock = new Semaphore(0);
    }

    public void run() {
        Console.debug("Session thread started.");
        try {
            _socket = new SessionSocket();
            // Note: this call releases initLock, signaling to the handshake
            // thread that it is safe to send the REGISTER message now.
            //
            // Without this, it *is* possible for the server to respond
            // before we get the TCP port open when the server and client
            // are communicating over loopback.
            _socket.waitForConnection(initLock);
            Console.debug("Got connection from server.");
            _client.setSessionSock(_socket);

            byte[] receiveRegistered = _socket.readMessage();
            String registered = new String(receiveRegistered);
            if(registered.equals(new String("REGISTERED"))) {
                Console.info("Received REGISTERED.");
                _client.setState(Client.State.REGISTERED);
            }
        } catch (SocketTimeoutException e) {
            Console.error("Timeout while waiting for REGISTERED response "
                          + "from server. Check your settings and retry your "
                          + "log on.");
            try {
                _client.setState(Client.State.OFFLINE);
            } catch (InterruptedException ie) {
            }
            this.exitCleanup();
            return;
        } catch (Exception e) {
            Console.error("While creating SessionSocket: " + e);
            e.printStackTrace();
            this.exitCleanup();
            return;
        }

        this.exitCleanup();
    }


    private void exitCleanup() {
        _client.setSessionSock(null);
        if (_socket != null) _socket.close();
        try {
            _client.setState(Client.State.OFFLINE);
        } catch (InterruptedException e) {
            // Nothing to do about this; we're exiting anyway.
        }
        Console.debug("Session thread terminated.");
    }
}
