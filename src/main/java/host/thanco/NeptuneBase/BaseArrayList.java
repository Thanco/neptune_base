// Copyright Terry Hancock 2023
package host.thanco.NeptuneBase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class BaseArrayList implements BaseDatabase {
    private static final String DATABASE_PATH = "json/databaseStore.json";
    private int recentsSize;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static BaseArrayList instance;
    private Hashtable<String, ArrayList<ChatItem>> messageLists;
    private Hashtable<String, Integer> currentItemIndexes;

    protected int minsBetweenSave = 30;

    private BaseArrayList() {
        initDatabase();
        startAutoSave();
    }

    public static BaseArrayList getInstance() {
        if(instance == null) {
            return new BaseArrayList();
        }
        else return instance;
    }

    public void initDatabase() {
        recentsSize = new ConfigurationHandler().getConfiguration().getRecentsSize();
        if (!new File(DATABASE_PATH).exists()) {
            try {
                new File(DATABASE_PATH).createNewFile();
                File imgFile = new File("img/");
                imgFile.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try (FileReader reader = new FileReader(DATABASE_PATH, StandardCharsets.UTF_16)) {
            messageLists = GSON.fromJson(reader, new TypeToken<Hashtable<String, ArrayList<ChatItem>>>() {}.getType());
            currentItemIndexes = new Hashtable<>();
            if (messageLists == null) {
                messageLists = new Hashtable<>();
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
    }

    private void startAutoSave() {
        long delay = intMinsTolongMillis(minsBetweenSave);
        Timer autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveLists();
                System.out.println("Autosave Complete!");
            }
        }, delay, delay);
    }

    private long intMinsTolongMillis(int mins) {
        return mins * 60000 ;
    }

    public void saveLists() {
        if (!new File(DATABASE_PATH).exists()) {
            try {
                new File(DATABASE_PATH).createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(DATABASE_PATH), StandardCharsets.UTF_16)) {
            GSON.toJson(messageLists, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Hashtable<String, ArrayList<ChatItem>> getLists() {
        return messageLists;
    }

    public void store(ChatItem item) {
        ChatItem newItem = new ChatItem(item);
        if (item.getType() == 'i') {
            try {
                String newFileName = "img/" + item.getChannel() + item.getItemIndex() + ".jpg";
                ImageHandler.saveImage(newFileName, (byte[]) item.getContent());item.setContent(newFileName);
                newItem.setContent(newFileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        addToList(newItem);
    }

    public void edit(ChatItem message) {
        ArrayList<ChatItem> currentList = messageLists.get(message.getChannel());
        ChatItem oldItem = new ChatItem(-1, "System", "Default", 't', "None");
        for (int i = currentList.size() - 1; i > -1; i--) {
            if (currentList.get(i).getItemIndex() == message.getItemIndex()) {
                oldItem = currentList.get(i);
                break;
            }
        }
        if (oldItem.getItemIndex() == -1 || 
        !message.getUserName().equals(oldItem.getUserName()) || 
        ((String) message.getContent()).equals((String) oldItem.getContent())) {
            return;
        }
        currentList.set(currentList.indexOf(oldItem), message);
    }

    public void delete(ChatItem message) {
        ArrayList<ChatItem> currentList = messageLists.get(message.getChannel());
        int itemIndex = -1;
        for (int i = currentList.size() - 1; i > -1; i--) {
            if (currentList.get(i).getItemIndex() == message.getItemIndex()) {
                itemIndex = i;
                break;
            }
        }
        currentList.remove(itemIndex);
    }
    
    private void addToList(ChatItem item) {
        messageLists.putIfAbsent(item.getChannel(), new ArrayList<>());
        messageLists.get(item.getChannel()).add(item);
        Collections.sort(messageLists.get(item.getChannel()));
    }

    public int getNextIndex(String channel) {
        currentItemIndexes.putIfAbsent(channel, -1);
        int currentItemIndex = currentItemIndexes.get(channel);
        return currentItemIndexes.put(channel, ++currentItemIndex);
    }

    public ArrayList<ChatItem> getRecents() {
        ArrayList<ChatItem> temp = new ArrayList<>();
        Object[] keys = messageLists.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            ArrayList<ChatItem> log = messageLists.get(keys[i]);
            int arrLength = Math.min(recentsSize, log.size());
            for (int j = log.size(); j > log.size() - arrLength; j--) {
                temp.add(log.get(j - 1));
            }
        }
        return temp;
    }

    public ArrayList<ChatItem> getRecents(String channel, int oldestMessage) {
        if (oldestMessage <= 0) {
            return new ArrayList<>();
        }
        ArrayList<ChatItem> log = messageLists.get(channel);
        if (log == null) {
            return new ArrayList<>();
        }
        ArrayList<ChatItem> temp = new ArrayList<>();
        int lowerRange = oldestMessage - recentsSize;
        int lastIndex = oldestMessage;
        for (int i = log.size() - 1; i >= 0; i--) {
            int itemIndex = log.get(i).getItemIndex();
            if (itemIndex < oldestMessage) {
                temp.add(log.get(i));
                if (lastIndex - itemIndex > 1) {
                    lowerRange -= (lastIndex - itemIndex - 1);
                }
            }
            if (itemIndex <= lowerRange) {
                break;
            }
            lastIndex = itemIndex;
        }
        Collections.reverse(temp);
        return temp;
    }

    public void removeChannel(String channel) {
        messageLists.remove(channel);
    }
}
