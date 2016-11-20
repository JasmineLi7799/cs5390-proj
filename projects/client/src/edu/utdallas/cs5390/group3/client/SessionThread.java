package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

public final class SessionThread extends Thread {
    SessionSocket _socket;
    Client _client;

    public SessionThread(Client c) {
        _client = c;
    }

    public void run() {
        Console.debug("Session thread started.");
        try {
            _socket = new SessionSocket(_client);
        } catch (SocketTimeoutException e) {
            Console.error("Timeout while waiting for REGISTERED response "
                          + "from server. Check your settings and retry your "
                          + "log on.");
            try {
                _client.setState(Client.State.OFFLINE);
            } catch (InterruptedException ie) {
            }
            exitCleanup();
            return;
        } catch (Exception e) {
            Console.error("While creating SessionSocket: " + e);
            exitCleanup();
            return;
        }

        Console.debug("Got connection from server.");
    }

    private void exitCleanup() {
        Console.debug("Session thread terminating.");
    }
}
