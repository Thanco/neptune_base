// Copyright Terry Hancock 2023
package host.thanco;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Map.Entry;

import com.corundumstudio.socketio.SocketIOClient;

public class UserHandler {
    private static UserHandler instance;
    private Hashtable<UUID, String> currentUsers;

    private UserHandler() {
        initUsers();
    }

    public static UserHandler getInstance() {
        if (instance == null) {
            instance = new UserHandler();
        }
        return instance;
    }

    private void initUsers() {
        currentUsers = new Hashtable<>();
    }

    public boolean addClient(SocketIOClient client, String userName) {
        if (userName.equals("null")) {
            return false;
        }
        currentUsers.put(client.getSessionId(), userName);
        return true;
    }

    public void removeClient(SocketIOClient client) {
        currentUsers.remove(client.getSessionId());
    }

    public ArrayList<String> getCurrentUsers() {
        ArrayList<String> userNames = new ArrayList<>();
        for (Entry<UUID, String> entry : currentUsers.entrySet()) {
            userNames.add(entry.getValue());
        }
        return userNames;
    }

    public String getClientUsername(SocketIOClient client) {
        return currentUsers.get(client.getSessionId());
    }
}
