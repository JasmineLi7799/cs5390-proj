package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.String;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public final class ClientSocket {

    private Socket _socket;
    private String _cryptKey;
    private Semaphore _sendLock;

    public ClientSocket(InetAddress addr, String cryptKey) {
        _cryptKey = cryptKey;
        _sendLock = new Semaphore(1);
    }

    /*
    public send(String data) {}
    public String receive() {}
    */
}
