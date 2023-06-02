// Copyright Terry Hancock 2023
package host.thanco.NeptuneBase;

import java.util.ArrayList;

import host.thanco.Configurator.Configuration;
public interface BaseDatabase {
    public final static BaseDatabase instance = getInstance();
    public static BaseDatabase getInstance() {
        if (instance == null) {
            Configuration configuration = new ConfigurationHandler().getConfiguration();
            if (configuration.isSqlServer()) {
                return BaseSQLDatabase.getInstance();
            }
            return BaseArrayList.getInstance();
        }
        return instance;
    }
    public void initDatabase();
    public void store(ChatItem message);
    public void edit(ChatItem message);
    public void delete(ChatItem message);
    public ArrayList<ChatItem> getRecents();
    public ArrayList<ChatItem> getRecents(String channel, int oldestMessage);
    public int getNextIndex(String channel);
    public void saveLists();
    public void removeChannel(String channel);
}
