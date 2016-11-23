package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.util.concurrent.ConcurrentHashMap;

import java.lang.Thread;
import java.lang.ThreadGroup;

import java.util.Iterator;

import java.lang.NullPointerException;

/* The Server class is a Singleton that handles the large-scale
 * operations of the server (startup, shutdown, initial configuration)
 * and provides access to core data services needed throughout the
 * system (configuration node, client database).
 * 
 * 
 */
public final class Server {
    // Runtime management
    private static Server _instance;
    private boolean _running;
    private ThreadGroup _threadGroup;
    private Thread _welcomeThread;
    

    // Lookup services
    private ConcurrentHashMap<Integer, Client> _clientDB;
    public Config config;
    
    
    //history list to save all the history between clients,
    

    // =========================================================================
    // Constructor & instance accessor. Initialization
    // =========================================================================

    /* Creates the server instance. */
    private Server() {
        _threadGroup = new ThreadGroup("server");
        _running = false;
        this.config = null;
    }

    /* Gets the server instance, creating it if necessary.
     *
     * @return The server instance.
     */
    public static Server instance() {
        if (_instance == null) {
            _instance = new Server();
        }
        return _instance;
    }

    /* Initializes the Config node and Client database.
     *
     * @param configFileName Config file to parse.
     */
    public void configure(String configFileName)
        throws IllegalStateException, NullPointerException {
        if (config != null) {
            throw new IllegalStateException(
                "Multiple invocations of Server.configure()");
        }
        this.config = new Config(configFileName);

        _clientDB = new ConcurrentHashMap<Integer, Client>();
        int[] uids = this.config.userIDs();
        String[] pkeys = this.config.privateKeys();
        for(int i = 0; i < uids.length; i++) {
            Client c = new Client(uids[i], pkeys[i]);
            _clientDB.put(c.id(), c);
        }
    }


    // =========================================================================
    // Accessors/mutators
    // =========================================================================

    /* Convenience accessor for the named "server" ThreadGroup. */
    public ThreadGroup threadGroup() {
        return _threadGroup;
    }

    // =========================================================================
    // Start/stop/status
    // =========================================================================

    /* Starts the server.
     *
     * For now this entails spinning the welcome thread.
     */
    public void start() {
        // Safeguard against starting an already-running server. We
        // want this to blow up the server (at least for now) because
        // something this fundamental can only result from a severe
        // fault in the code.
        if (_running) {
            throw new IllegalStateException(
                "Server.start() called while server already running.");
        }
        if (this.config == null) {
            throw new IllegalStateException(
                "Server.start() called before Server.configure()");
        }
        _welcomeThread = new WelcomeThread();
        _welcomeThread.start();
        _running = true;
    }

    /* Stops the server.
     *
     * Interrupts and joins all threads in the "server" ThreadGroup.
     */
    public void stop() {
        // Silently ignore multiple invocations. Calling stop()
        // multiple times is safe, but generates duplicate console
        // spam.
        if (!_running) return;

        Console.debug("Reaping threads...");
        _threadGroup.interrupt();

        Thread[] threads = getGroupThreads(_threadGroup);
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

        Console.info("Server terminated.");
        _running = false;
    }

    /* Checks whether the server is still alive.
     *
     * The welcome thread can die asynchronously for various reasons
     * (ex. someone brings down the network interface the welcome
     * thread is listening on). Main.java needs this check in its
     * console loop.
     *
     * @return Server status.
     */
    public boolean isAlive() {
        return _welcomeThread.isAlive();
    }

    // =========================================================================
    // Lookup services
    // =========================================================================

    /* Locate a Client object by its id.
     *
     * Many protocol messages identify the client by id number, but of
     * course our code needs to operate on the Client object. This
     * serves as a globally accessable directory for client lookups.
     *
     * @param id The id to search for.
     *
     * @return Matching Client object, if one was found.
     *
     * @throws NullPointerException Thrown if no match was found.
     */
    public Client findClient(int id) {
        Client client;
        try {
            client = _clientDB.get(id);
        } catch (NullPointerException e) {
            return null;
        }
        return client;
    }

    // =========================================================================
    // Internal utility functions
    // =========================================================================

    /* Finds all threads in the given ThreadGroup.
     *
     * Used by stop() to iterate over the threads in the "server"
     * group and join() each one.
     *
     * Unfortunately, this is a bit more complicated than you'd
     * expect, so this was spun off into its own method to avoid
     * cluttering stop().
     *
     * @group The ThreadGroup to enumerate.
     *
     * @return An array containing all Threads in the ThreadGroup.
     *
     * @throws NullPointerException Thrown if you pass a null
     * ThreadGroup for some reason.
     */
    private Thread[] getGroupThreads(final ThreadGroup group) {
        if (group == null)
            throw new NullPointerException("Null thread group");

        // use activeCount() to make an initial guess at the thread
        // count (see below).
        int bufSize = group.activeCount();
        if (bufSize == 0) return new Thread[0];

        // Synchronization dilemma:
        // By the time activeCount() completes, let alone by the
        // time enumerate() completes, the count may be wrong!
        //
        // Solution: if enumerate() returns a full array, double the
        // size of the array and try again. Repeat until not full.
        int threadCount = 0;
        Thread[] threads;
        do {
            bufSize *= 2;
            threads = new Thread[bufSize];
            threadCount = group.enumerate(threads);
        } while (threadCount == bufSize);
        return java.util.Arrays.copyOf(threads, threadCount);
    }
}
