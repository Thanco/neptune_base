package host.thanco.Configurator;

public class Configuration {
    private int port;
    private int recentsSize;

    private boolean gui; // cli if false
    private boolean sqlServer; // array list if false

    private String sqlDatabaseAddress;
    private String sqlDatabaseUsername;
    private String sqlDatabasePassword;
    private String sqlDatabaseName;

    public Configuration setDefaultConfiguration() {
        port = 80;
        recentsSize = 15;
        gui = false;
        sqlServer = false;
        sqlDatabaseAddress = "mysql://localhost:3306/";
        sqlDatabaseUsername = "";
        sqlDatabasePassword = "";
        sqlDatabaseName = "NeptuneBase";
        return this;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the gui
     */
    public boolean isGui() {
        return gui;
    }

    /**
     * @return the sqlServer
     */
    public boolean isSqlServer() {
        return sqlServer;
    }

    /**
     * @return the sqlDatabaseAddress
     */
    public String getSqlDatabaseAddress() {
        return sqlDatabaseAddress;
    }

    /**
     * @return the sqlDatabaseUsername
     */
    public String getSqlDatabaseUsername() {
        return sqlDatabaseUsername;
    }

    /**
     * @return the sqlDatabasePassword
     */
    public String getSqlDatabasePassword() {
        return sqlDatabasePassword;
    }

    /**
     * @return the sqlDatabaseName
     */
    public String getSqlDatabaseName() {
        return sqlDatabaseName;
    }

    /**
     * @return the recentsSize
     */
    public int getRecentsSize() {
        return recentsSize;
    }
}
