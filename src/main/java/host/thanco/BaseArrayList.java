// Copyright Terry Hancock 2023
package host.thanco;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.stream.*;

import com.corundumstudio.socketio.SocketIOClient;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class BaseArrayList implements BaseDatabase {
    private static final int RECENTS_SIZE = 15;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static BaseArrayList instance;
    private Hashtable<String, ArrayList<ChatItem>> messageLists;
    private ArrayList<String> currentUsers;
    private Hashtable<String, Integer> currentItemIndexes;

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
                File imgFile = new File("img/");
                imgFile.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try (FileReader reader = new FileReader(databasePath)) {
            messageLists = GSON.fromJson(reader, new TypeToken<Hashtable<String, ArrayList<ChatItem>>>() {}.getType());
            currentItemIndexes = new Hashtable<>();
            if (messageLists == null) {
                messageLists = new Hashtable<>();
                currentUsers = new ArrayList<>();
                return;
            }
            Object[] keys = messageLists.keySet().toArray();
            for (int i = 0; i < keys.length; i++) {
                ArrayList<ChatItem> messageList = messageLists.get(keys[i]);
                if (messageList.size() > 0) {
                    currentItemIndexes.putIfAbsent(keys[i].toString(), messageList.get(messageList.size() - 1).getItemIndex() + 1);                        
                    Collections.sort(messageList);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        currentUsers = new ArrayList<>();
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
            GSON.toJson(messageLists, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }        
    }

    @Override
    public Hashtable<String, ArrayList<ChatItem>> getMessageLists() {
        return messageLists;
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
                String tempFile = "img/temp.png";
                FileOutputStream out = new FileOutputStream(tempFile);
                out.write((byte[]) item.getContent());
                out.close();
                File file = new File(tempFile);
                BufferedImage image = ImageIO.read(file);
                String newFileName = "img/" + item.getChannel() + item.getItemIndex() + ".jpg";
                File newFile = new File(newFileName);

                OutputStream os = new FileOutputStream(newFile);

                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter writer = (ImageWriter) writers.next();

                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                writer.setOutput(ios);

                ImageWriteParam param = writer.getDefaultWriteParam();

                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.6f);
                writer.write(null, new IIOImage(image, null, null), param);

                os.close();
                ios.close();
                writer.dispose();
                file.delete();

                newItem = new ChatItem(item.getItemIndex(), item.getUserName(), item.getChannel(), item.getType(), newFileName);
                addToList(newItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        addToList(item);
    }

    private void addToList(ChatItem item) {
        messageLists.putIfAbsent(item.getChannel(), new ArrayList<>());
        messageLists.get(item.getChannel()).add(item);
        Collections.sort(messageLists.get(item.getChannel()));
    }

    public int getNextIndex(String channel) {
        currentItemIndexes.putIfAbsent(channel, -1);
        Integer currentItemIndex = currentItemIndexes.get(channel);
        return currentItemIndexes.put(channel, ++currentItemIndex);
    }

    public ArrayList<ChatItem> getRecents() {
        ArrayList<ChatItem> temp = new ArrayList<>();
        Object[] keys = messageLists.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            ArrayList<ChatItem> log = messageLists.get(keys[i]);
            int arrLength = Math.min(RECENTS_SIZE, log.size());
            for (int j = log.size(); j > log.size() - arrLength; j--) {
                temp.add(log.get(j - 1));
            }
        }
        return temp;
    }

    public ArrayList<ChatItem> getRecents(String channel, int oldestMessage) {
        ArrayList<ChatItem> log = messageLists.get(channel);
        if (log == null) {
            return new ArrayList<>();
        }
        ArrayList<ChatItem> temp = new ArrayList<>();
        for (int i = oldestMessage; i > 0 && i > oldestMessage - RECENTS_SIZE; i--) {
            temp.add(log.get(i - 1));
        }
        Collections.reverse(temp);
        return temp;
    }
}
