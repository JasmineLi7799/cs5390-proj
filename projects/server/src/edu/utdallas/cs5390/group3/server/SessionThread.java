package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetAddress;
import java.io.IOException;
import java.io.InputStream;
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
        super(Server.instance().threadGroup(), "client session " + client.id());
        _client = client;
        _clientAddr = addr;
        _clientPort = port;
    }

    // =========================================================================
    // Thread Runtime Management
    // =========================================================================

    public void run() {
        Console.debug(tag("Session thread started."));
        int sessionID = _client.getSessionID(_client.id());
        System.out.println("========== The session id is " + sessionID);
        if (!this.connectToClient()) {
            this.exitCleanup();
            return;
        }

        //wrote by Jason//
        try {
            this.sendRegistered();
            _client.setState(Client.State.REGISTERED_SENT);
            System.out.println("==========");
            _client.getState();
            
            byte[] mesRev = _socket.readMessage();
            String message = new String (mesRev);
            System.out.println("The received message is "+ message);
            String[] msg = message.split("\\s+");
            if(msg[0].equals(new String("CONNECT"))){
            	System.out.println("Connect received");
            	
            }
           
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // TODO: everything
        // See HandshakeThread in client and server for inspiration on
        // how to structure this as a state machine.

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
        try {
            Console.debug(tag("Connecting to client at "
                              + _clientAddr.getHostAddress()
                              + ":" + _clientPort + "..."));
            _socket = new SessionSocket(_client, _clientAddr, _clientPort);
            Console.debug(tag("Established TCP session to client."));

        } catch (SocketTimeoutException e) {
            Console.error(tag("Timeout while establishing TCP session with "
                              + "client."));
            try {
                _client.setState(Client.State.OFFLINE);
            } catch (InterruptedException ie) {
            }
            return false;
        } catch (Exception e) {
            Console.error(tag("While creating socket: " + e));
            return false;
        }

        _client.setSocket(_socket);

        Console.debug(tag("Connected to client "
                          + _client.id() + " on "
                          + _clientAddr.getHostAddress()
                          + ":" + _clientPort));

        return true;
    }

    /**
     * when the client type "history clientB_id"
     * check all the history from this client and return all the history for clientB
     */
    public String getHistory(InputStream _inStream){
    	String history = "";
    	String input = _inStream.toString();
    	String[] in = input.split(" ");

    	if(in[1]!=null){
    		int clientB = Integer.parseInt(in[1]);
    		history = _client.getHistory(clientB);
    	}
    	return history;
    }

    // wrote by Jason//
    private void sendRegistered() throws Exception{
        Console.debug(tag("Sending REGISTERED..."));
        byte[] msg = _socket.writeMessage("REGISTERED");
        _client.setState(Client.State.ONLINE);
    }
    

}
