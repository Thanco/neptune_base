// Copyright Terry Hancock 2023
package host.thanco;

public interface BaseUI {
    public void launchUI(String[] args);
    public void clientConnected(String userName, String id);
    public void clientDisconnected(String userName);
    public void printMessage(ChatItem message);
}
