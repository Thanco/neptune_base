// Copyright Terry Hancock 2023
package host.thanco.NeptuneBase;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class BaseGUI extends Application implements BaseUI {

    @FXML
    private Button exitBtn;

    @FXML
    private ListView<ChatItem> messageListView;

    private ObservableList<ChatItem> observableList = FXCollections.observableArrayList();

    private static Stage stage;
    private static Scene scene;

    @Override
    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;
        scene = new Scene(loadFXML("primary"));
        stage.setScene(scene);
        stage.show();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(BaseGUI.class.getResource("/fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    @FXML
    void pressExitBtn(ActionEvent event) {
        Platform.exit();
    }
    
    @FXML
    public void initialize() {
        // observableList.addAll(BaseDatabase.getInstance().getMessageLists().get("Default"));
        messageListView.setItems(observableList);
        messageListView.setCellFactory(e -> new ChatItemViewCell());
    }

    @Override
    public void launchUI(String[] args) {
        launch(args);
    }

    @Override
    public void printMessage(ChatItem message) {
        // Platform.runLater(() -> {
        //     try {
        //         scene = new Scene(loadFXML("primary"));
        //         stage.setScene(scene);
        //     } catch (Exception e) {
        //         e.printStackTrace();
        //     }
        // });
        
        // System.out.println(message);
        // observableList.setAll(BaseDatabase.getInstance().getMessageLists().get("Default"));
        // observableList.add(message);
        // observableList.clear();
        // observableList.setAll(BaseDatabase.getInstance().getMessageLists().get("Default"));
        // messageListView.setItems(observableList);
        try {
            // init();
            // launch();
            System.out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clientConnected(String userName, String id) {
        observableList.add(new ChatItem(-1, "System", "Log",'t', userName + " Connected on " + id));
    }

    @Override
    public void clientDisconnected(String userName) {
        observableList.add(new ChatItem(-1, "System", "Log", 't', userName + " Disconnected"));
    }

}
