// Copyright Terry Hancock 2023
package host.thanco;

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
        return "'" + userName + "','" + type + "','" + content + "'";
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