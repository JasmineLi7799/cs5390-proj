package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public final class SessionThread extends Thread {
    SessionSocket _socket;
    Client _client;
    public Semaphore initLock;

    public SessionThread() {
        super(Client.instance().threadGroup(), "session");
        _client = Client.instance();
        this.initLock = new Semaphore(0);
    }

    @Override
    public void interrupt() {
        Console.debug("Interrupted.");
        _socket.close();
        super.interrupt();
    }

    @Override
    public void run() {
        try {
            _socket = new SessionSocket();
            // Note: this call releases initLock, signaling to the handshake
            // thread that it is safe to send the REGISTER message now.
            //
            // Without this, it *is* possible for the server to respond
            // before we get the TCP port open when the server and client
            // are communicating over loopback.
            _socket.waitForConnection(initLock);
            _client.setSessionSock(_socket);

            String registered = _socket.readMessage();
            if (registered == null) {
                return;
            }
            if(registered.equals("REGISTERED")) {
                Console.info("You are now online. Type \"chat <client id>\""
                             + " to start a chat session.");
                _client.setState(Client.State.ONLINE);
            }
        } catch (SocketTimeoutException e) {
            Console.error("Timeout while waiting for REGISTERED response "
                          + "from server. Check your settings and retry your "
                          + "log on.");
            try {
                _client.setState(Client.State.OFFLINE);
            } catch (InterruptedException ie) {
                return;
            }
            this.exitCleanup();
            return;
        } catch (Exception e) {
            Console.error("While creating SessionSocket: " + e);
            e.printStackTrace();
            this.exitCleanup();
            return;
        }

        while (!Thread.interrupted() && !_socket.isClosed()) {
            try {
                String message = _socket.readMessage();
                if (message == null) {
                    break;
                }

                if (message.equals("REGISTERED")) {
                    if (!this.handleRegistered()) break;
                }

                else if (message.matches("^START [0-9]+ [1-9][0-9]*$")) {
                    if (!this.handleStart(message)) break;
                }

                else if (message.matches("^END_NOTIF [0-9]+$")) {
                    if (!this.handleEndNotify(message)) break;
                }

                else if (message.matches("^CHAT [0-9]+ \\S.*$")) {
                    if (!this.handleChat(message)) break;
                }

                else if (message.matches("^HISTORY_RESP [0-9][1-9]* \\S.*$")) {
                    if (!this.handleHistoryResp(message)) break;
                }

                else {
                    Console.error("Received unknown message: " + message);
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        this.exitCleanup();
    }

    private boolean handleRegistered() throws InterruptedException {
        if (_client.state() != Client.State.REGISTER_SENT) {
            Console.warn("Received REGISTERED from server while in invalid state.");
            return false;
        }
        _client.setState(Client.State.ONLINE);
        Console.info("You are now online. Type \"chat <client id>\" to start"
                     + " a chat session.");
        return true;
    }

    private boolean handleStart(String message) throws InterruptedException {
        if (!(_client.state() == Client.State.ONLINE
              || _client.state() == Client.State.WAIT_FOR_CHAT)) {
            Console.warn("Received START from server while in invalid state: "
                         + _client.state());
            return false;
        }
        Scanner scan = new Scanner(message);
        // Skip over "START "
        scan.next();
        int chatSessionId = scan.nextInt();
        int clientBId = scan.nextInt();
        _client.setChatSessionId(chatSessionId);
        _client.setChatPartnerId(clientBId);
        _client.setState(Client.State.ACTIVE_CHAT);
        Console.info("Chat started with client " + clientBId
                     + ". Type any message to chat or 'end chat' to"
                     + " end the chat session.");
        return true;
    }

    private boolean handleEndNotify(String message) throws InterruptedException {
        if (!(_client.state() == Client.State.ACTIVE_CHAT)) {
            Console.warn("Received END_NOTIF from server while in invalid state: "
                         + _client.state());
            return false;
        }
        Scanner scan = new Scanner(message);
        // Skip over "END_NOTIF "
        scan.next();
        int chatSessionId = scan.nextInt();
        if (chatSessionId != _client.chatSessionId()) {
            Console.error("Received END_NOTIF for chat session " + chatSessionId
                         + ", to which we are not a party.");
            return false;
        }
        Console.info("Chat session with client " + _client.chatPartnerId()
                     + " ended.");
        _client.setState(Client.State.ONLINE);
        return true;
    }

    private boolean handleChat(String message) throws InterruptedException {
        if (!(_client.state() == Client.State.ACTIVE_CHAT)) {
            Console.warn("Received CHAT from server while in invalid state: "
                         + _client.state());
            return false;
        }
        Scanner scan = new Scanner(message);
        // Skip over "CHAT "
        scan.next();
        int chatSessionId = scan.nextInt();
        if (chatSessionId != _client.chatSessionId()) {
            Console.error("Received CHAT for chat session " + chatSessionId
                         + ", to which we are not a party.");
            return false;
        }

        String clientMsg = scan.nextLine();
        Console.info("[client " + _client.chatPartnerId() + "]"
                     + clientMsg);
        return true;
    }

    private boolean handleHistoryResp(String message) throws InterruptedException {
        Scanner scan = new Scanner(message);
        // Skip over "HISTORY_RESP"
        scan.next();
        int partnerId = scan.nextInt();
        String chatMsg = scan.nextLine();

        Console.info("[History] From " + partnerId + ": " + chatMsg);
        return true;
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
