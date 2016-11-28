/**
 * ChatSession
 * when two client connect to each other successfully and start the conversation, it will create the ChatSession
 * each session collect all the chat history in this session (in ConcurrentLinkedQueu)
 * so each client should have several session
 */
package edu.utdallas.cs5390.group3.server;

import java.lang.String;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ChatSession {

    int _id;
    Client _clientA;
    Client _clientB;
    ConcurrentLinkedQueue<String> _messageHistory;

    public ChatSession(int id, Client clientA, Client clientB) {
        _id = id;
        _clientA = clientA;
        _clientB = clientB;
        _messageHistory= new ConcurrentLinkedQueue<String>();
    }

    public int id() { return _id; }

    public Client partner(Client clientX) {
        if (clientX == _clientA) {
            return _clientB;
        } else if (clientX == _clientB) {
            return _clientA;
        }
        return null;
    }

   /**
    * return the history from this chatSession
    * @return
    */
   public String getHistory(){
	   String history ="" ;
	   for(int i = 0; i < _messageHistory.size(); i ++){
		   history+="/n";
		   history+=_messageHistory.poll();
	   }
	   return history;
   }
}
