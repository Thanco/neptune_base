// Copywrite Terry Hancock 2023
package host.thanco.NeptuneBase;

import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class ChatItemCell {

    @FXML
    private HBox hBox;

    @FXML
    private Label userNameLbl, messageLbl;

    @FXML
    private ImageView imgView;

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
                imgView.setImageBytes(img);
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