package host.thanco;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import com.corundumstudio.socketio.AckCallback;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastAckCallback;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;

public class BaseController {

    private static final String VERSION = "0.8.5.1";

    private static final String CHAT_MESSAGE = "chatMessage";
    private static final String IMAGE = "image";
    private static final String USERNAME_SET = "usernameSet";
    private static final String USERNAME_SEND = "usernameSend";
    private static final String USER_LIST_SEND = "userListSend";
    private static final String USER_JOIN = "userJoin";
    private static final String USER_LEAVE = "userLeave";
    private static final String USER_TYPING = "userTyping";

    private static BaseDatabase database = BaseDatabase.getInstance();
    private static BaseUI ui = new BaseCLI();
    private static SocketIOServer server;


    public static void main(String[] args) {

        Configuration config = new Configuration();
        // config.setHostname("192.168.1.129");
        // config.setHostname("192.168.137.87");
        // config.setHostname("172.20.4.20");
        // config.setHostname("10.144.119.73");
        // config.setHostname("10.144.8.225");
        config.setHostname("localhost");
        config.setPort(50000);
        if (args.length != 0) {
            config.setPort(Integer.parseInt(args[0]));
        }
        // config.setTransports(Transport.WEBSOCKET);
        // config.setOrigin("http://173.93.225.199:50000/");

        // config.setMaxHttpContentLength(50000000);
        config.setMaxFramePayloadLength(50000000);
        // config.setPingInterval(25);
        // config.setPingTimeout(60);

        // config.setKeyStorePassword("");
        // InputStream stream = BaseController.class.getResourceAsStream("neptuneKeystore.jks");
        // try {
        //     InputStream in = new FileInputStream("neptuneKeystore.jks");
        //     config.setKeyStore(in);
        // } catch (IOException e1) {
        //     e1.printStackTrace();
        // }


        server = new SocketIOServer(config);
        server.addConnectListener(client -> clientConnected(client));
        server.addDisconnectListener(client -> clientDisconnected(client));
        server.addEventListener(CHAT_MESSAGE, String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String message, AckRequest ackRequest) throws Exception {
                ChatItem newItem = new ChatItem(database.getNextIndex(), database.getClientUsername(client), 't', message);
                database.store(newItem);
                server.getBroadcastOperations().sendEvent(CHAT_MESSAGE, newItem);
                ui.printMessage(newItem);
            }
        });
        server.addEventListener(USERNAME_SET, String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String message, AckRequest ackRequest) throws Exception {
                if (database.getClientUsername(client) != null) {
                    ui.printMessage(new ChatItem(-1, "System", 't', database.getClientUsername(client) + " Changed userName to " + message));
                    server.getBroadcastOperations().sendEvent(USER_LEAVE, database.getClientUsername(client));
                    database.removeClient(database.getClientUsername(client));
                }
                database.addClient(client, message);
                client.sendEvent(USERNAME_SEND, message);
                server.getBroadcastOperations().sendEvent(USER_JOIN, message);
                ui.clientConnected(database.getClientUsername(client), client.getSessionId().toString());
            }
        });
        server.addEventListener(IMAGE, byte[].class, new DataListener<byte[]>() {
            @Override
            public void onData(SocketIOClient client, byte[] bytes, AckRequest ackRequest) throws Exception {
                ChatItem newImage = new ChatItem(database.getNextIndex(), database.getClientUsername(client), 'i', bytes);
                database.store(newImage);
                System.out.println("newImage");
                server.getBroadcastOperations().sendEvent(IMAGE, newImage, new BroadcastAckCallback<>(Character.class, 30) {
                    protected void onAllSuccess() {
                        System.out.println("All clients successfully recieved image");
                    };
                    protected void onClientSuccess(SocketIOClient client, Character result) {
                        System.out.println(client.getSessionId() + " recived image.");
                    };
                    protected void onClientTimeout(SocketIOClient client) {
                        ui.printMessage(new ChatItem(-1, "System", 't', database.getClientUsername(client) + " Failed to AckImage, resending..."));
                        client.sendEvent(IMAGE, new AckCallback<>(Character.class, 30) {
                            public void onSuccess(Character arg0) {
                                ui.printMessage(new ChatItem(-1, "System", 't', database.getClientUsername(client) + "recived image resend."));
                            };
                            public void onTimeout() {
                                ui.printMessage(new ChatItem(-1, "System", 't', database.getClientUsername(client) + "failed to ack image resend."));
                            };
                        }, newImage);
                    };
                });
            }
        });
        server.addEventListener(USER_TYPING, String.class, new DataListener<String>() {
            public void onData(SocketIOClient client, String userName, AckRequest ackRequest) throws Exception {
                server.getBroadcastOperations().sendEvent(USER_TYPING, database.getClientUsername(client));
            };
        });

        server.start();

        ui.launchUI(args);

        ui.printMessage(new ChatItem(-1, "System", 't', server.getConfiguration().getHostname() + ":" + server.getConfiguration().getPort() + "  v" + VERSION));

        // while (true) {
        //     try {
        //         Thread.sleep(Integer.MAX_VALUE);
        //     } catch (InterruptedException e) {
        //         e.printStackTrace();
        //     }
        // }
    }

    private static void clientConnected(SocketIOClient client) {
        ui.clientConnected("newUser", client.getSessionId().toString());
        client.sendEvent(USER_LIST_SEND, database.getCurrentUsers());
        for (ChatItem chatItem : database.getRecents()) {
            switch (chatItem.getType()) {
                case 't':
                client.sendEvent(CHAT_MESSAGE, chatItem);
                    break;
                case 'i':
                    try {
                        File newFile = new File("img/" + chatItem.getItemIndex() + ".png");
                        byte[] bytes = Files.readAllBytes(newFile.toPath());
                        chatItem.setContent(bytes);
                        client.sendEvent(IMAGE, new AckCallback<>(Character.class, 30) {
                            public void onSuccess(Character arg0) {
                                System.out.println("New client successfully recieved image");
                            };
                        }, chatItem);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static void clientDisconnected(SocketIOClient client) {
        ui.clientDisconnected(database.getClientUsername(client));
        server.getBroadcastOperations().sendEvent(USER_LEAVE, database.getClientUsername(client));
        database.removeClient(database.getClientUsername(client));
    }
}
