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

import java.util.Queue;
import java.util.LinkedList;

public final class SessionSocket {
    private Server _server;
    private Client _client;
    private Socket _socket;
    private InputStream _inStream;
    private OutputStream _outStream;
    
    public SessionSocket(Client client, InetAddress addr, int port)
        throws SocketException, IOException, SocketTimeoutException {
        _server = Server.instance();
        _client = client;
        // dest addr, dest port, src addr, src port (0 = ephemeral)
        _socket = new Socket(addr, port, _server.config.bindAddr(), 0);
        
        _inStream = _socket.getInputStream();
        _outStream = _socket.getOutputStream();
    }
    
    // wrote by Jason
    
    public byte[] readMessage() throws Exception{
    	Queue<byte[]> _msgBuffer = new LinkedList<byte[]>();
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	int msgLength = Cryptor.CRYPT_LENGTH;
    	byte[] _msg = new byte[msgLength];
    	int len=0;
    	do{
    		len=_inStream.read(_msg);
    		byte[] tmpMsg = new byte[len];
    		for(int i=0; i<len; i++){
    			tmpMsg[i]=_msg[i];
    		}
    		_msgBuffer.add(tmpMsg);
//    		System.out.println("len is "+len);
    	}while(len==16);
    	    	
    	while(!_msgBuffer.isEmpty()){
    		bos.write(_msgBuffer.poll());
    	}
    	return bos.toByteArray();
    	
    }

    // TODO
    // public byte[] readMessage()
    //     throws InterruptedException, IOException {
    //
    //     Queue<byte[]> _msgBuffer = new LinkedList<byte[]>;

    //     // Read Cryptor.CRYPT_LENGTH bytes at a time from the _socket,
    //     // decrypt them (Cryptor.decrypt) and place the bytes in the
    //     // _msgBuffer.

    //     // Repeat ^^^ until you have a complete message (we need some way
    //     // to determine message length so we know when to stop).

    //     // Once you have a complete message in the _msgBuffer,
    //     // Merge them together with (for instance) ByteArrayOutputStream
    //     // and return the whole message.
    // }
    	
    
    
    // wrote by Jason
    public byte[] writeMessage(String message) throws Exception{
    	
//    			byte[] msg = Cryptor.encrypt(_client.cryptKey(), message);
    			byte[] msg = message.getBytes();
    			_outStream.write(msg);;
    			return msg;
    }
    // TODO
    // public byte[] writeMessage(String message)
    //     throws InterruptedException, IOException {
    //
    //     // This one is pretty straight forward.
    //     // Just call Cryptor.encrypt(_client.cryptKey(), message)
    //     // and send it on its way with _outStream.write()
    // }
}
