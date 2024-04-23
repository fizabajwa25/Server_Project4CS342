import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;
/*
 * Clicker: A: I really get it    B: No idea what you are talking about
 * C: kind of following
 */

public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Serializable> callback;
	
	
	Server(Consumer<Serializable> call){
	
		callback = call;
		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");
		  
			
		    while(true) {
		
				ClientThread c = new ClientThread(mysocket.accept(), count);
				callback.accept("client has connected to server: " + "client #" + count);
				clients.add(c);
				c.start();
				
				count++;
				
			    }
			}//end of try
				catch(Exception e) {
					callback.accept("Server socket did not launch");
				}
			}//end of while
		}
	

		class ClientThread extends Thread{
			
		
			Socket connection;
			int count;
			ObjectInputStream in;
			ObjectOutputStream out;
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;	
			}
			
			public void updateClients(String message) {
				for(int i = 0; i < clients.size(); i++) {
					ClientThread t = clients.get(i);
					try {
					 t.out.writeObject(message);
					}
					catch(Exception e) {}
				}
			}
			
			public void run(){
					
				try {
					in = new ObjectInputStream(connection.getInputStream());
					out = new ObjectOutputStream(connection.getOutputStream());
					connection.setTcpNoDelay(true);	
				}
				catch(Exception e) {
					System.out.println("Streams not open");
				}
				
				updateClients("new client on server: client #"+count);
					
				 while(true) {
					    try {
					    	Message data = (Message) in.readObject();
					    	callback.accept("client: " + count + " sent: " + data);
							System.out.println("type sent from server:"+data.getType());
							HandleData(data);
//							System.out.println("sent from client" + Arrays.deepToString(data.getBoardState()));
					    	updateClients("client #"+count+" said: "+data);
					    	
					    	}
					    catch(Exception e) {
					    	callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					    	updateClients("Client #"+count+" has left the server!");
					    	clients.remove(this);
					    	break;
					    }
					}
				}//end of run

			private void HandleData(Message data) throws IOException {
				switch (data.getType()) {
					case SET_BOARD:
						// send back board and message type "GET_BOARD"
						handleSetBoard(data);
						break;
					default:
						callback.accept("Waiting for proper message from client #" + count);
						break;
				}
			}
			private void handleSetBoard(Message data) throws IOException {
				// Process the board sent by the client
				int[][] boardState = data.getBoardState();
				// Perform any necessary operations with the board, such as validation, saving, etc.

				// Send the processed board back to the client
				for (ClientThread client : clients) {
					client.sendBoard(boardState);
				}
			}

			public void sendBoard(int[][] boardState) throws IOException {
				Message response = new Message(Message.MessageType.GET_BOARD, boardState);
				out.writeObject(response);
			}


		}//end of client thread
}


	
	

	
