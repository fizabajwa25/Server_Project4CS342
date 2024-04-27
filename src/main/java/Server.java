import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
/*
 * Clicker: A: I really get it    B: No idea what you are talking about
 * C: kind of following
 */
/*-------------------------------------------
Program 4: BattleShip
Course: CS 342, Spring 2024, UIC
System: IntelliJ
Author: Aleena Mehmood, Fiza Bajwa, Ashika Shekar
------------------------------------------- */

public class Server {

	private static final int GRID_SIZE = 10;
	private static final int EMPTY = 0;
	private static final int SHIP = 1;
	private static final int HIT = 2;
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Serializable> callback;
	private boolean player1Turn = true;

	Server(Consumer<Serializable> call) {

		callback = call;
		server = new TheServer();
		server.start();
	}

	public class TheServer extends Thread {

		public void run() {

			try (ServerSocket mysocket = new ServerSocket(5555);) {
				System.out.println("Server is waiting for a client!");

				while (true) {

					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("client has connected to server: " + "client #" + count);
					clients.add(c);
					c.start();
					count++;

				}
			}//end of try
			catch (Exception e) {
				callback.accept("Server socket did not launch");
			}
		}//end of while
	}


	class ClientThread extends Thread {


		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
		}

		public void updateClients(Message message) {
			for (int i = 0; i < clients.size(); i++) {
				ClientThread t = clients.get(i);
				try {
					t.out.writeObject(message);
				} catch (Exception e) {
				}
			}
		}
		private int[][] playerBoardState;
		private int[][] opponentBoardState;

		public void setPlayerBoardState(int[][] boardState) {
			this.playerBoardState = boardState;
		}

		public void setOpponentBoardState(int[][] boardState) {
			this.opponentBoardState = boardState;
		}

		public void sendPlayerBoard() throws IOException {
			Message response = new Message(Message.MessageType.GET_BOARD_PLAYER_VS_PLAYER, playerBoardState);
			out.writeObject(response);
		}

		public void sendOpponentBoard() throws IOException {
			Message response = new Message(Message.MessageType.GET_OPPONENT_BOARD, opponentBoardState);
			out.writeObject(response);
		}

		public void run() {

			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open");
			}

			while (true) {
				try {
					Message data = (Message) in.readObject();
					callback.accept("client: " + count + " sent: " + data);
					System.out.println("type sent from client:" + data.getType());
					HandleData(data);
				} catch (Exception e) {
					callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					clients.remove(this);
					break;
				}
			}
		}//end of run

		private void HandleData(Message data) throws IOException {
			switch (data.getType()) {
				case SET_BOARD:
					handleSetBoard(data);
					break;
				case SET_BOARD_PLAYER_VS_PLAYER:
					handleSetBoardPlayerVsPlayer(data);
					break;
				case PLAYER_TURN:
					int playerIndex = data.getPlayerIndex();
					int row = data.getRow();
					int col = data.getCol();
					handleShot(row, col);
					break;
				default:
					callback.accept("Waiting for proper message from client #" + count);
					break;
			}
		}

		private void handleSetBoard(Message data) throws IOException {
			System.out.println("do i reach set baord?");
			int[][] boardState = data.getBoardState();

			for (ClientThread client : clients) {
				client.sendBoard(boardState);
			}
		}

		public void sendBoard(int[][] boardState) throws IOException {
			Message response = new Message(Message.MessageType.GET_BOARD, boardState);
			out.writeObject(response);
		}

		private void handleSetBoardPlayerVsPlayer(Message data) throws IOException {
			System.out.println("do i reach set board pvp");
			int clientIndex = clients.indexOf(this);

			int opponentIndex = (clientIndex == 0) ? 1 : 0; // opps index = not the clients index

			int[][] clientBoardState = data.getBoardState();

			if (clientIndex == 0) {
				setPlayerBoardState(clientBoardState);
				clients.get(opponentIndex).setOpponentBoardState(clientBoardState);
			} else {
				setPlayerBoardState(clientBoardState);
				clients.get(opponentIndex).setOpponentBoardState(clientBoardState);
			}
			sendPlayerBoard();
			clients.get(opponentIndex).sendOpponentBoard();
		}
		public void handleShot(int row, int col) {
			Message message;
			int clientIndex = clients.indexOf(this);
			int opponentIndex = (clientIndex == 0) ? 1 : 0;
			boolean hit = (opponentBoardState[row][col] == SHIP);

			if (player1Turn && clientIndex == 0 || !player1Turn && clientIndex == 1) {
				if (hit) {
					message = new Message(Message.MessageType.HIT);
				} else {
					message = new Message(Message.MessageType.MISS);
				}
				player1Turn = !player1Turn;
			} else {
				message = new Message(Message.MessageType.NOT_YOUR_TURN);
			}
			updateClients(message);
		}
	}
}


	
	

	
