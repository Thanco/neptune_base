package host.thanco;

import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class ChatItemCell {

    @FXML
    private Label userNameLbl;

    @FXML
    private Label messageLbl;

    @FXML
    private ImageView imgView;

    @FXML
    private HBox hBox;

    public ChatItemCell() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/chatItemCell.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDetails(ChatItem chatItem) {
        userNameLbl.setText(chatItem.getUserName() + ":");
        switch (chatItem.getType()) {
            case 't':
                messageLbl.setText((String)chatItem.getContent());
                break;
            case 'i':
                File imgFile = new File((String)chatItem.getContent());
                Image img = new Image(imgFile.toURI().toString());
                imgView.setImage(img);
                break;
            default:
                messageLbl.setText("idiot");
                break;
        }
    }

    public HBox getHBox() {
        return hBox;
    }
}