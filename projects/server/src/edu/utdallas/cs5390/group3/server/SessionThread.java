package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetAddress;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Map;

import java.util.Scanner;
import java.nio.charset.StandardCharsets;

import java.util.LinkedList;

public final class SessionThread extends Thread {
    SessionSocket _socket;
    Client _client;
    Server _server;
    InetAddress _clientAddr;
    int _clientPort;

    // =========================================================================
    // Constructor
    // =========================================================================

    public SessionThread(Client client, InetAddress addr, int port) {
        super(Server.instance().threadGroup(), "client session " + client.id());
        _client = client;
        _server = Server.instance();
        _clientAddr = addr;
        _clientPort = port;
    }

    // =========================================================================
    // Thread Runtime Management
    // =========================================================================

    @Override
    public void interrupt() {
        Console.debug("Interrupted.");
        _socket.close();
        super.interrupt();
    }

    @Override
    public void run() {
        Console.debug(tag("Session thread started."));

        // Try to connect to the client, or die.
        if (!this.connectToClient()) {
            this.exitCleanup();
            return;
        }

        if (!_socket.writeMessage("REGISTERED")) {
            this.exitCleanup();
            return;
        }
        try {
            _client.setState(Client.State.ONLINE);
        } catch (InterruptedException e) {
            this.exitCleanup();
            return;
        }

        while(!Thread.interrupted() && !_socket.isClosed()) {
            try {
                String message = _socket.readMessage();
                if (message == null) {
                    Console.error(tag("Lost communication with client."));
                    break;
                }

                if (message.matches("^CONNECT [1-9][0-9]*")) {
                    if (!this.handleConnect(message)) break;
                }

                else if (message.matches("^END_REQUEST [0-9]+")) {
                    if (!this.handleEndRequest(message)) break;
                }

                else if (message.matches("^CHAT [0-9]+ \\S.*$")) {
                    if (!this.handleChat(message)) break;
                }

                else if (message.matches("^HISTORY_REQ [0-9][1-9]*$")) {
                    if (!this.handleHistoryReq(message)) break;
                }

                else {
                    Console.debug(tag("Received Unknown message from client: "
                                      + message));
                    break;
                }

            } catch (InterruptedException e) {
                this.exitCleanup();
                break;
            }
        }

        this.exitCleanup();
    }

    /* Thread cleanup tasks */
    private void exitCleanup() {
        _client.setSocket(null);
        try {
            // Terminate any chat sessions associated with this client.
            if (_client.state() == Client.State.ACTIVE_CHAT) {
                ChatSession chatSess = _client.chatSession();
                Client partner = chatSess.partner(_client);
                SessionSocket partnerSock = partner.socket();
                partnerSock.writeMessage("END_NOTIF " + chatSess.id());
                partner.setState(Client.State.ONLINE);
                partner.setChatSession(null);
                _server.unmapChatSession(chatSess.id());
            }
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
                return false;
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

    private boolean handleConnect(String message) throws InterruptedException {
        Scanner scan = new Scanner(message);
        // Skip over CONNECT
        scan.next();
        int clientBId = scan.nextInt();
        // silently refuse to let a client CONNECT to itself.
        if (_client.id() == clientBId) {
            return true;
        }

        Client clientB = _server.findClient(clientBId);
        if (clientB == null) {
            if (!_socket.writeMessage("UNREACHABLE " + clientBId)) {
                return false;
            }
            return true;
        }

        if (clientB.state() == Client.State.ACTIVE_CHAT) {
            if (!_socket.writeMessage("UNREACHABLE " + clientBId)) {
                return false;
            }
            return true;
        }
        if (clientB.state() != Client.State.ONLINE) {
            if (!_socket.writeMessage("UNREACHABLE " + clientBId)) {
                return false;
            }
            return true;
        }

        ChatSession chatSess = new ChatSession(_server.nextChatId(),
                                               _client, clientB);
        _client.setChatSession(chatSess);
        clientB.setChatSession(chatSess);
        _server.mapChatSession(chatSess.id(), chatSess);

        _client.setState(Client.State.ACTIVE_CHAT);
        clientB.setState(Client.State.ACTIVE_CHAT);
        SessionSocket socketB = clientB.socket();
        if (!_socket.writeMessage(
                "START " + chatSess.id() + " " + clientBId)) {
            return false;
        }
        if (!socketB.writeMessage(
                "START " + chatSess.id() + " " + _client.id())) {
            return false;
        }

        return true;
    }

    public boolean handleEndRequest(String message) throws InterruptedException {
        if (_client.state() != Client.State.ACTIVE_CHAT) {
            Console.debug("Received END_REQUEST from client in invalid state: "
                          + _client.state());
            return false;
        }
        ChatSession chatSess = _client.chatSession();
        Client partner = chatSess.partner(_client);
        SessionSocket partnerSock = partner.socket();
        _socket.writeMessage("END_NOTIF " + chatSess.id());
        partnerSock.writeMessage("END_NOTIF " + chatSess.id());
        _client.setState(Client.State.ONLINE);
        partner.setState(Client.State.ONLINE);
        _client.setChatSession(null);
        partner.setChatSession(null);
        _server.unmapChatSession(chatSess.id());
        return true;
    }

    public boolean handleChat(String message) throws InterruptedException {
        if (_client.state() != Client.State.ACTIVE_CHAT) {
            Console.debug("Received CHAT from client in invalid state: "
                          + _client.state());
            return false;
        }
        Scanner scan = new Scanner(message);
        // Skip over "CHAT "
        scan.next();
        int chatSessionId = scan.nextInt();
        if (_client.chatSession().id() != chatSessionId) {
            Console.debug("Received CHAT command for invalid chat session id: "
                          + chatSessionId);
            return false;
        }

        String clientMsg = scan.nextLine();
        Client partner = _client.chatSession().partner(_client);
        partner.addHistory(_client.id(), clientMsg);
        SessionSocket partnerSock = partner.socket();
        return partnerSock.writeMessage("CHAT " + chatSessionId + clientMsg);
    }

    public boolean handleHistoryReq(String message) throws InterruptedException {
        if (!(_client.state() == Client.State.ONLINE
              || _client.state() == Client.State.ACTIVE_CHAT)) {
            Console.debug("Received CHAT from client in invalid state: "
                          + _client.state());
            return false;
        }

        Scanner scan = new Scanner(message);
        // Skip over "HISTORY_REQ "
        scan.next();
        int partnerId = scan.nextInt();

        LinkedList<String> history = _client.getHistory(partnerId);
        if (history != null) {
            for (String msg : history) {
                _socket.writeMessage("HISTORY_RESP " + partnerId + msg);
            }
        }
        return true;
    }

}
