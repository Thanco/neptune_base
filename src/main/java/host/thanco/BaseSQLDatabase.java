// Copyright Terry Hancock 2023
package host.thanco;

import java.sql.*;
import java.util.*;

public class BaseSQLDatabase implements BaseDatabase {
    private static final int RECENTS_SIZE = 15;

    private static BaseDatabase instance;
    private Connection connection;

    private BaseSQLDatabase() {
        initDatabase();
    }

    public static BaseDatabase getInstance() {
        if (instance == null) {
            instance = new BaseSQLDatabase();
        }
        return instance;
    }

    public void initDatabase() {
        try {
        connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/devTest", "Neptune4590", "4xjD2oaffanmZXU8IhdJ");
        Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery("SELECT EXISTS (SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='devTest' AND TABLE_NAME='tableIDs')");
        rs.next();
        if (!rs.getBoolean(1)) {
            s.execute("CREATE TABLE tableIDs (channelName VARCHAR(20),id CHAR(32),PRIMARY KEY (channelName,id));");
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void store(ChatItem message) {
        if (message.getType() == 'i') {
            try {
                String imageLocation = "img/" + message.getChannel() + message.getItemIndex() + ".jpg";
                ImageHandler.saveImage(imageLocation, (byte[]) message.getContent());
                message.setContent(imageLocation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        addToTable(message);
    }

    private void addToTable(ChatItem item) {
        try {
            Statement s = connection.createStatement();
            if (!tableExists(item.getChannel())) {
                createChatTable(item.getChannel());
            }
            
            item.setContent(((String) item.getContent()).replace("'", "\\'"));
            UUID channelUUID = getChannelUUID(item.getChannel());
            s.execute("INSERT INTO " + uuidDeform(channelUUID) + " (userName,type,content) VALUES(" + item.toSQLString() + ")");
            item.setContent(((String) item.getContent()).replace("\\'", "'"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void edit(ChatItem message) {
        try {
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM " + uuidDeform(getChannelUUID(message.getChannel())) + " WHERE itemIndex=" + message.getItemIndex());
            ChatItem oldItem = resultSetToChatItem(rs, message.getChannel()).get(0);
            if (oldItem.getItemIndex() == -1 || 
            !message.getUserName().equals(oldItem.getUserName()) || 
            ((String) message.getContent()).equals((String) oldItem.getContent())) {
                return;
            }
            ((String) message.getContent()).replace("'", "'''");
            UUID channelUUID = getChannelUUID(message.getChannel());
            s.execute("REPLACE " + uuidDeform(channelUUID) + " (itemIndex,userName,type,content) VALUES(" + message.getItemIndex() + "," + message.toSQLString() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(ChatItem message) {
        try {
            Statement s = connection.createStatement();
            s.execute("DELETE FROM " + uuidDeform(getChannelUUID(message.getChannel())) + " WHERE itemIndex=" + message.getItemIndex());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean tableExists(String tableName) {
        try {
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT EXISTS (SELECT * FROM tableIDs WHERE channelName= '" + tableName + "');");
            rs.next();
            boolean tableExists = rs.getInt(1) == 1;
            s.close();
            return tableExists;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean tableExists(UUID channelUUID) {
        try {
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT EXISTS (SELECT * FROM tableIDs WHERE id= '" + uuidDeform(channelUUID) + "');");
            rs.next();
            boolean tableExists = rs.getInt(1) == 1;
            s.close();
            return tableExists;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createChatTable(String channel) {
        try {
            UUID tableUUID = UUID.randomUUID();
            Statement s = connection.createStatement();
            s.execute("CREATE TABLE " + uuidDeform(tableUUID) + " (itemIndex INT AUTO_INCREMENT, userName VARCHAR(20), type ENUM('t','i'), content TEXT(65500), PRIMARY KEY (itemIndex));");
            s.execute("INSERT INTO tableIDs (channelName,id) VALUES('" + channel + "','" + uuidDeform(tableUUID) + "')");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getChannel(UUID channelUUID) {
        try {
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT channelName FROM tableIDs WHERE id='" + uuidDeform(channelUUID) + "'");
            rs.next();
            return rs.getString(1);
        } catch (Exception e) {
            e.printStackTrace();
            return "Default";
        }
    }

    private UUID getChannelUUID(String channel) {
        try {
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT id FROM tableIDs WHERE channelName='" + channel + "'");
            rs.next();
            String uuid = rs.getString(1);
            return uuidReform(uuid);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String uuidDeform(UUID uuid) {
        return uuid.toString().replace("-","");
    }

    private UUID uuidReform(String uuid) {
        String reformedUUID = uuid.substring(0, 8) +
            "-" + uuid.substring(8, 12) +
            "-" + uuid.substring(12, 16) +
            "-" + uuid.substring(16, 20) +
            "-" + uuid.substring(20, 32);
        return UUID.fromString(reformedUUID);
    }

    private ArrayList<ChatItem> resultSetToChatItem(ResultSet resultSet, String channel) {
        try {
            ArrayList<ChatItem> items = new ArrayList<>();
            while (resultSet.next()) {
                int itemIndex = resultSet.getInt(1);
                String userName = resultSet.getString(2);
                char type = resultSet.getString(3).toCharArray()[0];
                String content = resultSet.getString(4);
                ChatItem item = new ChatItem(itemIndex, userName, channel, type, content);
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ArrayList<ChatItem> getRecents() {
        ArrayList<ChatItem> recents = new ArrayList<>();
        try {
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM tableIDs");
            Hashtable<UUID, String> tables = new Hashtable<>();
            while (rs.next()) {
                String table = rs.getString(1);
                String id = rs.getString(2);
                tables.put(uuidReform(id), table);
            }
            s.close();
            s = connection.createStatement();
            for (UUID tableUUID : tables.keySet()) {
                if (!tableExists(tableUUID)) {
                    continue;
                }
                String tableIDStr = uuidDeform(tableUUID);
                rs = s.executeQuery("SELECT MAX(itemIndex) FROM " + tableIDStr);
                rs.next();
                int lastIndex = rs.getInt(1);
                String indexes = "(";
                for (int i = lastIndex; i > lastIndex - RECENTS_SIZE; i--) {
                    indexes += i + ",";
                }
                indexes = indexes.substring(0, indexes.length() - 1);
                indexes += ")";
                String selectQuery = "SELECT * FROM " + tableIDStr + " WHERE itemIndex IN " + indexes;
                String channel = getChannel(tableUUID);
                recents.addAll(resultSetToChatItem(s.executeQuery(selectQuery), channel));
            }
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recents;
    }

    public ArrayList<ChatItem> getRecents(String channel, int oldestMessage) {
        ArrayList<ChatItem> items = new ArrayList<>();
        try {
            Statement s = connection.createStatement();
            String channelIDStr = uuidDeform(getChannelUUID(channel));
            ResultSet rs = s.executeQuery("SELECT * FROM " + channelIDStr + " WHERE itemIndex>" + (oldestMessage - RECENTS_SIZE) + " AND itemIndex<" + oldestMessage);
            items = resultSetToChatItem(rs, channel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public int getNextIndex(String channel) {
        try {
            Statement s = connection.createStatement();
            if (!tableExists(channel)) {
                return 0;
            }
            String channelIDStr = uuidDeform(getChannelUUID(channel));
            ResultSet rs = s.executeQuery("SELECT MAX(itemIndex) FROM " + channelIDStr);
            rs.next();
            return rs.getInt(1) + 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void saveList() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
