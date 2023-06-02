// Copyright Terry Hancock 2023
package host.thanco.Configurator;

import java.util.*;

import host.thanco.NeptuneBase.*;

public class Configurator {
    private static BaseSQLDatabase sqlDatabase;
    private static BaseArrayList arrayDatabase;

    public static void main(String[] args) {
        transferArrayListToSQL();
    }

    private static void initDatabases() {
        arrayDatabase = BaseArrayList.getInstance();
        sqlDatabase = BaseSQLDatabase.getInstance();
    }

    private static void transferArrayListToSQL() {
        initDatabases();
        long time1 = System.currentTimeMillis();
        for (String channel : arrayDatabase.getLists().keySet()) {
            ArrayList<ChatItem> items = arrayDatabase.getLists().get(channel);
            for (ChatItem item : items) {
                time1 = System.currentTimeMillis();
                sqlDatabase.store(item);
                System.out.println(System.currentTimeMillis() - time1);
            }
        }
    }
}