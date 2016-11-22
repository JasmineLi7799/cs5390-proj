package edu.utdallas.cs5390.group3.client;

import edu.utdallas.cs5390.group3.core.Console;
import edu.utdallas.cs5390.group3.core.Cryptor;

import java.net.ServerSocket;
import java.net.Socket;

import java.net.InetSocketAddress;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.Queue;
import java.util.LinkedList;

public final class SessionSocket {
    private Client _client;
    private Socket _socket;
    private Queue<byte[]> _msgQueue;
    
    //wrote by Jason//
    private InputStream _inStream;
    private OutputStream _outStream;
   
    public SessionSocket()
        throws SocketException, IOException, SocketTimeoutException {
        _client = Client.instance();

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
    
  
    // wrote by Jason
    
    public byte[] writeMessage(String message) throws Exception{
//		byte[] msg = Cryptor.encrypt(_client.cryptKey(), message);
		byte[] msg = message.getBytes();
		_outStream.write(message.getBytes());;
		return msg;
}
}
