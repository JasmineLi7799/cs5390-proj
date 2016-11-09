package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import java.net.SocketAddress;

import java.lang.NullPointerException;

public final class Server {

    private static Server _instance;
    private Thread _welcomeThread;
    private boolean _haveShutdown;
    private ConcurrentHashMap<Integer, Client> _clientDB;
    private ConcurrentHashMap<SocketAddress, ClientThread> _threadMap;

    private Server() {
        _haveShutdown = false;
        _threadMap = new ConcurrentHashMap<SocketAddress, ClientThread>();
        _clientDB = new ConcurrentHashMap<Integer, Client>();
        this.initDB();
    }

    public static Server instance() {
        if (_instance == null) {
            _instance = new Server();
        }
        return _instance;
    }

    // TODO: reap client connection threads.
    public void shutDown() {
        /* If someone already manually invoked shutDown (normal exit), don't do it again.
           Calling shutDown() multiple times is safe, but it results in redundant console
           output. */
        if (_haveShutdown) return;

        Console.debug("Reaping threads...");
        Thread.currentThread().getThreadGroup().interrupt();

        Console.info("Server terminated.");
        _haveShutdown = true;
    }

    public void spinWelcomeThread() {
        _welcomeThread = new WelcomeThread();
        _welcomeThread.start();
    }

    public boolean welcomeIsAlive() {
        return _welcomeThread.isAlive();
    }

    // TODO: populate database from config file.
    private void initDB() {
        Client c = new Client(1, "foo");
        _clientDB.put(c.id(), c);
        c = new Client(2, "bar");
        _clientDB.put(c.id(), c);
    }

    public Client findClientById(int id) {
        Client client;
        try {
            client = _clientDB.get(id);
        } catch (NullPointerException e) {
            return null;
        }
        return client;
    }

    public ClientThread findThreadBySocket(SocketAddress sockAddr) {
        ClientThread thread;
        try {
            thread = _threadMap.get(sockAddr);
        } catch (NullPointerException e) {
            return null;
        }
        return thread;
    }

    public void mapThread(SocketAddress sockAddr, ClientThread thread) {
        _threadMap.put(sockAddr, thread);
    }

    public ClientThread unMapThread(SocketAddress sockAddr) {
        return _threadMap.remove(sockAddr);
    }
}
