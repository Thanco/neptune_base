package host.thanco.NeptuneBase;

// import org.java_websocket.WebSocket;
// import org.java_websocket.handshake.ClientHandshake;
// import org.java_websocket.server.WebSocketServer;

// import com.corundumstudio.socketio.SocketIOServer;
// import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.AckRequest;
// import com.corundumstudio.socketio.Configuration;
import com.google.gson.Gson;

// import java.net.InetSocketAddress;
import java.util.*;

public class SignalingServer {
    private final Hashtable<Integer, SocketIOClient> members = new Hashtable<>();
    // private SocketIOServer server;
    // private final ArrayList<WebSocket> members = new ArrayList<>();

    // public SignalingServer() {
        // start();
        // super(new InetSocketAddress(27415));
    // }

    // private void start() {
        // Configuration config = new Configuration();
        // config.setHostname("0.0.0.0");
        // config.setPort(27415);
        // config.setTransports(Transport.WEBSOCKET);
        // config.setMaxFramePayloadLength(50000000);
        // server = new SocketIOServer(config);

        // addServerListeners();

        // server.start();

        // System.out.println("Signal server running on " + server.getConfiguration());
    // }

    // public void stop() {
        // server.stop();
    // }

    // @Override
    // public void onStart() {
    //     System.out.println("Signal server started on " + getAddress());
    // }

    // private void addServerListeners() {
        // server.addConnectListener((client) -> {
        //     System.out.println("client connected to signal server: " + client.getRemoteAddress());
        //     System.out.println("New client entered room");
        //     System.out.println("new connection joining the previous " + members.size());
        //     int id = generateNewId();
        //     String msg = new Message("serverResponse", id, new ArrayList<Integer>(members.keySet())).toJson();
        //     System.out.println(msg);
        //     client.sendEvent("serverResponse", msg);
        //     // client.send(msg);
        //     members.put(id, client);
        //     members.forEach((clientID, clientSocket) -> {
        //         System.out.print(clientID + " on " + clientSocket.getRemoteAddress() + " with ");
        //     });
        //     System.out.println();
        // });
        // server.addDisconnectListener((client) -> {
        //     if (members.contains(client)) {
        //         Set<Integer> keys = members.keySet(); 
        //         Iterator<Integer> i = keys.iterator();
        //         while ((i.hasNext())) {
        //             int current = i.next();
        //             if (members.get(current).equals(client)) {
        //                 members.remove(current);
        //                 System.out.println("Client disconnected: " + client.getRemoteAddress());
        //                 return;
        //             }
        //         }
        //     };
        // });
        // server.addEventListener("offer", String.class, (SocketIOClient client, String offerJson, AckRequest ackRequest) -> {
        //     Message offer = new Gson().fromJson(offerJson, Message.class);
        //     System.out.println("Offer from " + offer.fromID + " to " + offer.id);
        //     send("offer", offer.id, offer.toJson());
        // });
        // server.addEventListener("answer", String.class, (SocketIOClient client, String answerJson, AckRequest ackRequest) -> {
        //     Message answer = new Gson().fromJson(answerJson, Message.class);
        //     System.out.println("Answer from " + answer.id + " to " + answer.fromID);
        //     send("answer", answer.id, answerJson);
        // });
        // server.addEventListener("candidate", String.class, (SocketIOClient client, String candidateJson, AckRequest ackRequest) -> {
        //     Message candidate = new Gson().fromJson(candidateJson, Message.class);
        //     System.out.println("Candidate from " + candidate.fromID + " to " + candidate.id);
        //     // System.out.println(messageJson);
        //     send("candidate", candidate.id, candidateJson);
        //     // sendToAll(connection, messageJson);
        // });
    // }

    public void onConnect(SocketIOClient client) { 
        System.out.println("client connected to signal server: " + client.getRemoteAddress());
        System.out.println("New client entered room");
        System.out.println("new connection joining the previous " + members.size());
        int id = generateNewId();
        String msg = new Message("serverResponse", id, new ArrayList<Integer>(members.keySet())).toJson();
        System.out.println(msg);
        client.sendEvent("serverResponse", msg);
        // client.send(msg);
        members.put(id, client);
        members.forEach((clientID, clientSocket) -> {
            System.out.print(clientID + " on " + clientSocket.getRemoteAddress() + " with ");
        });
        System.out.println();
    }

    public void onDisconnect(SocketIOClient client) {
        if (members.contains(client)) {
            Set<Integer> keys = members.keySet(); 
            Iterator<Integer> i = keys.iterator();
            while ((i.hasNext())) {
                int current = i.next();
                if (members.get(current).equals(client)) {
                    members.remove(current);
                    System.out.println("Client disconnected: " + client.getRemoteAddress());
                    return;
                }
            }
        }
    }

