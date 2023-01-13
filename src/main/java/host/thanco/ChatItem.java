package host.thanco;

public class ChatItem {
    private int itemIndex;
    private String userName;
    private char type;
    private Object content;

    public ChatItem(int itemIndex, String userName, char type, Object content) {
        this.itemIndex = itemIndex;
        this.userName = userName;
        this.type = type;
        this.content = content;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String toString() {
        return type + " " + itemIndex + ". " + userName + ": " + content;
    }

    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public void setItemIndex(int itemIndex) {
        this.itemIndex = itemIndex;
    }

}