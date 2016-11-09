package edu.utdallas.cs5390.group3.client;

import java.lang.String;

import java.util.concurrent.Semaphore;

import java.lang.InterruptedException;

public final class Client {
    public static enum State {
        START,
        HELLO_SENT,
        CHALLENGE_RECV,
        RESPONSE_SENT,
        AUTHENTICATED,
        REGISTERED
        // ...
    }

    private int _id;
    private String _privateKey;
    private State _state;
    private Semaphore _stateLock;

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

    public Client(int id, String privateKey) {
        // TODO: read this info from a config file, or command line
        // arguments, or whatever
        _id = id;
        _privateKey = privateKey;
        _stateLock = new Semaphore(1);
        _state = State.START;
    }

    public String hello() {
        return "HELLO " + _id;
    }
}
