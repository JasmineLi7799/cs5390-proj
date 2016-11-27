package edu.utdallas.cs5390.group3.server;

public class ChatThread implements Runnable{
	SessionSocket _socket;
    Client _client;
	String chatID;
	int sessionID;
	
	public ChatThread(SessionSocket _socket, Client _client, String chatID, int sessionID){
		this._socket = _socket;
		this._client = _client;
		this.chatID = chatID;
		this.sessionID = sessionID;
	}
			public void run(){
				System.out.println("now at chat thread");
				System.out.println("chatID is "+ chatID);
				while(true){
					try{
						System.out.println("enter while");
						 byte[] chatCotent = _socket.readMessage();
		                 String content = new String(chatCotent);
		                 if(content.substring(0, 11).equals(new String("END_REQUEST"))) {
		                	 System.out.println("+++++ revceive");
		                	 break;
		                 }
		                 System.out.println("The chat content received from client A is "+ content);
		                 // send content to client B
		               
		                 _socket.getSocket(chatID).writeMessage(content);
					} catch(Exception e){
						e.printStackTrace();
						}   
				}
		            System.out.println("The client enter End Chat");
		            String endChat = "END_NOTIF " + sessionID;
		            System.out.println("The end notification msg is "+ endChat);
		            try {
						_socket.writeMessage(endChat);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            System.out.println("The end chat msg has sent to client");
		            
					
	            	 
			}
      }

