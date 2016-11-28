package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.net.ServerSocket;
import java.net.Socket;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.concurrent.Semaphore;

public final class SessionSocket {
    private Client _client;
    private ServerSocket _bindSock;
    private Socket _socket;

    //wrote by Jason//
    private InputStream _inStream;
    private OutputStream _outStream;

    public SessionSocket()
        throws IOException {
        _client = Client.instance();

        _bindSock = new ServerSocket();
        _bindSock.setSoTimeout(_client.config.timeoutInterval());
        _bindSock.bind(new InetSocketAddress(
                          _client.config.clientAddr(),
                          _client.config.clientPort()));
    }

    public void waitForConnection(Semaphore initLock)
        throws IOException, SocketException, SocketTimeoutException,
               InterruptedException {

        try {
            Console.debug("Listening for server connection on "
                        + _bindSock.getInetAddress().getHostAddress()
                        + ":" + _bindSock.getLocalPort());
            // Signal to the handshake thread that we are in the accepting
            // state. Until we reach this point, it is not safe for the
            // handshake thread to send the REGISTER message because we
            // aren't listening yet.
            initLock.release();
            _socket = _bindSock.accept();
            Console.debug("Accepting connection from server at "
                        + _socket.getInetAddress().getHostAddress()
                        + ":" + _socket.getPort());
            this.closeBindSock();
            _inStream = _socket.getInputStream();
            _outStream = _socket.getOutputStream();
        } catch (SocketTimeoutException|SocketException e) {
            this.closeBindSock();
            throw e;
        } catch (IOException ioe) {
            this.closeBindSock();
            throw ioe;
        }
    }

    private void closeBindSock() {
        try {
            Console.debug("Closing bind socket "
                        + _bindSock.getInetAddress().getHostAddress()
                        + ":" + _bindSock.getLocalPort());
            _bindSock.close();
        } catch (IOException e) {
            Console.warn("Could not close socket "
                        + _bindSock.getInetAddress().getHostAddress()
                        + ":" + _bindSock.getLocalPort()
                        + ": " + e);
        }
    }

    public void close() {
        try {
            Console.debug("Closing socket "
                          + _socket.getLocalAddress().getHostAddress()
                          + ":" + _socket.getLocalPort()
                          + " connected to server at "
                          + _socket.getInetAddress().getHostAddress()
                          + ":" + _socket.getPort()
                          + ".");
            _socket.close();
        } catch (IOException e) {
            Console.warn("Could not close socket "
                          + _socket.getLocalAddress().getHostAddress()
                          + ":" + _socket.getLocalPort()
                          + " connected to server at "
                          + _socket.getInetAddress().getHostAddress()
                          + ":" + _socket.getPort()
                          + ":" + e);
        }
    }

    public boolean isClosed() {
        return _socket.isClosed();
    }

    public void finalize() {
        Console.debug("If you see this mesasge, someone somewhere forgot"
                      + " to close the SessionSocket.");
        Thread.dumpStack();
        this.closeBindSock();
        this.close();
    }

    // public String readMessage() {
    //     int blockLen = Cryptor.CRYPT_LENGTH;
    //     try {
    //         // Read the first block
    //         byte[] blockBytes = new byte[blockLen];
    //         if (_inStream.read(blockBytes) != blockLen) {
    //             return null;
    //         }
    //         byte[] plainBytes;
    //         try {
    //             plainBytes = Cryptor.decrypt(_client.cryptKey(), blockBytes);
    //         } catch (Exception e) {
    //             Console.debug("header");
    //             Console.error("In writeMessage() caught: " + e);
    //             return null;
    //         }

    //         // Extract the message length
    //         ByteBuffer msgLenBuff = ByteBuffer.wrap(
    //             Arrays.copyOfRange(plainBytes, 0, 4));
    //         int msgLen = msgLenBuff.getInt();

    //         // Put the rest in the output buffer
    //         ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
    //         msgBuffer.write(Arrays.copyOfRange(plainBytes, 4, plainBytes.length));

    //         int bytesRead = plainBytes.length - 4;
    //         while (bytesRead < msgLen) {
    //             if (_inStream.read(blockBytes) != blockLen) {
    //                 Console.error("Got truncated encryption block from socket.");
    //                 return null;
    //             }
    //             try {
    //                 plainBytes = Cryptor.decrypt(_client.cryptKey(), blockBytes);
    //             } catch (Exception e) {
    //                 Console.debug("body");
    //                 Console.error("In writeMessage() caught: " + e);
    //                 return null;
    //             }
    //             bytesRead += plainBytes.length;
    //             msgBuffer.write(plainBytes);
    //         }

    //         return new String(msgBuffer.toByteArray(), StandardCharsets.UTF_8);
    //     } catch (IOException e) {
    //         Console.error("In readMessage caught: " + e);
    //         return null;
    //     }
    // }

    // public boolean writeMessage(String message) {
    //     return writeMessage(message.getBytes(StandardCharsets.UTF_8));
    // }

    // public boolean writeMessage(byte[] msgBytes) {
    //     byte plainMsg[] = new byte[msgBytes.length + 4];
    //     ByteBuffer msgBuff = ByteBuffer.wrap(plainMsg);
    //     msgBuff.putInt(msgBytes.length);
    //     msgBuff.put(msgBytes);

    //     byte[] cryptMsg;
    //     try {
    //         cryptMsg = Cryptor.encrypt(_client.cryptKey(), plainMsg);
    //     } catch (Exception e) {
    //         Console.error("In writeMessage() caught: " + e);
    //         return false;
    //     }
    //     try {
    //         _outStream.write(cryptMsg);
    //         return true;
    //     } catch (IOException e) {
    //         Console.error("In writeMessage() caught: " + e);
    //         return false;
    //     }
    // }

    public String readMessage() {
        try {
            // Extract the message length
            byte[] lenBytes = new byte[4];
            if (_inStream.read(lenBytes) != 4) {
                Console.error("Lost connection to server.");
                return null;
            }
            ByteBuffer msgLenBuff = ByteBuffer.wrap(lenBytes);
            int msgLen = msgLenBuff.getInt();

            byte[] message = new byte[msgLen];
            if (_inStream.read(message) != msgLen) {
                Console.error("Lost connection to server.");
                return null;
            }

            return new String(message, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Console.error("In readMessage caught: " + e);
            return null;
        }
    }

    public boolean writeMessage(String message) {
        return writeMessage(message.getBytes(StandardCharsets.UTF_8));
    }

    public boolean writeMessage(byte[] msgBytes) {
        byte plainMsg[] = new byte[msgBytes.length + 4];
        ByteBuffer msgBuff = ByteBuffer.wrap(plainMsg);
        msgBuff.putInt(msgBytes.length);
        msgBuff.put(msgBytes);

        try {
            _outStream.write(plainMsg);
            return true;
        } catch (IOException e) {
            Console.error("In writeMessage() caught: " + e);
            return false;
        }
    }
}
