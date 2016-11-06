package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.String;
import java.net.InetAddress;

public final class Client {

    private int _id;
    private String _privateKey;
    private ClientThread _thread;
    private ClientConnection _connection;
    private ChatSession _chat;

    public int id() { return _id; }
    public ClientConnection connection() { return _connection; }
    public ClientThread thread() { return _thread; }

    /*
    public bool isRegistered() {}
    public bool hasChatSession() {}
    public ClientConnection register(InetAddress addres, String cryptKey) {}
    public String Authenticate(String response) {}
    public void startChat(int chatId, Client partner) {}
    */

    public Client(int id, String k) {
        _id = id;
        _privateKey = k;
    }
}
