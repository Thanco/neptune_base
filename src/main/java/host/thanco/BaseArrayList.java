package host.thanco;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.corundumstudio.socketio.SocketIOClient;

public class BaseArrayList implements BaseDatabase {
    private static final int RECENTS_SIZE = 15;

    private static BaseArrayList instance;
    private ArrayList<ChatItem> messageList;
    private ArrayList<String> currentUsers;
    private int currentItemIndex;

    private BaseArrayList() {
        initDatabase();
    }

    public static BaseArrayList getInstance() {
        if(instance == null) {
            return new BaseArrayList();
        }
        else return instance;
    }

    public void initDatabase() {
        currentItemIndex = 0;
        messageList = new ArrayList<>();
        currentUsers = new ArrayList<>();
        File imgFile = new File("img/");
        imgFile.mkdirs();
    }

    @Override
    public ArrayList<ChatItem> getMessageList() {
        return messageList;
    }

    @Override
    public void addClient(SocketIOClient client, String userName) {
        client.set("userName", userName);
        currentUsers.add(userName);
    }

    public void removeClient(String userName) {
        currentUsers.remove(userName);
    }

    public ArrayList<String> getCurrentUsers() {
        return currentUsers;
    }

    @Override
    public String getClientUsername(SocketIOClient client) {
        try {
            return client.get("userName");
        } catch (Exception e) {
            return "";
        }
    }

    public void store(ChatItem item) {
        if (item.getType() == 'i') {
            try {
                String newFileName = "img/" + item.getItemIndex() + ".png";
                File newFile = new File(newFileName);
                FileOutputStream out = new FileOutputStream(newFile);
                out.write((byte[]) item.getContent());
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        messageList.add(item);
    }

    public int getNextIndex() {
        return currentItemIndex++;
    }

    // public ArrayList<ChatMessage> getRecents() {
    //     int arrLength = Math.min(RECENTS_SIZE, messageList.size());
    //     ArrayList<ChatMessage> temp = new ArrayList<>();
    //     for (int i = messageList.size(); i > messageList.size() - arrLength; i--) {
    //         temp.add(messageList.get(i - 1));
    //     }
    //     Collections.reverse(temp);
    //     return temp;
    // }

    public ArrayList<ChatItem> getRecents() {
        return messageList;
    }
}
