import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;
    private int[][] boardState;
    private int[][] oppBoardState;
    private MessageType type;
    private int playerIndex;
    private int row;
    private int col;
    boolean playersTurn;
    boolean oppsTurn;



    public enum MessageType {
        SET_BOARD,
        GET_BOARD,
        SET_OPPONENT_BOARD,
        GET_OPPONENT_BOARD,
        SET_BOARD_PLAYER_VS_PLAYER,
        GET_BOARD_PLAYER_VS_PLAYER,
        PLAYER_TURN,
        NOT_YOUR_TURN,
        TRY_MOVE,
        HIT,
        SHOT_FIRED,
        TURN, MISS
    }

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, int[][] boardState) {
        this.type = type;
        this.boardState = boardState;
    }

    public Message(MessageType type, int row, int col, boolean playersTurn, boolean oppsTurn) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.playersTurn = playersTurn;
        this.oppsTurn = oppsTurn;
    }

    public Message(MessageType type, boolean playersTurn, boolean oppsTurn) {
        this.type = type;
        this.playersTurn = playersTurn;
        this.oppsTurn = oppsTurn;
    }

    public Message(MessageType type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
    }




    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public int[][] getBoardState() {
        return boardState;
    }

    public void setBoardState(int[][] boardState) {
        this.boardState = boardState;
    }
    public int[][] getOppBoardState(){
        return oppBoardState;
    }
    public void setOppBoardState(int[][] oppBoardState){
        this.oppBoardState = oppBoardState;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    public void setPlayerIndex(int playerIndex) {
        this.playerIndex = playerIndex;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public boolean isPlayersTurn() {
        return playersTurn;
    }

    public boolean isOppsTurn() {
        return oppsTurn;
    }

    public void setOppsTurn(boolean oppsTurn) {
        this.oppsTurn = oppsTurn;
    }

    public void setPlayersTurn(boolean playersTurn) {
        this.playersTurn = playersTurn;
    }
}

