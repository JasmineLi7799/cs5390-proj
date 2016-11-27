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
            _socket.setSoTimeout(_client.config.timeoutInterval());
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

    public void finalize() {
        Console.debug("If you see this mesasge, someone somewhere forgot"
                      + " to close the SessionSocket.");
        Thread.dumpStack();
        this.closeBindSock();
        this.close();
    }

    // wrote by Jason
//    public byte[] readMessage() throws Exception {
//        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//
//        byte[] cipherBlock = new byte[Cryptor.CRYPT_LENGTH];
//        if (_inStream.read(cipherBlock) != Cryptor.CRYPT_LENGTH) {
//            Console.error("Short read from SessionSocket. IO Failure?");
//            throw new IOException();
//        }
//        byte[] plainBlock = Cryptor.decrypt(_client.cryptKey(), cipherBlock);
//        int msgLength = ByteBuffer.wrap(
//            Arrays.copyOfRange(plainBlock, 0, 4)).getInt();
//
//        // Trim length field from plainBlock
//        plainBlock = Arrays.copyOfRange(plainBlock, 4, plainBlock.length);
//        int bytesRead = plainBlock.length;
//
//        buffer.write(plainBlock);
//
//        while (bytesRead < msgLength) {
//            Console.debug("bytesRead = " + bytesRead);
//            if (_inStream.read(cipherBlock) != Cryptor.CRYPT_LENGTH) {
//                Console.error("Short read from SessionSocket. IO Failure?");
//                throw new IOException();
//            }
//            plainBlock = Cryptor.decrypt(_client.cryptKey(), cipherBlock);
//            buffer.write(plainBlock);
//            bytesRead += plainBlock.length;
//            Console.debug("bytesRead = " + bytesRead);
//        }
//
//        return buffer.toByteArray();
//    }

    // wrote by Jason
//    public byte[] writeMessage(String message) throws Exception {
//        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
//        byte[] lengthField = new byte[4];
//        ByteBuffer lfb = ByteBuffer.wrap(lengthField);
//        lfb.putInt(msgBytes.length);
//
//        // Concatenate length field + msg into plainMsg
//        byte[] plainMsg = new byte[4 + msgBytes.length];
//        System.arraycopy(lengthField, 0, plainMsg, 0, 4);
//        System.arraycopy(msgBytes, 0, plainMsg, 4, msgBytes.length);
//
//        byte[] cryptMsg = Cryptor.encrypt(_client.cryptKey(), plainMsg);
//        _outStream.write(cryptMsg);
//        return cryptMsg;
//    }
    
    public byte[] readMessage() throws Exception{
        Queue<byte[]> _msgBuffer = new LinkedList<byte[]>();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int msgLength = Cryptor.CRYPT_LENGTH;
        byte[] _msg = new byte[msgLength];
        int len = 0;
        do {
            len = _inStream.read(_msg);
            byte[] tmpMsg = new byte[len];
            for(int i = 0; i < len; i++){
                tmpMsg[i] = _msg[i];
            }
            _msgBuffer.add(tmpMsg);
            // System.out.println("len is "+len);
        } while (len == 16);


        while (!_msgBuffer.isEmpty()) {
            bos.write(_msgBuffer.poll());
        }
        return bos.toByteArray();
    }
    
    //this sends to server I think
    //so, using this to write messages to the client during chat is wrong, I think
    public byte[] writeMessage(String message) throws Exception{
        //Console.debug("Writing " + message + " to server");
        byte[] msg = message.getBytes();
        _outStream.write(message.getBytes());;
        return msg;
    }
}
