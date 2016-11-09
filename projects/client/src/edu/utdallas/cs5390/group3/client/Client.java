package edu.utdallas.cs5390.group3.client;

import java.lang.String;

public final class Client {
    private int _id;
    private String _privateKey;

    public int id() { return _id; }
    public String privateKey() { return _privateKey; }

    public Client(int i) {
        // TODO: read this info from a config file, or command line
        // arguments, or whatever
        _id = i;
        _privateKey = "foo";
    }

    public String hello() {
        return "HELLO " + _id;
    }
}
