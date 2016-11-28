package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetAddress;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Map;

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
        _client.sessionIDisTrue(sessionID);
        System.out.println("========The sessionID state is " + _client.getSessionIDstate(sessionID));
    
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
            System.out.println("The client state is "+ _client.getState());
            
            
            byte[] mesRev = _socket.readMessage();
            String s = _socket._socket.getRemoteSocketAddress().toString();
            System.out.println("=============================xingzhe" + s );
            String message = new String (mesRev);
            System.out.println("The received message is abc "+ message);
            String[] msg = message.split("\\s+");
            if(msg[0].equals(new String("CONNECT"))){
            	System.out.println("Connect Message received");
            }
            
            // send start msg to client
            String chatID = msg[1];
            String startMsg1 = "START " + sessionID + " " + chatID;
            System.out.println("The start1 is "+ startMsg1);
            String startMsg2 = "START " + sessionID + " " + Integer.toString(_client.id());
            System.out.println("The start2 is "+ startMsg2);
            
//            _socket.writeMessage(startMsg1);
            
            if(Server.getClient(Integer.parseInt(chatID)).getState().equals(new String("REGISTERED_SENT"))){
            	_socket.writeMessage(startMsg1);
            	_socket.getSocket(chatID).writeMessage(startMsg2);
            	_client.setState(Client.State.ACTIVE_CHAT);
            	Server.getClient(Integer.parseInt(chatID)).setState(Client.State.ACTIVE_CHAT);
            }else{
            	String unreachMsg = "UNREACHABLE " + chatID;
            	_socket.writeMessage(unreachMsg);
            }
            System.out.println("++++++");
            System.out.println("=========================The clientB addr: "+ _socket.getSocket(chatID)._socket.getRemoteSocketAddress());
            Thread chat1 = new Thread(new ChatThread(_socket, chatID, sessionID));
            chat1.start();
            Thread chat2 = new Thread(new ChatThread(_socket.getSocket(chatID), Integer.toString(_client.id()), sessionID));
            chat2.start();
            
//            while(true){
//            	synchronized(this) {
//            		 byte[] chatCotent = _socket.readMessage();
//                     String content = new String(chatCotent);
//                     if(content.substring(0, 11).equals(new String("END_REQUEST"))) {
//                    	 System.out.println("+++++ revceive");
//                    	 break;
//                     }
//                     System.out.println("The chat content received from client A is "+ content);
//                     // send content to client B
//                     _socket.getSocket(chatID).writeMessage(content);
//                     
//            	}
//            	
//            }
//            System.out.println("The client enter End Chat");
//            String endChat = "END_NOTIF " + sessionID;
//            System.out.println("The end notification msg is "+ endChat);
//            _socket.writeMessage(endChat);
//            System.out.println("The end chat msg has sent to client");
           
        } catch (Exception e) {
            // TODO Auto-generated catch blosck
            e.printStackTrace();
        }

        // TODO: everything
        // See HandshakeThread in client and server for inspiration on
        // how to structure this as a state machine.

//        this.exitCleanup();
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
            System.out.println("id "+ _client.id() + "addr "+ _clientAddr.toString() + "port "+ _clientPort);
            System.out.println("================Remoteaddr: " + _socket._socket.getRemoteSocketAddress().toString());
            _socket.setSocket(_client.id(), _socket);
            System.out.println("================id: "+ _socket.getSocket(Integer.toString(_client.id()))._socket.getRemoteSocketAddress().toString());
            
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
