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
    private ThreadGroup _threadGroup;

    // Client state and guard semaphore.
    private State _state;
    public static enum State {
        OFFLINE,
        HELLO_SENT,
        CHALLENGE_RECV,
        RESPONSE_SENT,
        AUTHENTICATED,
        REGISTERED
        // ...
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    /* Creates a Client in the OFFLINE state.
     *
     * @param cfg Client configuration object.
     */
    public Client(Config cfg) {
        this.config = cfg;
        _id = this.config.clientId();
        _privateKey = this.config.privateKey();
        _state = State.OFFLINE;
        _threadGroup = new ThreadGroup("client");
    }

    // =========================================================================
    // Accessors/Mutators
    // =========================================================================

    public int id() { return _id; }
    public String privateKey() { return _privateKey; }
    public SecretKeySpec cryptKey() { return _cryptKey; }

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
}
