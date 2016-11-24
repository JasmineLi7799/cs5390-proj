package edu.utdallas.cs5390.group3.client;

import java.lang.ThreadGroup;

import java.lang.InterruptedException;

import javax.crypto.spec.SecretKeySpec;

/* The Client class stores client information and represents the
 * client as a state machine that changes in response to inputs from
 * various network threads.
 */
public final class Client {
    // Basic client info.
    private int _id;
    private String _privateKey;
    private SecretKeySpec _cryptKey;

    // Configuration node
    public Config config;

    // Runtime management
    private static Client _instance;
    private ThreadGroup _threadGroup;

    // Client state and guard semaphore.
    private State _state;
    public static enum State {
        OFFLINE,
        HELLO_SENT,
        CHALLENGE_RECV,
        RESPONSE_SENT,
        REGISTER_SENT,
        //JASON//
        REGISTERED,
        ACTIVE_CHAT
        // ...
    }

    private SessionSocket _sessionSock;

    // =========================================================================
    // Constructor & instance accessor. Initialization
    // =========================================================================

    /* Creates a Client in the unconfigured, OFFLINE state. */
    private Client() {
        _state = State.OFFLINE;
        _threadGroup = new ThreadGroup("client");
        this.config = null;
    }

    /* Gets the client instance, creating it if necessary.
     *
     * @return The client instance.
     */
    public static Client instance() {
        if (_instance == null) {
            _instance = new Client();
        }
        return _instance;
    }

    /* Initializes the Config node and configures the client.
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

        _id = this.config.clientId();
        _privateKey = this.config.privateKey();
    }

    // =========================================================================
    // Accessors/Mutators
    // =========================================================================

    public int id() { return _id; }
    public String privateKey() { return _privateKey; }
    public SecretKeySpec cryptKey() { return _cryptKey; }
    public SessionSocket sessionSock() { return _sessionSock; }

    /* Convenience accessor for the named "client" ThreadGroup. */
    public ThreadGroup threadGroup() {
        return _threadGroup;
    }

    public State state() throws InterruptedException {
        State retVal;
        synchronized(_state) {
            retVal = _state;
        }
        return retVal;
    }

    public void setState(State newState) throws InterruptedException {
        synchronized(_state) {
            _state = newState;
        }
    }

    public void setCryptKey(byte[] key) {
        _cryptKey = new SecretKeySpec(key, "AES");
    }

    public void setSessionSock(SessionSocket sock) {
        _sessionSock = sock;
    }
    
    public void getState(){
    	String s = _state.toString();
    	System.out.println("The current state is " + s);
    }
}
