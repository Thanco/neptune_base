package host.thanco;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;

import com.corundumstudio.socketio.SocketIOClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BaseArrayList implements BaseDatabase {
    private static final int RECENTS_SIZE = 15;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        String databasePath = "json/databaseStore.json";
        if (!new File(databasePath).exists()) {
            try {
                new File(databasePath).createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try (FileReader reader = new FileReader(databasePath)) {
            ChatItem[] chatArr = GSON.fromJson(reader, ChatItem[].class);
            messageList = new ArrayList<>();
            if (chatArr != null) {
                Collections.addAll(messageList, chatArr);
                currentItemIndex = messageList.get(messageList.size() - 1).getItemIndex() + 1;
                Collections.sort(messageList);
            } else {
                currentItemIndex = 0;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        currentUsers = new ArrayList<>();
        File imgFile = new File("img/");
        imgFile.mkdirs();
    }

    public void saveList() {
        String databasePath = "json/databaseStore.json";
        if (!new File("json/databaseStore.json").exists()) {
            try {
                new File("json/").mkdirs();
                new File("json/databaseStore.json").createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(databasePath))) {
            GSON.toJson(messageList, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }        
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
            ChatItem newItem;
            try {
                String newFileName = "img/" + item.getItemIndex() + ".png";
                File newFile = new File(newFileName);
                FileOutputStream out = new FileOutputStream(newFile);
                out.write((byte[]) item.getContent());
                out.close();
                newItem = new ChatItem(item.getItemIndex(), item.getUserName(), item.getType(), newFileName);
                messageList.add(newItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        messageList.add(item);
        Collections.sort(messageList);
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
