package edu.utdallas.cs5390.group3.server;

import java.lang.Thread;

import java.net.DatagramPacket;

import java.net.SocketException;
import java.io.IOException;

import java.util.HashMap;

import java.util.Scanner;
import java.io.ByteArrayInputStream;

public final class WelcomeThread extends Thread {
    private WelcomeSocket _socket;

    // Map from port number to Client for routing UDP datagrams
    // received by the welcome port to the appropriate client
    // thread.
    private HashMap<Integer, Client> _clientPortMap;

    public WelcomeThread() {
        _socket = new WelcomeSocket();
        _clientPortMap = new HashMap<Integer, Client>();
    }

    // Fun fact: DatagramSocket.receive() is a blocking call that does
    // not throw InterruptedException. Hence, the usual thread
    // interruption mechanism won't work at this point in the program
    // flow (and this is where WelcomeThread spends 99% of its
    // time...)
    //
    // Closing the socket will do the trick, since this will cause
    // DatagramSocket.receive() to immediately unblock and throw an
    // IOException. And we need to close the socket on our way out,
    // anyway.
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

            // We have a datagram. Now check what type of message it contains.
            Scanner scan = new Scanner(
                new ByteArrayInputStream(dgram.getData(), 0, dgram.getLength())
            );
            if (scan == null) continue; // IOException occurred.
            if (scan.hasNext("HELLO")) {
                scan.next();
                this.processHello(
                    scan,
                    dgram.getAddress().getHostAddress(),
                    dgram.getPort()
                );
            /*
            } elseif (scan.hasNext("ANOTHER_TYPE_OF_VALID_PACKET")) {
                // Do stuff...
            }
            */
            } else {
                Console.warn("Welcome thread: "
                             + "Received malformed packet from: "
                             + dgram.getAddress());
                continue;
            }
        }
        if (!_socket.isClosed()) {
            _socket.close();
        }
    }

    private void processHello(Scanner scan, String src, int port) {
        int clientId;
        if (scan.hasNextInt()) {
            clientId = scan.nextInt();
        } else {
            Console.warn("Welcome thread: "
                            + "Received malformed HELLO (src="
                            + src
                            + ")");
            return;
        }
        Client client = Server.instance().findClientById(clientId);
        if (client == null) {
            Console.warn("Welcome thread: "
                            + "Received HELLO from unknown client: "
                            + clientId
                            + " (src="
                            + src
                            + ")");
            return;
        }
        Console.info("Welcome thread: Received HELLO from client: "
                        + clientId
                        + " (src="
                        + src
                        + ")");
        /*
        // Associate this UDP src port with the client so that all
        // subsequent UDP packets for this client are routed to their
        // ClientThread instead of the WelcomeThread.
        _clientPortMap.put(port, client);
        ClientThread thread = new ClientThread
        */
    }

    private Client findClientByPort(int port) {
        return _clientPortMap.get(port);
    }

    /*
    public void send(int dport, String datagram) {}
    */
}
