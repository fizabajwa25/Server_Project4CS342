import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;
    private int[][] boardState;
    private MessageType type;
    private int playerIndex;
    private int row;
    private int col;



    public enum MessageType {
        SET_BOARD,
        GET_BOARD,
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
}
