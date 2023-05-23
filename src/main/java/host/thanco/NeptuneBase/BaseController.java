// Copyright Terry Hancock 2023
package host.thanco.NeptuneBase;

import java.util.ArrayList;
import java.util.Hashtable;
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

    private static final String VERSION = "0.9.1.0";

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

    // private static final Gson GSON = new Gson();

    private static BaseDatabase database;
    private static BaseUI ui;
    private static UserHandler userHandler;
    private static SocketIOServer server;
    private static SignalingServer signalingServer;
    private static Hashtable<SocketIOClient, String> clientTypes;
    // private static EncryptionHandler encryptionHandler;

    public static void launch(String[] args) {

        database = BaseDatabase.getInstance();
        ui = new BaseCLI();
        // encryptionHandler = new EncryptionHandler();
        userHandler = UserHandler.getInstance();
        clientTypes = new Hashtable<>();

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
        stopServer();
    }

    private static void stopServer() {
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
        signalingServer = new SignalingServer();
        addServerListeners();

        server.start();

    }

    /**
     * Adds event listeners for events from clients
     */
    private static void addServerListeners() {
        // server.addEventListener("publicKey", String.class, (client, publicKeyElements, ackRequest) -> {
        //     String encryptedSessionKey = encryptionHandler.generateNewClientEncryption(client, publicKeyElements);
        //     // encryptionHandler.sendToClient(client, "sessionKey", encryptedSessionKey);
        //     client.sendEvent("sessionKey", encryptedSessionKey);
        // });

        // server.addConnectListener(client -> chatClientConnected(client));
        server.addDisconnectListener(client -> {
            // encryptionHandler.clientDisconnect(client);
            switch (clientTypes.get(client)) {
                case "chatClient":
                    System.out.println("Chat Client leave");
                    chatClientDisconnected(client);
                    break;
                case "vcClient":
                    System.out.println("VC Client leave");
                    signalingServer.onDisconnect(client);
                    break;
            }
        });
        server.addEventListener("chatClient", String.class, (client, message, ackRequest) -> chatClientConnected(client));
        server.addEventListener(CHAT_MESSAGE, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
            try {
                // String decryptedItemJson = encryptionHandler.AESDecryptData(client, itemJson);
                // ChatItem item = ChatItem.fromJson(decryptedItemJson);
                ChatItem item = ChatItem.fromJson(itemJson);
                item.setItemIndex(database.getNextIndex(item.getChannel()));
                item.trim();
                database.store(item);
                ui.printMessage(item);

                // sendToClients(CHAT_MESSAGE, GSON.toJson(item));
                server.getBroadcastOperations().sendEvent(CHAT_MESSAGE, item);
            } catch (Exception e) {
                ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
            }
        });
        server.addEventListener(USERNAME_SET, String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String userName, AckRequest ackRequest) throws Exception {
                String localUsername = userHandler.getClientUsername(client);
                if (localUsername != null) {
                    ui.printMessage(new ChatItem(-1, "System", "none", 't', localUsername + " Changed userName to " + userName));
                    // sendToClients(USER_LEAVE, localUsername);
                    server.getBroadcastOperations().sendEvent(USER_LEAVE, localUsername);
                    userHandler.removeClient(client);
                }
                if (!userHandler.addClient(client, userName)) {
                    client.disconnect();
                }
                // encryptionHandler.sendToClient(client, USERNAME_SEND, userName);
                client.sendEvent(USERNAME_SEND, userName);
                // sendToClients(USER_JOIN, userName);
                server.getBroadcastOperations().sendEvent(USER_JOIN, userName);
                ui.clientConnected(userName, client.getSessionId().toString());
            }
        });
        server.addEventListener(IMAGE, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
            try {
                ChatItem item = ChatItem.fromJson(itemJson);
                byte[] bytes = new Gson().fromJson(item.getContent().toString(), byte[].class);
                ChatItem newImage = new ChatItem(database.getNextIndex(item.getChannel()), item.getUserName(), item.getChannel(), 'i', bytes);
                database.store(newImage);
                newImage.setContent(ImageHandler.getImageBytes(newImage));
                System.out.println("newImage");
                // sendToClients(IMAGE, GSON.toJson(newImage), true);
                server.getBroadcastOperations().sendEvent(IMAGE, newImage, new BroadcastAckCallback<>(Character.class, 30) {
                    protected void onAllSuccess() {
                        System.out.println("All clients successfully recieved image");
                    };
                    protected void onClientSuccess(SocketIOClient client, Character result) {
                        System.out.println(userHandler.getClientUsername(client) + " recived image.");
                    };
                    protected void onClientTimeout(SocketIOClient client) {
                        ui.printMessage(new ChatItem(-1, "System", "none", 't', userHandler.getClientUsername(client) + " Failed to AckImage, resending..."));
                        client.sendEvent(IMAGE, new AckCallback<>(Character.class, 30) {
                            public void onSuccess(Character arg0) {
                                ui.printMessage(new ChatItem(-1, "System", "none", 't', userHandler.getClientUsername(client) + "recived image resend."));
                            };
                            public void onTimeout() {
                                ui.printMessage(new ChatItem(-1, "System", "none", 't', userHandler.getClientUsername(client) + "failed to ack image resend."));
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
                // sendToClients(USER_TYPING, GSON.toJson(item));
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
                database.edit(newItem);
                // sendToClients(EDIT_MESSAGE, GSON.toJson(newItem));
                server.getBroadcastOperations().sendEvent(EDIT_MESSAGE, newItem);
            } catch (Exception e) {
                ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
            }
        });
        server.addEventListener(DELETE_MESSAGE, String.class, (SocketIOClient client, String itemJson, AckRequest ackRequest) -> {
            try {
                ChatItem deleteItem = ChatItem.fromJson(itemJson);
                database.delete(deleteItem);
                // sendToClients(DELETE_MESSAGE, GSON.toJson(deleteItem));
                server.getBroadcastOperations().sendEvent(DELETE_MESSAGE, deleteItem);
            } catch (Exception e) {
                ui.printMessage(new ChatItem(-1, "System", "Log", 't', e.getMessage()));
            }
        });
    
        server.addEventListener("vcClient", String.class, (client, message, ackRequest) -> {
            clientTypes.put(client, "vcClient");
            signalingServer.onConnect(client);
        });
        server.addEventListener("offer", String.class, signalingServer.onOffer());
        server.addEventListener("answer", String.class, signalingServer.onAnswer());
        server.addEventListener("candidate", String.class, signalingServer.onCandidate());
    }

    /**
     * 
     * 
     * @param client the client that has connected
     */
    private static void chatClientConnected(SocketIOClient client) {
        System.out.println("Chat Client join");
        clientTypes.put(client, "chatClient");
        ui.clientConnected("newUser", client.getSessionId().toString());
        // encryptionHandler.sendToClient(client, USER_LIST_SEND, GSON.toJson(userHandler.getCurrentUsers()));
        client.sendEvent(USER_LIST_SEND, userHandler.getCurrentUsers());
        backlogFill(client);
    }

    private static void chatClientDisconnected(SocketIOClient client) {
        String username = userHandler.getClientUsername(client);
        ui.clientDisconnected(username);
        if (!username.equals("null")) {
            // sendToClients(USER_LEAVE, username);
            server.getBroadcastOperations().sendEvent(USER_LEAVE, username);
        }
        userHandler.removeClient(client);
    }

    private static void backlogFill(SocketIOClient client) {
        backlogFill(client, database.getRecents());
    }

    private static void backlogFill(SocketIOClient client, ArrayList<ChatItem> messages) {
        if (messages.size() == 0) {
            return;
        }
        ArrayList<ChatItem> textChats = new ArrayList<>();
        for (ChatItem chatItem : messages) {
            switch (chatItem.getType()) {
                case 't':
                    // client.sendEvent(BACKLOG_FILL, chatItem);
                    textChats.add(chatItem);
                    break;
                case 'i':
                    try {
                        ChatItem newItem = new ChatItem(chatItem.getItemIndex(), chatItem.getUserName(), chatItem.getChannel(), chatItem.getType(), ImageHandler.getImageBytes(chatItem));
                        // encryptionHandler.sendToClient(client, BACKLOG_IMAGE, GSON.toJson(newItem), true);
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
        // encryptionHandler.sendToClient(client, BACKLOG_FILL, GSON.toJson(textChats));
        client.sendEvent(BACKLOG_FILL, textChats);
    }

    // private static void sendToClients(String type, String message) {
    //     sendToClients(type, message, false);
    // }

    // private static void sendToClients(String type, String message, boolean withAck) {
    //     SocketIOClient[] clients = new SocketIOClient[server.getAllClients().size()];
    //     server.getAllClients().toArray(clients);
    //     for (int i = 0; i < clients.length; i++) {
    //         encryptionHandler.sendToClient(clients[i], type, message, withAck);
    //     }
    // }
}
