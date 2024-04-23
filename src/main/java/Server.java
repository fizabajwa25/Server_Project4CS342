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
import javafx.scene.control.ListView;
/*
 * Clicker: A: I really get it    B: No idea what you are talking about
 * C: kind of following
 */

public class Server {

	private static final int GRID_SIZE = 10;
	private static final int EMPTY = 0;
	private static final int SHIP = 1;
	private static final int HIT = 2;
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	private int currentPlayerIndex = 0; // Index of the current player

	TheServer server;
	private Consumer<Serializable> callback;

	private int[][][] playerGrids = new int[2][GRID_SIZE][GRID_SIZE];
	private boolean[][][] shotsFired = new boolean[2][GRID_SIZE][GRID_SIZE];
	private boolean gameActive = false;


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

		public void updateClients(String message) {
			for (int i = 0; i < clients.size(); i++) {
				ClientThread t = clients.get(i);
				try {
					t.out.writeObject(message);
				} catch (Exception e) {
				}
			}
		}



		public void run() {

			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open");
			}

			updateClients("new client on server: client #" + count);

			while (true) {
				try {
					Message data = (Message) in.readObject();
					callback.accept("client: " + count + " sent: " + data);
					System.out.println("type sent from server:" + data.getType());
					HandleData(data);
//							System.out.println("sent from client" + Arrays.deepToString(data.getBoardState()));
					updateClients("client #" + count + " said: " + data);

				} catch (Exception e) {
					callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					updateClients("Client #" + count + " has left the server!");
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
				case SHOT_FIRED:
					// Extract shot data from the message
					int playerIndex = data.getPlayerIndex();
					int row = data.getRow();
					int col = data.getCol();

					// Handle the shot
					boolean hit = clients.get(playerIndex).handleShot(playerIndex, row, col);

					// Send shot result to clients if necessary
					// For example, you could create a new message and send it to all clients
					// with the result of the shot

					// For now, let's just print the result
					if (hit) {
						callback.accept("Player " + playerIndex + " hit at row " + row + ", column " + col);
					} else {
						callback.accept("Player " + playerIndex + " missed at row " + row + ", column " + col);
					}

					// Switch turns
					currentPlayerIndex = (currentPlayerIndex + 1) % 2;

					// Inform clients whose turn it is
					for (ClientThread client : clients) {
						client.sendTurn(currentPlayerIndex);
					}
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


		private void startGame() {
			generateRandomShips();
			gameActive = true;
		}

		private void generateRandomShips() {
			Random random = new Random();
			int[] shipSizes = {5, 4, 3, 3, 2}; // Ship sizes allowed

			for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
				int[][] ownGrid = playerGrids[playerIndex];
				int[][] opponentGrid = playerGrids[(playerIndex + 1) % 2];

				for (int shipSize : shipSizes) {
					int startX = random.nextInt(GRID_SIZE);
					int startY = random.nextInt(GRID_SIZE);
					boolean isHorizontal = random.nextBoolean();

					// Check if ship size is valid
					if (shipSize < 2 || shipSize > 5) {
						throw new IllegalArgumentException("Invalid ship size: " + shipSize);
					}

					if (isShipPlacementValid(ownGrid, startX, startY, shipSize, isHorizontal)) {
						placeShip(ownGrid, startX, startY, shipSize, isHorizontal);
					}
				}

				generateRandomShipsForOpponent(opponentGrid);
			}
		}

		private void generateRandomShipsForOpponent(int[][] opponentGrid) {
			Random random = new Random();
			ArrayList<Integer> shipSizes = new ArrayList<>(Arrays.asList(2, 3, 4, 5)); // Ship sizes allowed

			while (!shipSizes.isEmpty()) {
				int shipSize = shipSizes.get(0); // Get the next ship size to place
				boolean isHorizontal = random.nextBoolean(); // Randomly determine orientation

				// Randomly select starting position until a valid placement is found
				int startX, startY;
				do {
					startX = random.nextInt(GRID_SIZE);
					startY = random.nextInt(GRID_SIZE);
				} while (!isShipPlacementValidAndClustered(opponentGrid, startX, startY, shipSize, isHorizontal));

				// Place the ship
				placeShip(opponentGrid, startX, startY, shipSize, isHorizontal);
				shipSizes.remove(0); // Remove the used ship size
			}
		}

		public void sendTurn(int playerIndex) throws IOException {
			int[][] turnData = new int[1][1];
			turnData[0][0] = playerIndex;
			out.writeObject(turnData);
		}


		private boolean isShipPlacementValidAndClustered(int[][] grid, int startX, int startY, int shipSize, boolean isHorizontal) {
			if (isHorizontal && startX + shipSize > GRID_SIZE) {
				return false;
			}
			if (!isHorizontal && startY + shipSize > GRID_SIZE) {
				return false;
			}

			// Check if the ship can be placed without separating clusters
			for (int i = 0; i < shipSize; i++) {
				int row = isHorizontal ? startY : startY + i;
				int col = isHorizontal ? startX + i : startX;

				// Check if the spot is already occupied or if it separates clusters
				if (grid[row][col] != EMPTY || separatesClusters(grid, row, col)) {
					return false;
				}
			}
			return true;
		}

		private boolean separatesClusters(int[][] grid, int row, int col) {
			int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Up, Down, Left, Right

			for (int[] direction : directions) {
				int newRow = row + direction[0];
				int newCol = col + direction[1];

				if (newRow >= 0 && newRow < GRID_SIZE && newCol >= 0 && newCol < GRID_SIZE && grid[newRow][newCol] == SHIP) {
					return false; // A neighboring cell with a ship is found, so clusters are not separated
				}
			}
			return true; // No neighboring ship found, clusters are separated
		}


		private boolean isShipPlacementValid(int[][] grid, int startX, int startY, int shipSize, boolean isHorizontal) {
			if (isHorizontal && startX + shipSize > GRID_SIZE) {
				return false;
			}
			if (!isHorizontal && startY + shipSize > GRID_SIZE) {
				return false;
			}
			for (int i = 0; i < shipSize; i++) {
				int row = isHorizontal ? startY : startY + i;
				int col = isHorizontal ? startX + i : startX;
				if (grid[row][col] != EMPTY) {
					return false;
				}
			}
			return true;
		}

		private void placeShip(int[][] grid, int startX, int startY, int shipSize, boolean isHorizontal) {
			for (int i = 0; i < shipSize; i++) {
				int row = isHorizontal ? startY : startY + i;
				int col = isHorizontal ? startX + i : startX;
				grid[row][col] = SHIP;
			}
		}


		public boolean handleShot(int playerIndex, int row, int col) {
			if (!gameActive) {
				return false; // Game not active
			}

			int[][] opponentGrid = playerGrids[(playerIndex + 1) % 2]; // Opponent's grid

			if (opponentGrid[row][col] == HIT) {
				return false; // Spot already hit
			}

			shotsFired[playerIndex][row][col] = true; // Mark the spot as fired

			if (opponentGrid[row][col] == SHIP) {
				opponentGrid[row][col] = HIT; // Mark the spot as hit
				return true; // Hit
			} else {
				return false; // Miss
			}
		}
	}
}


	
	

	
