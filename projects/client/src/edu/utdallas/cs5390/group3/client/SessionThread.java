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
            // receive Registered
//            System.out.println("+++++++++++++++++");
            byte[] receiveRegistered = _socket.readMessage();
            String registered = new String(receiveRegistered);
//            System.out.println(registered);
//            System.out.println("=================");
            if(registered.equals(new String("REGISTERED"))) {
            	Console.info("Received Registered");
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

//        Console.debug("Got connection from server.");
        
//        Console.info("Waiting for REGISTERED...");
        
        
        this.exitCleanup();
    }
    

    private void exitCleanup() {
        Console.debug("Session thread terminating.");
    }
}
