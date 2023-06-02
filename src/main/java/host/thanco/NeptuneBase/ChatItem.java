// Copyright Terry Hancock 2023
package host.thanco.NeptuneBase;

import java.io.Serializable;

import com.google.gson.Gson;

public class ChatItem implements Comparable<ChatItem>, Serializable {
    private int itemIndex;
    private String userName;
    private String channel;
    private char type;
    private Object content;

    public ChatItem(int itemIndex, String userName, String channel, char type, Object content) {
        this.itemIndex = itemIndex;
        this.userName = userName;
        this.channel = channel;
        this.type = type;
        this.content = content;
    }

    public ChatItem(ChatItem copyItem) {
        this.itemIndex = copyItem.getItemIndex();
        this.userName = copyItem.getUserName();
        this.channel = copyItem.getChannel();
        this.type = copyItem.getType();
        this.content = copyItem.getContent();
    }

    public static ChatItem fromJson(String jsonString) {
        return new Gson().fromJson(jsonString, ChatItem.class);
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String toString() {
        return type + " " + channel + " " + itemIndex + ". " + userName + ": " + content;
    }

    public String toSQLString() {
        String ret = "'" + BaseSQLDatabase.formatForSQL(userName) + "','" + type + "','";
        if (content instanceof String) {
            return ret + BaseSQLDatabase.formatForSQL((String) content) + "'";
        }
        return  ret + content + "'";
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

    public void trim() {
        if (content.getClass() == String.class) {
            content = ((String) content).trim();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        ChatItem item = (ChatItem) obj;
        return item.getItemIndex() == this.itemIndex 
            && item.userName.equals(this.userName) 
            && item.getChannel() == this.channel;
    }

    @Override
    public int compareTo(ChatItem item) {
        return itemIndex - item.getItemIndex();
    }

}