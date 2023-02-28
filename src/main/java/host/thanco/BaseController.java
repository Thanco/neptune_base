// Copyright Terry Hancock 2023
package host.thanco;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import com.corundumstudio.socketio.AckCallback;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastAckCallback;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.listener.DataListener;
import com.google.gson.Gson;

public class BaseController {

    private static final String VERSION = "0.8.9";

    private static final String CHAT_MESSAGE = "chatMessage";
    private static final String BACKLOG_FILL = "backlogFill";
    private static final String IMAGE = "image";
    private static final String BACKLOG_IMAGE = "backlogImage";
    private static final String USERNAME_SET = "usernameSet";
    private static final String USERNAME_SEND = "usernameSend";
    private static final String USER_LIST_SEND = "userListSend";
    private static final String USER_JOIN = "userJoin";
    private static final String USER_LEAVE = "userLeave";
    private static final String USER_TYPING = "userTyping";
    private static final String MESSAGE_REQUEST = "messageRequest";
    private static final String EDIT_MESSAGE = "edit";
    private static final String DELETE_MESSAGE = "delete";

    private static BaseDatabase database;
    private static BaseUI ui;
    private static SocketIOServer server;

    public static void launch(String[] args) {

        database = BaseDatabase.getInstance();
        ui = new BaseCLI();

        String port = "80";
        if (args.length != 0) {
            port = args[0];
        }
        startServer(port);

        ui.launchUI(args);

        ui.printMessage(new ChatItem(-1, "System", "Log", 't', server.getConfiguration().getHostname() + ":" + server.getConfiguration().getPort() + " " + server.getConfiguration().getOrigin() + "  v" + VERSION));

        Scanner in = new Scanner(System.in);
        in.nextLine();
        in.close();
        server.stop();

        ui.printMessage(new ChatItem(-1, "System", "Log", 't', "Server Closed... \nExiting..."));
    }

    /**
     * Starts the Socket.IO server
     * 
     * @param port the port to start the server on
     */
    private static void startServer(String port) {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(Integer.parseInt(port));
        config.setTransports(Transport.WEBSOCKET);

        config.setMaxFramePayloadLength(50000000);

        server = new SocketIOServer(config);
        addServerListeners();

        server.start();
    }

