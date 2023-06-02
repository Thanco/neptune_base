// Copyright Terry Hancock 2023
package host.thanco.NeptuneBase;

import java.sql.*;
import java.util.*;

import host.thanco.Configurator.Configuration;

public class BaseSQLDatabase implements BaseDatabase {
    private static BaseSQLDatabase instance;

    private int recentsSize;
    private HashMap<String, String> tableIDs;

    private String address;
    private String dbName;
    private String userName;
    private String password;

    private BaseSQLDatabase() {
        loadConfiguration();
        initDatabase();
    }

    public static BaseSQLDatabase getInstance() {
        if (instance == null) {
            instance = new BaseSQLDatabase();
        }
        return instance;
    }

    private Connection openConnection() throws SQLException {
        if (!address.endsWith("/")) {
            address += "/";
        }
        return DriverManager.getConnection("jdbc:" + address + dbName, userName, password);
    }

    public void initDatabase() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:" + address, userName, password);
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SHOW DATABASES LIKE '" + dbName + "'");
            if (!rs.next()) {
                s.execute("CREATE DATABASE " + dbName);
            }
            s.close();
            rs.close();
            connection.close();
            connection = openConnection();
            s = connection.createStatement();
            rs = s.executeQuery("SELECT EXISTS (SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + dbName + "' AND TABLE_NAME='tableIDs')");
            rs.next();
            if (!rs.getBoolean(1)) {
                s.execute("CREATE TABLE tableIDs (channelName VARCHAR(20),id CHAR(32),PRIMARY KEY (channelName,id));");
                connection.close();
                return;
            }
            rs = s.executeQuery("SELECT * FROM tableIDs");
            initTableIDsMap(rs);

            rs.close();
            s.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTableIDsMap(ResultSet tables) throws SQLException {
        tableIDs = new HashMap<>();
        while (tables.next()) {
            String channelName = tables.getString("channelName");
            String id = tables.getString("id");
            tableIDs.put(channelName, id);
        }
    }

    private void loadConfiguration() {
        Configuration configuration = new ConfigurationHandler().getConfiguration();
        recentsSize = configuration.getRecentsSize();
        address = configuration.getSqlDatabaseAddress();
        dbName = configuration.getSqlDatabaseName();
        userName = configuration.getSqlDatabaseUsername();
        password = configuration.getSqlDatabasePassword();
    }

    public void store(ChatItem message) {
        if (message.getType() == 'i' && !(message.getContent() instanceof String)) {
            try {
                String imageLocation = "img/" + message.getChannel() + message.getItemIndex() + ".jpg";
                ImageHandler.saveImage(imageLocation, (byte[]) message.getContent());
                message.setContent(imageLocation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // message.setContent(((String) message.getContent()).replace("'", "\'"));
        addToTable(message);
    }

    private void addToTable(ChatItem item) {
        try (Connection connection = openConnection();
             Statement s = connection.createStatement()) {
            if (!tableExistsByName(item.getChannel())) {
                createChatTable(connection, item.getChannel());
            }
            // item.setContent(formatForSQL((String) item.getContent()));
            String channel = getChannelID(item.getChannel());
            s.execute("INSERT INTO " + channel + " (userName,type,content) VALUE(" + item.toSQLString() + ")");
            // item.setContent(unformatForSQL((String) item.getContent()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String formatForSQL(String input) {
        return input.replace("\\", "\\\\").replace("'", "\\'");
    }

    public void edit(ChatItem message) {
        try (Connection connection = openConnection();
             Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM " + getChannelID(message.getChannel()) + " WHERE itemIndex=" + message.getItemIndex());
            rs.next();
            ChatItem oldItem = resultSetToChatItem(rs, message.getChannel());
            if (oldItem.getItemIndex() == -1 || 
            !message.getUserName().equals(oldItem.getUserName()) || 
            ((String) message.getContent()).equals((String) oldItem.getContent())) {
                return;
            }
            ((String) message.getContent()).replace("'", "'''");
            String channelID = getChannelID(message.getChannel());
            s.execute("REPLACE " + channelID + " (itemIndex,userName,type,content) VALUES(" + message.getItemIndex() + "," + message.toSQLString() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(ChatItem message) {
        try (Connection connection = openConnection();
             Statement s = connection.createStatement()) {
        s.execute("DELETE FROM " + getChannelID(message.getChannel()) + " WHERE itemIndex=" + message.getItemIndex());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean tableExistsByName(String tableName) {
        return tableIDs.containsKey(tableName);
        // try (Statement s = connection.createStatement()) {
        //     ResultSet rs = s.executeQuery("SELECT EXISTS (SELECT * FROM tableIDs WHERE channelName= '" + tableName + "');");
        //     rs.next();
        //     boolean tableExists = rs.getInt(1) == 1;
        //     s.close();
        //     return tableExists;
        // } catch (Exception e) {
        //     e.printStackTrace();
        //     return false;
        // }
    }

    // private boolean tableExistsByID(String channelID) {
    //     return tableIDs.containsValue(channelID);
        // try (Statement s = connection.createStatement()) {
        //     ResultSet rs = s.executeQuery("SELECT EXISTS (SELECT * FROM tableIDs WHERE id= '" + channelID + "');");
        //     rs.next();
        //     boolean tableExists = rs.getInt(1) == 1;
        //     return tableExists;
        // } catch (Exception e) {
        //     e.printStackTrace();
        //     return false;
        // }
    // }

    private void createChatTable(Connection connection, String channel) {
        try (Statement s = connection.createStatement()) {
            UUID tableUUID = UUID.randomUUID();
            String channelID = uuidDeform(tableUUID);
            s.execute("CREATE TABLE " + channelID + " (itemIndex INT AUTO_INCREMENT, userName VARCHAR(20), type ENUM('t','i'), content TEXT(65500), PRIMARY KEY (itemIndex));");
            s.execute("INSERT INTO tableIDs (channelName,id) VALUES('" + channel + "','" + channelID + "')");
            tableIDs.put(channel, channelID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private String getChannelName(String channelID) {
    //     String channel = "";
    //     tableIDs.entrySet().forEach((entry) -> {
    //         if (entry.getValue().equals(channelID)) {
    //             channel.concat(entry.getKey());
    //         }
    //     });
    //     return channel;
    //     // try (Statement s = connection.createStatement()) {
    //     //     ResultSet rs = s.executeQuery("SELECT channelName FROM tableIDs WHERE id='" + channelID + "'");
    //     //     rs.next();
    //     //     return rs.getString(1);
    //     // } catch (Exception e) {
    //     //     e.printStackTrace();
    //     //     return "Default";
    //     // }
    // }

    private String getChannelID(String channel) {
        return tableIDs.get(channel);
        // try (Statement s = connection.createStatement()) {
        //     ResultSet rs = s.executeQuery("SELECT id FROM tableIDs WHERE channelName='" + channel + "'");
        //     rs.next();
        //     String id = rs.getString(1);
            
        //     return id;
        // } catch (Exception e) {
        //     e.printStackTrace();
        //     return null;
        // }
    }

    private String uuidDeform(UUID uuid) {
        return uuid.toString().replace("-","");
    }

    // private UUID uuidReform(String uuid) {
    //     String reformedUUID = uuid.substring(0, 8) +
    //         "-" + uuid.substring(8, 12) +
    //         "-" + uuid.substring(12, 16) +
    //         "-" + uuid.substring(16, 20) +
    //         "-" + uuid.substring(20, 32);
    //     return UUID.fromString(reformedUUID);
    // }

    private ChatItem resultSetToChatItem(ResultSet resultSet, String channel) {
        try {
            int itemIndex = resultSet.getInt(1);
            String userName = resultSet.getString(2);
            char type = resultSet.getString(3).toCharArray()[0];
            String content = resultSet.getString(4);
            return new ChatItem(itemIndex, userName, channel, type, content);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<ChatItem> resultSetToChatItemList(ResultSet resultSet, String channel) {
        try {
            ArrayList<ChatItem> items = new ArrayList<>();
            while (resultSet.next()) {
                items.add(resultSetToChatItem(resultSet, channel));
            }
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ArrayList<ChatItem> getRecents() {
        ArrayList<ChatItem> recents = new ArrayList<>();
        try (Connection connection = openConnection();
             Statement s = connection.createStatement()) {
            // ResultSet rs = s.executeQuery("SELECT * FROM tableIDs");
            // Hashtable<String, String> tables = new Hashtable<>();
            // while (rs.next()) {
            //     String table = rs.getString(1);
            //     String id = rs.getString(2);
            //     tables.put(id, table);
            // }
            // String selectQuery = "";
            for (String channel : tableIDs.keySet()) {
                // if (!tableExistsByID(connection, tableID)) {
                //     continue;
                // }
                int lastIndex = getNextIndex(channel);
                // ResultSet rs = s.executeQuery("SELECT MAX(itemIndex) FROM " + getChannelID(channel));
                // rs.next();
                // int lastIndex = rs.getInt(1);
                // String indexes = "(";
                // for (int i = lastIndex; i > lastIndex - RECENTS_SIZE; i--) {
                //     indexes += i + ",";
                // }
                // indexes = indexes.substring(0, indexes.length() - 1);
                // indexes += ")";
                // selectQuery += "SELECT * FROM " + tableIDStr + " WHERE itemIndex IN " + indexes;
                // String channel = getChannelName(connection, tableID);
                // recents.addAll(resultSetToChatItemList(s.executeQuery(selectQuery), channel));
                recents.addAll(getRecentsOnConnection(connection, channel, lastIndex + 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recents;
    }

    public ArrayList<ChatItem> getRecents(String channel, int oldestMessage) {
        try (Connection connection = openConnection()) {
            ArrayList<ChatItem> items = getRecentsOnConnection(connection, channel, oldestMessage);
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private ArrayList<ChatItem> getRecentsOnConnection(Connection connection, String channel, int oldestMessage) {
        ArrayList<ChatItem> items = new ArrayList<>();
        try (Statement s = connection.createStatement()) {
            String channelID = getChannelID(channel);
            ResultSet rs = s.executeQuery("SELECT * FROM " + channelID + " WHERE " + (oldestMessage - recentsSize - 1) + "<itemIndex AND itemIndex<" + oldestMessage);
            items = resultSetToChatItemList(rs, channel);
            rs.close();
            s.close();
            while (items.size() < recentsSize && !items.isEmpty() && items.get(0).getItemIndex() > 1) {
                ArrayList<ChatItem> addItems = getRecentsOnConnection(connection, channel, oldestMessage - recentsSize);
                for (int i = items.size(), j = 0; j < addItems.size() && i <= recentsSize; i++, j++) {
                    items.add(addItems.get(j));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public int getNextIndex(String channel) {
        try (Connection connection = openConnection();
             Statement s = connection.createStatement()) {
            if (!tableExistsByName(channel)) {
                return 0;
            }
            String channelID = getChannelID(channel);
            ResultSet rs = s.executeQuery("SELECT MAX(itemIndex) FROM " + channelID);
            rs.next();
            return rs.getInt(1) + 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void saveLists() {
        System.out.println("Let hope no data is lost on exit here lol");
    }

    public void removeChannel(String channel) {
        try (Connection connection = openConnection();
             Statement s = connection.createStatement()) {
            if (!tableExistsByName(channel)) {
                return;
            }
            String channelID = getChannelID(channel);
            s.execute("DELETE FROM tableIDs WHERE id='" + channelID + "'");
            tableIDs.remove(channel);
            s.execute("DROP TABLE " + channelID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
