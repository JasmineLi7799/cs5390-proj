package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.lang.Thread;
import java.lang.ThreadGroup;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import java.net.SocketAddress;

import java.lang.NullPointerException;

public final class Server {
    private static Server _instance;

    private Thread _welcomeThread;
    private ThreadGroup _threadGroup;
    private boolean _haveShutdown;
    private static ConcurrentHashMap<Integer, Client> _clientDB;
    private ConcurrentHashMap<SocketAddress, ClientThread> _threadMap;

    private Server() {
        _threadGroup = new ThreadGroup("server");
        _haveShutdown = false;
        _threadMap = new ConcurrentHashMap<SocketAddress, ClientThread>();
    }

    /*  2 versions:
            instance()
                Can be used before configure file created,
                but doesn't initialize DB.
                Used in registerShutdownHook().
            instance(cfg)
                Initializes DB using config file info.  */
    public static Server instance() {
        if (_instance == null) {
            _instance = new Server();
        }
        return _instance;
    }
    public static Server instance(Config cfg) {
        if (_instance == null) {
            _instance = new Server();
        }
        if(_clientDB == null){
            initDB(cfg);
        }
        return _instance;
    }

    public ThreadGroup threadGroup() { return _threadGroup; }

    public void shutDown() {
        /* If someone already manually invoked shutDown (normal exit),
           don't do it again.  Calling shutDown() multiple times is
           safe, but it results in redundant console output. */
        if (_haveShutdown) return;

        Console.debug("Reaping threads...");
        _threadGroup.interrupt();
        Thread[] threads = getGroupThreads(_threadGroup);
        if (threads != null) {
            for (Thread thread : threads) {
                try {
                    Console.debug("Waiting for thread '"
                                + thread.getName() + "' to terminate...");
                    thread.join();
                    Console.debug("Thread '"
                                + thread.getName() + "' terminated.");
                } catch (InterruptedException e) {
                    Console.fatal("Shutdown thread interrupted. "
                                +"Server may not have exited cleanly.");
                    break;
                }
            }
            Console.debug("All threads killed.");
        }

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

    private static void initDB(Config cfg) {
        _clientDB = new ConcurrentHashMap<Integer, Client>();

        int[] uids = cfg.userIDs();
        String[] pks = cfg.privateKeys();
        
        for(int i=0; i<uids.length; i++){
            Client c = new Client(uids[i], pks[i]);
            _clientDB.put(c.id(), c);
        }
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

    private Thread[] getGroupThreads(final ThreadGroup group) {
        if (group == null)
            throw new NullPointerException("Null thread group");
        int nAlloc = group.activeCount();
        if (nAlloc == 0) return null;
        int n = 0;
        Thread[] threads;
        do {
            nAlloc *= 2;
            threads = new Thread[nAlloc];
            n = group.enumerate(threads);
        } while (n == nAlloc);
        return java.util.Arrays.copyOf(threads, n);
    }
}
