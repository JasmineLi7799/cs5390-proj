package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

public final class SessionThread extends Thread {
    SessionSocket _socket;
    Client _client;

    public SessionThread() {
        _client = Client.instance();
    }

    public void run() {
        Console.debug("Session thread started.");
        try {
            _socket = new SessionSocket();
            Console.debug("Got connection from server.");

            byte[] receiveRegistered = _socket.readMessage();
            String registered = new String(receiveRegistered);
            if(registered.equals(new String("REGISTERED"))) {
                Console.info("Received REGISTERED.");
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
            this.exitCleanup();
            return;
        }

        this.exitCleanup();
    }


    private void exitCleanup() {
        if (_socket != null) _socket.close();
        try {
            _client.setState(Client.State.OFFLINE);
        } catch (InterruptedException e) {
            // Nothing to do about this; we're exiting anyway.
        }
        Console.debug("Session thread terminated.");
    }
}
