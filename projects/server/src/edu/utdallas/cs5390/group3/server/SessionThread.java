package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetAddress;

import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.xml.bind.DatatypeConverter;

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
        //wrote by Jason//
        try {
			this.sendRegistered();
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
        Console.debug(tag("Session thread started."));
        try {
            _socket = new SessionSocket(_client, _clientAddr, _clientPort);
            System.out.println("tcp session success");
            
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
    
    
  // wrote by Jason//
    private void sendRegistered() throws Exception{
    	     Console.debug(tag("REGISTERED is going to send"));
    		 byte[] msg = _socket.writeMessage("REGISTERED");
    		 String ckeyString = DatatypeConverter.printHexBinary(msg);
    		 System.out.println("The sending msg is " + ckeyString );
    		 _client.setState(Client.State.ONLINE);
    	
    }
  

}
