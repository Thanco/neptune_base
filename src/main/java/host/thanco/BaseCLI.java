package host.thanco;

public class BaseCLI implements BaseUI {
    @Override
    public void launchUI(String[] args) {
        System.out.println("Neptune Base CLI successfully started...");
    }

    @Override
    public void clientConnected(String userName, String id) {
        System.out.println(userName + " connected on " + id);
    }

    @Override
    public void clientDisconnected(String userName) {
        System.out.println(userName + " disconnected!");
    }

    @Override
    public void printMessage(ChatItem message) {
        System.out.println(message);
    }
}
