package edu.utdallas.cs5390.group3.client;

import java.lang.String;

import java.util.concurrent.Semaphore;

import java.lang.InterruptedException;

/* The Client class stores client information and represents the
 * client as a state machine that changes in response to inputs from
 * its ClientThread.
 *
 * The Client's state also controls whether and how other
 * ClientThreads are permitted to interact with the Client.  For
 * instance, when Client B attempts to send a message to Client A,
 * Client B's ClientThread will check whether Client A is in a valid
 * state to receive the message.
 */
public final class Client {
    // Basic client info.
    private int _id;
    private String _privateKey;

    // Client state and guard semaphore.
    private State _state;
    private Semaphore _stateLock;
    public static enum State {
        START,
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

    /* Creates a Client in the START state.
     *
     * @param id Client ID
     * @param privateKey Private key used for authentication and
     * encryption.
     */
    public Client(int id, String privateKey) {
        _id = id;
        _privateKey = privateKey;
        _stateLock = new Semaphore(1);
        _state = State.START;
    }

    // =========================================================================
    // Accessors/Mutators
    // =========================================================================

    public int id() { return _id; }
    public String privateKey() { return _privateKey; }

    public State state() throws InterruptedException {
        State retVal;
        _stateLock.acquire();
        retVal = _state;
        _stateLock.release();
        return retVal;
    }

    public void setState(State newState) throws InterruptedException {
        _stateLock.acquire();
        _state = newState;
        _stateLock.release();
    }

    // =========================================================================
    // Protocol messages
    // =========================================================================

    /* Generates the HELLO message payload.
     *
     * @return the HELLO message.
     */
    public String hello() {
        return "HELLO " + _id;
    }
}
