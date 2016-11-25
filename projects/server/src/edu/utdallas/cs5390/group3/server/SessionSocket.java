package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.net.Socket;
import java.net.InetAddress;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.Arrays;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;

import java.nio.ByteBuffer;

public final class SessionSocket {
    private Server _server;
    private Client _client;
    private Socket _socket;
    private InputStream _inStream;
    private OutputStream _outStream;
    private static HashMap<Integer, SessionSocket> idMapSockt = new HashMap<Integer, SessionSocket>();
    public SessionSocket(Client client, InetAddress addr, int port)
        throws SocketException, IOException, SocketTimeoutException {
        _server = Server.instance();
        _client = client;
        // dest addr, dest port, src addr, src port (0 = ephemeral)
        _socket = new Socket(addr, port, _server.config.bindAddr(), 0);
        
        _inStream = _socket.getInputStream();
        _outStream = _socket.getOutputStream();
    }
    
    public SessionSocket getSocket(String clientID){
    	return idMapSockt.get(_client.id());
    }

    public void setSocket(int clientId, SessionSocket socket){
    	idMapSockt.put(clientId, socket);
    }
    
    public byte[] readMessage() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] cipherBlock = new byte[Cryptor.CRYPT_LENGTH];
        if (_inStream.read(cipherBlock) != Cryptor.CRYPT_LENGTH) {
            Console.error("Short read from SessionSocket. IO Failure?");
            throw new IOException();
        }
        byte[] plainBlock = Cryptor.decrypt(_client.cryptKey(), cipherBlock);
        int msgLength = ByteBuffer.wrap(
            Arrays.copyOfRange(plainBlock, 0, 4)).getInt();

        // Trim length field from plainBlock
        plainBlock = Arrays.copyOfRange(plainBlock, 4, plainBlock.length);
        int bytesRead = plainBlock.length;

        buffer.write(plainBlock);

        while (bytesRead < msgLength) {
            Console.debug("bytesRead = " + bytesRead);
            if (_inStream.read(cipherBlock) != Cryptor.CRYPT_LENGTH) {
                Console.error("Short read from SessionSocket. IO Failure?");
                throw new IOException();
            }
            plainBlock = Cryptor.decrypt(_client.cryptKey(), cipherBlock);
            buffer.write(plainBlock);
            bytesRead += plainBlock.length;
            Console.debug("bytesRead = " + bytesRead);
        }

        return buffer.toByteArray();
    }

    // wrote by Jason
    public byte[] writeMessage(String message) throws Exception {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] lengthField = new byte[4];
        ByteBuffer lfb = ByteBuffer.wrap(lengthField);
        lfb.putInt(msgBytes.length);

        // Concatenate length field + msg into plainMsg
        byte[] plainMsg = new byte[4 + msgBytes.length];
        System.arraycopy(lengthField, 0, plainMsg, 0, 4);
        System.arraycopy(msgBytes, 0, plainMsg, 4, msgBytes.length);

        byte[] cryptMsg = Cryptor.encrypt(_client.cryptKey(), plainMsg);
        _outStream.write(cryptMsg);
        return cryptMsg;
    }
}
