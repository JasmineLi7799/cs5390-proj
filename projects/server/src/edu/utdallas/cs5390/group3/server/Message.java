package edu.utdallas.cs5390.group3.server;

import java.lang.String;

public final class Message {
    String _content;
    int _senderId;

    public Message(int senderId, String content) {
        _content = content;
        _senderId = senderId;
    }

    public int senderId() { return _senderId; }
    public String content() { return _content; }
    
    /**
     * put the content in the right format as "<session_id> <from: sending client> <chat message>"
     * this is the format for chat history
     * @param tag
     * @return
     */
    public String setContent(String tag){
    	_content = "<from: "+_senderId+ " >"; 
    	return tag+_content;
    }
}
