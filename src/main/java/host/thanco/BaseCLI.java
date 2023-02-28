// Copywrite Terry Hancock 2023
package host.thanco;

public class BaseCLI implements BaseUI {
    @Override
    public void launchUI(String[] args) {
        System.out.println("Neptune Base CLI successfully started...");
        // Thread exitChecker = new Thread(new Runnable() {
        //     @Override
        //     public void run() {
        //         Scanner in = new Scanner(System.in);
        //         while (!in.nextLine().toLowerCase().equals("exit")) {
        //             continue;
        //         }
        //         in.close();
        //         BaseDatabase.getInstance().saveList();
        //         System.exit(0);
        //     }
        // });
        // exitChecker.setDaemon(true);
        // exitChecker.setName("Exit Check");
        // exitChecker.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                BaseDatabase.getInstance().saveList();
            }
        }));
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
