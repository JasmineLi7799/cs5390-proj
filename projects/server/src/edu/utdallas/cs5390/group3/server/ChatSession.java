package edu.utdallas.cs5390.group3.server;

import java.lang.String;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ChatSession {

    int _id;
    Client _clientA;
    Client _clientB;
    ConcurrentLinkedQueue<Message> _messages;

    public ChatSession(int id, Client clientA, Client clientB) {
        _id = id;
        _clientA = clientA;
        _clientB = clientB;
    }

    /*
    public sendMessage(Client sender, String message) {}
    public ConcurrentLinkedQueue<Message> history();
    public void terminate()
    */
}