    public DataListener<String> onOffer() {
        return (SocketIOClient client, String offerJson, AckRequest ackRequest) -> {
        Message offer = new Gson().fromJson(offerJson, Message.class);
        System.out.println("Offer from " + offer.fromID + " to " + offer.id);
        send("offer", offer.id, offer.toJson());};
    }

    public DataListener<String> onAnswer() {
        return (SocketIOClient client, String answerJson, AckRequest ackRequest) -> {
            Message answer = new Gson().fromJson(answerJson, Message.class);
            System.out.println("Answer from " + answer.id + " to " + answer.fromID);
            send("answer", answer.id, answerJson);
        };
    }

    public DataListener<String> onCandidate() {
        return (SocketIOClient client, String candidateJson, AckRequest ackRequest) -> {
            Message candidate = new Gson().fromJson(candidateJson, Message.class);
            System.out.println("Candidate from " + candidate.fromID + " to " + candidate.id);
            send("candidate", candidate.id, candidateJson);
        };
    }

    // @Override
    // public void onOpen(WebSocket connection, ClientHandshake handshake) {
    //     System.out.println("client connected to signal server: " + connection.getRemoteSocketAddress());
    //     System.out.println("New client entered room");
    //     System.out.println("new connection joining the previous " + members.size());
    //     int id = generateNewId();
    //     String msg = new Message("serverResponse", id, new ArrayList<Integer>(members.keySet())).toJson();
    //     System.out.println(msg);
    //     connection.send(msg);
    //     members.put(id, connection);
    //     members.forEach((clientID, clientSocket) -> {
    //         System.out.print(clientID + " on " + clientSocket.getRemoteSocketAddress() + " with ");
    //     });
    //     System.out.println();
    // }

    private int generateNewId() {
        Random r = new Random(System.currentTimeMillis());
        int id = r.nextInt();
        while (members.containsKey(id)) {
            id = r.nextInt();
        }
        return id;
    }

    // @Override
    // public void onMessage(WebSocket connection, String messageJson) {
    //     System.out.println(messageJson);
    //     try {
    //         Message message = new Gson().fromJson(messageJson, Message.class);
    //         String messageType = message.type;
    //         switch (messageType) {
    //             case "offer":
    //                 System.out.println("Offer from " + connection.getRemoteSocketAddress());
    //                 send(message.id, messageJson);
    //                 break;
    //             case "answer":
    //                 System.out.println("Answer from " + connection.getRemoteSocketAddress());
    //                 send(message.id, messageJson);
    //                 break;
    //             default:
    //                 System.out.println("Candidate from " + message.fromID + " to " + message.id);
    //                 // System.out.println(messageJson);
    //                 send(message.id, messageJson);
    //                 // sendToAll(connection, messageJson);
    //                 break;
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    //     System.out.println();
    // }

    // @Override
    // public void onClose(WebSocket connection, int code, String reason, boolean remote) {
    //     if (members.contains(connection)) {
    //         Set<Integer> keys = members.keySet(); 
    //         Iterator<Integer> i = keys.iterator();
    //         while ((i.hasNext())) {
    //             int current = i.next();
    //             if (members.get(current).equals(connection)) {
    //                 members.remove(current);
    //                 System.out.println("Client disconnected: " + reason + " " + connection.getRemoteSocketAddress());
    //                 return;
    //             }
    //         }
    //     };
    // }

    // @Override
    // public void onError(WebSocket connection, Exception e) {
    //     e.printStackTrace();
    // }

    // private int generateRoomNumber() {
    //     return new Random(System.currentTimeMillis()).nextInt();
    // }

    // private void sendToAll(WebSocket connection, String message) {
    //     Set<Integer> keys = members.keySet(); 
    //     Iterator<Integer> i = keys.iterator();
    //     while ((i.hasNext())) {
    //         WebSocket socket = members.get(i.next());
    //         if (socket != connection) {
    //             socket.send(message);
    //         }
    //     }
    // }

    private void send(String type, int id, String message) {
        members.get(id).sendEvent(type, message);
    }

    @SuppressWarnings("unused")
    private class Message {
        private String type;
        private int id;
        private int fromID;
        private ArrayList<Integer> ids;
        private String sdp;
        public Message(String type, int id, ArrayList<Integer> ids) {
            this.type = type;
            this.id = id;
            this.ids = ids;
        }
        public String toJson() {
            return new Gson().toJson(this);
        }
    } 
}