    /**
     * Adds event listeners for events from clients
     */
    private static void addServerListeners() {
        server.addConnectListener(client -> clientConnected(client));
        server.addDisconnectListener(client -> clientDisconnected(client));
        server.addEventListener(CHAT_MESSAGE, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
                try {
                    ChatItem item = ChatItem.fromJson(itemJson);
                    item.setItemIndex(database.getNextIndex(item.getChannel()));
                    database.store(item);
                    ui.printMessage(item);
                    server.getBroadcastOperations().sendEvent(CHAT_MESSAGE, item);
                } catch (Exception e) {
                    ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
                }
        });
        server.addEventListener(USERNAME_SET, String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String message, AckRequest ackRequest) throws Exception {
                if (database.getClientUsername(client) != null) {
                    ui.printMessage(new ChatItem(-1, "System", "none", 't', database.getClientUsername(client) + " Changed userName to " + message));
                    server.getBroadcastOperations().sendEvent(USER_LEAVE, database.getClientUsername(client));
                    database.removeClient(database.getClientUsername(client));
                }
                database.addClient(client, message);
                client.sendEvent(USERNAME_SEND, message);
                server.getBroadcastOperations().sendEvent(USER_JOIN, message);
                ui.clientConnected(database.getClientUsername(client), client.getSessionId().toString());
            }
        });
        server.addEventListener(IMAGE, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
            try {
                ChatItem item = ChatItem.fromJson(itemJson);
                byte[] bytes = new Gson().fromJson(item.getContent().toString(), byte[].class);
                ChatItem newImage = new ChatItem(database.getNextIndex(item.getChannel()), database.getClientUsername(client), item.getChannel(), 'i', bytes);
                database.store(newImage);
                newImage.setContent(Files.readAllBytes(Paths.get(new File("img/" + newImage.getChannel() + newImage.getItemIndex() + ".jpg").toURI())));
                System.out.println("newImage");
                server.getBroadcastOperations().sendEvent(IMAGE, newImage, new BroadcastAckCallback<>(Character.class, 30) {
                    protected void onAllSuccess() {
                        System.out.println("All clients successfully recieved image");
                    };
                    protected void onClientSuccess(SocketIOClient client, Character result) {
                        System.out.println(client.getSessionId() + " recived image.");
                    };
                    protected void onClientTimeout(SocketIOClient client) {
                        ui.printMessage(new ChatItem(-1, "System", "none", 't', database.getClientUsername(client) + " Failed to AckImage, resending..."));
                        client.sendEvent(IMAGE, new AckCallback<>(Character.class, 30) {
                            public void onSuccess(Character arg0) {
                                ui.printMessage(new ChatItem(-1, "System", "none", 't', database.getClientUsername(client) + "recived image resend."));
                            };
                            public void onTimeout() {
                                ui.printMessage(new ChatItem(-1, "System", "none", 't', database.getClientUsername(client) + "failed to ack image resend."));
                            };
                        }, newImage);
                    };
                });
            } catch (Exception e) {
                ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
            }
        });
        server.addEventListener(USER_TYPING, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
            try {
                ChatItem item = ChatItem.fromJson(itemJson);
                server.getBroadcastOperations().sendEvent(USER_TYPING, item);
            } catch (Exception e) {
                ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
            }
        });
        server.addEventListener(MESSAGE_REQUEST, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
            try {
                ChatItem item = ChatItem.fromJson(itemJson);
                backlogFill(client, database.getRecents(item.getChannel(), item.getItemIndex()));
            } catch (Exception e) {
                ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
            }
        });
        server.addEventListener(EDIT_MESSAGE, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
            try {
                ChatItem newItem = ChatItem.fromJson(itemJson);
                ArrayList<ChatItem> currentList = database.getMessageLists().get(newItem.getChannel());
                ChatItem oldItem = new ChatItem(-1, "System", "Default", 't', "None");
                for (int i = currentList.size() - 1; i > -1; i--) {
                    if (currentList.get(i).getItemIndex() == newItem.getItemIndex()) {
                        oldItem = currentList.get(i);
                        break;
                    }
                }
                if (oldItem.getItemIndex() == -1 || 
                !newItem.getUserName().equals(oldItem.getUserName()) || 
                ((String) newItem.getContent()).equals((String) oldItem.getContent())) {
                    return;
                }
                currentList.set(currentList.indexOf(oldItem), newItem);
                server.getBroadcastOperations().sendEvent(EDIT_MESSAGE, newItem);
            } catch (Exception e) {
                ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
            }
        });
        server.addEventListener(DELETE_MESSAGE, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
            try {
                ChatItem deleteItem = ChatItem.fromJson(itemJson);
                ArrayList<ChatItem> currentList = database.getMessageLists().get(deleteItem.getChannel());
                int itemIndex = -1;
                for (int i = currentList.size() - 1; i > -1; i--) {
                    if (currentList.get(i).getItemIndex() == deleteItem.getItemIndex()) {
                        itemIndex = i;
                        break;
                    }
                }
                currentList.remove(itemIndex);
                server.getBroadcastOperations().sendEvent(DELETE_MESSAGE, deleteItem);
            } catch (Exception e) {
                ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
            }
        });
    }

    /**
     * 
     * 
     * @param client the client that has connected
     */
    private static void clientConnected(SocketIOClient client) {
        ui.clientConnected("newUser", client.getSessionId().toString());
        client.sendEvent(USER_LIST_SEND, database.getCurrentUsers());
        backlogFill(client);
    }

    private static void clientDisconnected(SocketIOClient client) {
        String username = database.getClientUsername(client);
        if (username.equals("null")) {
            ui.clientDisconnected(username);
            database.removeClient(username);
            return;
        }
        ui.clientDisconnected(username);
        server.getBroadcastOperations().sendEvent(USER_LEAVE, username);
        database.removeClient(username);
    }

    private static void backlogFill(SocketIOClient client) {
        backlogFill(client, database.getRecents());
    }

    private static void backlogFill(SocketIOClient client, ArrayList<ChatItem> messages) {
        for (ChatItem chatItem : messages) {
            switch (chatItem.getType()) {
                case 't':
                    client.sendEvent(BACKLOG_FILL, chatItem);
                    break;
                case 'i':
                    try {
                        String imgPath = (String) chatItem.getContent();
                        File newFile = new File(imgPath);
                        byte[] bytes = Files.readAllBytes(newFile.toPath());
                        ChatItem newItem = new ChatItem(chatItem.getItemIndex(), chatItem.getUserName(), chatItem.getChannel(), chatItem.getType(), bytes);
                        client.sendEvent(BACKLOG_IMAGE, new AckCallback<>(Character.class, 30) {
                            public void onSuccess(Character arg0) {
                                System.out.println("New client successfully recieved image");
                            };
                        }, newItem);
                    } catch (Exception e) {
                        ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
