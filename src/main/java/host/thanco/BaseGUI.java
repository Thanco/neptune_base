package host.thanco;

import java.io.IOException;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.Callback;

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
        
    }
    
    @FXML
    public void initialize() {
        observableList.setAll(BaseDatabase.getInstance().getMessageList());
        messageListView.setItems(observableList);
        messageListView.setCellFactory(new Callback<ListView<ChatItem>, javafx.scene.control.ListCell<ChatItem>>() {
            @Override
            public ListCell<ChatItem> call(ListView<ChatItem> messageListView) {
                return new ChatItemViewCell();
            }
        });
    }

    @Override
    public void launchUI(String[] args) {
        launch(args);
    }

    @Override
    public void printMessage(ChatItem message) {
        observableList.setAll(BaseDatabase.getInstance().getMessageList());
        observableList.add(message);
    }

    @Override
    public void clientConnected(String userName, String id) {
        observableList.add(new ChatItem(-1, "System", 't', userName + " Connected on " + id));
    }

    @Override
    public void clientDisconnected(String userName) {
        observableList.add(new ChatItem(-1, "System", 't', userName + " Disconnected"));
    }

}
