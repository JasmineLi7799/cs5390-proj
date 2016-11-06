package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;
import java.util.concurrent.ConcurrentHashMap;

public final class Server {

    private static Server _instance;
    private Thread _welcomeThread;
    private boolean _haveShutdown;
    private ConcurrentHashMap<Integer, Client> _clientDB;

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
        if (_welcomeThread.isAlive()) {
            Console.info("Sending shutdown signal to welcome thread...");
            _welcomeThread.interrupt();
            Console.info("Waiting for welcome thread to terminate...");
            try {
                _welcomeThread.join();
            } catch (InterruptedException e) {
                Console.fatal("Shutdown handler interrupted while trying to shutdown.");
                Console.fatal("Server may not have shutdown cleanly.");
            }
        }
        Console.info("Server terminated.");
        _haveShutdown = true;
    }

    private Server() {
        _haveShutdown = false;
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
        return _clientDB.get(id);
    }
}
