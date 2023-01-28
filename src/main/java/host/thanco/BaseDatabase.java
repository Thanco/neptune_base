package host.thanco;

import java.util.ArrayList;

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
    public ArrayList<ChatItem> getMessageList();
    public ArrayList<ChatItem> getRecents();
    public void addClient(SocketIOClient client, String userName);
    public void removeClient(String userName);
    public ArrayList<String> getCurrentUsers();
    public String getClientUsername(SocketIOClient client);
    public int getNextIndex();
    public void saveList();
}
