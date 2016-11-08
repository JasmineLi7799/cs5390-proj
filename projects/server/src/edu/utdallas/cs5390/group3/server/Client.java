package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.String;
import java.net.InetAddress;

public final class Client {

    private int _id;
    private String _privateKey;
    private ClientConnection _connection;
    private ChatSession _chat;

    public int id() { return _id; }
    public ClientConnection connection() { return _connection; }

    /*
    public boolean isRegistered() {}
    public boolean hasChatSession() {}
    public ClientConnection register(InetAddress addres, String cryptKey) {}
    public String authenticate(String response) {}
    public void startChat(int chatId, Client partner) {}
    */

    public Client(int id, String k) {
        _id = id;
        _privateKey = k;
    }
}
