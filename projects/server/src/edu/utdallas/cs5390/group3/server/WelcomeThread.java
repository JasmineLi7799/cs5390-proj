package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.InetAddress;

import java.io.IOException;
import java.lang.InterruptedException;
import java.net.SocketException;

public final class WelcomeThread extends Thread {
    private WelcomeSocket _welcomeSock;
    private Server _server;

    public WelcomeThread() {
        _welcomeSock = new WelcomeSocket();
        _server = Server.instance();
    }

    // For whatever reason, DatagramSocket.receive() does not throw
    // InterruptedException, so we cannot interrupt the welcome thread
    // if it happens to be doing this. The "standard" solution to this
    // is to hook the interrupt() method so that it also closes the
    // DatagramSocket, which will effectively interrupt the thread by
    // tossing an immediate SocketException.
    @Override
    public void interrupt() {
        super.interrupt();
        _welcomeSock.close();
    }

    @Override
    public void run() {

        if (!_welcomeSock.open()) {
            return;
        }

        // Keep pulling datagrams out of the socket until something
        // interrupts the thread.
        while (!Thread.interrupted()
               && !_welcomeSock.isClosed()) {

            DatagramPacket dgram = _welcomeSock.receive();
            if (dgram == null) {
                // IOException occurred. Abort this iteration.
                continue;
            }

            // Check if the datagram is associated with an existing
            // client thread.
            SocketAddress sockAddr = dgram.getSocketAddress();
            ClientThread cthread =
                _server.findThreadBySocket(sockAddr);
            if (cthread == null) {
                // If not, create one.
                cthread = new ClientThread(sockAddr, _welcomeSock);
                // And add the thread to the map so we know where to
                // forward future datagrams.
                _server.mapThread(sockAddr, cthread);
                cthread.start();
            }
            // Forward the datagram to its listener thread.
            try {
                cthread.udpPut(dgram);
            } catch (InterruptedException e) {
                // Not really necessary since we're about to hit the
                // bottom of the loop anyway, and the loop condition
                // will break on interrupt.
                break;
            }
        }

        // Close up shop cleanly.
        if (!_welcomeSock.isClosed()) {
            _welcomeSock.close();
        }
    }

}
