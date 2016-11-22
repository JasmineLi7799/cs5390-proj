package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Cryptor;

import java.lang.String;
import java.net.InetAddress;

import javax.crypto.spec.SecretKeySpec;

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
    private SecretKeySpec _cryptKey;

    private State _state;
    public static enum State {
        OFFLINE,
        HELLO_RECV,
        CHALLENGE_SENT,
        RESPONSE_RECV,
        AUTHENTICATED,
        REGISTERED,
        //JASON
        ONLINE,
        REGISTERED_SENT
        // ...
    }

    // The TCP socket associated with this Client when it has reached
    // the REGISTERED state. It is owned by the Client rather than its
    // ClientThread because multiple threads need access to the socket.
    //
    // For instance, suppose Client A sends a CHAT protocol message.
    // Client A's ClientThread will receive the message and relay
    // it to Client B's socket.
    private SessionSocket _socket;

    private ChatSession _chat;

    // =========================================================================
    // Constructor
    // =========================================================================

    /* Creates a Client in the OFFLINE state.
     *
     * @param id Client ID
     * @param k Private key used for authentication and
     * encryption.
     */
    public Client(int id, String k) {
        _id = id;
        _privateKey = k;
        _state = Client.State.OFFLINE;
    }

    // =========================================================================
    // Accessors/Mutators
    // =========================================================================

    public int id() { return _id; }
    public String privateKey() { return _privateKey; }
    public SecretKeySpec cryptKey() { return _cryptKey; }
    public SessionSocket socket() { return _socket; }

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

    public void setSocket(SessionSocket sock) {
        _socket = sock;
    }
}
