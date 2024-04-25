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
	private int currentPlayerIndex = 0; // Index of the current player

	TheServer server;
	private Consumer<Serializable> callback;

	private int[][][] playerGrids = new int[2][GRID_SIZE][GRID_SIZE];
	private boolean[][][] shotsFired = new boolean[2][GRID_SIZE][GRID_SIZE];
	private boolean gameActive = false;
	private Rectangle[][] gridRectangles;
	private GridPane gridPane;



	Server(Consumer<Serializable> call) {

		callback = call;
		server = new TheServer();
		server.start();
	}
//	public void handleNewClient(Socket clientSocket) {
//		// Create a new ClientThread instance for the new client
//		ClientThread newClient = new ClientThread(clientSocket, count);
//
//		// Add the new client to the list of connected clients
//		clients.add(newClient);
//
//		// Assign opponents to the new client and existing clients
//		assignOpponents();
//
//		// Start the client thread
//		newClient.start();
//
//		// Increment the client count
//		count++;
//	}
//
//	private void assignOpponents() {
//		// Check if there are at least two clients connected
//		if (clients.size() >= 2) {
//			// Get the last two clients from the list
//			ClientThread client1 = clients.get(clients.size() - 1);
//			ClientThread client2 = clients.get(clients.size() - 2);
//
//			// Assign each client as an opponent to the other
//			client1.setOpponent(client2);
//			client2.setOpponent(client1);
//		}
//		// You can extend this logic to handle more complex scenarios
//	}



	public class TheServer extends Thread {

		public void run() {

			try (ServerSocket mysocket = new ServerSocket(5555);) {
				System.out.println("Server is waiting for a client!");


				while (true) {

					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("client has connected to server: " + "client #" + count);
					clients.add(c);
					c.start();
//					assignOpponents();

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

//		private ClientThread opponent;
//
//		public void setOpponent(ClientThread opponent) {
//			this.opponent = opponent;
//		}
//		private int[][] playerBoardState;
//
//		public void setPlayerBoardState(int[][] boardState) {
//			this.playerBoardState = boardState;
//		}
//
//		public int[][] getPlayerBoardState() {
//			return playerBoardState;
//		}

		private int[][] playerBoardState;
		private int[][] opponentBoardState;

		public void setPlayerBoardState(int[][] boardState) {
			this.playerBoardState = boardState;
		}

		public void setOpponentBoardState(int[][] boardState) {
			this.opponentBoardState = boardState;
		}

		public void sendPlayerBoard() throws IOException {
			// Send the player's own board state to the client
			Message response = new Message(Message.MessageType.GET_BOARD_PLAYER_VS_PLAYER, playerBoardState);
			out.writeObject(response);
		}

		public void sendOpponentBoard() throws IOException {
			// Send the opponent's board state to the client
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

//			updateClients("new client on server: client #" + count);

			while (true) {
				try {
					Message data = (Message) in.readObject();
					callback.accept("client: " + count + " sent: " + data);
					System.out.println("type sent from client:" + data.getType());
					HandleData(data);
//							System.out.println("sent from client" + Arrays.deepToString(data.getBoardState()));
//					updateClients("client #" + count + " said: " + data);

				} catch (Exception e) {
					callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
//					updateClients("Client #" + count + " has left the server!");
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
//				case SET_OPPONENT_BOARD:
//					handleSetOpponentBoard(data);
//					break;
				case SET_BOARD_PLAYER_VS_PLAYER:
					handleSetBoardPlayerVsPlayer(data);
					break;
				case PLAYER_TURN:
					int playerIndex = data.getPlayerIndex();
					int row = data.getRow();
					int col = data.getCol();
					boolean playersTurn = data.isPlayersTurn();
					boolean oppsTurn = data.isOppsTurn();
					// Handle the shot
					boolean hit = handleShot(playerIndex, row, col);
					boolean isOppsTurn = !playersTurn;
					break;
				default:
					callback.accept("Waiting for proper message from client #" + count);
					break;
			}
		}

		private void handleSetBoard(Message data) throws IOException {
			System.out.println("do i reach set baord?");
			int[][] boardState = data.getBoardState();

			// Send the processed board back to the client
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
			// Retrieve the client's index
			int clientIndex = clients.indexOf(this);

			// Retrieve the opponent's index
			int opponentIndex = (clientIndex == 0) ? 1 : 0;

			// Get the board states for both the client and the opponent
			int[][] clientBoardState = data.getBoardState();

			// Separate the board states for the client and the opponent
			if (clientIndex == 0) {
				// If this client is the first one, it has its own board state
				setPlayerBoardState(clientBoardState);
				// The opponent's board state is set for the second client
				clients.get(opponentIndex).setOpponentBoardState(clientBoardState);
			} else {
				// If this client is the second one, it has its own board state
				setPlayerBoardState(clientBoardState);
				// The opponent's board state is set for the first client
				clients.get(opponentIndex).setOpponentBoardState(clientBoardState);
			}

			// Send each player's board to the respective clients
			sendPlayerBoard();
			clients.get(opponentIndex).sendOpponentBoard();

			System.out.println("opponent's board: " + Arrays.deepToString(clientBoardState));
			System.out.println("client's board: " + Arrays.deepToString(clientBoardState));
		}

//		public void setPlayerBoardState(int[][] boardState) {
//			this.playerBoardState = boardState;
//		}

//		public void setOpponentBoardState(int[][] boardState) {
//			// Set the board state for the opponent
//			this.opponent.playerBoardState = boardState;
//		}
		public void sendBoardPlayervPlayer(int[][] boardState) throws IOException {
			// Create a message containing the board state
			Message response = new Message(Message.MessageType.GET_BOARD_PLAYER_VS_PLAYER, boardState);

			// Send the message to the client
			out.writeObject(response);

			// Print confirmation message
			System.out.println("Sending board state to player: " + Arrays.deepToString(boardState));
		}
//		private void handleSetBoardHuman(Message data) throws IOException {
//			System.out.println("do i reach set baord human?");
//			int[][] boardState = data.getBoardState();
//
//			for (ClientThread client : clients) {
//				client.sendBoardHuman(boardState);
//			}
//		}
//		public void sendBoardHuman(int[][] boardState) throws IOException {
//			Message response = new Message(Message.MessageType.GET_BOARD_PLAYER_VS_PLAYER, boardState);
//			out.writeObject(response);
//		}
//	private void handleSetBoardPlayerVsPlayer(Message data) throws IOException {
//			System.out.println("do i reach set board pvp");
//		// Retrieve the client's index
//		int clientIndex = clients.indexOf(this);
//
//		// Retrieve the opponent's index
//		int opponentIndex = (clientIndex == 0) ? 1 : 0;
//
//		// Get the board states for both the client and the opponent
//		int[][] clientBoardState = data.getBoardState();
//		int[][] opponentBoardState = clients.get(opponentIndex).getPlayerBoardState();
//
//		// Send messages containing both board states to the client and the opponent
//		sendBoardPlayervPlayer(clientBoardState);
//		clients.get(opponentIndex).sendBoardPlayervPlayer(opponentBoardState);
//		System.out.println("opponents board: "+ Arrays.deepToString(opponentBoardState));
//		System.out.println("clients board: "+ Arrays.deepToString(clientBoardState));
//	}
//		public void sendBoardPlayervPlayer(int[][] boardState) throws IOException {
//			Message response = new Message(Message.MessageType.GET_BOARD_PLAYER_VS_PLAYER, boardState);
//			out.writeObject(response);
//			System.out.println("sending out: " +response.getType());
//		}

//		private void handleSetOpponentBoard(Message data) throws IOException {
//			int[][] opponentBoardState = generateRandomBoard();
//			sendOpponentBoard(opponentBoardState);
//			System.out.println("Sending opponent board: " + Arrays.deepToString(opponentBoardState));
//		}
//
//		private void sendOpponentBoard(int[][] boardState) throws IOException {
//			Message response = new Message(Message.MessageType.GET_OPPONENT_BOARD, boardState);
//			out.writeObject(response);
//		}



		//		public void sendOppBoard(int[][] boardState) throws IOException {
//			Message response = new Message(Message.MessageType.GET_OPPONENT_BOARD, boardState);
//			out.writeObject(response);
//		}
//
//		private void handleSetOpponentBoard(Message data) throws IOException {
//			int[][] opponentBoardState = generateRandomBoard();
//			for (ClientThread client : clients) {
//				client.sendOppBoard(opponentBoardState);
//				System.out.println("sending board: " + Arrays.deepToString(opponentBoardState));
//			}
//		}
		private int[][] generateRandomBoard() {
			// Generate a new random board
			Random random = new Random();
			int[][] boardState = new int[GRID_SIZE][GRID_SIZE];
			// Logic to randomly populate the board with ships
			addRandomShips();
			return boardState;
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
			int opponentIndex = (playerIndex + 1) % 2; // Determine the opponent's index

			// Check if the shot hits a ship on the opponent's board
			boolean hit = (opponentBoardState[row][col] == 1);

			// Create a message indicating the result of the shot
			Message message;
			if (hit) {
				// Mark the position as hit
//				playerBoardState[row][col] = 1;
				message = new Message(Message.MessageType.HIT, true, false);
			} else {
				// Mark the position as missed
//				playerBoardState[row][col] = 0;
				message = new Message(Message.MessageType.MISS, true, false);
			}

			// Inform both players about the result of the shot
			updateClients(message);

			return hit;
//			if (!gameActive) {
//				return false; // Game not active
//			}
//
//			int[][] opponentGrid = playerGrids[(playerIndex + 1) % 2]; // Opponent's grid
//
//			if (opponentGrid[row][col] == HIT) {
//				return false; // Spot already hit
//			}
//
//			shotsFired[playerIndex][row][col] = true; // Mark the spot as fired
//
//			if (opponentGrid[row][col] == SHIP) {
//				opponentGrid[row][col] = HIT; // Mark the spot as hit
//				return true; // Hit
//			} else {
//				return false; // Miss
//			}
		}



		private GridPane createGridPane() {
			GridPane gridPane = new GridPane();
			gridPane.setPadding(new Insets(10));
			gridPane.setHgap(2);
			gridPane.setVgap(2);

			gridRectangles = new Rectangle[GRID_SIZE][GRID_SIZE];

			// Add labels to the grid
			for (int row = 0; row < GRID_SIZE; row++) {
				Text rowLabel = new Text(String.valueOf(row + 1));
				rowLabel.setFill(Color.WHITE);
				gridPane.add(rowLabel, 0, row + 1);
				for (int col = 0; col < GRID_SIZE; col++) {
					if (row == 0) {
						Text colLabel = new Text(String.valueOf((char) ('A' + col)));
						colLabel.setFill(Color.WHITE);
//					colLabel.setWrappingWidth(new Insets(0, 5, 0, 0));
						gridPane.add(colLabel, col + 1, 0);
					}
					Rectangle rectangle = new Rectangle(30, 30);
					rectangle.setFill(Color.LIGHTBLUE);
					gridRectangles[row][col] = rectangle;
					gridPane.add(rectangle, col + 1, row + 1);
				}
			}
			return gridPane;
		}


		private int[][] addRandomShips() {
			Random random = new Random();
			// Define the ships
			int[][] ships = {
					{5, random.nextInt(GRID_SIZE - 4)},  // Carrier (5 holes)
					{4, random.nextInt(GRID_SIZE - 3)},  // Battleship (4 holes)
					{3, random.nextInt(GRID_SIZE - 2)},  // Cruiser (3 holes)
					{3, random.nextInt(GRID_SIZE - 2)},  // Submarine (3 holes)
					{2, random.nextInt(GRID_SIZE - 1)}   // Destroyer (2 holes)
			};

			// Clear existing ship placements
			clearGridPane(gridPane);

			for (int[] ship : ships) {
				int size = ship[0];
				int startX = ship[1];
				int startY = random.nextInt(GRID_SIZE);

				Color color = getRandomColor(); // Generate a unique random color for each ship
//				shipColors.add(color);

				boolean canPlaceShip;
				do {
					canPlaceShip = true;
					for (int i = 0; i < size; i++) {
						if (startX + i >= GRID_SIZE || gridRectangles[startY][startX + i].getFill() != Color.LIGHTBLUE) {
							// Ship cannot be placed because a cell is already occupied or it exceeds grid boundary
							canPlaceShip = false;
							startX = random.nextInt(GRID_SIZE - size + 1);
							startY = random.nextInt(GRID_SIZE);
							break;
						}
					}
				} while (!canPlaceShip);

				for (int i = 0; i < size; i++) {
					Rectangle rectangle = gridRectangles[startY][startX + i];
					rectangle.setFill(color);
				}
			}
			return ships;
		}

		private void clearGridPane(GridPane gridPane) {
			// Clear all ship placements (rectangles filled with colors)
			for (int row = 0; row < GRID_SIZE; row++) {
				for (int col = 0; col < GRID_SIZE; col++) {
					Rectangle rectangle = gridRectangles[row][col];
					rectangle.setFill(Color.LIGHTBLUE); // Reset cell color
				}
			}
		}

		private Color getRandomColor() {
			Random random = new Random();
			return Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
		}
	}
}


	
	

	
