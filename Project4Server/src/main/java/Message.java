import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;
    private int[][] boardState;
    private MessageType type;

    public enum MessageType {
        SET_BOARD,
        GET_BOARD,
        TRY_MOVE,
        HIT,
        MISS
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
}
