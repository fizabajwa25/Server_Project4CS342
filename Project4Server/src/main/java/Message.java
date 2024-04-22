import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;
    private String type;
    private String content;

    public Message(String type, String content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
