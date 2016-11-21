package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;

import java.net.ServerSocket;
import java.net.Socket;

import java.net.InetSocketAddress;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.Queue;
import java.util.LinkedList;

public final class SessionSocket {
    private Client _client;
    private Socket _socket;
    private Queue<byte[]> _msgQueue;

    public SessionSocket(Client c)
        throws SocketException, IOException, SocketTimeoutException {
        _client = c;

        ServerSocket bindSock = new ServerSocket();
        bindSock.setSoTimeout(_client.config.timeoutInterval());
        bindSock.bind(new InetSocketAddress(
                          _client.config.clientAddr(),
                          _client.config.clientPort()));
        Console.debug("Listening for server connection on "
                      + bindSock.getInetAddress().getHostAddress()
                      + ":" + bindSock.getLocalPort());
        _socket = bindSock.accept();
        Console.debug("Accepting connection from server at "
                      + _socket.getInetAddress().getHostAddress()
                      + ":" + _socket.getPort());

        _msgQueue = new LinkedList<byte[]>();
    }
}
