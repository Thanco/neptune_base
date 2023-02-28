// Copyright Terry Hancock 2023
package host.thanco;

import java.util.ArrayList;
import java.util.Hashtable;

import com.corundumstudio.socketio.SocketIOClient;

public interface BaseDatabase {
    final static BaseDatabase instance = getInstance();
    public static BaseDatabase getInstance() {
        if (instance == null) {
            return BaseArrayList.getInstance();
        }
        return instance;
    }
    public void initDatabase();
    public void store(ChatItem message);
    public Hashtable<String, ArrayList<ChatItem>> getMessageLists();
    public ArrayList<ChatItem> getRecents();
    public ArrayList<ChatItem> getRecents(String channel, int oldestMessage);
    public void addClient(SocketIOClient client, String userName);
    public void removeClient(String userName);
    public ArrayList<String> getCurrentUsers();
    public String getClientUsername(SocketIOClient client);
    public int getNextIndex(String channel);
    public void saveList();
}
