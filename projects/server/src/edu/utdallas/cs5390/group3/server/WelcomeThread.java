package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;

import java.net.DatagramPacket;

import java.io.IOException;
import java.lang.InterruptedException;
import java.net.SocketException;

import java.util.Scanner;
import java.io.ByteArrayInputStream;

public final class WelcomeThread extends Thread {
    private WelcomeSocket _socket;
    private Server _server;

    public WelcomeThread() {
        _socket = new WelcomeSocket();
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
        _socket.close();
    }

    @Override
    public void run() {

        if (!_socket.open()) {
            return;
        }

        // Keep pulling datagrams out of the socket until something
        // interrupts the thread.
        while (!Thread.interrupted()
               && !_socket.isClosed()) {

            DatagramPacket dgram = _socket.receive();
            if (dgram == null) {
                // IOException occurred. Abort this iteration.
                continue;
            }

            // Check if the UDP packet is associated with existing
            // client thread.
            ClientThread cthread = _server.findThreadByPort(dgram.getPort());
            if (cthread != null) {
                // If so, add it to the client thread's work queue...
                try {
                    cthread.udpPut(dgram);
                } catch (InterruptedException e) {
                    break;
                }
                // And go back to listening for more datagrams.
                continue;
            }

            // Otherwise, we need to parse the datagram and figure out
            // what it is. We'll do this with a Scanner.
            Scanner scan = new Scanner(
                new ByteArrayInputStream(dgram.getData(),
                                         0,
                                         dgram.getLength())
            );
            if (scan == null) continue; // IOException occurred.

            // Is it a HELLO packet for a new client connection?
            if (scan.hasNext("HELLO")) {
                scan.next();
                // If so, set up a listner thread for the client,
                // etc. etc.
                this.processHello(
                    scan,
                    dgram.getAddress().getHostAddress(),
                    dgram.getPort()
                );
                // And go back to listening for more datagrams.
                continue;
            }

            // If it's not a HELLO and it doesn't belong to an
            // existing handshake-in-progress, then it isn't valid,
            // so just drop the packet.
            Console.warn("Welcome thread: "
                            + "Received malformed packet from: "
                            + dgram.getAddress());
            // And go back to listening for more datagrams.
            continue;
        }

        // Close up shop cleanly.
        if (!_socket.isClosed()) {
            _socket.close();
        }
    }

    private void processHello(Scanner scan, String src, int port) {
        int clientId;

        // Validate the format of the clientId...
        if (scan.hasNextInt()) {
            clientId = scan.nextInt();
        } else {
            Console.warn("Welcome thread: "
                            + "Received malformed HELLO (src="
                            + src
                            + ")");
            return;
        }

        // Then validate the content of the clientId...
        Client client = _server.findClientById(clientId);
        if (client == null) {
            Console.warn("Welcome thread: "
                            + "Received HELLO from unknown client: "
                            + clientId
                            + " (src="
                            + src
                            + ")");
            return;
        }

        // All is well.
        Console.info("Welcome thread: Received HELLO from client: "
                        + clientId
                        + " (src="
                        + src
                        + ")");
        // Create a listener thread for this client.
        ClientThread thread = new ClientThread(client, port, _socket);
        // Associate the UDP src port with the listener thread so that
        // we can route related packets there in the future.
        _server.mapThread(port, thread);
        thread.run();
    }
}
